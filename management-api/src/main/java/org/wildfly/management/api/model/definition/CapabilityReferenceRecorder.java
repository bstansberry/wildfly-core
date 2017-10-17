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

package org.wildfly.management.api.model.definition;

import java.util.Set;

import org.jboss.dmr.ModelNode;
import org.wildfly.management.api.ResourceAddress;
import org.wildfly.management.api.AddressElement;
import org.wildfly.management.api.capability.RuntimeCapability;
import org.wildfly.management.api.model.ResourceType;
import org.wildfly.management.api.model.Resource;
import org.wildfly.management.api.runtime.RuntimeUpdateContext;

/**
 * Records information about capability reference information encoded in an attribute's value.
 *
 * @author Brian Stansberry (c) 2015 Red Hat Inc.
 */
public interface CapabilityReferenceRecorder {

    /**
     * Registers capability requirement information to the given context.
     * @param context         the context
     * @param resource        the resource on which requirements are gathered
     * @param attributeName   the name of the attribute
     * @param attributeValues the values of the attribute, which may contain null
     */
    void addCapabilityRequirements(RecorderContext context, Resource resource, String attributeName, String... attributeValues);

    /**
     * Deregisters capability requirement information from the given context.
     * @param context         the context
     * @param resource        the resource on which requirements are gathered
     * @param attributeName   the name of the attribute
     * @param attributeValues the values of the attribute, which may contain null
     */
    void removeCapabilityRequirements(RecorderContext context, Resource resource, String attributeName, String... attributeValues);

    /**
     * @return requirement name of the capability this reference depends on
     */
    String getBaseRequirementName();

    /** Provides a context to a {@link CapabilityReferenceRecorder} when it is adding or removing requirements. */
    interface RecorderContext {

        /**
         * Registers an additional hard requirement a {@link RuntimeCapability} has beyond what it was aware of when
         * it was initially registered (e.g. when a resource that configures the capability was added.) Used for cases
         * where a capability optionally depends on another capability, and whether or not that requirement is needed is
         * not known when the capability is first registered.
         *
         * @param required the name of the required capability. Cannot be {@code null}
         * @param dependent the name of the capability that requires the other capability. Cannot be {@code null}
         * @param attribute the name of the attribute that triggered this requirement. Cannot be {@code null}
         *
         * @throws java.lang.IllegalStateException if {@code dependent} is not registered
         */
        void registerAdditionalCapabilityRequirement(String required, String dependent, String attribute);

        /**
         * Record that a previously registered requirement for a capability will no longer exist.
         * <p>
         * <strong>Semantics with "reload-required" and "restart-required":</strong>
         * Deregistering a capability requirement does not obligate the caller to cease using a
         * {@link RuntimeUpdateContext#getCapabilityRuntimeAPI(String, Class) previously obtained} reference to that
         * capability's {@link RuntimeCapability#getRuntimeAPI() runtime API}. But, if
         * the caller will not cease using the capability, it must
         * {@link RuntimeUpdateContext#reloadRequired() put the process in reload-required}
         * or {@link RuntimeUpdateContext#restartRequired()} () restart-required} state. This will reflect the fact that the model says the
         * capability is not required, but in the runtime it still is.
         * </p>
         *
         * @param required the name of the no longer required capability
         * @param dependent the name of the capability that no longer has the requirement
         */
        void deregisterCapabilityRequirement(String required, String dependent);

        /**
         * Gets the address associated with the currently executing operation step.
         * @return the address. Will not be {@code null}
         */
        ResourceAddress getCurrentAddress();

        /**
         * Gets the {@link AddressElement#getValue() value} of the {@link #getCurrentAddress() current address'}
         * {@link ResourceAddress#getLastElement() last element}.
         *
         * @return the last element value
         *
         * @throws java.lang.IllegalStateException if {@link #getCurrentAddress()} is the empty address
         */
        String getCurrentAddressValue();

        /**
         * Gets the name of the operation associated with the currently executing operation step.
         *
         * @return the name. Will not be {@code null}
         */
        String getCurrentOperationName();

        /**
         * Gets the value of the parameter of the given name for the operation associated with the currently executing operation step.
         * @param parameterName the name of the parameter. Cannot be {@code null}
         * @return the parameter value. Will not return {@code null}, returning of a node of {@link org.jboss.dmr.ModelType#UNDEFINED} if there was no such parameter
         */
        ModelNode getCurrentOperationParameter(String parameterName);

        /**
         * Gets whether the operation associated with the currently executing operation step
         * {@link ModelNode#has(String) has} a parameter with the given name.
         *
         * @param parameterName the name of the parameter. Cannot be {@code null}
         * @return {@code true} if the currently executing operation has a parameter with the given name.
         */
        boolean hasCurrentOperationParameter(String parameterName);

        /**
         * Gets the type definition of the resource whose address is
         * {@link #getCurrentAddress() associated with the currently executing operation step}.
         *
         * @return the resource type. Will not be {@code null}
         */
        ResourceType getCurrentResourceType();
    }

    /**
     * Default implementation of {@link CapabilityReferenceRecorder}.
     * Derives the required capability name from the {@code baseRequirementName} provided to the constructor and from
     * the attribute value. Derives the dependent capability name from the {@code baseDependentName} provided to the
     * constructor, and, if the dependent name is dynamic, from the address of the resource currently being processed.
     */
    class DefaultCapabilityReferenceRecorder implements CapabilityReferenceRecorder {

        private final String baseRequirementName;
        private final String baseDependentName;
        private final boolean dynamicDependent;

        DefaultCapabilityReferenceRecorder(String baseRequirementName, String baseDependentName, boolean dynamicDependent) {
            this.baseRequirementName = baseRequirementName;
            this.baseDependentName = baseDependentName;
            this.dynamicDependent = dynamicDependent;
        }

        @Override
        public final void addCapabilityRequirements(RecorderContext context, Resource resource, String attributeName, String... attributeValues) {
            processCapabilityRequirement(context, attributeName, false, attributeValues);
        }

        @Override
        public final void removeCapabilityRequirements(RecorderContext context, Resource resource, String attributeName, String... attributeValues) {
            processCapabilityRequirement(context, attributeName, true, attributeValues);
        }

        private void processCapabilityRequirement(RecorderContext context, String attributeName, boolean remove, String... attributeValues) {
            String dependentName;
            if (dynamicDependent) {
                dependentName = RuntimeCapability.buildDynamicCapabilityName(baseDependentName, getDynamicDependentName(context.getCurrentAddress()));
            } else {
                dependentName = baseDependentName;
            }

            for (String attributeValue : attributeValues) {
                // This implementation does not handle null attribute values
                if (attributeValue != null) {
                    String requirementName = RuntimeCapability.buildDynamicCapabilityName(baseRequirementName, attributeValue);
                    if (remove) {
                        context.deregisterCapabilityRequirement(requirementName, dependentName);
                    } else {
                        context.registerAdditionalCapabilityRequirement(requirementName, dependentName, attributeName);
                    }
                }
            }
        }

        /**
         * Determines the dynamic portion of the dependent capability's name. Only invoked if {@code dynamicDependent}
         * is set to {@code true} in the constructor.
         * <p>
         * This base implementation returns the value of the last element in {@code currentAddress}. Subclasses that
         * wish to extract the relevant name from some other element in the address may override this.
         * </p>
         *
         * @param currentAddress the address of the resource currently being processed. Will not be {@code null}
         * @return the dynamic portion of the dependenty capability name. Cannot be {@code null}
         */
        String getDynamicDependentName(ResourceAddress currentAddress) {
            return currentAddress.getLastElement().getValue();
        }

        @Override
        public String getBaseRequirementName() {
            return baseRequirementName;
        }

    }

    /**
     * {@link CapabilityReferenceRecorder} that determines the dependent capability from
     * the {@link RecorderContext}. This assumes that the
     * {@link RecorderContext#getCurrentResourceType() resource registration associated with currently executing step}
     * will expose a {@link ResourceType#getCapabilities() capability set} including
     * one and only one capability. <strong>This recorder cannot be used with attributes associated with resources
     * that do not meet this requirement.</strong>
     */
    class ContextDependencyRecorder implements CapabilityReferenceRecorder {

        final String baseRequirementName;

        ContextDependencyRecorder(String baseRequirementName) {
            this.baseRequirementName = baseRequirementName;
        }

        /**
         * {@inheritDoc}
         *
         * @throws AssertionError if the requirements discussed in the class javadoc are not fulfilled
         */
        @Override
        public final void addCapabilityRequirements(RecorderContext context, Resource resource, String attributeName, String... attributeValues) {
            processCapabilityRequirement(context, resource, attributeName, false, attributeValues);
        }

        /**
         * {@inheritDoc}
         *
         * @throws AssertionError if the requirements discussed in the class javadoc are not fulfilled
         */
        @Override
        public final void removeCapabilityRequirements(RecorderContext context, Resource resource, String attributeName, String... attributeValues) {
            processCapabilityRequirement(context, resource, attributeName, true, attributeValues);
        }

        void processCapabilityRequirement(RecorderContext context, Resource resource, String attributeName, boolean remove, String... attributeValues) {
            RuntimeCapability cap = getDependentCapability(context);
            String dependentName = getDependentName(cap, context);
            for (String attributeValue : attributeValues) {
                //if (attributeValue != null || !cap.getDynamicOptionalRequirements().contains(baseRequirementName)) { //once we figure out what to do with optional requirements
                if (attributeValue != null) {
                    String requirementName = getRequirementName(context, resource, attributeValue);
                    if (remove) {
                        context.deregisterCapabilityRequirement(requirementName, dependentName);
                    } else {
                        context.registerAdditionalCapabilityRequirement(requirementName, dependentName, attributeName);
                    }
                }
            }
        }

        protected RuntimeCapability getDependentCapability(RecorderContext context) {
            ResourceType mrr = context.getCurrentResourceType();
            Set<RuntimeCapability> capabilities = mrr.getCapabilities();
            assert capabilities != null && capabilities.size() == 1;
            return capabilities.iterator().next();
        }

        String getDependentName(RuntimeCapability cap, RecorderContext context) {
            if (cap.isDynamicallyNamed()) {
                return cap.fromBaseCapability(context.getCurrentAddress()).getName();
            } else {
                return cap.getName();
            }
        }
        protected String getRequirementName(RecorderContext context, Resource resource, String attributeValue){
            return RuntimeCapability.buildDynamicCapabilityName(baseRequirementName, attributeValue);
        }

        @Override
        public String getBaseRequirementName() {
            return baseRequirementName;
        }
    }

    /**
     * {@link CapabilityReferenceRecorder} that determines the dependent capability from
     * the {@link RecorderContext} and any additional attributes on same resource.
     * This assumes that the {@link RecorderContext#getCurrentResourceType() resource type associated with currently executing step}
     * will expose a {@link ResourceType#getCapabilities() capability set} including
     * one and only one capability. <strong>This recorder cannot be used with attributes associated with resources
     * that do not meet this requirement.</strong>
     */
    class CompositeAttributeDependencyRecorder extends ContextDependencyRecorder {

        private ItemDefinition[] attributes;
        private RuntimeCapability capability;

        /**
         * @param baseRequirementName base requirement name
         * @param attributes list of additional attributes on same resource that are used to dynamically construct name of capability
         */
        CompositeAttributeDependencyRecorder(String baseRequirementName, ItemDefinition... attributes) {
            super(baseRequirementName);
            this.attributes = attributes;
            this.capability = null;
        }

        /**
         * @param capability dependant capability, useful when resource provides more than single capability.
         * @param baseRequirementName base requirement name
         * @param attributes          list of additional attributes on same resource that are used to dynamically construct name of capability
         */
        CompositeAttributeDependencyRecorder(RuntimeCapability capability, String baseRequirementName, ItemDefinition... attributes) {
            super(baseRequirementName);
            this.attributes = attributes;
            this.capability = capability;
        }

        @Override
        protected RuntimeCapability getDependentCapability(RecorderContext context) {
            if (capability != null) {
                return capability;
            }
            return super.getDependentCapability(context);
        }

        @Override
        protected String getRequirementName(RecorderContext context, Resource resource, String attributeValue) {
            ModelNode model = resource.getModel();
            String[] dynamicParts = new String[attributes.length + 1];
            for (int i = 0; i < attributes.length; i++) {
                ItemDefinition ad = attributes[i];
                ModelNode adVal;
                if (model.hasDefined(ad.getName())) {
                    adVal = model.get(ad.getName());
                } else {
                    adVal = ad.getDefaultValue();
                    if (adVal == null) {
                        adVal = new ModelNode();
                    }
                }
                dynamicParts[i] = adVal.asString();
            }
            dynamicParts[attributes.length] = attributeValue;
            return RuntimeCapability.buildDynamicCapabilityName(baseRequirementName, dynamicParts);
        }

    }

}
