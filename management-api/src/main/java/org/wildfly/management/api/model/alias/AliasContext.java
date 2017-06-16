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

package org.wildfly.management.api.model.alias;

import org.jboss.dmr.ModelNode;
import org.wildfly.management.api.PathAddress;
import org.wildfly.management.api.model.Resource;

/**
 * Provides contextual information when
 * {@link AliasAddressConverter#convertToTargetAddress(PathAddress, AliasContext) converting alias addresses}.
 */
@SuppressWarnings("unused")
public interface AliasContext {
    /**
     * A special operation name provided by {@link #getCurrentOperationName()} when recursive global operations
     * like {@code read-resource} are executing.
     */
    String RECURSIVE_GLOBAL_OP = "recursive-global-op";

    /**
     * Read an addressable resource from the root of the model. Reads never block. If a write action was previously performed,
     * the value read will be from an uncommitted copy of the the management model.
     * <p>
     * Note: By default the returned resource is read-only copy of the entire sub-model. In case the entire sub-model
     * is not required use {@link #readResourceFromRoot(PathAddress, boolean)} instead.
     *
     * @param address the (possibly empty) address
     * @return a read-only reference from the model
     */
    Resource readResourceFromRoot(final PathAddress address);

    /**
     * Read an addressable resource from the root of the model. Reads never block. If a write action was previously performed,
     * the value read will be from an uncommitted copy of the the management model.
     * <p>
     * Use the {@code recursive} parameter to avoid the expense of making read-only copies of large portions of the
     * resource tree. If {@code recursive} is {@code false}, the returned resource will only have placeholder resources
     * for immediate children. Those placeholder resources will return an empty
     * {@link Resource#getModel() model} and will not themselves have any children.
     * Their presence, however, allows the caller to see what immediate children exist under the target resource.
     *
     * @param address the (possibly empty) address
     * @param recursive whether the model should be read recursively or not
     * @return a read-only reference from the model
     */
    Resource readResourceFromRoot(final PathAddress address, final boolean recursive);

    /**
     * Gets the name of the operation associated with the currently executing operation step.
     * For the global operations when processing children recursively it will be a placeholder operation whose name is
     * {@link #RECURSIVE_GLOBAL_OP}.
     *
     * @return the name. Will not be {@code null}
     */
    String getCurrentOperationName();

    /**
     * Gets the address associated with the currently executing operation step.
     * @return the address. Will not be {@code null}
     */
    PathAddress getCurrentAddress();

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
}
