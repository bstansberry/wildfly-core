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

import org.jboss.as.controller.ExpressionResolver;
import org.jboss.as.controller.ProxyController;
import org.jboss.as.controller.extension.ExtensionRegistry;
import org.jboss.as.controller.persistence.ExtensibleConfigurationPersister;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.domain.controller.LocalHostControllerInfo;
import org.jboss.as.host.controller.ignored.IgnoredDomainResourceRegistry;
import org.jboss.as.repository.ContentRepository;
import org.jboss.as.repository.HostFileRepository;
import org.jboss.dmr.ModelNode;

/**
 * Internal API for Host Controller functions.
 *
 * @author Brian Stansberry
 */
public interface HostController {

    /**
     * Gets the local host controller info.
     *
     * @return the local host info
     */
    LocalHostControllerInfo getLocalHostInfo();

    /**
     * Registers a running server in the domain model
     *
     * @param serverControllerClient client the controller can use to communicate with the server.
     */
    void registerRunningServer(final ProxyController serverControllerClient);

    /**
     * Unregisters a running server from the domain model
     *
     * @param serverName the name of the server
     */
    void unregisterRunningServer(String serverName);

    /**
     * Get the operations needed to create the given profile.
     *
     * @param profileName the name of the profile
     *
     * @return the operations
     */
    ModelNode getProfileOperations(String profileName);

    /**
     * Gets the file repository backing this host controller
     *
     * @return the file repository
     */
    HostFileRepository getLocalFileRepository();

    /**
     * Gets the file repository backing the master domain controller
     *
     * @return the file repository
     *
     * @throws IllegalStateException if the {@link #getLocalHostInfo() local host info}'s
     *          {@link LocalHostControllerInfo#isMasterDomainController()} method would return {@code true}
     *
     */
    HostFileRepository getRemoteFileRepository();

    /**
     * Stop this host controller with a specific exit code.
     *
     * @param exitCode the exit code passed to the ProcessController
     */
    void stopLocalHost(int exitCode);

    /**
     * Gets the resolver this host controller is using for expression resolution.
     *
     * @return the expression resolver
     */
    ExpressionResolver getExpressionResolver();

    /**
     * Initialize the domain portion of the root resource definition for a host that is acting as the master.
     *
     * @param root  the root resource registration
     * @param configurationPersister the configuration persister
     * @param contentRepository the content repository
     * @param fileRepository the file repository
     * @param extensionRegistry the extension registry
     */
    void initializeMasterDomainRegistry(final ManagementResourceRegistration root,
                                        final ExtensibleConfigurationPersister configurationPersister, final ContentRepository contentRepository,
                                        final HostFileRepository fileRepository,
                                        final ExtensionRegistry extensionRegistry);

    /**
     * Initialize the domain portion of the root resource definition for a host that is not acting as the master.
     *
     * @param root  the root resource registration
     * @param configurationPersister the configuration persister
     * @param contentRepository the content repository
     * @param hostControllerInfo  the host controller info
     * @param extensionRegistry the extension registry
     * @param ignoredDomainResourceRegistry the ignored resource registry
     */
    void initializeSlaveDomainRegistry(final ManagementResourceRegistration root,
                                       final ExtensibleConfigurationPersister configurationPersister, final ContentRepository contentRepository,
                                       final HostFileRepository fileRepository, final LocalHostControllerInfo hostControllerInfo,
                                       final ExtensionRegistry extensionRegistry,
                                       final IgnoredDomainResourceRegistry ignoredDomainResourceRegistry);
}
