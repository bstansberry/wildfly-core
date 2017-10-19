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

import org.wildfly.management.api.AddressElement;
import org.wildfly.management.api._private.ManagementApiLogger;

/**
 * A {@link Resource} that supports external addition and removal of children.
 * <p>
 * <strong>Ordering of Child Resources</strong>
 * <p>
 * Beyond the ordering requirement for children discussed in the class javadoc of {@link Resource}, a mutable resource
 * must retain consistent ordering of children of type where addition and removal is supported such that
 * the {@link #getChildrenNames(String)} and {@link #getChildren(String)} methods return sets whose
 * iterators return items in the order established as they were added.
 *
 * @author Brian Stansberry
 */
public interface MutableResource extends Resource {

    /**
     * Add a child resource. The added resource will be ordered last within the set of children
     * with the same child type.
     *
     * @param address the address of the child relative to this resource's address. Cannot be {@code null}. The
     *                address element's {@link AddressElement#getKey() key} determines the child's
     *                {@link #getChildTypes() child type} while its {@link AddressElement#getValue() value}
     *                determines its {@link #getName() name}.
     * @param resource the resource to add. Cannot be {@code null}
     *
     * @throws ImmutableTypeException if the resource does not support adding a child with the address'
     *                                  {@link AddressElement#getKey() key}
     * @throws IllegalStateException if a child with the given address already exists
     */
    void addChild(final AddressElement address, final Resource resource);

    /**
     * Add a child resource, ordered within the set of children
     * with the same child type at the given index.
     *
     * @param address the address of the child relative to this resource's address. Cannot be {@code null}. The
     *                address element's {@link AddressElement#getKey() key} determines the child's
     *                {@link #getChildTypes() child type} while its {@link AddressElement#getValue() value}
     *                determines its {@link #getName() name}.
     * @param index zero based index of where to place the new child within the set of children of the same type.
     *              Any value greater than or equal to the current number of children of that type results in
     *              the child being ordered last.
     * @param resource the resource to add. Cannot be {@code null}
     *
     * @throws ImmutableTypeException if the resource does not support adding a child with the address'
     *                                  {@link AddressElement#getKey() key}
     * @throws IllegalArgumentException if {@code index} is less than zero
     * @throws IllegalStateException if a child with the given address already exists
     */
    void addChild(final AddressElement address, final int index, final Resource resource);

    /**
     * Remove the child with the given address.
     * @param address the address. Cannot be {@code null}.
     * @return the child, or {@code null} if there is no child at the given address.
     *
     * @throws ImmutableTypeException if the resource does not support removing a child with the address'
     *                                  {@link AddressElement#getKey() key}
     */
    Resource removeChild(AddressElement address);

    /**
     * Exception indicating a resource does not allow children of a particular type to be
     * externally added or removed.
     */
    final class ImmutableTypeException extends IllegalStateException {

        /**
         * Creates a new ImmutableTypeException.
         *
         * @param resourceAddress the address of the resource that does not allow addition or removal.
         * @param invalidChildType the child type whose addition or removal is not allowed.
         */
        public ImmutableTypeException(AddressElement resourceAddress, String invalidChildType) {
            super(ManagementApiLogger.ROOT_LOGGER.nonMutableChildType(resourceAddress, invalidChildType));
        }
    }
}
