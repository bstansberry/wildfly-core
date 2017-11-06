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

package org.jboss.as.controller.registry;

import org.jboss.as.controller.CapabilityRegistry;
import org.jboss.as.controller.ProcessType;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.access.management.AccessConstraintUtilizationRegistry;
import org.jboss.as.controller.registry.bridge.BridgeResourceType;
import org.wildfly.common.Assert;

/**
 * Factory for the root resource type for a management model.
 *
 * @author Brian Stansberry
 */
public final class BridgeResourceTypeFactory {

    public static BridgeResourceTypeFactory forProcessType(ProcessType processType) {
        return new BridgeResourceTypeFactory(processType);
    }

    private final ProcessType processType;

    private BridgeResourceTypeFactory(ProcessType processType) {
        this.processType = processType;
    }

    /**
     * Create a new root model node registration.
     *
     * @param resourceDefinition the facotry for the model description provider for the root model node
     * @return the new root model resource type
     *
     * @throws SecurityException if the caller does not have {@link ImmutableManagementResourceRegistration#ACCESS_PERMISSION}
     */
    public BridgeResourceType createRootResourceType(final ResourceDefinition resourceDefinition) {
        return createRootResourceType(resourceDefinition, null, null);
    }

    /**
     * Create a new root model resource type.
     *
     * @param resourceDefinition the facotry for the model description provider for the root model node
     * @param constraintUtilizationRegistry registry for recording access constraints. Can be {@code null} if
     *                                      tracking access constraint usage is not supported
     * @param registry the capability registry (can be {@code null})
     * @return the new root model resource registration
     *
     * @throws SecurityException if the caller does not have {@link ImmutableManagementResourceRegistration#ACCESS_PERMISSION}
     */
    public BridgeResourceType createRootResourceType(final ResourceDefinition resourceDefinition,
                                                             AccessConstraintUtilizationRegistry constraintUtilizationRegistry,
                                                             CapabilityRegistry registry) {
        Assert.checkNotNullParam("resourceDefinition", resourceDefinition);
        ConcreteResourceRegistration resourceRegistration =
                new ConcreteResourceRegistration(resourceDefinition, constraintUtilizationRegistry, registry, processType);
        resourceDefinition.registerAttributes(resourceRegistration);
        resourceDefinition.registerOperations(resourceRegistration);
        resourceDefinition.registerChildren(resourceRegistration);
        resourceDefinition.registerNotifications(resourceRegistration);
        return resourceRegistration;
    }


}
