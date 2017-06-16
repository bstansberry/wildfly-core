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

package org.wildfly.management.api.capability;

import org.wildfly.management.api.PathAddress;
import org.wildfly.management.api.PathElement;
import org.wildfly.management.api.model.Resource;
import org.wildfly.management.api.model.definition.AttributeDefinition;

/**
 * Performs registration and removal of capabilities or requirements associated with a resource
 * as part of the overall handling of the creation of that resource in an {@code add}
 * operation or the removal of that resource in a {@code remove} operation.
 * <p>
 * <strong>Usage note:</strong> This should be used for recording optional capabilities or optional requirements
 * that cannot be configured declaratively. Implementations of this interface are not meant to be used
 * for capabilities that a resource always exposes; use
 * {@link org.wildfly.management.api.model.definition.ResourceTypeDefinition.Builder#setCapabilities(RuntimeCapability[])}
 * for configuring that declaratively. It also should not be used for non-optional static requirements of
 * a capability; use {@link RuntimeCapability.Builder#addRequirements(String...)} for that. Requirements that
 * arise as a result of attributes whose values refer to the required capability should be configured using
 * {@link org.wildfly.management.api.model.definition.ItemDefinition.Builder#setCapabilityReference(String)} or one of
 * its overloaded variants.
 *
 * @author Brian Stansberry
 */
public interface CapabilityRegistrar {

    /**
     * Register additional capabilities or requirements beyond those that can be specified declaratively.
     * Called as part of handling an {@code add} operation that adds a resource.
     *
     * @param resource the resource being added.
     * @param context contextual object to support the registration
     */
    void resourceAdded(Resource resource, Context context);

    /**
     * In response to modification of an attribute, register or deregister additional capabilities or requirements
     * beyond those that can be specified declaratively.
     *
     * @param currentResource  the resource in its current state, following any modifications made by the currently
     *                         executing management operation
     * @param updatedAttribute the definition of the attribute that was modified
     * @param originalResource the resource in its original state prior to any change by the currently executing management operation
     * @param context          contextual object to support the registration or deregistration
     */
    void resourceUpdated(Resource currentResource, AttributeDefinition updatedAttribute, Resource originalResource, Context context);

    /**
     * Remove capabilities previously registered by {@link #resourceAdded(Resource, Context)}.
     * Called as part of handling a {@code remove} operation that removes a resource.
     *
     * @param resource the resource being removed.
     * @param context contextual object to support the registration
     */
    void resourceRemoved(Resource resource, Context context);

    /**
     * A contextual object to support a {@link CapabilityRegistrar} implementation.
     */
    interface Context {

        /**
         * Gets the address associated with the currently executing operation step.
         * @return the address. Will not be {@code null}
         */
        PathAddress getCurrentAddress();

        /**
         * Gets the {@link PathElement#getValue() value} of the {@link #getCurrentAddress() current address'}
         * {@link PathAddress#getLastElement() last element}.
         *
         * @return the last element value
         *
         * @throws java.lang.IllegalStateException if {@link #getCurrentAddress()} is the empty address
         */
        String getCurrentAddressValue();


        /**
         * Registers a capability with the system. Any {@link RuntimeCapability#getRequirements() requirements}
         * associated with the capability will be recorded as requirements.
         *
         * @param capability  the capability. Cannot be {@code null}
         */
        void registerCapability(RuntimeCapability capability);

        /**
         * Registers an additional hard requirement a capability has beyond what it was aware of when {@code capability}
         * was passed to {@link #registerCapability(RuntimeCapability)}. Used for cases
         * where a capability optionally depends on another capability, and whether or not that requirement is needed is
         * not known when the capability is first registered.
         * <p>
         * This method should be used in preference to
         * {@link org.wildfly.management.api.runtime.RuntimeUpdateContext#requireOptionalCapability(String, String, String)}
         * when, based on its own configuration, the caller knows that the optional capability is actually required in the current process.
         * </p>
         *
         * @param required the name of the required capability. Cannot be {@code null}
         * @param dependent the name of the capability that requires the other capability. Cannot be {@code null}
         * @param attribute the name of the attribute that triggered this requirement, or {@code null} if no single
         *                  attribute was responsible
         *
         * @throws java.lang.IllegalStateException if {@code dependent} is not registered
         */
        void registerAdditionalCapabilityRequirement(String required, String dependent, String attribute);

        /**
         * Record that a previously registered requirement for a capability will no longer exist.
         * <p>
         * <strong>Semantics with "reload-required" and "restart-required":</strong>
         * Deregistering a capability requirement does not obligate the resource to cease using a
         * {@link org.wildfly.management.api.runtime.RuntimeUpdateContext#getCapabilityRuntimeAPI(String, Class) previously obtained}
         * reference to that capability's {@link RuntimeCapability#getRuntimeAPI() runtime API}. But, if
         * the caller will not cease using the capability, it must put the process in {@code reload-required}
         * or {@code restartRequired() restart-required} state. This will reflect the fact that the model says the
         * capability is not required, but in the runtime it still is.
         * </p>
         *
         * @param required the name of the no longer required capability
         * @param dependent the name of the capability that no longer has the requirement
         */
        void deregisterCapabilityRequirement(String required, String dependent);

        /**
         * Record that a previously registered capability will no longer be available. Invoking this operation will also
         * automatically {@link #deregisterCapabilityRequirement(String, String) deregister any requirements} that are
         * associated with this capability, including optional ones.
         * <p><strong>Semantics with "reload-required" and "restart-required":</strong>
         * Deregistering a capability does not eliminate the obligation to other capabilities that have
         * previously depended upon it to support them by providing expected runtime services. It does require that those other
         * capabilities also {@link #deregisterCapabilityRequirement(String, String) deregister their requirements} as
         * part of the same operation. Requiring that they do so ensures that the management model is consistent.
         * However, those other capabilities may simply put the process in {@code reload-required}
         * or {@code restart-required} state and then continue to use the existing services. So, an operation that invokes
         * this method should also always put the process into {@code reload-required} or {@code restart-required} state.
         * This will reflect the fact that the model says the capability is not present, but in the runtime it still is.
         * </p>
         *
         * @param capabilityName the name of the capability
         */
        void deregisterCapability(String capabilityName);

    }
}
