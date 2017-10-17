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

package org.wildfly.management.api.model;

import org.jboss.msc.service.ServiceRegistry;
import org.wildfly.management.api.AddressElement;
import org.wildfly.management.api.ResourceAddress;

/**
 * Factory for creating a {@link Resource} during execution of a management operation that is adding a resource.
 * <p>
 * Implementations of this interface must be public and must provide a public no-argument constructor.
 * <p>
 * A {@code ResourceFactory} will be invoked during a stage of management operation execution that happens before
 * any runtime modifications are performed (e.g. by any {@link org.wildfly.management.api.runtime.RuntimeUpdateHandler}
 * associated with the resource type.)
 *
 * @author Brian Stansberry
 */
@FunctionalInterface
public interface ResourceFactory {

    /**
     * Create a resource.
     *
     * @param context contextual object to provide information about the conditions that have led to this request.
     * @return the resource. Cannot return {@code null}
     */
    Resource createResource(Context context);

    /** Contextual object provided to a {@link ResourceFactory} */
    interface Context {

        /**
         * Create a default resource that matches the {@link #getCurrentAddress() current address}.
         * Useful if this factory wishes to wrap a standard resource and simply provide extended behavior.
         *
         * @return the resource. Will not return {@code null}.
         *
         * @see DelegatingResource
         */
        Resource createDefaultResource();

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
         * Get the service registry.  The returned registry must not be used to remove services and if an attempt is made
         * to call {@code ServiceController.setMode(REMOVE)} on a {@code ServiceController} returned from this registry an
         * {@code IllegalStateException} will be thrown. Callers also <strong>MUST NOT</strong> make any modifications to
         * any {@code ServiceController} or {@code Service} implementation obtained via this registry. A
         * {@link ResourceFactory} and any {@link Resource} it creates should only perform reads.
         *
         * @return the service registry
         */
        ServiceRegistry getServiceRegistry();

    }
}
