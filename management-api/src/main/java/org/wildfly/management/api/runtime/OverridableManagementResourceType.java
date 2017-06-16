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

package org.wildfly.management.api.runtime;

import org.wildfly.management.api.PathElement;
import org.wildfly.management.api.model.ResourceType;

/**
 * View onto a {@link ResourceType} that has a {@link PathElement#WILDCARD_VALUE wildcard registration} that
 * allows that base type to be overridden for specific concrete addresses.
 *
 * @author Brian Stansberry
 */
@SuppressWarnings("unused")
public interface OverridableManagementResourceType extends ResourceType {

    /**
     * Register a specifically named resource that overrides this {@link PathElement#WILDCARD_VALUE wildcard registration}
     * by adding additional attributes or child types.
     *
     * @param overrideDefinition the definition of the override resource type. Cannot be {@code null}
     *
     * @return a resource registration which may be used to add attributes, operations and sub-models
     *
     * @throws IllegalArgumentException if either parameter is null or if there is already a registration under {@code name}
     * @throws SecurityException if the caller does not have permission to invoke methods on a {@code ManagementResourceType}
     */
    ResourceType registerOverrideModel(final OverrideResourceDefinition overrideDefinition);

    /**
     * Unregister a specifically named resource that overrides a {@link PathElement#WILDCARD_VALUE wildcard registration}
     * by adding additional attributes, operations or child types.
     *
     * @param name the specific name of the resource. Cannot be {@code null} or {@link PathElement#WILDCARD_VALUE}
     *
     * @throws SecurityException if the caller does not have permission to invoke methods on a {@code ManagementResourceType}
     */
    void unregisterOverrideModel(final String name);
}
