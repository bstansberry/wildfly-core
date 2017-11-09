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

package org.wildfly.management.api.access;

import org.wildfly.management.api.model.definition.AttributeDefinition;
import org.wildfly.management.api.model.definition.OperationDefinition;
import org.wildfly.management.api.model.definition.ResourceTypeDefinition;

/**
 * Definition of a constraint that can be associated with a {@link ResourceTypeDefinition}, {@link OperationDefinition}
 * or an {@link AttributeDefinition}.
 *
 * @author Brian Stansberry (c) 2017 Red Hat Inc.
 */
public interface AccessConstraintDefinition {

    /**
     * Get the name of the constraint
     *
     * @return the name
     */
    String getName();

    /**
     * Get the type of constraint
     *
     * @return the type of constraint
     */
    String getType();

    /**
     * Gets whether the definition is provided by the core management system.
     * @return {@code true} if the definition is provided by the core; {@code false} if it
     *         is provided by a subsystem
     */
    boolean isCore();

    /**
     * Gets the name of the subsystem that provides this definition, it is not {@link #isCore() core}.
     *
     * @return the subsystem name, or {@code null} if {@link #isCore()}
     */
    String getSubsystemName();
}
