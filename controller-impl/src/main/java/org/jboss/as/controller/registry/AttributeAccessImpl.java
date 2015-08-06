/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

import java.util.EnumSet;
import java.util.Set;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.logging.ControllerLogger;

/**
 * Standard implementation of {@link AttributeAccess}.
 *
 * @author Brian Stansberry
 */
class AttributeAccessImpl implements AttributeAccess {

    private final AccessType access;
    private final Storage storage;
    private final OperationStepHandler readHandler;
    private final OperationStepHandler writeHandler;
    private final EnumSet<Flag> flags;
    private final AttributeDefinition definition;

    AttributeAccessImpl(final AccessType access, final Storage storage, final OperationStepHandler readHandler,
                    final OperationStepHandler writeHandler, AttributeDefinition definition, final EnumSet<Flag> flags) {
        assert access != null;
        assert storage != null;
        assert definition != null;
        this.access = access;
        this.readHandler = readHandler;
        this.writeHandler = writeHandler;
        this.storage = storage;
        this.definition = definition;
        if (flags != null && flags.contains(Flag.ALIAS)) {
            if (readHandler == null) {
                throw ControllerLogger.ROOT_LOGGER.nullVar("writeHandler");
            }
        }
        if(access == AccessType.READ_WRITE && writeHandler == null) {
            throw ControllerLogger.ROOT_LOGGER.nullVar("writeHandler");
        }
        this.flags = flags == null ? EnumSet.noneOf(Flag.class) : EnumSet.copyOf(flags);
        switch (storage) {
            case CONFIGURATION:
                this.flags.add(Flag.STORAGE_CONFIGURATION);
                this.flags.remove(Flag.STORAGE_RUNTIME);
                break;
            case RUNTIME:
                this.flags.add(Flag.STORAGE_RUNTIME);
                this.flags.remove(Flag.STORAGE_CONFIGURATION);
                break;
            default:
                throw ControllerLogger.ROOT_LOGGER.unexpectedStorage(storage);
        }
    }

    @Override
    public AccessType getAccessType() {
        return access;
    }

    @Override
    public Storage getStorageType() {
        return storage;
    }

    @Override
    public OperationStepHandler getReadHandler() {
        return readHandler;
    }

    @Override
    public OperationStepHandler getWriteHandler() {
        return writeHandler;
    }

    @Override
    public AttributeDefinition getAttributeDefinition() {
        return definition;
    }

    @Override
    public Set<Flag> getFlags() {
        return EnumSet.copyOf(flags);
    }
}
