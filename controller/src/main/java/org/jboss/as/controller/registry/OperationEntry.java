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
import java.util.List;

import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.access.management.AccessConstraintDefinition;
import org.jboss.as.controller.descriptions.DescriptionProvider;

/**
 * Information about a registered {@code OperationStepHandler}.
 *
 * @author Emanuel Muckenhuber
 */
public interface OperationEntry {

    /** Types of operation entries */
    enum EntryType {
        /** An entry for an operation that is part of the publicly accessible management interface */
        PUBLIC,
        /** An entry for an operation this is meant for internal use only, not for use by end users */
        PRIVATE
    }

    /** Flags to indicate special characteristics of an operation */
    enum Flag {
        /** Operation only reads, does not modify */
        READ_ONLY,
        /** The operation modifies the configuration and can be applied to the runtime without requiring a restart */
        RESTART_NONE,
        /** The operation modifies the configuration but can only be applied to the runtime via a full jvm restart */
        RESTART_JVM,
        /** The operation modifies the configuration but can only be applied to the runtime via a restart of all services;
         *  however it does not require a full jvm restart */
        RESTART_ALL_SERVICES,
        /** The operation modifies the configuration but can only be applied to the runtime via a restart of services,
         *  associated with the affected resource, but does not require a restart of all services or a full jvm restart */
        RESTART_RESOURCE_SERVICES,
        /** A domain or host-level operation that should be pushed to the servers even if the default behavior
         *  would indicate otherwise */
        DOMAIN_PUSH_TO_SERVERS,
        /** A host-level operation that should only be executed on the HostController and not on the servers,
         * even if the default behavior would indicate otherwise */
        HOST_CONTROLLER_ONLY,
        /** A domain-level operation that should only be executed on the master HostController and not on the slaves,
         * even if the default behavior would indicate otherwise */
        MASTER_HOST_CONTROLLER_ONLY,
        /** Operations with this flag do not affect the mode or change the installed services. The main intention for
         * this is to only make RUNTIME_ONLY methods on domain mode servers visible to end users. */
        RUNTIME_ONLY
    }

    /**
     * Gets the handler for the operation.
     * @return the handler. Will not be {@code null}
     */
    OperationStepHandler getOperationHandler();

    /**
     * Gets the provider of the description of the operation.
     * @return the description provider. Will not be {@code null}
     */
    DescriptionProvider getDescriptionProvider();

    /**
     * Gets whether this entry will be inherited by child resource registrations
     * @return {@code true} if the entry will be inherited
     */
    boolean isInherited();

    /**
     * Gets the type of the entry.
     * @return the type. Will not be {@code null}
     */
    EntryType getType();

    /**
     * Gets any flags associated with the entry.
     * @return the flags. Will not be {@code null}
     */
    EnumSet<Flag> getFlags();

    /**
     * Gets the definitions of any access contraints associated with operation
     * @return the access constraint definitions. Will not be {@code null} but may be empty
     */
    List<AccessConstraintDefinition> getAccessConstraints();
}
