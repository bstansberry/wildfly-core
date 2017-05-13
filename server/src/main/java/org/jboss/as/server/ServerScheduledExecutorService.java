/*
Copyright 2017 Red Hat, Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */

package org.jboss.as.server;

import static java.security.AccessController.doPrivileged;

import java.security.PrivilegedAction;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.threads.JBossThreadFactory;

/**
 * Service that provides a generally sharable {@link ScheduledExecutorService}. The executor is
 * generally sharable because any s
 *
 * @author Brian Stansberry
 */
public final class ServerScheduledExecutorService implements Service<ScheduledExecutorService> {

    private final ThreadFactory threadFactory;
    private final InjectedValue<ExecutorService> executorInjector = new InjectedValue<>();
    private ExecutorImpl scheduledExecutorService;

    public static void addService(ServiceTarget serviceTarget, ThreadGroup threadGroup, String threadNamePattern) {
        final ServerScheduledExecutorService serverScheduledExecutorService = new ServerScheduledExecutorService(threadGroup, threadNamePattern);
        serviceTarget.addService(ServerService.JBOSS_SERVER_SCHEDULED_EXECUTOR, serverScheduledExecutorService)
                .addDependency(ServerService.MANAGEMENT_EXECUTOR, ExecutorService.class, serverScheduledExecutorService.executorInjector)
                .install();
    }

    private ServerScheduledExecutorService(ThreadGroup threadGroup, String namePattern) {
        this.threadFactory = doPrivileged(new PrivilegedAction<ThreadFactory>() {
            public ThreadFactory run() {
                return new JBossThreadFactory(threadGroup, Boolean.FALSE, null, namePattern, null, null);
            }
        });
    }

    @Override
    public synchronized void start(final StartContext context) throws StartException {
        ExecutorService nonSched = executorInjector.getValue();
        ScheduledThreadPoolExecutor delegate = new ScheduledThreadPoolExecutor(1, threadFactory);
        delegate.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
        this.scheduledExecutorService = new ExecutorImpl(delegate, nonSched);
    }

    @Override
    public synchronized void stop(final StopContext context) {
        Runnable r = new Runnable() {
            @Override
            public void run() {
                try {
                    scheduledExecutorService.shutdownInternal();
                } finally {
                    scheduledExecutorService = null;
                    context.complete();
                }
            }
        };
        try {
            executorInjector.getValue().execute(r);
        } catch (RejectedExecutionException e) {
            r.run();
        } finally {
            context.asynchronous();
        }
    }

    @Override
    public synchronized ScheduledExecutorService getValue() throws IllegalStateException {
        return scheduledExecutorService;
    }

    /**
     * ScheduledExecutorService that uses two different executors, one simply for scheduling, the other
     * for actual task execution. The goal here is to require a minimal sized pool for scheduling without
     * having to worry about the pool threads being tied up executing arbitrary tasks.
     */
    private static class ExecutorImpl implements ScheduledExecutorService {

        private final ScheduledExecutorService scheduledDelegate;
        private final ExecutorService nonScheduledDelegate;

        private ExecutorImpl(ScheduledExecutorService scheduledDelegate, ExecutorService nonScheduledDelegate) {
            this.scheduledDelegate = scheduledDelegate;
            this.nonScheduledDelegate = nonScheduledDelegate;
        }

        @Override
        public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
            ScheduledFuture<Future<Void>> toWrap = scheduledDelegate.schedule(wrap(command), delay, unit);
            return new ScheduledCallableFuture<>(toWrap);
        }

        @Override
        public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
            ScheduledFuture<Future<V>> toWrap = scheduledDelegate.schedule(wrap(callable), delay, unit);
            return new ScheduledCallableFuture<>(toWrap);
        }

        @Override
        public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
            WrappedRunnable wrapped = new WrappedRunnable(command);
            ScheduledFuture<?> delegate = scheduledDelegate.scheduleAtFixedRate(command, initialDelay, period, unit);
            return new ScheduledRunnableFuture(delegate, wrapped);
        }

        @Override
        public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
            WrappedRunnable wrapped = new WrappedRunnable(command);
            ScheduledFuture<?> delegate = scheduledDelegate.scheduleWithFixedDelay(wrapped, initialDelay, delay, unit);
            return new ScheduledRunnableFuture(delegate, wrapped);
        }

        @Override
        public void shutdown() {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Runnable> shutdownNow() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isShutdown() {
            return scheduledDelegate.isShutdown();
        }

        @Override
        public boolean isTerminated() {
            return scheduledDelegate.isTerminated();
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T> Future<T> submit(Callable<T> task) {
            return nonScheduledDelegate.submit(task);
        }

        @Override
        public <T> Future<T> submit(Runnable task, T result) {
            return nonScheduledDelegate.submit(task, result);
        }

        @Override
        public Future<?> submit(Runnable task) {
            return nonScheduledDelegate.submit(task);
        }

        @Override
        public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
            return nonScheduledDelegate.invokeAll(tasks);
        }

        @Override
        public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
            return nonScheduledDelegate.invokeAll(tasks, timeout, unit);
        }

        @Override
        public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
            return nonScheduledDelegate.invokeAny(tasks);
        }

        @Override
        public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            return nonScheduledDelegate.invokeAny(tasks, timeout, unit);
        }

        @Override
        public void execute(final Runnable command) {
            nonScheduledDelegate.execute(command);
        }

        private void shutdownInternal() {
            scheduledDelegate.shutdown();
        }

        private <T> Callable<Future<T>> wrap(final Callable<T> toWrap) {
            return new WrappedCallable<>(toWrap);
        }

        private Callable<Future<Void>> wrap(Runnable toWrap) {
            return new WrappedCallable<>(toCallable(toWrap));
        }

        private static Callable<Void> toCallable(Runnable runnable) {
            return () -> {
                runnable.run();
                return null;
            };
        }

        private class WrappedRunnable implements Runnable {
            private final Runnable wrapped;
            private final ReentrantLock lock = new ReentrantLock();
            private final Condition condition = lock.newCondition();
            boolean cancelled;
            Future<?> future;

            private WrappedRunnable(Runnable wrapped) {
                this.wrapped = wrapped;
            }

            @Override
            public void run() {
                lock.lock();
                try {
                    if (!cancelled) {
                        future = nonScheduledDelegate.submit(wrapped);
                        condition.signalAll();
                    }
                } finally {
                    lock.unlock();
                }

            }

            boolean cancel(boolean mayInterruptIfRunning) {
                lock.lock();
                try {
                    boolean result = future == null || future.cancel(mayInterruptIfRunning);
                    cancelled = true;
                    return result;
                } finally {
                    lock.unlock();
                }
            }

            boolean isDone() {
                lock.lock();
                try {
                    return !cancelled && future != null && future.isDone();
                } finally {
                    lock.unlock();
                }
            }

            void get() throws InterruptedException, ExecutionException {
                for (;;) {
                    if (!cancelled && future == null) {
                        lock.lock();
                        try {
                            condition.await();
                        } finally {
                            lock.unlock();
                        }
                    } else {
                        break;
                    }
                }
                if (cancelled) {
                    throw new CancellationException();
                }
                future.get();
            }

            void get(long expiryNS) throws ExecutionException, InterruptedException, TimeoutException {
                Future<?> theFuture = null;
                for (;;) {
                    if (!cancelled && (theFuture = future) == null) {
                        lock.lock();
                        try {
                            long remaining = expiryNS - System.nanoTime();
                            if (remaining > 0) {
                                condition.await(remaining, TimeUnit.NANOSECONDS);
                            } else {
                                break;
                            }
                        } finally {
                            lock.unlock();
                        }
                    } else {
                        break;
                    }
                }
                if (cancelled) {
                    throw new CancellationException();
                } else if (theFuture == null) {
                    throw new TimeoutException();
                }
                long remaining = expiryNS - System.nanoTime();
                if (remaining > 0) {
                    theFuture.get(remaining, TimeUnit.NANOSECONDS);
                } else {
                    throw new TimeoutException();
                }
            }
        }

        private class WrappedCallable<T> implements Callable<Future<T>> {
            private final Callable<T> wrapped;
            private WrappedCallable(Callable<T> wrapped) {
                this.wrapped = wrapped;
            }

            @Override
            public synchronized Future<T> call() {
                return nonScheduledDelegate.submit(wrapped);
            }
        }

        private class ScheduledCallableFuture<T> implements ScheduledFuture<T> {
            private final ScheduledFuture<Future<T>> delegate;

            private ScheduledCallableFuture(ScheduledFuture<Future<T>> delegate) {
                this.delegate = delegate;
            }

            @Override
            public long getDelay(TimeUnit unit) {
                return delegate.getDelay(unit);
            }

            @Override
            public int compareTo(Delayed o) {
                return delegate.compareTo(o);
            }

            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
                boolean result = delegate.cancel(mayInterruptIfRunning);
                if (!result) {
                    try {
                        Future<?> wrapped = delegate.get();
                        result = wrapped.cancel(mayInterruptIfRunning);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } catch (ExecutionException e) {
                        // ignore
                    }
                }
                return result;
            }

            @Override
            public boolean isCancelled() {
                return delegate.isCancelled();
            }

            @Override
            public boolean isDone() {
                boolean result = delegate.isDone();
                if (result) {
                    try {
                        result = delegate.get().isDone();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } catch (ExecutionException | CancellationException ignored) {
                        //
                    }
                    // any exception ^^^ would have been from delegate.get(),
                    // but delegate.isDone() already said true, so we fall through
                    // and go with that, as the contract of isDone() says return
                    // true if there was any failure
                }
                return result;
            }

            @Override
            public T get() throws InterruptedException, ExecutionException {
                return delegate.get().get();
            }

            @Override
            public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
                long ns = unit.toMillis(timeout);
                long now = System.nanoTime();
                Future<T> future = delegate.get(ns, TimeUnit.NANOSECONDS);
                long remaining = ns - (System.nanoTime() - now);
                if (remaining > 0) {
                    return future.get(remaining, TimeUnit.NANOSECONDS);
                }
                throw new TimeoutException();
            }
        }

        private class ScheduledRunnableFuture implements ScheduledFuture<Void> {
            private final ScheduledFuture<?> delegate;
            private final WrappedRunnable runnable;

            private ScheduledRunnableFuture(ScheduledFuture<?> delegate, WrappedRunnable runnable) {
                this.delegate = delegate;
                this.runnable = runnable;
            }

            @Override
            public long getDelay(TimeUnit unit) {
                return delegate.getDelay(unit);
            }

            @Override
            public int compareTo(Delayed o) {
                return delegate.compareTo(o);
            }

            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
                return delegate.cancel(mayInterruptIfRunning) || runnable.cancel(mayInterruptIfRunning);
            }

            @Override
            public boolean isCancelled() {
                return delegate.isCancelled();
            }

            @Override
            public boolean isDone() {
                return delegate.isDone() && runnable.isDone();
            }

            @Override
            public Void get() throws InterruptedException, ExecutionException {
                runnable.get();
                return null;
            }

            @Override
            public Void get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
                long ns = unit.toMillis(timeout);
                long now = System.nanoTime();
                delegate.get(ns, TimeUnit.NANOSECONDS);
                long remaining = ns - (System.nanoTime() - now);
                if (remaining > 0) {
                    runnable.get(remaining);
                    return null;
                }
                throw new TimeoutException();
            }
        }
    }
}
