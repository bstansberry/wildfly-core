/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.wildfly.management.api.model;

import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Set;

import org.jboss.dmr.ModelNode;
import org.wildfly.management.api.AddressElement;
import org.wildfly.management.api.OperationClientException;
import org.wildfly.management.api.ResourceAddress;
import org.wildfly.management.api._private.ControllerLoggerDuplicate;

/**
 * An addressable resource in the management model, representing a configuration model and child resources.
 * <p>Instances of this class are <b>not</b> thread-safe and need to be synchronized externally.
 *
 * @author Emanuel Muckenhuber
 */
public interface Resource extends Cloneable {

    /**
     * Get the name this resource was registered under.
     *
     * @return the resource name
     */
    String getName();

    /**
     * Get the path element this resource was registered under.
     *
     * @return the path element
     */
    AddressElement getAddressElement();

    /**
     * Get the configuration model.
     *
     * @return the model
     */
    ModelNode getModel();

    /**
     * Determine whether the model of this resource is defined.
     *
     * @return {@code true} if the local model is defined
     */
    boolean isModelDefined();

    /**
     * Determine whether the model of this resource is resolved. In a resolved model, all nodes of type
     * {@link org.jboss.dmr.ModelType#EXPRESSION} have been resolved and replaced with some other type,
     * and all {@link org.jboss.dmr.ModelType#UNDEFINED undefined} nodes for which a default value exists
     * have been replaced with a node containing that default value.
     *
     * @return {@code true} if the local model is resolved
     */
    boolean isModelResolved();

    /**
     * Determine whether this resource has a child with the given address. In case the {@code PathElement} has
     * a wildcard as value, it will determine whether this resource has any resources of a given type.
     *
     * @param element the path element
     * @return {@code true} if there is child with the given address
     */
    boolean hasChild(AddressElement element);

    /**
     * Get a single child of this resource with the given address. If no such child exists this will return {@code null}.
     *
     * @param element the path element
     * @return the resource, {@code null} if there is no such child resource
     */
    Resource getChild(AddressElement element);

    /**
     * Get a single child of this resource with the given address. If no such child exists, an exception is thrown.
     *
     * @param element the path element
     * @return the resource
     * @throws NoSuchResourceException if the child does not exist
     */
    Resource requireChild(AddressElement element);

    /**
     * Determine whether this resource has any child of a given type.
     *
     * @param childType the child type
     * @return {@code true} if there is any child of the given type
     */
    boolean hasChildren(String childType);

    /**
     * Navigate the resource tree from this resource to a child.
     *
     * @param address the address of the child, relative to this resource
     * @return the resource
     * @throws NoSuchResourceException if any resource in the path does not exist
     */
    Resource navigate(ResourceAddress address);

    /**
     * Get a list of registered child types for this resource.
     *
     * @return the registered child types
     */
    Set<String> getChildTypes();

    /**
     * Get the children names for a given type.
     *
     * @param childType the child type
     * @return the names of registered child resources
     */
    Set<String> getChildrenNames(String childType);

    /**
     * Get the children for a given type.
     *
     * @param childType the child type
     * @return the registered children
     */
    Set<Resource> getChildren(String childType);

    /**
     * Return the child types for which the order matters.
     *
     * @return {@code true} if the order of the children matters. If there are no ordered
     * children and empty set is returned. This method should never return {@code null}
     */
    Set<String> getOrderedChildTypes();

    /**
     * Gets whether this resource only exists in the runtime and has no representation in the
     * persistent configuration model.
     *
     * @return {@code true} if the resource has no representation in the
     * persistent configuration model; {@code false} otherwise
     */
    boolean isRuntime();

    /**
     * Gets whether operations against this resource will be proxied to a remote process.
     *
     * @return {@code true} if this resource represents a remote resource; {@code false} otherwise
     */
    boolean isProxy();

    class Tools {

        /**
         * A {@link ResourceFilter} that returns {@code false} for {@link Resource#isRuntime() runtime} and
         * {@link Resource#isProxy() proxy} resources.
         */
        public static final ResourceFilter ALL_BUT_RUNTIME_AND_PROXIES_FILTER = new ResourceFilter() {
            @Override
            public boolean accepts(ResourceAddress address, Resource resource) {
                return !resource.isRuntime() && !resource.isProxy();
            }
        };

        private Tools() { }

        /**
         * Recursively reads an entire resource tree, ignoring runtime-only and proxy resources, and generates
         * a DMR tree representing all of the non-ignored resources.
         *
         * @param resource the root resource
         * @return the DMR tree
         */
        public static ModelNode readModel(final Resource resource) {
            return readModel(resource, -1);
        }

        /**
         * Reads a resource tree, recursing up to the given number of levels but ignoring runtime-only and proxy resources,
         * and generates a DMR tree representing all of the non-ignored resources.
         *
         * @param resource the model
         * @param level the number of levels to recurse, or {@code -1} for no limit
         * @return the DMR tree
         */
        public static ModelNode readModel(final Resource resource, final int level) {
            return readModel(resource, level, ALL_BUT_RUNTIME_AND_PROXIES_FILTER);
        }

        /**
         * Recursively reads an entire resource tree, ignoring runtime-only and proxy resources, and generates
         * a DMR tree representing all of the non-ignored resources.  This variant can use a resource
         * registration to help identify runtime-only and proxy resources more efficiently.
         *
         * @param resource the root resource
         * @param mrr the resource registration for {@code resource}, or {@code null}
         * @return the DMR tree
         */
        public static ModelNode readModel(final Resource resource, final ResourceType mrr) {
            return readModel(resource, -1, mrr, ALL_BUT_RUNTIME_AND_PROXIES_FILTER);
        }

        /**
         * Reads a resource tree, recursing up to the given number of levels but ignoring runtime-only and proxy resources,
         * and generates a DMR tree representing all of the non-ignored resources.  This variant can use a resource
         * registration to help identify runtime-only and proxy resources more efficiently.
         *
         * @param resource the model
         * @param level the number of levels to recurse, or {@code -1} for no limit
         * @param mrr the resource registration for {@code resource}, or {@code null}
         * @return the DMR tree
         */
        public static ModelNode readModel(final Resource resource, final int level, final ResourceType mrr) {
            return readModel(resource, level, mrr, ALL_BUT_RUNTIME_AND_PROXIES_FILTER);
        }

        /**
         * Reads a resource tree, recursing up to the given number of levels but ignoring resources not accepted
         * by the given {@code filter}, and generates a DMR tree representing all of the non-ignored resources.
         *
         * @param resource the model
         * @param level the number of levels to recurse, or {@code -1} for no limit
         * @param filter a resource filter
         * @return the model
         */
        public static ModelNode readModel(final Resource resource, final int level, final ResourceFilter filter) {
            return readModel(resource, level, null, filter);
        }

        private static ModelNode readModel(final Resource resource, final int level,
                                           final ResourceType mrr, final ResourceFilter filter) {
            if (filter.accepts(ResourceAddress.EMPTY_ADDRESS, resource)) {
                return readModel(ResourceAddress.EMPTY_ADDRESS, resource, level, mrr, filter);
            } else {
                return new ModelNode();
            }
        }

        private static ModelNode readModel(final ResourceAddress address, final Resource resource, final int level,
                                           final ResourceType mrr, final ResourceFilter filter) {
            final ModelNode model = resource.getModel().clone();
            final boolean recursive = level == -1 || level > 0;
            if (recursive) {
                final int newLevel = level == -1 ? -1 : level - 1;
                Set<String> validChildTypes = mrr == null ? null : getNonIgnoredChildTypes(mrr);
                for (final String childType : resource.getChildTypes()) {
                    if (validChildTypes != null && !validChildTypes.contains(childType)) {
                        continue;
                    }
                    model.get(childType).setEmptyObject();
                    for (final Resource child : resource.getChildren(childType)) {
                        if (filter.accepts(address.append(child.getAddressElement()), resource)) {
                            ResourceType childMrr =
                                    mrr == null ? null : mrr.getChildResourceType(address.append(child.getAddressElement()));
                            model.get(childType, child.getName()).set(readModel(child, newLevel, childMrr, filter));
                        }
                    }
                }
            }
            return model;
        }

        private static Set<String> getNonIgnoredChildTypes(ResourceType mrr) {
            Set<String> result = new HashSet<>();
            for (AddressElement pe : mrr.getChildAddresses(ResourceAddress.EMPTY_ADDRESS)) {
                ResourceType childMrr = mrr.getChildResourceType(ResourceAddress.pathAddress(pe));
                if (childMrr != null && !childMrr.isRemote() && !childMrr.isRuntimeOnly()) {
                    result.add(pe.getKey());
                }
            }
            return result;
        }
    }

    /**
     * A {@link NoSuchElementException} variant that can be thrown by {@link Resource#requireChild(AddressElement)} and
     * {@link Resource#navigate(ResourceAddress)} implementations to indicate a client error when invoking a
     * management operation.
     */
    class NoSuchResourceException extends NoSuchElementException implements OperationClientException {

        private static final long serialVersionUID = -2409240663987141424L;

        public NoSuchResourceException(AddressElement childPath) {
            this(ControllerLoggerDuplicate.ROOT_LOGGER.childResourceNotFound(childPath));
        }

        public NoSuchResourceException(String message) {
            super(message);
        }

        @Override
        public ModelNode getFailureDescription() {
            return new ModelNode(getLocalizedMessage());
        }

        @Override
        public String toString() {
            return super.toString() + " [ " + getFailureDescription() + " ]";
        }
    }

}
