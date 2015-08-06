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

package org.jboss.as.controller.registry;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.access.management.AccessConstraintDefinition;
import org.jboss.as.controller.descriptions.DescriptionProvider;

/**
 * Information about a registered {@code OperationStepHandler}.
 *
 * @author Emanuel Muckenhuber
 */
final class OperationEntryImpl implements OperationEntry {

    private final OperationStepHandler operationHandler;
    private final DescriptionProvider descriptionProvider;
    private final EntryType type;
    private final EnumSet<Flag> flags;
    private final boolean inherited;
    private final List<AccessConstraintDefinition> accessConstraints;

    OperationEntryImpl(final OperationStepHandler operationHandler, final DescriptionProvider descriptionProvider,
                       final boolean inherited, final EntryType type, final EnumSet<Flag> flags, final List<AccessConstraintDefinition> accessConstraints) {
        this.operationHandler = operationHandler;
        this.descriptionProvider = descriptionProvider;
        this.inherited = inherited;
        this.type = type;
        this.flags = flags == null ? EnumSet.noneOf(Flag.class) : flags;
        this.accessConstraints = accessConstraints == null ? Collections.<AccessConstraintDefinition>emptyList() : accessConstraints;
    }

    OperationEntryImpl(final OperationStepHandler operationHandler, final DescriptionProvider descriptionProvider, final boolean inherited, final EntryType type) {
       this(operationHandler, descriptionProvider, inherited, type, EnumSet.noneOf(Flag.class), null);
    }

    @Override
    public OperationStepHandler getOperationHandler() {
        return operationHandler;
    }

    @Override
    public DescriptionProvider getDescriptionProvider() {
        return descriptionProvider;
    }

    @Override
    public boolean isInherited() {
        return inherited;
    }

    @Override
    public EntryType getType() {
        return type;
    }

    @Override
    public EnumSet<Flag> getFlags() {
        return flags == null ? EnumSet.noneOf(Flag.class) : flags.clone();
    }

    @Override
    public List<AccessConstraintDefinition> getAccessConstraints() {
        return accessConstraints;
    }

}
