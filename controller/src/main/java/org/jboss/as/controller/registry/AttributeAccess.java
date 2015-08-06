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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationStepHandler;

/**
 * Information about handling an attribute in a {@link ManagementResourceRegistration}.
 *
 * @author Brian Stansberry
 */
public interface AttributeAccess {

    /**
     * Get the access type.
     *
     * @return the access type
     */
    AccessType getAccessType();

    /**
     * Get the storage type.
     *
     * @return the storage type
     */
    Storage getStorageType();

    /**
     * Get the read handler.
     *
     * @return the read handler, <code>null</code> if not defined
     */
    OperationStepHandler getReadHandler();

    /**
     * Get the write handler.
     *
     * @return the write handler, <code>null</code> if not defined.
     */
    OperationStepHandler getWriteHandler();

    /**
     * Gets the definition of the attribute.
     *
     * @return the definition. Will not be {@code null}
     */
    AttributeDefinition getAttributeDefinition();

    /**
     * Gets the flags associated with this attribute.
     * @return the flags. Will not return {@code null}
     */
    Set<Flag> getFlags();

    /**
     * Indicates how an attributed is accessed.
     */
    enum AccessType {
        /** A read-only attribute, which can be either {@code Storage.CONFIGURATION} or {@code Storage.RUNTIME} */
        READ_ONLY("read-only", false),
        /** A read-write attribute, which can be either {@code Storage.CONFIGURATION} or {@code Storage.RUNTIME} */
        READ_WRITE("read-write", true),
        /** A read-only {@code Storage.RUNTIME} attribute */
        METRIC("metric", false);

        private final String label;
        private final boolean writable;

        AccessType(final String label, final boolean writable) {
            this.label = label;
            this.writable = writable;
        }

        @Override
        public String toString() {
            return label;
        }

        private static final Map<String, AccessType> MAP;

        static {
            final Map<String, AccessType> map = new HashMap<String, AccessType>();
            for (AccessType element : values()) {
                final String name = element.getLocalName();
                if (name != null) map.put(name, element);
            }
            MAP = map;
        }

        public static AccessType forName(String localName) {
            return MAP.get(localName);
        }

        private String getLocalName() {
            return label;
        }

        public boolean isWritable() {
            return writable;
        }
    }

    /**
     * Indicates whether an attribute is derived from the persistent configuration or is a purely runtime attribute.
     */
    enum Storage {
        /**
         * An attribute whose value is stored in the persistent configuration.
         * The value may also be stored in runtime services.
         */
        CONFIGURATION("configuration"),
        /**
         * An attribute whose value is only stored in runtime services, and
         * isn't stored in the persistent configuration.
         */
        RUNTIME("runtime");

        private final String label;

        Storage(final String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }

    }

    /** Flags to indicate special characteristics of an attribute */
    enum Flag {
        /** A modification to the attribute can be applied to the runtime without requiring a restart */
        RESTART_NONE,
        /** A modification to the attribute can only be applied to the runtime via a full jvm restart */
        RESTART_JVM,
        /** A modification to the attribute can only be applied to the runtime via a restart of all services,
         *  but does not require a full jvm restart */
        RESTART_ALL_SERVICES,
        /** A modification to the attribute can only be applied to the runtime via a restart of services,
         *  associated with the attribute's resource, but does not require a restart of all services or a full jvm restart */
        RESTART_RESOURCE_SERVICES,
        /**
         * An attribute whose value is stored in the persistent configuration.
         * The value may also be stored in runtime services.
         */
        STORAGE_CONFIGURATION,
        /**
         * An attribute whose value is only stored in runtime services, and
         * isn't stored in the persistent configuration.
         */
        STORAGE_RUNTIME,
        /**
         * The attribute is an alias to something else
         */
        ALIAS
    }

}
