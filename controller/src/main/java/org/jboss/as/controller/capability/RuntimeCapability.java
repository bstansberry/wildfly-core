/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

package org.jboss.as.controller.capability;

import java.util.function.Function;

import org.jboss.as.controller.PathAddress;
import org.jboss.msc.service.ServiceName;
import org.wildfly.management.api.capability.Capability;

/**
 * A capability exposed in a running WildFly process.
 *
 * @param <T> the type of the runtime API object exposed by the capability
 *
 * @author Brian Stansberry (c) 2014 Red Hat Inc.
 */
public class RuntimeCapability<T> extends CapabilityBase  {

    //todo remove, here only for binary compatibility of elytron subsystem, drop once it is in.
    public static String buildDynamicCapabilityName(String baseName, String dynamicNameElement) {
        return buildDynamicCapabilityName(baseName, new String[]{dynamicNameElement});
    }

    //only here for binary compatibility, remove once elytron subsystem lands
    public RuntimeCapability<T> fromBaseCapability(String dynamicElement) {
        return fromBaseCapability(new String[]{dynamicElement});
    }

    //end remove

    /**
     * Constructs a full capability name from a static base name and a dynamic element.
     *
     * @param baseName the base name. Cannot be {@code null}
     * @param dynamicNameElement  the dynamic portion of the name. Cannot be {@code null}
     * @return the full capability name. Will not return {@code null}
     */
    public static String buildDynamicCapabilityName(String baseName, String ... dynamicNameElement) {
        assert baseName != null;
        assert dynamicNameElement != null;
        assert dynamicNameElement.length > 0;
        StringBuilder sb = new StringBuilder(baseName);
        for (String part:dynamicNameElement){
            sb.append(".").append(part);
        }
        return sb.toString();
    }

    private final org.wildfly.management.api.capability.RuntimeCapability<T> wrapped;

    private RuntimeCapability(org.wildfly.management.api.capability.RuntimeCapability<T> wrapped) {
        this.wrapped = wrapped;
    }

    /**
     * Gets the underlying non-legacy representation of this capability.
     * @return the capability. Will not return {@code null}
     */
    @Override
    public org.wildfly.management.api.capability.RuntimeCapability<T> asNonLegacyCapability() {
        return wrapped;
    }

    /**
     * Gets the name of the service provided by this capability, if there is one.
     *
     * @return the name of the service. Will not be {@code null}
     *
     * @throws IllegalArgumentException if the capability does not provide a service
     */
    public ServiceName getCapabilityServiceName() {
        return wrapped.getCapabilityServiceName();
    }

    /**
     * Gets the name of service provided by this capability.
     *
     * @param serviceValueType the expected type of the service's value. Only used to provide validate that
     *                         the service value type provided by the capability matches the caller's
     *                         expectation. May be {@code null} in which case no validation is performed
     *
     * @return the name of the service. Will not be {@code null}
     *
     * @throws IllegalArgumentException if the capability does not provide a service or if its value type
     *                                  is not assignable to {@code serviceValueType}
     */
    public ServiceName getCapabilityServiceName(Class<?> serviceValueType) {
        return wrapped.getCapabilityServiceName(serviceValueType);
    }

    /**
     * Gets the name of the service provided by this capability, if there is one. Only usable with
     * {@link #isDynamicallyNamed() dynamically named} capabilities.
     *
     * @param dynamicNameElements the dynamic portion of the capability name. Cannot be {@code null}
     *
     * @return the name of the service. Will not be {@code null}
     *
     * @throws IllegalArgumentException if the capability does not provide a service
     * @throws AssertionError if {@link #isDynamicallyNamed()} does not return {@code true}
     */
    public ServiceName getCapabilityServiceName(String... dynamicNameElements) {
        return getCapabilityServiceName(null, dynamicNameElements);
    }

    /**
     * Gets the name of the service provided by this capability, if there is one. Only usable with
     * {@link #isDynamicallyNamed() dynamically named} capabilities.
     *
     * @param address Path address for which service name is calculated from Cannot be {@code null}
     *
     * @return the name of the service. Will not be {@code null}
     *
     * @throws IllegalArgumentException if the capability does not provide a service
     * @throws AssertionError if {@link #isDynamicallyNamed()} does not return {@code true}
     */
    public ServiceName getCapabilityServiceName(PathAddress address) {
        return getCapabilityServiceName(address, null);
    }

    /**
     * Gets the name of service provided by this capability.
     *
     * @param dynamicNameElement the dynamic portion of the capability name. Cannot be {@code null}
     * @param serviceValueType the expected type of the service's value. Only used to provide validate that
     *                         the service value type provided by the capability matches the caller's
     *                         expectation. May be {@code null} in which case no validation is performed
     *
     * @return the name of the service. Will not be {@code null}
     *
     * @throws IllegalArgumentException if the capability does not provide a service or if its value type
     *                                  is not assignable to {@code serviceValueType}
     * @throws IllegalStateException if {@link #isDynamicallyNamed()} does not return {@code true}
     */
    public ServiceName getCapabilityServiceName(String dynamicNameElement, Class<?> serviceValueType) {
        return getCapabilityServiceName(serviceValueType, dynamicNameElement);
    }

    public ServiceName getCapabilityServiceName(Class<?> serviceValueType, String... dynamicNameElements) {
        return fromBaseCapability(dynamicNameElements).getCapabilityServiceName(serviceValueType);
    }

    /**
     * Gets the name of service provided by this capability.
     *
     * @param address the path from which dynamic portion of the capability name is calculated from. Cannot be {@code null}
     * @param serviceValueType the expected type of the service's value. Only used to provide validate that
     *                         the service value type provided by the capability matches the caller's
     *                         expectation. May be {@code null} in which case no validation is performed
     *
     * @return the name of the service. Will not be {@code null}
     *
     * @throws IllegalArgumentException if the capability does not provide a service or if its value type
     *                                  is not assignable to {@code serviceValueType}
     * @throws IllegalStateException if {@link #isDynamicallyNamed()} does not return {@code true}
     */
    public ServiceName getCapabilityServiceName(PathAddress address, Class<?> serviceValueType) {
        return fromBaseCapability(address).getCapabilityServiceName(serviceValueType);
    }

    /**
     * Gets the valid type to pass to {@link #getCapabilityServiceName(Class)}.
     *
     * @return  the valid type. May be {@code null} if this capability does not provide a
     *          service
     */
    public Class<?> getCapabilityServiceValueType() {
        return wrapped.getCapabilityServiceValueType();
    }

    /**
     * Object encapsulating the API exposed by this capability to other capabilities that require it, if it does
     * expose such an API.
     *
     * @return the API object, or {@code null} if the capability exposes no API to other capabilities
     */
    public T getRuntimeAPI() {
        return wrapped.getRuntimeAPI();
    }

    /**
     * Gets whether this capability can be registered at more than one point within the same
     * overall scope.
     *
     * @return {@code true} if the capability can legally be registered in more than one location in the same scope;
     *         {@code false} if an attempt to do this should result in an exception
     */
    public boolean isAllowMultipleRegistrations() {
        return wrapped.isAllowMultipleRegistrations();
    }

    /**
     * Creates a fully named capability from a {@link #isDynamicallyNamed() dynamically named} base
     * capability. Capability providers should use this method to generate fully named capabilities in logic
     * that handles dynamically named resources.
     *
     * @param dynamicElement the dynamic portion of the full capability name. Cannot be {@code null} or empty
     * @return the fully named capability.
     *
     * @throws AssertionError if {@link #isDynamicallyNamed()} returns {@code false}
     */
    public RuntimeCapability<T> fromBaseCapability(String ... dynamicElement) {
        return new RuntimeCapability<T>(wrapped.fromBaseCapability(dynamicElement));

    }

    /**
     * Creates a fully named capability from a {@link #isDynamicallyNamed() dynamically named} base
     * capability. Capability providers should use this method to generate fully named capabilities in logic
     * that handles dynamically named resources.
     *
     * @param path the dynamic portion of the full capability name. Cannot be {@code null} or empty
     * @return the fully named capability.
     *
     * @throws AssertionError if {@link #isDynamicallyNamed()} returns {@code false}
     */
    public RuntimeCapability<T> fromBaseCapability(PathAddress path) {
        return new RuntimeCapability<T>(wrapped.fromBaseCapability(path.asResourceAddress()));
    }

    @Override
    final Capability getCapability() {
        return wrapped;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@code true} if {@code o} is the same type as this object and its {@link #getName() name} is equal
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RuntimeCapability that = (RuntimeCapability) o;

        return wrapped.equals(that.wrapped);

    }

    @Override
    public int hashCode() {
        return wrapped.hashCode();
    }

    /**
     * {@inheritDoc}
     *
     * @return the value returned by {@link #getName()}
     */
    @Override
    public String toString() {
        return getName();
    }

    /**
     * Builder for a {@link RuntimeCapability}.
     *
     * @param <T> the type of the runtime API object exposed by the capability
     */
    public static class Builder<T> {

        /**
         * Create a builder for a non-dynamic capability with no custom runtime API.
         * @param name the name of the capability. Cannot be {@code null} or empty.
         * @return the builder
         */
        public static Builder<Void> of(String name) {
            return new Builder<Void>(name, false, null);
        }

        /**
         * Create a builder for a possibly dynamic capability with no custom runtime API.
         * @param name the name of the capability. Cannot be {@code null} or empty.
         * @param dynamic {@code true} if the capability is a base capability for dynamically named capabilities
         * @return the builder
         */
        public static Builder<Void> of(String name, boolean dynamic) {
            return new Builder<Void>(name, dynamic, null);
        }

        /**
         * Create a builder for a non-dynamic capability that installs a service with the given value type.
         *
         * @param name  the name of the capability. Cannot be {@code null} or empty.
         * @param serviceValueType the value type of the service installed by the capability
         * @return the builder
         */
        public static Builder<Void> of(String name, Class<?> serviceValueType) {
            return new Builder<Void>(name, false, null).setServiceType(serviceValueType);
        }

        /**
         * Create a builder for a possibly dynamic capability that installs a service with the given value type.
         *
         * @param name  the name of the capability. Cannot be {@code null} or empty.
         * @param dynamic {@code true} if the capability is a base capability for dynamically named capabilities
         * @param serviceValueType the value type of the service installed by the capability
         * @return the builder
         */
        public static Builder<Void> of(String name, boolean dynamic, Class<?> serviceValueType) {
            return new Builder<Void>(name, dynamic, null).setServiceType(serviceValueType);
        }

        /**
         * Create a builder for a non-dynamic capability that provides the given custom runtime API.
         * @param name the name of the capability. Cannot be {@code null} or empty.
         * @param runtimeAPI the custom API implementation exposed by the capability
         * @param <T> the type of the runtime API object exposed by the capability
         * @return the builder
         */
        public static <T> Builder<T> of(String name, T runtimeAPI) {
            return new Builder<T>(name, false, runtimeAPI);
        }

        /**
         * Create a builder for a possibly dynamic capability that provides the given custom runtime API.
         * @param name the name of the capability. Cannot be {@code null} or empty.
         * @param dynamic {@code true} if the capability is a base capability for dynamically named capabilities
         * @param runtimeAPI the custom API implementation exposed by the capability
         * @param <T> the type of the runtime API object exposed by the capability
         * @return the builder
         */
        public static <T> Builder<T> of(String name, boolean dynamic, T runtimeAPI) {
            return new Builder<T>(name, dynamic, runtimeAPI);
        }

        private final org.wildfly.management.api.capability.RuntimeCapability.Builder<T> wrapped;

        private Builder(String baseName, boolean dynamic, T runtimeAPI) {
            this.wrapped = org.wildfly.management.api.capability.RuntimeCapability.Builder.of(baseName, dynamic, runtimeAPI);
        }

        /**
         * Sets that the capability installs a service with the given value type.
         * @param type the value type of the service installed by the capability. May be {@code null}
         * @return the builder
         */
        public Builder<T> setServiceType(Class<?> type) {
            wrapped.setServiceType(type);
            return this;
        }

        /**
         * Adds the names of other capabilities that this capability requires. The requirement
         * for these capabilities will automatically be registered when this capability is registered.
         *
         * @param requirements the capability names
         * @return the builder
         */
        public Builder<T> addRequirements(String... requirements) {
            wrapped.addRequirements(requirements);
            return this;
        }

        /**
         * Sets whether this capability can be registered at more than one point within the same
         * overall scope.
         * @param allowMultipleRegistrations {@code true} if the capability can legally be registered in more than
         *                                               one location in the same scope; {@code false} if an attempt
         *                                               to do this should result in an exception
         * @return the builder
         */
        public Builder<T> setAllowMultipleRegistrations(boolean allowMultipleRegistrations) {
            wrapped.setAllowMultipleRegistrations(allowMultipleRegistrations);
            return this;
        }

        /*
         * Sets dynamic name mapper, can be used for cases when you need to customize dynamic name
         *
         * @param mapper function
         * @return the builder
         */
        public Builder<T> setDynamicNameMapper(Function<PathAddress,String[]> mapper) {
            wrapped.setDynamicNameMapper(resourceAddress -> mapper.apply(PathAddress.pathAddress(resourceAddress)));
            return this;
        }

        /**
         * Builds the capability.
         *
         * @return the capability. Will not return {@code null}
         */
        public RuntimeCapability<T> build() {
            org.wildfly.management.api.capability.RuntimeCapability<T> toWrap = wrapped.build();
            return new RuntimeCapability<>(toWrap);
        }
    }
}
