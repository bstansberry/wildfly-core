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

import java.util.Set;

import org.wildfly.management.api.PathAddress;
import org.wildfly.management.api.PathElement;
import org.wildfly.management.api.ProcessType;
import org.wildfly.management.api.capability.RuntimeCapability;
import org.wildfly.management.api.model.alias.AliasEntry;

/**
 * Type definition of a management resource.
 *
 * @author Brian Stansberry (c) 2017 Red Hat Inc.
 */
@SuppressWarnings("unused")
public interface ResourceType {

    /**
     * Gets the address under which we are registered.
     *
     * @return the address. Will not be {@code null}
     */
    PathAddress getPathAddress();

    /**
     * Gets the type of process in which this management resource type is registered.
     * @return the process type. Will not return {@code null}
     */
    ProcessType getProcessType();

    /**
     * Gets the registration for this resource type's parent, if there is one.
     * @return the parent, or {@code null} if {@link #getPathAddress()} returns an address with a
     *         {@link PathAddress#size() size} of {@code 0}
     */
    ResourceType getParent();

    /**
     * Gets the maximum number of times a resource of the type described by this registration
     * can occur under its parent resource (or, for a root resource, the minimum number of times it can
     * occur at all.)
     *
     * @return the minimum number of occurrences
     */
    default int getMaxOccurs() {
        PathAddress pa = getPathAddress();
        return pa.size() == 0 || !pa.getLastElement().isWildcard() ? 1 : Integer.MAX_VALUE;
    }

    /**
     * Gets the minimum number of times a resource of the type described by this registration
     * can occur under its parent resource (or, for a root resource, the number of times it can
     * occur at all.)
     *
     * @return the minimum number of occurrences
     */
    default int getMinOccurs() {
        return getPathAddress().size() == 0 ? 1 : 0;
    }

    /**
     * Gets whether resources of this type only exist in the runtime and have no representation in the
     * persistent configuration model.
     *
     * @return {@code true} if the resources have no representation in the
     * persistent configuration model; {@code false} otherwise
     *
     * @throws SecurityException if the caller does not have permission to invoke methods on a {@code ManagementResourceType}
     */
    boolean isRuntimeOnly();

    /**
     * Gets whether operations against the resource of this type will be proxied to
     * a remote process.
     *
     * @return {@code true} if this registration represents a remote resource; {@code false} otherwise
     * @throws SecurityException if the caller does not have permission to invoke methods on a {@code ManagementResourceType}
     */
    boolean isRemote();

    /**
     * Gets whether resources of this type are aliases to other resources.
     *
     * @return {@code true} if this registration represents an alias; {@code false} otherwise
     * @throws SecurityException if the caller does not have permission to invoke methods on a {@code ManagementResourceType}
     */
    boolean isAlias();

    /**
     * Gets the alias entry for this type if it is an alias.
     *
     * @return the alias entry if this registration represents an aliased resource; {@code null} otherwise
     * @throws IllegalStateException if {@link #isAlias()} returns {@code false}
     * @throws SecurityException if the caller does not have permission to invoke methods on a {@code ManagementResourceType}
     */
    AliasEntry getAliasEntry();

    /**
     * Get the names of the attributes exposed by resources of this type.
     *
     * @return the attribute names. If there are none an empty set is returned
     * @throws SecurityException if the caller does not have permission to invoke methods on a {@code ManagementResourceType}
     */
    Set<String> getAttributeNames();

    /**
     * Get the names of the attributes exposed by resources.
     *
     * @param address the address, relative to this node
     * @return the attribute names. If there are none an empty set is returned
     * @throws SecurityException if the caller does not have permission to invoke methods on a {@code ManagementResourceType}
     */
    Set<String> getAttributeNames(PathAddress address);

    /**
     * Get the names of the types of children exposed by resources of this type.
     *
     * @return the child type names. If there are none an empty set is returned
     * @throws SecurityException if the caller does not have permission to invoke methods on a {@code ManagementResourceType}
     */
    Set<String> getChildNames();

    /**
     * Get the names of the types of children exposed by resources.
     *
     * @param address the address, relative to this node
     * @return the child type names. If there are none an empty set is returned
     * @throws SecurityException if the caller does not have permission to invoke methods on a {@code ManagementResourceType}
     */
    Set<String> getChildNames(PathAddress address);

    /**
     * Gets the set of direct child address elements under the node at the passed in PathAddress
     *
     * @param address the address we want to find children for
     * @return the set of direct child elements
     * @throws SecurityException if the caller does not have permission to invoke methods on a {@code ManagementResourceType}
     */
    Set<PathElement> getChildAddresses(PathAddress address);

    /**
     * Get a sub model registration.
     *
     * @param address the address, relative to this node
     * @return the node registration, <code>null</code> if there is none
     * @throws SecurityException if the caller does not have permission to invoke methods on a {@code ManagementResourceType}
     */
    ResourceType getChildResourceType(PathAddress address);

    /**
     * Gets whether resources of this type are ordered within their parent resource.
     *
     * @return {@code true} if this child is ordered within the parent, {@code false} otherwise
     */
    boolean isOrderedChildResource();

    /**
     * Return the names of the child types registered to be ordered.
     *
     * @return the set of ordered child types, and and empty set if there are none
     */
    Set<String> getOrderedChildTypes();

    /**
     * Returns all capabilities provided by this resource. This will only include capabilities for which
     * this resource controls the registration of the capability. If any children of this resource are involved
     * in providing the capability, the registration for the children must not include the capability in the
     * value they return from this method.
     *
     * @return Set of capabilities if any registered otherwise an empty set
     *
     * @see #getIncorporatingCapabilities()
     */
    Set<RuntimeCapability> getCapabilities();

    /**
     * Returns all capabilities provided by parents of this resource, to which this resource contributes. This will
     * only include capabilities for which this resource <strong>does not</strong> control the registration of the
     * capability. Any capabilities registered by this resource will instead be included in the return value for
     * {@link #getCapabilities()}.
     * <p>
     * Often, this method will return {@code null}, which has a special meaning. A {@code null} value means
     * this resource contributes to any capabilities provided by resources higher in its branch of the resource tree,
     * with the search for such capabilities continuing through ancestor resources until:
     * <ol>
     *     <li>The ancestor has registered a capability; i.e. once a capability is identified, higher levels
     *     are not searched</li>
     *     <li>The ancestor returns a non-null value from this method; i.e. once an ancestor declares an incorporating
     *     capability or that there are no incorporating capabilities, higher levels are not searched</li>
     *     <li>The ancestor is a root resource. Child resources do not contribute to root capabilities unless
     *     they specifically declare they do so</li>
     *     <li>The ancestor has single element address whose key is {@code host}. Child resources do not contribute
     *     to host root capabilities unless they specifically declare they do so</li>
     *     <li>For subsystem resources, the ancestor resource is not provided by the subsystem. Subsystem resources
     *     do not contribute to capabilities provided by the kernel</li>
     * </ol>
     * <p>
     * A non-{@code null} value indicates no search of parent resources for capabilities should be performed, and
     * only those capabilities included in the return set should be considered as incorporating this resource
     * (or none at all if the return set is empty.)
     * <p>
     * An instance of this interface that returns a non-empty set from {@link #getCapabilities()}
     * <strong>must not</strong> return {@code null} from this method. If a resource itself provides a capability but
     * also contributes to a different capability provided by a parent, that relationship must be specifically noted
     * in the return value from this method.
     * <p>Note that providing a capability that is in turn a requirement of a parent resource's capability is not
     * the kind of "contributing" to the parent resource's capability that is being considered here. The relationship
     * between a capability and its requirements is separately tracked by the {@link RuntimeCapability} itself.  A
     * typical "contributing" resource would be one that represents a chunk of configuration directly used by the parent
     * resource's capability.
     *
     * @return set of capabilities, or {@code null} if default resolution of capabilities to which this resource
     *         contributes should be used; an empty set can be used to indicate this resource does not contribute
     *         to capabilities provided by its parent. Will not return {@code null} if {@link #getCapabilities()}
     *         returns a non-empty set.
     *
     * @see #getCapabilities()
     */
    Set<RuntimeCapability> getIncorporatingCapabilities();
}
