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

import static org.jboss.as.host.controller.DomainModelControllerService.DomainConnectResult.ABORT;
import static org.jboss.as.host.controller.DomainModelControllerService.DomainConnectResult.FAILED;
import static org.jboss.as.host.controller.logging.HostControllerLogger.ROOT_LOGGER;
import static org.jboss.as.host.controller.model.host.AdminOnlyDomainConfigPolicy.REQUIRE_LOCAL_CONFIG;

import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

import org.jboss.as.controller.ControlledProcessState;
import org.jboss.as.controller.RunningMode;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.host.controller.discovery.DiscoveryOption;
import org.jboss.as.host.controller.mgmt.RejectSlavesOperationHandlerService;
import org.jboss.as.process.CommandLineConstants;
import org.jboss.as.remoting.management.ManagementRemotingServices;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StopContext;

/**
 * Performs domain initialization for a host that is not the master.
 *
 * @author Brian Stansberry
 */
class SlaveDomainInitialization extends AbstractDomainInitialization {

    static final ServiceName SLAVE_NAME = INITIALIZER_NAME.append("slave");

    static Future<Boolean> install(ServiceTarget serviceTarget,
                                   DomainModelControllerService domainController,
                                   ControlledProcessState processState,
                                   RunningMode runningMode,
                                   HostControllerEnvironment environment,
                                   AtomicReference<Integer> lockPermitHolder) {
        SlaveDomainInitialization service = new SlaveDomainInitialization(domainController, processState, runningMode,
                environment, serviceTarget, lockPermitHolder);
        return install(serviceTarget, SLAVE_NAME, service);
    }

    /**
     *  We cache the ServiceTarget used to install us (from DomainModeControllerService)
     *  and use it to install other services, rather than using StartContext.getChildTarget().
     *  This allows those other services to start without waiting for this one to complete
     *  the 'initialize' method. The initialize method will not complete until services added by
     *  domainModelControllerService.connectToDomainMaster are started, so we cannot have those depend on us.
     */
    private final ServiceTarget parentTarget;

    private SlaveDomainInitialization(DomainModelControllerService domainModelControllerService,
                              ControlledProcessState processState,
                              RunningMode runningMode,
                              HostControllerEnvironment environment,
                              ServiceTarget parentTarget,
                              AtomicReference<Integer> lockPermitHolder) {
        super(domainModelControllerService, processState, runningMode, environment, lockPermitHolder);
        this.parentTarget = parentTarget;
    }

    @Override
    void terminate(StopContext context) {
        domainModelControllerService.disconnectFromMaster(context.getController().getServiceContainer());
    }

    boolean initialize(ServiceTarget serviceTarget) {

        // Install a handler factory to reject slave registration requests
        // Use parentTarget as there is no reason to have this wait on us completing initialize/start
        ManagementRemotingServices.installManagementChannelServices(parentTarget, ManagementRemotingServices.MANAGEMENT_ENDPOINT,
                new RejectSlavesOperationHandlerService(hostControllerInfo.isMasterDomainController(), environment.getDomainTempDir()),
                DomainModelControllerService.SERVICE_NAME, ManagementRemotingServices.DOMAIN_CHANNEL,
                HostControllerService.HC_EXECUTOR_SERVICE_NAME, HostControllerService.HC_SCHEDULED_EXECUTOR_SERVICE_NAME);

        boolean ok = true;
        boolean useLocalDomainXml = false;
        boolean isCachedDC = environment.isUseCachedDc();
        List<DiscoveryOption> discoveryOptions = hostControllerInfo.getRemoteDomainControllerDiscoveryOptions();
        boolean discoveryConfigured = (discoveryOptions != null) && !discoveryOptions.isEmpty();
        if (runningMode != RunningMode.ADMIN_ONLY) {
            if (discoveryConfigured) {
                // Try and connect.
                // If can't connect && !environment.isUseCachedDc(), abort
                // Otherwise if can't connect, use local domain.xml and start trying to reconnect later
                DomainModelControllerService.DomainConnectResult connectResult = domainModelControllerService.connectToDomainMaster(parentTarget);
                if (connectResult == ABORT) {
                    ok = false;
                } else if (connectResult == FAILED) {
                    useLocalDomainXml = true;
                }
            } else {
                // Invalid configuration; no way to get the domain config
                ROOT_LOGGER.noDomainControllerConfigurationProvided(runningMode,
                        CommandLineConstants.ADMIN_ONLY, RunningMode.ADMIN_ONLY);
                // Don't call System.exit as DMCS is blocking waiting on the future and that will prevent
                // shutdown hook completion. So we just signal 'false' and let DMCS.boot abort
                // SystemExiter.abort(ExitCodes.HOST_CONTROLLER_ABORT_EXIT_CODE);
                ok = false;
            }
        } else {
            // We're in admin-only mode. See how we handle access control config
            // if cached-dc is specified, we try and use the last configuration we have before failing.
            if (isCachedDC) {
                useLocalDomainXml = true;
            }
            switch (hostControllerInfo.getAdminOnlyDomainConfigPolicy()) {
                case ALLOW_NO_CONFIG:
                    // our current setup is good, if we're using --cached-dc, we'll try and load the config below
                    // if not, we'll start empty.
                    break;
                case FETCH_FROM_MASTER:
                    if (discoveryConfigured) {
                        // Try and connect.
                        // If can't connect && !environment.isUseCachedDc(), abort
                        // Otherwise if can't connect, use local domain.xml but DON'T start trying to reconnect later
                        DomainModelControllerService.DomainConnectResult connectResult =
                                domainModelControllerService.connectToDomainMaster(parentTarget);
                        ok = connectResult != ABORT;
                    } else {
                        // try and use a local cached version below before failing
                        if (isCachedDC) {
                            break;
                        }
                        // otherwise, this is an invalid configuration; no way to get the domain config
                        ROOT_LOGGER.noDomainControllerConfigurationProvidedForAdminOnly(
                                ModelDescriptionConstants.ADMIN_ONLY_POLICY,
                                REQUIRE_LOCAL_CONFIG,
                                CommandLineConstants.CACHED_DC, RunningMode.ADMIN_ONLY);

                        // Don't call System.exit as DMCS is blocking waiting on the future and that will prevent
                        // shutdown hook completion. So we just signal 'false' and let DMCS.boot abort
                        // SystemExiter.abort(ExitCodes.HOST_CONTROLLER_ABORT_EXIT_CODE);
                        ok = false;
                        break;
                    }
                    break;
                case REQUIRE_LOCAL_CONFIG:
                    // if we have a cached copy, and --cached-dc we can try to use that below
                    if (isCachedDC) {
                        break;
                    }

                    // otherwise, this is an invalid configuration; no way to get the domain config
                    ROOT_LOGGER.noAccessControlConfigurationAvailable(runningMode,
                            ModelDescriptionConstants.ADMIN_ONLY_POLICY,
                            REQUIRE_LOCAL_CONFIG,
                            CommandLineConstants.CACHED_DC, runningMode);

                    // Don't call System.exit as DMCS is blocking waiting on the future and that will prevent
                    // shutdown hook completion. So we just signal 'false' and let DMCS.boot abort
                    // SystemExiter.abort(ExitCodes.HOST_CONTROLLER_ABORT_EXIT_CODE);
                    ok = false;
                    break;
                default:
                    throw new IllegalStateException(hostControllerInfo.getAdminOnlyDomainConfigPolicy().toString());
            }
        }

        if (ok && useLocalDomainXml) {
            loadDomainWideConfig();
        }

        return ok;
    }
}
