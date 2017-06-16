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

package org.wildfly.management.api.model.definition;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.wildfly.management.api.PathElement;
import org.wildfly.management.api.RunLevel;
import org.wildfly.management.api.SchemaVersion;
import org.wildfly.management.api.access.AccessConstraintDefinition;
import org.wildfly.management.api.capability.CapabilityRegistrar;
import org.wildfly.management.api.capability.RuntimeCapability;
import org.wildfly.management.api.model.ResourceFactory;
import org.wildfly.management.api.model.ResourceType;
import org.wildfly.management.api.model.alias.AliasAddressConverter;
import org.wildfly.management.api.model.validation.ResourceValidator;
import org.wildfly.management.api.runtime.RuntimeUpdateHandler;

/**
 * Type definition of a management resource.
 *
 * @author Brian Stansberry
 */
public final class ResourceTypeDefinition {

    /** Builder for a {@link ResourceTypeDefinition} */
    public static final class Builder {

        static final AttributeDefinition[] EMPTY_ATTRS = new AttributeDefinition[0];
        private static final AttributeGroupDefinition[] EMPTY_GROUPS = new AttributeGroupDefinition[0];
        private static final OperationDefinition[] EMPTY_OPS = new OperationDefinition[0];
        private static final ResourceTypeDefinition[] EMPTY_CHILDREN = new ResourceTypeDefinition[0];

        public static Builder of(PathElement path, ResourceDescriptionResolver descriptionResolver) {
            return new Builder(path, descriptionResolver);
        }

        private final PathElement path;
        private final ResourceDescriptionResolver descriptionResolver;
        private AttributeDefinition[] rootAttributes = EMPTY_ATTRS;
        private AttributeGroupDefinition[] attributeGroups = EMPTY_GROUPS;
        private RuntimeCapability[] capabilities;
        private CapabilityRegistrar capabilityRegistrar;
        private OperationDefinition[] operations = EMPTY_OPS;
        private ResourceTypeDefinition[] children = EMPTY_CHILDREN;
        private Class<? extends RuntimeUpdateHandler> addHandler;
        private boolean bootAddOnly;
        private RuntimeUpdateHandler removeHandler;
        private boolean removeRequiresAllow;
        private AliasAddressConverter aliasAddressConverter;
        private Class<? extends ResourceFactory> resourceFactory;
        private ResourceValidator validator;
        private boolean modelOnly;
        private boolean orderedChildResource;
        private Set<RuntimeCapability> incorporatingCapabilities;
        private AccessConstraintDefinition[] accessConstraints;
        private int minOccurs = 0;
        private int maxOccurs = Integer.MAX_VALUE;
        private boolean runtime;
        private RunLevel runLevel = RunLevel.SUSPENDED;
        private DeprecationData deprecationData;
        private SchemaVersion[] since;

        private Builder(PathElement path, ResourceDescriptionResolver descriptionResolver) {
            this.path = path;
            this.descriptionResolver = descriptionResolver;
        }

        /**
         * Builds a {@link ResourceTypeDefinition} that matches this builder's settings.
         * @return the definition. Will not return {@code null}
         */
        public ResourceTypeDefinition build() {
            return new ResourceTypeDefinition(this);
        }

        /**
         * Sets the attributes this resource exposes that are not part of any {@link AttributeGroupDefinition attribute group}.
         * Use {@link #setAttributeGroups(AttributeGroupDefinition...)} for attributes that are part of a group.
         * @param attributes the attributes. May be {@code null}
         * @return a builder that can be used to continue building the resource type definition
         */
        @SuppressWarnings("unused")
        public Builder setRootAttributes(AttributeDefinition... attributes) {
            this.rootAttributes = attributes == null || attributes.length == 0 ? EMPTY_ATTRS :  attributes;
            return this;
        }

        /**
         * Sets the attribute groups this resource exposes.
         * @param groups the groups. May be {@code null}
         * @return a builder that can be used to continue building the resource type definition
         */
        @SuppressWarnings({"unused", "WeakerAccess"})
        public Builder setAttributeGroups(AttributeGroupDefinition... groups) {
            this.attributeGroups = groups == null || groups.length == 0 ? EMPTY_GROUPS :  groups;
            return this;
        }

        /**
         * Sets the definitions of the operations this resource exposes. This may, but doesn't have to, include
         * definitions for {@code add} and {@code remove} operations, the definition of which can be set up
         * separately.
         *
         * @param operations the operations. May be {@code null}
         * @return a builder that can be used to continue building the resource type definition
         */
        public Builder setOperations(OperationDefinition... operations) {
            this.operations = operations == null || operations.length == 0 ? EMPTY_OPS :  operations;
            return this;
        }

        /**
         * Sets the handler to use to update the process runtime when an {@code add} operation is invoked
         * for resources of this type. This is equivalent to
         * {@link #setAddRuntimeHandler(Class, boolean) setAddRuntimeHandler(addHandler, false)}.
         *
         * @param addHandler class that provides the add handler
         */
        @SuppressWarnings("unused")
        public Builder setAddRuntimeHandler(Class<? extends RuntimeUpdateHandler> addHandler) {
            return setAddRuntimeHandler(addHandler, false);
        }

        /**
         * Sets the handler to use to update the process runtime when an {@code add} operation is invoked
         * for resources of this type.
         *
         * @param addHandler class that provides the add handler
         * @param bootOnly {@code true} if the {@code addHandler} should only be invoked when the process is
         *                             booting, with the process set to {@code reload-required} otherwise.
         */
        @SuppressWarnings("WeakerAccess")
        public Builder setAddRuntimeHandler(Class<? extends RuntimeUpdateHandler> addHandler, boolean bootOnly) {
            this.addHandler = addHandler;
            this.bootAddOnly = bootOnly;
            return this;
        }

        /**
         * Sets the handler to use to update the process runtime when a {@code remove} operation is invoked
         * for resources of this type. This is equivalent to
         * {@link #setRemoveRuntimeHandler(RuntimeUpdateHandler, boolean) setRemoveRuntimeHandler(removeHandler, false)}.
         * <p>
         * If no remove handler is configured and the resource is not {@link #setModelOnly() model-only}, then the
         * kernel will put the process in {@code reload-required} if a {@code remove} operation is invoked.
         *
         * @param removeHandler the remove handler
         */
        @SuppressWarnings("unused")
        public Builder setRemoveRuntimeHandler(RuntimeUpdateHandler removeHandler) {
            return setRemoveRuntimeHandler(removeHandler, false);
        }

        /**
         * Sets the handler to use to update the process runtime when a {@code remove} operation is invoked
         * for resources of this type.
         * <p>
         * If the {@code requireAllowHeader} parameter is set to {@code true}, then the {@code removeHandler} will
         * only be invoked if the operation includes the {@code allow-resource-service-restart} operation header
         * set to {@code true}. If the header is not present the kernel will put the process in {@code reload-required}
         * state.
         * <p>
         * If no remove handler is configured and the resource is not {@link #setModelOnly() model-only}, then the
         * kernel will put the process in {@code reload-required} if a {@code remove} operation is invoked.
         *
         * @param removeHandler the remove handler
         * @param requireAllowHeader whether the {@code allow-resource-service-restart} header is required
         *                           before the handler can be invoked.
         */
        @SuppressWarnings("WeakerAccess")
        public Builder setRemoveRuntimeHandler(RuntimeUpdateHandler removeHandler, boolean requireAllowHeader) {
            this.removeHandler = removeHandler;
            this.removeRequiresAllow = requireAllowHeader;
            return  this;
        }

        /**
         * Sets the child resource types this resource exposes.
         * @param children the children. May be {@code null}
         * @return a builder that can be used to continue building the resource type definition
         */
        public Builder setChildren(ResourceTypeDefinition... children) {
            this.children = children == null || children.length == 0 ? EMPTY_CHILDREN :  children;
            return this;
        }

        /**
         * Treat resources of this type as aliases for other resources.
         * @param aliasAddressConverter converter for converting the address for a resource of this type
         *                              to the address of the aliased resource
         * @return a builder that can be used to continue building the resource type definition
         */
        @SuppressWarnings("unused")
        public Builder markAsAlias(AliasAddressConverter aliasAddressConverter) {
            assert aliasAddressConverter != null;
            this.aliasAddressConverter = aliasAddressConverter;
            return this;
        }

        /**
         * Sets the factory for creating resources of this type. Only needed if the default resource
         * implementation is not sufficient.
         * @param factory the factory
         * @return a builder that can be used to continue building the resource type definition
         */
        @SuppressWarnings("unused")
        public Builder setResourceFactory(Class<? extends ResourceFactory> factory) {
            this.resourceFactory = factory;
            return this;
        }

        /**
         * Sets a validator for custom resource validation checks. The validator will be called as
         * part of any operation that modifies a resource of the type being defined, with the validation
         * occurring before operation execution proceeds to the stage where modifications of the process
         * runtime are performed.
         *
         * @param validator the validator
         * @return a builder that can be used to continue building the resource type definition
         */
        public Builder setValidator(ResourceValidator validator) {
            this.validator = validator;
            return this;
        }

        /**
         * Marks resources of this type as only being used for storing data in the configuration model,
         * with no operations needing to update the runtime.
         *
         * @return a builder that can be used to continue building the resource type definition
         */
        @SuppressWarnings({"unused", "WeakerAccess"})
        public Builder setModelOnly() {
            this.modelOnly = true;
            return this;
        }

        /**
         * Call to indicate that a resource is of a type where ordering matters amongst the siblings of the same type.
         * If not called, the default is {@code false}.
         *
         * @return a builder that can be used to continue building the resource type definition
         */
        public Builder setOrderedChild() {
            this.orderedChildResource = true;
            return this;
        }

        /**
         * Call to indicate that a resource is runtime-only. If not called, the default is {@code false}
         *
         * @return a builder that can be used to continue building the resource type definition
         */
        public Builder setRuntime() {
            this.runtime = true;
            return this;
        }

        /**
         * Call to indicate that a resource is runtime-only. If not called, the default is {@code false}
         *
         * @return a builder that can be used to continue building the resource type definition
         */
        public Builder setRuntime(boolean isRuntime) {
            this.runtime = isRuntime;
            return this;
        }

        /**
         * Sets the minimum {@link RunLevel} at which the process must be running before operations
         * targeting resources of this type will make any changes to the process runtime.
         * <p>
         * The default setting is {@link RunLevel#SUSPENDED}.
         *
         * @param runLevel the run level. Cannot be {@code null}
         * @return a builder that can be used to continue building the resource type definition
         */
        @SuppressWarnings("unused")
        public Builder setRunLevel(RunLevel runLevel) {
            assert runLevel != null;
            this.runLevel = runLevel;
            return this;
        }

        /**
         * Call to deprecate the resource
         *
         * @param deprecationData Information describing deprecation of this resource.
         * @return a builder that can be used to continue building the resource type definition
         * @throws IllegalStateException if the {@code deprecationData} is null
         */
        public Builder setDeprecationData(DeprecationData deprecationData) {
            this.deprecationData = deprecationData;
            return this;
        }

        /**
         * Sets the {@link SchemaVersion schema versions} when this resource type first appeared in the management API.
         * @param since the versions when the type first appeared. Use multiple versions if the type appeared on
         *              more than one branch of the version tree.
         * @return a builder that can be used to continue building the item definition
         */
        public Builder setSince(SchemaVersion... since) {
            this.since = since;
            return this;
        }

        /**
         * Sets the capabilities exposed by resources of this type.
         * @param capabilities the capabilities.
         * @return a builder that can be used to continue building the resource type definition
         */
        public Builder setCapabilities(RuntimeCapability... capabilities) {
            this.capabilities = capabilities;
            return this;
        }

        /**
         * Add possible capabilities for this resource to any that are already set.
         * @param capabilities capabilities to register
         * @return a builder that can be used to continue building the resource type definition
         */
        @SuppressWarnings("unused")
        public Builder addCapabilities(RuntimeCapability ... capabilities) {
            if (this.capabilities == null) {
                setCapabilities(capabilities);
            } else if (capabilities != null && capabilities.length > 0) {
                RuntimeCapability[] combo = Arrays.copyOf(this.capabilities, this.capabilities.length + capabilities.length);
                System.arraycopy(capabilities, 0, combo, this.capabilities.length, capabilities.length);
                setCapabilities(combo);
            }
            return this;
        }

        /**
         * Configure and handler for custom registration and removal of capabilities or requirements associated with
         * the resource as part of the overall handling of the creation of that resource in an {@code add}
         * operation or the removal of that resource in a {@code remove} operation.
         * @param registrar the registrar
         * @return a builder that can be used to continue building the resource type definition
         */
        @SuppressWarnings("unused")
        public Builder setCapabilityRegistrar(CapabilityRegistrar registrar) {
            this.capabilityRegistrar = registrar;
            return this;
        }

        /**
         * Set access constraint definitions for this resource
         * @param accessConstraints access constraint definitions for this resource
         * @return a builder that can be used to continue building the resource type definition
         */
        public Builder setAccessConstraints(AccessConstraintDefinition ... accessConstraints){
            this.accessConstraints = accessConstraints;
            return this;
        }

        /**
         * Add access constraint definitions for this resource to any that are already set.
         * @param accessConstraints access constraint definitions for this resource
         * @return a builder that can be used to continue building the resource type definition
         */
        public Builder addAccessConstraints(AccessConstraintDefinition ... accessConstraints) {
            if (this.accessConstraints == null) {
                setAccessConstraints(accessConstraints);
            } else if (accessConstraints != null && accessConstraints.length > 0) {
                AccessConstraintDefinition[] combo = Arrays.copyOf(this.accessConstraints, this.accessConstraints.length + accessConstraints.length);
                System.arraycopy(accessConstraints, 0, combo, this.accessConstraints.length, accessConstraints.length);
                setAccessConstraints(combo);
            }
            return this;
        }

        /**
         * Set the maximum number of occurrences for this resource type.
         * @param maxOccurs the maximum number of times this resource can occur
         * @return a builder that can be used to continue building the resource type definition
         */
        public Builder setMaxOccurs(final int maxOccurs){
            this.maxOccurs = maxOccurs;
            return this;
        }

        /**
         * Sets the minimum number of occurrences for this resource type.
         * @param minOccurs the minimum number of times this resource must occur
         * @return a builder that can be used to continue building the resource type definition
         */
        public Builder setMinOccurs(final int minOccurs){
            this.minOccurs = minOccurs;
            return this;
        }

        /**
         * Registers a set of capabilities that this resource does not directly provide but to which it contributes. This
         * will only include capabilities for which this resource <strong>does not</strong> control the
         * registration of the capability. Any capabilities registered by this resource should instead be declared using
         * {@link #setCapabilities(RuntimeCapability[])}.
         * <p>
         * Use of this method is only necessary if the caller wishes to specifically record capability incorporation,
         * instead of relying on the default resolution mechanism detailed in
         * {@link ResourceType#getIncorporatingCapabilities()}, or
         * if it wishes disable the default resolution mechanism and specifically declare that this resource does not
         * contribute to parent capabilities. It does the latter by passing an empty set as the {@code capabilities}
         * parameter. Passing an empty set is not necessary if this resource itself directly
         * {@link #setCapabilities(RuntimeCapability[]) provides a capability}, as it is the contract of
         * {@link ResourceType#getIncorporatingCapabilities()} that in that case it must return an empty set.
         *
         * @param  incorporatingCapabilities set of capabilities, or {@code null} if default resolution of capabilities to which this
         *                      resource contributes should be used; an empty set can be used to indicate this resource
         *                      does not contribute to capabilities provided by its parent
         *
         * @return a builder that can be used to continue building the resource type definition
         */
        @SuppressWarnings({"WeakerAccess", "UnusedReturnValue"})
        public Builder setIncorporatingCapabilities(Set<RuntimeCapability> incorporatingCapabilities) {
            this.incorporatingCapabilities = incorporatingCapabilities;
            return this;
        }

        /**
         * Adds incorporating capabilities to any that have already been set.
         * @param incorporatingCapabilities capabilities to add
         * @return a builder that can be used to continue building the resource type definition
         */
        @SuppressWarnings("unused")
        public Builder addIncorporatingCapabilities(Set<RuntimeCapability> incorporatingCapabilities) {
            if (this.incorporatingCapabilities == null) {
                setIncorporatingCapabilities(incorporatingCapabilities);
            } else if (incorporatingCapabilities != null && !incorporatingCapabilities.isEmpty()) {
                Set<RuntimeCapability> combo = new HashSet<>();
                combo.addAll(this.incorporatingCapabilities);
                combo.addAll(incorporatingCapabilities);
                setIncorporatingCapabilities(combo);
            }
            return this;
        }
    }

    private final PathElement path;
    private final ResourceDescriptionResolver descriptionResolver;
    private final AttributeDefinition[] rootAttributes;
    private final AttributeGroupDefinition[] attributeGroups;
    private final OperationDefinition[] operations;
    private final ResourceTypeDefinition[] children;
    private final RuntimeCapability[] capabilities;
    private final CapabilityRegistrar capabilityRegistrar;
    private final Set<RuntimeCapability> incorporatingCapabilities;
    private final Class<? extends ResourceFactory> resourceFactory;
    private final Class<? extends RuntimeUpdateHandler> addHandler;
    private final boolean bootAddOnly;
    private final RuntimeUpdateHandler removeHandler;
    private final boolean removeRequiresAllow;
    private final AliasAddressConverter aliasAddressConverter;
    private final ResourceValidator validator;
    private final boolean modelOnly;
    private final boolean orderedChildResource;
    private final AccessConstraintDefinition[] accessConstraints;
    private final int minOccurs;
    private final int maxOccurs;
    private final boolean runtime;
    private final RunLevel runLevel;
    private final DeprecationData deprecationData;
    private final SchemaVersion[] since;

    private ResourceTypeDefinition(Builder builder) {
        this.path = builder.path;
        this.descriptionResolver = builder.descriptionResolver;
        this.rootAttributes = builder.rootAttributes;
        this.attributeGroups = builder.attributeGroups;
        this.operations = builder.operations;
        this.addHandler = builder.addHandler;
        this.bootAddOnly = builder.bootAddOnly;
        this.removeHandler = builder.removeHandler;
        this.removeRequiresAllow = builder.removeRequiresAllow;
        this.children = builder.children;
        this.capabilities = builder.capabilities;
        this.capabilityRegistrar = builder.capabilityRegistrar;
        this.incorporatingCapabilities = builder.incorporatingCapabilities;
        this.runtime = builder.runtime;
        this.resourceFactory = builder.resourceFactory;
        this.aliasAddressConverter = builder.aliasAddressConverter;
        this.validator = builder.validator;
        this.modelOnly = builder.modelOnly;
        this.orderedChildResource = builder.orderedChildResource;
        this.accessConstraints = builder.accessConstraints;
        this.minOccurs = builder.minOccurs;
        this.maxOccurs = builder.maxOccurs;
        this.runLevel = builder.runLevel;
        this.deprecationData = builder.deprecationData;
        this.since = builder.since;
    }
}
