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

import java.util.concurrent.Future;

import org.jboss.as.controller.ControlledProcessState;
import org.jboss.as.controller.ProcessType;
import org.jboss.as.controller.RunningMode;
import org.jboss.as.host.controller.mgmt.DomainHostExcludeRegistry;
import org.jboss.as.host.controller.mgmt.HostControllerOperationExecutor;
import org.jboss.as.host.controller.mgmt.MasterDomainControllerOperationHandlerService;
import org.jboss.as.host.controller.mgmt.RejectSlavesOperationHandlerService;
import org.jboss.as.remoting.management.ManagementRemotingServices;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StopContext;

/**
 * Performs domain initialization for a host that is the master.
 *
 * @author Brian Stansberry
 */
class MasterDomainInitialization extends AbstractDomainInitialization {

    private static final ServiceName SLAVE_NAME = INITIALIZER_NAME.append("slave");

    static Future<Boolean> install(ServiceTarget serviceTarget,
                                   DomainModelControllerService domainController,
                                   ControlledProcessState processState,
                                   ProcessType processType,
                                   RunningMode runningMode,
                                   HostControllerEnvironment environment,
                                   HostControllerOperationExecutor operationExecutor,
                                   MasterDomainControllerOperationHandlerService.TransactionalOperationExecutor txOperationExecutor,
                                   DomainHostExcludeRegistry domainHostExcludeRegistry) {
        MasterDomainInitialization service = new MasterDomainInitialization(domainController, processState, processType, runningMode, environment, operationExecutor, txOperationExecutor, domainHostExcludeRegistry);
        return install(serviceTarget, SLAVE_NAME, service);
    }

    private final ProcessType processType;
    private final HostControllerOperationExecutor operationExecutor;
    private final MasterDomainControllerOperationHandlerService.TransactionalOperationExecutor txOperationExecutor;
    private final DomainHostExcludeRegistry domainHostExcludeRegistry;

    private MasterDomainInitialization(DomainModelControllerService domainController,
                               ControlledProcessState processState,
                               ProcessType processType,
                               RunningMode runningMode,
                               HostControllerEnvironment environment,
                               HostControllerOperationExecutor operationExecutor,
                               MasterDomainControllerOperationHandlerService.TransactionalOperationExecutor txOperationExecutor,
                               DomainHostExcludeRegistry domainHostExcludeRegistry) {
        super(domainController, processState, runningMode, environment);
        this.processType = processType;
        this.operationExecutor = operationExecutor;
        this.txOperationExecutor = txOperationExecutor;
        this.domainHostExcludeRegistry = domainHostExcludeRegistry;
    }

    @Override
    public void stop(StopContext context) {
        domainModelControllerService.unregisterLocalHost();
        super.stop(context);
    }

    boolean initialize(final ServiceTarget serviceTarget) {

        boolean ok = loadDomainWideConfig();

        if (ok) {
            if (runningMode.equals(RunningMode.NORMAL) && processType != ProcessType.EMBEDDED_HOST_CONTROLLER) {
                ManagementRemotingServices.installManagementChannelServices(serviceTarget, ManagementRemotingServices.MANAGEMENT_ENDPOINT,
                        new MasterDomainControllerOperationHandlerService(domainModelControllerService, operationExecutor, txOperationExecutor,
                                environment.getDomainTempDir(), domainHostExcludeRegistry),
                        DomainModelControllerService.SERVICE_NAME, ManagementRemotingServices.DOMAIN_CHANNEL,
                        HostControllerService.HC_EXECUTOR_SERVICE_NAME, HostControllerService.HC_SCHEDULED_EXECUTOR_SERVICE_NAME);
            } else if (processType != ProcessType.EMBEDDED_HOST_CONTROLLER) {
                // Install a handler factory to reject slave registration requests
                ManagementRemotingServices.installManagementChannelServices(serviceTarget, ManagementRemotingServices.MANAGEMENT_ENDPOINT,
                        new RejectSlavesOperationHandlerService(hostControllerInfo.isMasterDomainController(), environment.getDomainTempDir()),
                        DomainModelControllerService.SERVICE_NAME, ManagementRemotingServices.DOMAIN_CHANNEL,
                        HostControllerService.HC_EXECUTOR_SERVICE_NAME, HostControllerService.HC_SCHEDULED_EXECUTOR_SERVICE_NAME);
            }

            // Tell DMCS to record an event registering itself as a host controller
            domainModelControllerService.registerLocalHost();
        }
        return ok;
    }
}
