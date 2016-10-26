/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.host.controller;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;

import org.jboss.as.host.controller.discovery.DiscoveryOption;
import org.jboss.as.host.controller.discovery.DomainControllerManagementInterface;
import org.jboss.as.network.NetworkInterfaceBinding;
import org.jboss.as.server.services.net.NetworkInterfaceService;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * Service responsible for allowing a domain controller to be discovered by
 * slave host controllers.
 *
 * @author Farah Juma
 */
class DiscoveryService implements Service<Void> {

    static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("host", "controller", "discovery");

    private final Map<String, InjectedValue<NetworkInterfaceBinding>> interfaceBindings = new HashMap<String, InjectedValue<NetworkInterfaceBinding>>();
    private final InjectedValue<ExecutorService> executorService = new InjectedValue<ExecutorService>();
    private final List<DiscoveryOption> discoveryOptions;
    private final List<DomainControllerManagementInterface> managementInterfaces;

    /**
     * Create the DiscoveryService instance.
     *  @param discoveryOptions the list of discovery options
     * @param managementInterfaces configuration information for a management interface
     */
    private DiscoveryService(List<DiscoveryOption> discoveryOptions, List<DomainControllerManagementInterface> managementInterfaces) {
        this.discoveryOptions = discoveryOptions;
        this.managementInterfaces = managementInterfaces;
        for(DomainControllerManagementInterface managementInterface : managementInterfaces) {
            interfaceBindings.put(managementInterface.getHost(), new InjectedValue<NetworkInterfaceBinding>());
        }
    }

    static void install(final ServiceTarget serviceTarget, final List<DiscoveryOption> discoveryOptions,
                        final List<DomainControllerManagementInterface> managementInterfaces) {
        assert discoveryOptions != null;
        assert managementInterfaces != null;
        final DiscoveryService discovery = new DiscoveryService(discoveryOptions, managementInterfaces);
        ServiceBuilder builder = serviceTarget.addService(DiscoveryService.SERVICE_NAME, discovery)
                .addDependency(HostControllerService.HC_EXECUTOR_SERVICE_NAME, ExecutorService.class, discovery.executorService);
        Set<String> alreadyDefinedInterfaces = new HashSet<String>();
        for(DomainControllerManagementInterface managementInterface : managementInterfaces) {
            if(!alreadyDefinedInterfaces.contains(managementInterface.getAddress())) {
                builder.addDependency(NetworkInterfaceService.JBOSS_NETWORK_INTERFACE.append(managementInterface.getAddress()), NetworkInterfaceBinding.class, discovery.interfaceBindings.get(managementInterface.getAddress()));
                alreadyDefinedInterfaces.add(managementInterface.getAddress());
            }
        }
       builder.install();
    }

    static void uninstall(ServiceContainer serviceContainer) {
        ServiceController<?> controller = serviceContainer.getService(SERVICE_NAME);
        if (controller != null) {
            controller.setMode(ServiceController.Mode.REMOVE);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void start(final StartContext context) throws StartException {
        final Runnable task = new Runnable() {
            @Override
            public void run() {
                try {
                    // Allow slave host controllers to discover this domain controller using any
                    // of the provided discovery options.

                    for(DomainControllerManagementInterface managementInterface : managementInterfaces) {
                        String host = interfaceBindings.get(managementInterface.getAddress()).getValue().getAddress().getHostAddress();
                        managementInterface.setHost(host);
                    }
                    for (DiscoveryOption discoveryOption : discoveryOptions) {
                        discoveryOption.allowDiscovery(managementInterfaces);
                    }
                    context.complete();
                } catch (Exception e) {
                    context.failed(new StartException(e));
                }
            }
        };
        try {
            executorService.getValue().execute(task);
        } catch (RejectedExecutionException e) {
            task.run();
        } finally {
            context.asynchronous();
        }
    }

    /** {@inheritDoc} */
    @Override
    public void stop(final StopContext context) {
        final Runnable task = new Runnable() {
            @Override
            public void run() {
                try {
                    for (DiscoveryOption discoveryOption : discoveryOptions) {
                        discoveryOption.cleanUp();
                    }
                } finally {
                    context.complete();
                }
            }
        };
        try {
            executorService.getValue().execute(task);
        } catch (RejectedExecutionException e) {
            task.run();
        } finally {
            context.asynchronous();
        }
    }

    /** {@inheritDoc} */
    @Override
    public Void getValue() throws IllegalStateException, IllegalArgumentException {
       return null;
    }
}
