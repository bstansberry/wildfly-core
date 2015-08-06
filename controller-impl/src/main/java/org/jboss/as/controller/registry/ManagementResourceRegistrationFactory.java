/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

package org.jboss.as.controller.registry;

import java.util.Collections;
import java.util.List;

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.access.management.AccessConstraintDefinition;
import org.jboss.as.controller.access.management.AccessConstraintUtilizationRegistry;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.logging.ControllerLogger;

/**
 * A factory for creating a new root management model resource registration.
 */
public class ManagementResourceRegistrationFactory {

    private ManagementResourceRegistrationFactory() {
    }

    /**
     * Create a new root model node registration.
     *
     * @param rootModelDescriptionProvider the model description provider for the root model node
     * @return the new root model node registration
     *
     * @throws SecurityException if the caller does not have {@link ImmutableManagementResourceRegistration#ACCESS_PERMISSION}
     * @deprecated DescriptionProvider shouldn't be used anymore, use ResourceDefinition variant
     */
    @Deprecated
    public static ManagementResourceRegistration create(final DescriptionProvider rootModelDescriptionProvider) {
        return create(rootModelDescriptionProvider, null);
    }

    /**
     * Create a new root model node registration.
     *
     * @param rootModelDescriptionProvider the model description provider for the root model node
     * @param constraintUtilizationRegistry registry for recording access constraints. Can be {@code null} if
     *                                      tracking access constraint usage is not supported
     * @return the new root model node registration
     *
     * @throws SecurityException if the caller does not have {@link ImmutableManagementResourceRegistration#ACCESS_PERMISSION}
     * @deprecated DescriptionProvider shouldn't be used anymore, use ResourceDefinition variant
     */
    @Deprecated
    public static ManagementResourceRegistration create(final DescriptionProvider rootModelDescriptionProvider,
                                                        AccessConstraintUtilizationRegistry constraintUtilizationRegistry) {
        if (rootModelDescriptionProvider == null) {
            throw ControllerLogger.ROOT_LOGGER.nullVar("rootModelDescriptionProvider");
        }
        ResourceDefinition rootResourceDefinition = new ResourceDefinition() {

            @Override
            public PathElement getPathElement() {
                return null;
            }

            @Override
            public DescriptionProvider getDescriptionProvider(ImmutableManagementResourceRegistration resourceRegistration) {
                return rootModelDescriptionProvider;
            }

            @Override
            public void registerOperations(ManagementResourceRegistration resourceRegistration) {
                //  no-op
            }

            @Override
            public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
                //  no-op
            }

            @Override
            public void registerChildren(ManagementResourceRegistration resourceRegistration) {
                //  no-op
            }

            @Override
            public void registerNotifications(ManagementResourceRegistration resourceRegistration) {
                //  no-op
            }

            @Override
            public void registerCapabilities(ManagementResourceRegistration resourceRegistration) {
                // no-op
            }

            @Override
            public List<AccessConstraintDefinition> getAccessConstraints() {
                return Collections.emptyList();
            }

            @Override
            public boolean isRuntime() {
                return false;
            }

            @Override
            public boolean isOrderedChild() {
                return false;
            }
        };
        return new ConcreteResourceRegistration(null, null, rootResourceDefinition, constraintUtilizationRegistry, rootResourceDefinition.isRuntime(), false);
    }

    /**
     * Create a new root model node registration.
     *
     * @param resourceDefinition the facotry for the model description provider for the root model node
     * @return the new root model node registration
     *
     * @throws SecurityException if the caller does not have {@link ImmutableManagementResourceRegistration#ACCESS_PERMISSION}
     */
    public static ManagementResourceRegistration create(final ResourceDefinition resourceDefinition) {
        return create(resourceDefinition, null);
    }

    /**
     * Create a new root model node registration.
     *
     * @param resourceDefinition the facotry for the model description provider for the root model node
     * @param constraintUtilizationRegistry registry for recording access constraints. Can be {@code null} if
     *                                      tracking access constraint usage is not supported
     * @return the new root model node registration
     *
     * @throws SecurityException if the caller does not have {@link ImmutableManagementResourceRegistration#ACCESS_PERMISSION}
     */
    public static ManagementResourceRegistration create(final ResourceDefinition resourceDefinition,
                                                        AccessConstraintUtilizationRegistry constraintUtilizationRegistry) {
        if (resourceDefinition == null) {
            throw ControllerLogger.ROOT_LOGGER.nullVar("rootModelDescriptionProviderFactory");
        }
        ConcreteResourceRegistration resourceRegistration =
                new ConcreteResourceRegistration(null, null, resourceDefinition,
                        constraintUtilizationRegistry, resourceDefinition.isRuntime(), false);
        resourceDefinition.registerAttributes(resourceRegistration);
        resourceDefinition.registerOperations(resourceRegistration);
        resourceDefinition.registerChildren(resourceRegistration);
        resourceDefinition.registerNotifications(resourceRegistration);
        return resourceRegistration;
    }
}
