/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.subsystem.resource.capability;

import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.capability.BinaryCapabilityNameResolver;
import org.jboss.as.controller.capability.QuaternaryCapabilityNameResolver;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.capability.TernaryCapabilityNameResolver;
import org.jboss.as.controller.capability.UnaryCapabilityNameResolver;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.wildfly.service.descriptor.BinaryServiceDescriptor;
import org.wildfly.service.descriptor.NullaryServiceDescriptor;
import org.wildfly.service.descriptor.QuaternaryServiceDescriptor;
import org.wildfly.service.descriptor.TernaryServiceDescriptor;
import org.wildfly.service.descriptor.UnaryServiceDescriptor;
import org.wildfly.subsystem.resource.ResourceModelResolver;
import org.wildfly.subsystem.resource.ResourceResolver;
import org.wildfly.subsystem.resource.SimpleResource;
import org.wildfly.subsystem.service.ServiceDependency;

/**
 * A {@link CapabilityReference} specialization that records requirements of a resource, rather than an attribute.
 * @param <T> the requirement type
 * @author Paul Ferraro
 */
public interface ResourceCapabilityReference<T> extends CapabilityReference<T>, ResourceModelResolver<ServiceDependency<T>> {

    /**
     * Returns the resolver of the requirement name from a path address.
     * @return a requirement name resolver
     */
    Function<PathAddress, String[]> getRequirementNameResolver();

    /**
     * Registers capability requirements for the specified resource.
     * @param context the context
     * @param resource the resource on which requirements are gathered
     */
    void addCapabilityRequirements(OperationContext context, Resource resource);

    /**
     * Unregisters capability requirements for the specified resource.
     * @param context the context
     * @param resource the resource on which requirements were gathered
     */
    void removeCapabilityRequirements(OperationContext context, Resource resource);

    @Override
    default void addCapabilityRequirements(OperationContext context, Resource resource, String attributeName, String... attributeValues) {
        this.addCapabilityRequirements(context, resource);
    }

    @Override
    default void removeCapabilityRequirements(OperationContext context, Resource resource, String attributeName, String... attributeValues) {
        this.removeCapabilityRequirements(context, resource);
    }

    /**
     * Creates a builder for a new reference between the specified capability and the specified requirement.
     * @param capability the capability referencing the specified requirement
     * @param requirement the requirement of the specified capability
     */
    static <T> Builder<T> builder(RuntimeCapability<Void> capability, NullaryServiceDescriptor<T> requirement) {
        return new DefaultBuilder<>(capability, NaryServiceDescriptor.of(requirement), ResourceCapabilityServiceDescriptorReference.EMPTY_RESOLVER);
    }

    /**
     * Creates a builder for a new reference between the specified capability and the specified requirement.
     * By default, the requirement name will resolve against the path of the current resource.
     * @param capability the capability referencing the specified requirement
     * @param requirement the requirement of the specified capability
     */
    static <T> NaryBuilder<T> builder(RuntimeCapability<Void> capability, UnaryServiceDescriptor<T> requirement) {
        return new DefaultBuilder<>(capability, NaryServiceDescriptor.of(requirement), UnaryCapabilityNameResolver.DEFAULT);
    }

    /**
     * Creates a builder for a new reference between the specified capability and the specified requirement.
     * By default, the requirement name will resolve against the paths of the parent and current resources, respectively.
     * @param capability the capability referencing the specified requirement
     * @param requirement the requirement of the specified capability
     * @param requirementNameResolver function for resolving the dynamic components of the requirement name
     */
    static <T> NaryBuilder<T> builder(RuntimeCapability<Void> capability, BinaryServiceDescriptor<T> requirement) {
        return new DefaultBuilder<>(capability, NaryServiceDescriptor.of(requirement), BinaryCapabilityNameResolver.PARENT_CHILD);
    }

    /**
     * Creates a builder for a new reference between the specified capability and the specified requirement.
     * By default, the requirement name will resolve against the paths of the grandparent, parent, and current resources, respectively.
     * @param capability the capability referencing the specified requirement
     * @param requirement the requirement of the specified capability
     * @param requirementNameResolver function for resolving the dynamic components of the requirement name
     */
    static <T> NaryBuilder<T> builder(RuntimeCapability<Void> capability, TernaryServiceDescriptor<T> requirement) {
        return new DefaultBuilder<>(capability, NaryServiceDescriptor.of(requirement), TernaryCapabilityNameResolver.GRANDPARENT_PARENT_CHILD);
    }

    /**
     * Creates a builder for a new reference between the specified capability and the specified requirement.
     * By default, the requirement name will resolve against the paths of the great-grandparent, grandparent, parent, and current resources, respectively.
     * @param capability the capability referencing the specified requirement
     * @param requirement the requirement of the specified capability
     * @param requirementNameResolver function for resolving the dynamic components of the requirement name
     */
    static <T> NaryBuilder<T> builder(RuntimeCapability<Void> capability, QuaternaryServiceDescriptor<T> requirement) {
        return new DefaultBuilder<>(capability, NaryServiceDescriptor.of(requirement), QuaternaryCapabilityNameResolver.GREATGRANDPARENT_GRANDPARENT_PARENT_CHILD);
    }

    interface Builder<T> {
        /**
         * Only reference the provided capability if value of the specified attribute complies with the specified predicate.
         * @param attribute an attribute of the resource to use for conditional registration
         * @param predicate conditionally determines whether to require this capability, depending on the resolve value of the specified attribute
         * @return a reference to this builder
         */
        default Builder<T> when(AttributeDefinition attribute, Predicate<ModelNode> predicate) {
            ResourceModelResolver<ModelNode> resolver = attribute::resolveModelAttribute;
            return this.when(resolver, predicate);
        }

        /**
         * Only reference the provided capability if value of the specified attribute complies with the specified predicate.
         * @param resolver a resolver of the resource to use for conditional registration
         * @param predicate conditionally determines whether to require this capability, depending on the resolve value
         * @return a reference to this builder
         */
        <V> Builder<T> when(ResourceResolver<V> resolver, Predicate<V> predicate);

        /**
         * Builds the configured capability reference recorder.
         * @return a capability reference recorder
         */
        ResourceCapabilityReference<T> build();
    }

    interface NaryBuilder<T> extends Builder<T> {
        /**
         * Overrides the default requirement name resolver.
         * @param requirementNameResolver a capability name resolver
         * @return a reference to this builder
         */
        Builder<T> withRequirementNameResolver(Function<PathAddress, String[]> requirementNameResolver);
    }

    static class DefaultBuilder<T> implements NaryBuilder<T> {
        private final RuntimeCapability<Void> capability;
        private final NaryServiceDescriptor<T> requirement;

        private Function<PathAddress, String[]> requirementNameResolver;
        private BiPredicate<OperationContext, Resource> predicate = ResourceCapabilityServiceDescriptorReference.ALWAYS;

        DefaultBuilder(RuntimeCapability<Void> capability, NaryServiceDescriptor<T> requirement, Function<PathAddress, String[]> defaultRequirementNameResolver) {
            this.capability = capability;
            this.requirement = requirement;
            this.requirementNameResolver = defaultRequirementNameResolver;
        }

        @Override
        public Builder<T> withRequirementNameResolver(Function<PathAddress, String[]> requirementNameResolver) {
            this.requirementNameResolver = requirementNameResolver;
            return this;
        }

        @Override
        public <V> Builder<T> when(ResourceResolver<V> resolver, Predicate<V> predicate) {
            this.predicate = new BiPredicate<>() {
                @Override
                public boolean test(OperationContext context, Resource resource) {
                    try {
                        return predicate.test(resolver.resolve(context, resource));
                    } catch (OperationFailedException e) {
                        // OFE would be due to an expression that can't be resolved right now (OperationContext.Stage.MODEL).
                        // Very unlikely an expression is used and that it uses a resolution source not available in MODEL.
                        return true;
                    }
                }
            };
            return this;
        }

        @Override
        public ResourceCapabilityReference<T> build() {
            return new ResourceCapabilityServiceDescriptorReference<>(this.capability, this.requirement, this.requirementNameResolver, this.predicate);
        }
    }

    abstract class AbstractResourceCapabilityServiceDescriptorReference<T> extends AbstractServiceDescriptorReference<T> implements org.wildfly.subsystem.resource.capability.ResourceCapabilityReference<T> {
        private final Function<PathAddress, String[]> requirementNameResolver;

        public AbstractResourceCapabilityServiceDescriptorReference(RuntimeCapability<Void> capability, NaryServiceDescriptor<T> requirement, Function<PathAddress, String[]> requirementNameResolver) {
            super(capability, requirement);
            this.requirementNameResolver = requirementNameResolver;
        }

        @Override
        public Function<PathAddress, String[]> getRequirementNameResolver() {
            return this.requirementNameResolver;
        }

        @Override
        public String[] getRequirementPatternSegments(String name, PathAddress address) {
            String[] segments = this.requirementNameResolver.apply(address);
            for (int i = 0; i < segments.length; ++i) {
                String segment = segments[i];
                if (segment.charAt(0) == '$') {
                    segments[i] = segment.substring(1);
                }
            }
            return segments;
        }
    }

    class ResourceCapabilityServiceDescriptorReference<T> extends AbstractResourceCapabilityServiceDescriptorReference<T> {
        static final Function<PathAddress, String[]> EMPTY_RESOLVER = address -> new String[0];
        private static final BiPredicate<OperationContext, Resource> ALWAYS = (context, resource) -> true;

        private final BiPredicate<OperationContext, Resource> predicate;

        ResourceCapabilityServiceDescriptorReference(RuntimeCapability<Void> capability, NaryServiceDescriptor<T> requirement, Function<PathAddress, String[]> requirementNameResolver, BiPredicate<OperationContext, Resource> predicate) {
            super(capability, requirement, requirementNameResolver);
            this.predicate = predicate;
        }

        @Override
        public ServiceDependency<T> resolve(OperationContext context, Resource resource) throws OperationFailedException {
            if (this.predicate.test(context, resource)) {
                Map.Entry<String, String[]> resolved = this.resolve(context, resource, "");
                return ServiceDependency.on(resolved.getKey(), this.getRequirement().getType(), resolved.getValue());
            }
            return ServiceDependency.empty();
        }

        @Override
        public ServiceDependency<T> resolve(OperationContext context, ModelNode model) throws OperationFailedException {
            return this.resolve(context, new SimpleResource(model));
        }

        @Override
        public Map.Entry<String, String[]> resolve(OperationContext context, Resource resource, String value) {
            return this.getRequirement().resolve(this.getRequirementNameResolver().apply(context.getCurrentAddress()));
        }

        @Override
        public void addCapabilityRequirements(OperationContext context, Resource resource) {
            if (this.predicate.test(context, resource)) {
                context.registerAdditionalCapabilityRequirement(this.resolveRequirementName(context), this.resolveDependentName(context), null);
            }
        }

        @Override
        public void removeCapabilityRequirements(OperationContext context, Resource resource) {
            if (this.predicate.test(context, resource)) {
                context.deregisterCapabilityRequirement(this.resolveRequirementName(context), this.resolveDependentName(context));
            }
        }

        private String resolveRequirementName(OperationContext context) {
            Map.Entry<String, String[]> resolved = this.resolve(context, null, "");
            return (resolved.getValue().length > 0) ? RuntimeCapability.buildDynamicCapabilityName(resolved.getKey(), resolved.getValue()) : resolved.getKey();
        }
    }
}
