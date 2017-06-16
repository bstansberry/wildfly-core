/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.wildfly.management.api.access;

import org.wildfly.management.api.model.definition.AttributeDefinition;
import org.wildfly.management.api.model.definition.OperationDefinition;
import org.wildfly.management.api.model.definition.ResourceTypeDefinition;

/**
 * Definition of a constraint that can be associated with a {@link ResourceTypeDefinition}, {@link OperationDefinition}
 * or an {@link AttributeDefinition}.
 *
 * @author Brian Stansberry (c) 2013 Red Hat Inc.
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
