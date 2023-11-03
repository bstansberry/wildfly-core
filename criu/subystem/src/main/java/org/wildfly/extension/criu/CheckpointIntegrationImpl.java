/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.criu;

import static org.jboss.as.controller.ControlledProcessState.State.RUNNING;
import static org.jboss.as.controller.ControlledProcessState.State.STARTING;
import static org.jboss.as.controller.ControlledProcessState.State.STOPPED;
import static org.jboss.as.controller.ControlledProcessState.State.STOPPING;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RELOAD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESUME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUSPEND;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TIMEOUT;
import static org.wildfly.common.Assert.checkNotNullParam;
import static org.wildfly.extension.criu.CRIUIntegration.getMarkerPath;
import static org.wildfly.extension.criu.Messages.ROOT_LOGGER;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.jboss.as.controller.ModelControllerClientFactory;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ProcessStateNotifier;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.server.ElapsedTime;
import org.jboss.dmr.ModelNode;
import org.jboss.threads.JBossThreadFactory;

/**
 * {@link CRIUExecutor} implementation that can be used with JVM-level CRIU implementations
 * that support the notion of "before checkpoint" and "after restore" notifications.
 */
abstract class CheckpointIntegrationImpl implements CRIUExecutor {

    /** Checkpointing strategies supported by the server kernel. */
    private final Set<CheckpointStrategy> supportedStrategies;
    /** Allows tracking of process state changes resulting from checkpoint/restore.*/
    private final ProcessStateNotifier processStateNotifier;
    /** Elapsed time tracker for the  process*/
    private final ElapsedTime elapsedTime;
    /** Shares information between threads about progress preparing for a checkpoint */
    private final AtomicReference<CountDownLatch> checkpointReference = new AtomicReference<>();
    /** Shares information between threads about progress completing server boot after a restoration */
    private final AtomicReference<CountDownLatch> restoreReference = new AtomicReference<>();
    /** Shares information between threads about progress triggering a checkpoint */
    private final AtomicReference<TriggerOperation> operationReference = new AtomicReference<>();
    private final AtomicBoolean checkpointViaSuspend = new AtomicBoolean(false);
    /** Used to execute a process reload */
    private volatile ModelControllerClientFactory clientFactory;
    /** Used to execute a process reload */
    private volatile Executor clientExecutor;
    /**
     * Strategy to use when checkpointing if the checkpoint isn't initiated by triggerCheckpoint
     * or no strategy is passed to trigger checkpoint.
     */
    private volatile CheckpointStrategy defaultCheckpointStrategy = CheckpointStrategy.RELOAD_TO_MODEL;
    /** Strategy being used by a currently executing checkpoint preparation, or null if no checkpoint is being prepared */
    private volatile CheckpointStrategy currentStrategy;

    /**
     * Creates a new CheckpointIntegrationImpl.
     * @param processStateNotifier notifier to allow tracking of process state changes resulting from checkpoint/restore.
     */
    CheckpointIntegrationImpl(final Set<CheckpointStrategy> supportedStrategies, final ProcessStateNotifier processStateNotifier,
                              final ElapsedTime elapsedTime) {
        this.supportedStrategies = EnumSet.copyOf(supportedStrategies);
        this.processStateNotifier = processStateNotifier;
        this.elapsedTime = elapsedTime;
    }

    @Override
    public void register(ModelControllerClientFactory clientFactory, Executor clientExecutor) {
        checkNotNullParam("clientFactory", clientFactory);
        checkNotNullParam("clientExecutor", clientExecutor);
        ROOT_LOGGER.debug(String.format("Registering ModelControllerClient requirements %s and %s ", clientFactory, clientExecutor));
        this.clientFactory = clientFactory;
        this.clientExecutor = clientExecutor;
    }

    @Override
    public boolean isCheckpointingSupported() {
        return true;
    }

    @Override
    public void setDefaultCheckpointStrategy(CheckpointStrategy strategy) {
        checkNotNullParam("strategy", strategy);
        validateCheckpointStrategy(strategy);
        this.defaultCheckpointStrategy = strategy;
    }

    @Override
    public void triggerCheckpoint(CheckpointStrategy strategy) {
        validateCheckpointStrategy(strategy);
        final TriggerOperation operation = new TriggerOperation(strategy, new CountDownLatch(1));
        if (operationReference.compareAndSet(null, operation)) {
            final AtomicReference<Exception> exceptionRef = new AtomicReference<>();
            Executor checkpointExecutor = Executors.newSingleThreadExecutor(
                    new JBossThreadFactory(null, Boolean.FALSE, null, "%G - checkpoint thread - %t", null, null));
            try {
                checkpointExecutor.execute(() -> {
                    try {
                        executeCheckpoint();
                    } catch (Exception e) {
                        exceptionRef.set(e);
                        operation.latch.countDown();
                    }
                });
                // Wait for the call to beforeCheckpoint()
                operation.latch.await();
                if (exceptionRef.get() != null) {
                    throw exceptionRef.get();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            throw ROOT_LOGGER.checkpointPreparationAlreadyInProgress();
        }
    }

    @Override
    public CheckpointStrategy getCurrentCheckpointStrategy() {
        return currentStrategy;
    }

    @Override
    public void readyForCheckpoint() {
        final CountDownLatch checkpointLatch = checkpointReference.getAndSet(null);
        if (checkpointLatch != null) {
            ROOT_LOGGER.readyForCheckpoint();
            final CountDownLatch restoreLatch = new CountDownLatch(1);
            if (restoreReference.compareAndSet(null, restoreLatch)) {
                checkpointLatch.countDown(); // This unblocks beforeCheckpoint
                try {
                    restoreLatch.await(); // wait until we get the afterRestore notification
                } catch (InterruptedException e) {
                    ROOT_LOGGER.interruptedAwaitingCheckpointRestoration();
                    Thread.currentThread().interrupt();
                }
            }  else {
                checkpointLatch.countDown(); // just in case
                throw ROOT_LOGGER.checkpointPreparationAlreadyInProgress();
            }
        } // else it's the usual case where a boot op has reached the checkpoint stage,
          // but the boot was not part of checkpoint-trigger reload.
    }

    /** Tell the JVM to create a checkpoint. */
    abstract void executeCheckpoint() throws Exception;

    /** Handle a notification from the JVM-level CRIU integration that a checkpoint is about to take place. */
    void beforeCheckpoint() {
        long start = System.currentTimeMillis();
        CheckpointStrategy strategy = this.defaultCheckpointStrategy;
        TriggerOperation operation = operationReference.getAndSet(null);
        if (operation != null) {
            strategy = operation.strategy;
            operation.latch.countDown();
        }
        final ModelControllerClientFactory factory = this.clientFactory;
        if (factory != null) {
            ROOT_LOGGER.beforeCheckpointBegin();
            try {
                currentStrategy = strategy;
                switch (strategy) {
                    case SUSPEND_RESUME:
                        suspendServer();
                        break;
                    case RELOAD_TO_MODEL:
                        // Track 3 process state changes from a reload op, plus the readyForCheckpoint call
                        final CountDownLatch checkpointLatch = new CountDownLatch(4);
                        if (checkpointReference.compareAndSet(null, checkpointLatch)) {
                            // Initiate a reload and wait until the kernel calls readyForCheckpoint()
                            initiateReload(checkpointLatch);
                            try {
                                checkpointLatch.await();
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                throw new RuntimeException(e); // TODO
                            }
                            recordCheckpoint(start);
                        } else {
                            throw ROOT_LOGGER.checkpointPreparationAlreadyInProgress();
                        }
                        break;
                    default:
                        throw new IllegalStateException();
                }
            } finally {
                currentStrategy = null;
            }
            long now = System.currentTimeMillis();
            ROOT_LOGGER.beforeCheckpointEnd(now - start);
        } else {
            throw ROOT_LOGGER.checkpointPreparationWithNoInitiator();
        }
    }

    private void recordCheckpoint(long start) {
        try (PrintWriter writer = new PrintWriter(new FileOutputStream(getMarkerPath().toFile()))) {
            writer.println(start);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    /** Handle a notification from the JVM-level CRIU integration that a checkpoint restoration has occurred. */
    void afterRestore() {
        ROOT_LOGGER.afterRestoreBegin();
        if (checkpointViaSuspend.getAndSet(false)) {
            restoreFromSuspend();
        } else {
            updateSystemProperties();
            restoreFromReload();
        }
        ROOT_LOGGER.afterRestoreEnd();
    }

    private void suspendServer() {
        try (ModelControllerClient mcc = clientFactory.createSuperUserClient(clientExecutor)) {
            // Suspend the server, with no timeout. This blocks until the suspend happens,
            // so there is no need for the kind of multi-threaded tracking we use for
            // checkpointing with a reload.
            ModelNode op = Util.createEmptyOperation(SUSPEND, PathAddress.EMPTY_ADDRESS);
            op.get(TIMEOUT).set(-1);
            ModelNode response = mcc.execute(op);
            if (SUCCESS.equals(response.get(OUTCOME).asString())) {
                checkpointViaSuspend.set(true);
            } else {
                throw ROOT_LOGGER.checkpointPreparationSuspendFailed(response.get(FAILURE_DESCRIPTION).asString());
            }
        } catch (IOException e) {
            throw ROOT_LOGGER.checkpointPreparationSuspendFailed(e.toString());
        }
    }

    private void initiateReload(final CountDownLatch checkpointLatch) {
        // Listen for process state changes and count down the latch
        // as the reload we are about to trigger causes them.
        final PropertyChangeListener listener = new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                long count = checkpointLatch.getCount();
                ROOT_LOGGER.debug("checkpointLatch received " + evt.getNewValue() + " at count " + count);
                if ((count == 4L && STOPPING.equals(evt.getNewValue()))
                        || (count == 3L && STOPPED.equals(evt.getNewValue()))
                        || (count == 2L && STARTING.equals(evt.getNewValue()))) {
                    checkpointLatch.countDown();
                    if (count == 2L) {
                        // The last countdown of checkpointLatch is from readyForCheckpoint.
                        // So we don't need to listen for further process state changes.
                        processStateNotifier.removePropertyChangeListener(this);
                    }
                }
            }
        };
        processStateNotifier.addPropertyChangeListener(listener);

        // Reload the server. Remember, the reload op returns as soon as the reload MSC service bounce is triggered;
        // it doesn't magically return from the reloaded server. So this method will return promptly.
        try (ModelControllerClient mcc = clientFactory.createSuperUserClient(clientExecutor)) {
            // Reload the server. Remember, the reload op returns as soon as the reload MSC service bounce is triggered;
            // it doesn't magically return from the reloaded server. So this method will return promptly.
            ModelNode response = mcc.execute(Util.createEmptyOperation(RELOAD, PathAddress.EMPTY_ADDRESS));
            if (!SUCCESS.equals(response.get(OUTCOME).asString())) {
                throw ROOT_LOGGER.checkpointPreparationReloadFailed(response.get(FAILURE_DESCRIPTION).asString());
            }
        } catch (IOException e) {
            throw ROOT_LOGGER.checkpointPreparationReloadFailed(e.toString());
        }
    }

    private CountDownLatch installStartedListener() {
        CountDownLatch cdl = new CountDownLatch(1);
        final PropertyChangeListener listener = new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (RUNNING.equals(evt.getNewValue())) {
                    processStateNotifier.removePropertyChangeListener(this);
                    cdl.countDown();
                }
            }
        };
        processStateNotifier.addPropertyChangeListener(listener);
        return cdl;
    }

    private void restoreFromSuspend() {
        try (ModelControllerClient mcc = clientFactory.createSuperUserClient(clientExecutor)) {
            // Suspend the server, with no timeout. This blocks until the suspend happens,
            // so there is no need for the kind of multi-threaded tracking we use for
            // checkpointing with a reload.
            ModelNode response = mcc.execute(Util.createEmptyOperation(RESUME, PathAddress.EMPTY_ADDRESS));
            if (!SUCCESS.equals(response.get(OUTCOME).asString())) {
                throw ROOT_LOGGER.checkpointRestoreResumeFailed(response.get(FAILURE_DESCRIPTION).asString());
            }
        } catch (IOException e) {
            throw ROOT_LOGGER.checkpointRestoreResumeFailed(e.toString());
        }
    }

    private void restoreFromReload() {
        CountDownLatch restoreLatch = restoreReference.getAndSet(null);
        if (restoreLatch != null) {
            elapsedTime.reset();
            try {
                // Install a listener for when we reach 'running' state and then
                // let the reload op that we blocked in beforeCheckpoint proceed
                CountDownLatch startedLatch = installStartedListener();
                restoreLatch.countDown();
                startedLatch.await();
            } catch (InterruptedException e) {
                ROOT_LOGGER.interruptedAwaitingCheckpointRestoration();
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        } else {
            ROOT_LOGGER.checkpointRestorationWithNoPreparation();
        }
    }

    private void updateSystemProperties() {
        Path configDir = new File(System.getProperty("jboss.server.config.dir")).toPath();
        Path sysProps = configDir.resolve("criu-restore-sys-props.txt");
        if (Files.exists(sysProps)) {
            Properties newProperies = new Properties();
            try (InputStream is = new FileInputStream(sysProps.toFile())) {
                newProperies.load(is);
            } catch (IOException ioe) {
                throw new RuntimeException(ioe);
            }
            for (Map.Entry<Object, Object> entry : newProperies.entrySet()) {
                String key = (String) entry.getKey();
                String value = (String) entry.getValue();
                if (value != null && !value.isEmpty()) {
                    System.setProperty(key, value);
                } else {
                    System.clearProperty(key);
                }
            }
        }
    }

    private void validateCheckpointStrategy(CheckpointStrategy strategy) {
        if (!supportedStrategies.contains(strategy)) {
            throw ROOT_LOGGER.checkpointStrategyNotSupported(strategy);
        }
    }

    private static class TriggerOperation {
        private final CheckpointStrategy strategy;
        private final CountDownLatch latch;

        private TriggerOperation(CheckpointStrategy strategy, CountDownLatch latch) {
            this.strategy = strategy;
            this.latch = latch;
        }
    }
}
