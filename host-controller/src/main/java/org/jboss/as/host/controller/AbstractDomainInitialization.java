/*
Copyright 2016 Red Hat, Inc.

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

package org.jboss.as.host.controller;

import static org.jboss.as.host.controller.logging.HostControllerLogger.ROOT_LOGGER;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicReference;

import org.jboss.as.controller.ControlledProcessState;
import org.jboss.as.controller.RunningMode;
import org.jboss.as.controller.persistence.ConfigurationPersistenceException;
import org.jboss.as.domain.controller.LocalHostControllerInfo;
import org.jboss.as.host.controller.logging.HostControllerLogger;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.threads.AsyncFutureTask;

/**
 * Base class for services that initializes the domain-wide aspects of a host's function, including
 * <ol>
 *     <li>registration with the master (if this host is not the master) and obtaining the domain wide config from it</li>
 *     <li>or, parsing the domain wide config from local persistence</li>
 *     <li>handling of slave registration requests (rejecting or accepting as appropriate for the hosts's configuration</li>
 * </ol>
 *
 * @author Brian Stansberry
 */
abstract class AbstractDomainInitialization implements Service<Void> {

    private static class FutureDomainInitialization extends AsyncFutureTask<Boolean> {

        protected FutureDomainInitialization() {
            super(null);
        }

        void setInitializationResult(boolean result) {
            super.setResult(result);
        }

        void setFailure(final Throwable t) {
            super.setFailed(t);
        }
    }

    static final ServiceName INITIALIZER_NAME = DomainModelControllerService.SERVICE_NAME.append("domain", "initialization");

    static Future<Boolean> install(ServiceTarget serviceTarget,
                                   ServiceName serviceName,
                                   AbstractDomainInitialization service) {
        serviceTarget.addService(serviceName, service)
                .addDependency(HostControllerService.HC_EXECUTOR_SERVICE_NAME, ExecutorService.class, service.injectedExecutorService)
                .install();
        return service.futureDomainInitialization;
    }

    final DomainModelControllerService domainModelControllerService;
    final LocalHostControllerInfo hostControllerInfo;
    final RunningMode runningMode;
    final HostControllerEnvironment environment;
    private final ControlledProcessState processState;
    final FutureDomainInitialization futureDomainInitialization = new FutureDomainInitialization();
    final AtomicReference<Integer> lockPermitHolder;
    private final InjectedValue<ExecutorService> injectedExecutorService = new InjectedValue<ExecutorService>();


    AbstractDomainInitialization(DomainModelControllerService domainModelControllerService,
                                 ControlledProcessState processState, RunningMode runningMode,
                                 HostControllerEnvironment environment, AtomicReference<Integer> lockPermitHolder) {
        this.domainModelControllerService = domainModelControllerService;
        this.processState = processState;
        this.hostControllerInfo = domainModelControllerService.getLocalHostInfo();
        this.runningMode = runningMode;
        this.environment = environment;
        this.lockPermitHolder = lockPermitHolder;
    }

    @Override
    public final void start(final StartContext context) throws StartException {
        Runnable r = new Runnable() {
            @Override
            public void run() {
                initializeAndReport(context);
            }
        };

        final ExecutorService executorService = injectedExecutorService.getValue();
        try {
            try {
                executorService.execute(r);
            } catch (RejectedExecutionException e) {
                r.run();
            }
        } finally {
            context.asynchronous();
        }
    }

    @Override
    public void stop(final StopContext context) {
        Runnable r = new Runnable() {
            @Override
            public void run() {
                try {
                    if (processState.getState() != ControlledProcessState.State.STOPPING) {
                        lockPermitHolder.set(domainModelControllerService.acquireExclusiveLock());
                    }
                    terminate(context);
                } catch (InterruptedException ie) {
                    HostControllerLogger.ROOT_LOGGER.interruptedAwaitingLockToChangeMasterState();
                } finally {
                    context.complete();
                }
            }
        };

        final ExecutorService executorService = injectedExecutorService.getValue();
        try {
            try {
                executorService.execute(r);
            } catch (RejectedExecutionException e) {
                r.run();
            }
        } finally {
            context.asynchronous();
        }
    }

    @Override
    public final Void getValue() throws IllegalStateException, IllegalArgumentException {
        return null;
    }

    abstract boolean initialize(ServiceTarget serviceTarget);

    abstract void terminate(StopContext context);

    final boolean loadDomainWideConfig() {

        boolean ok = false;
        if (processState.getState() == ControlledProcessState.State.STARTING) {
            try {
                ok = domainModelControllerService.loadDomainWideConfig();
            } catch (ConfigurationPersistenceException e) {
                ROOT_LOGGER.caughtExceptionDuringBoot(e);
                ok = false;
            }
        }
        return ok;
    }

    private void initializeAndReport(StartContext startContext) {
        boolean ok = true;
        boolean failed = false;
        try {
            ok = initialize(startContext.getChildTarget());
            startContext.complete();
        } catch (Exception e) {
            failed = true;
            futureDomainInitialization.setFailure(e);
            startContext.failed(new StartException(e));
        } finally {
            try {
                Integer permit = lockPermitHolder.getAndSet(null);
                if (permit != null) {
                    domainModelControllerService.releaseExclusiveLock(permit);
                }
            } finally {
                if (!failed) {
                    futureDomainInitialization.setInitializationResult(ok);
                }
            }
        }
    }
}
