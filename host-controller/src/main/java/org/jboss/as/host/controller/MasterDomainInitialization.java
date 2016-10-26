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

import static org.jboss.as.remoting.Protocol.REMOTE;
import static org.jboss.as.remoting.Protocol.REMOTE_HTTP;
import static org.jboss.as.remoting.Protocol.REMOTE_HTTPS;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

import org.jboss.as.controller.ControlledProcessState;
import org.jboss.as.controller.ProcessType;
import org.jboss.as.controller.RunningMode;
import org.jboss.as.host.controller.discovery.DiscoveryOption;
import org.jboss.as.host.controller.discovery.DomainControllerManagementInterface;
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
        // We installed DiscoveryService in initialize but the ServiceTarget we used doesn't add a dep on us,
        // so we need to remove it ourselves
        DiscoveryService.uninstall(context.getController().getServiceContainer());
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

            publishDiscoveryOptions(serviceTarget);

            // Tell DMCS to record an event registering itself as a host controller
            domainModelControllerService.registerLocalHost();
        }
        return ok;
    }


    /** Allow slave host controllers to discover this domain controller using anmy of the provided discovery options. */
    private void publishDiscoveryOptions(final ServiceTarget serviceTarget) {
        List<DiscoveryOption> discoveryOptions = hostControllerInfo.getRemoteDomainControllerDiscoveryOptions();
        if (discoveryOptions != null) {
            List<DomainControllerManagementInterface> interfaces = new ArrayList<>();
            if (hostControllerInfo.getNativeManagementInterface() != null && !hostControllerInfo.getNativeManagementInterface().isEmpty()
                    && hostControllerInfo.getNativeManagementPort() > 0) {
                interfaces.add(new DomainControllerManagementInterface(hostControllerInfo.getNativeManagementPort(),
                        hostControllerInfo.getNativeManagementInterface(), REMOTE));
            }
            if (hostControllerInfo.getHttpManagementSecureInterface() != null && !hostControllerInfo.getHttpManagementSecureInterface().isEmpty()
                    && hostControllerInfo.getHttpManagementSecurePort() > 0) {
                interfaces.add(new DomainControllerManagementInterface(hostControllerInfo.getHttpManagementSecurePort(),
                        hostControllerInfo.getHttpManagementSecureInterface(), REMOTE_HTTPS));
            }
            if (hostControllerInfo.getHttpManagementInterface() != null && !hostControllerInfo.getHttpManagementInterface().isEmpty()
                    && hostControllerInfo.getHttpManagementPort() > 0) {
                interfaces.add(new DomainControllerManagementInterface(hostControllerInfo.getHttpManagementPort(),
                        hostControllerInfo.getHttpManagementInterface(), REMOTE_HTTP));
            }

            DiscoveryService.install(serviceTarget, discoveryOptions, interfaces);
        }
    }
}
