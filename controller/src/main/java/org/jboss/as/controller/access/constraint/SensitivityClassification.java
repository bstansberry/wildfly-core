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

package org.jboss.as.controller.access.constraint;

import org.wildfly.management.api.access.SensitiveTargetAccessConstraintDefinition;

/**
 * Classification to apply to resources, attributes or operations to allow configuration
 * of whether access, reads or writes are sensitive.
 *
 * @author Brian Stansberry (c) 2013 Red Hat Inc.
 */
public class SensitivityClassification extends AbstractSensitivity {

    public static final SensitivityClassification ACCESS_CONTROL = new SensitivityClassification(SensitiveTargetAccessConstraintDefinition.ACCESS_CONTROL, true);
    public static final SensitivityClassification AUTHENTICATION_CLIENT_REF = new SensitivityClassification(SensitiveTargetAccessConstraintDefinition.AUTHENTICATION_CLIENT_REF, true);
    public static final SensitivityClassification AUTHENTICATION_FACTORY_REF = new SensitivityClassification(SensitiveTargetAccessConstraintDefinition.AUTHENTICATION_FACTORY_REF, true);
    public static final SensitivityClassification CREDENTIAL = new SensitivityClassification(SensitiveTargetAccessConstraintDefinition.CREDENTIAL, true);
    public static final SensitivityClassification DOMAIN_CONTROLLER = new SensitivityClassification(SensitiveTargetAccessConstraintDefinition.DOMAIN_CONTROLLER, true);
    public static final SensitivityClassification DOMAIN_NAMES = new SensitivityClassification(SensitiveTargetAccessConstraintDefinition.DOMAIN_NAMES, true);
    public static final SensitivityClassification ELYTRON_SECURITY_DOMAIN_REF = new SensitivityClassification(SensitiveTargetAccessConstraintDefinition.ELYTRON_SECURITY_DOMAIN_REF, true);
    public static final SensitivityClassification EXTENSIONS = new SensitivityClassification(SensitiveTargetAccessConstraintDefinition.EXTENSIONS, true);
    public static final SensitivityClassification JVM = new SensitivityClassification(SensitiveTargetAccessConstraintDefinition.JVM, true);
    public static final SensitivityClassification MANAGEMENT_INTERFACES = new SensitivityClassification(SensitiveTargetAccessConstraintDefinition.MANAGEMENT_INTERFACES, true);
    public static final SensitivityClassification MODULE_LOADING = new SensitivityClassification(SensitiveTargetAccessConstraintDefinition.MODULE_LOADING, true);
    public static final SensitivityClassification PATCHING = new SensitivityClassification(SensitiveTargetAccessConstraintDefinition.PATCHING, true);
    public static final SensitivityClassification READ_WHOLE_CONFIG = new SensitivityClassification(SensitiveTargetAccessConstraintDefinition.READ_WHOLE_CONFIG, true);
    public static final SensitivityClassification SECURITY_REALM = new SensitivityClassification(SensitiveTargetAccessConstraintDefinition.SECURITY_REALM, true);
    public static final SensitivityClassification SECURITY_REALM_REF = new SensitivityClassification(SensitiveTargetAccessConstraintDefinition.SECURITY_REALM_REF, true);
    public static final SensitivityClassification SECURITY_DOMAIN = new SensitivityClassification(SensitiveTargetAccessConstraintDefinition.SECURITY_DOMAIN, true);
    public static final SensitivityClassification SECURITY_DOMAIN_REF = new SensitivityClassification(SensitiveTargetAccessConstraintDefinition.SECURITY_DOMAIN_REF, true);
    public static final SensitivityClassification SECURITY_VAULT = new SensitivityClassification(SensitiveTargetAccessConstraintDefinition.SECURITY_VAULT, true);
    public static final SensitivityClassification SERVER_SSL = new SensitivityClassification(SensitiveTargetAccessConstraintDefinition.SERVER_SSL, true);
    public static final SensitivityClassification SERVICE_CONTAINER = new SensitivityClassification(SensitiveTargetAccessConstraintDefinition.SERVICE_CONTAINER, true);
    public static final SensitivityClassification SOCKET_BINDING_REF = new SensitivityClassification(SensitiveTargetAccessConstraintDefinition.SOCKET_BINDING_REF, true);
    public static final SensitivityClassification SOCKET_CONFIG = new SensitivityClassification(SensitiveTargetAccessConstraintDefinition.SOCKET_CONFIG, true);
    public static final SensitivityClassification SNAPSHOTS = new SensitivityClassification(SensitiveTargetAccessConstraintDefinition.SNAPSHOTS, true);
    public static final SensitivityClassification SSL_REF = new SensitivityClassification(SensitiveTargetAccessConstraintDefinition.SSL_REF, true);
    public static final SensitivityClassification SYSTEM_PROPERTY = new SensitivityClassification(SensitiveTargetAccessConstraintDefinition.SYSTEM_PROPERTY, true);

    private final boolean core;
    private final String subsystem;
    private final String name;

    /**
     * Constructor based on the management-api module's {@link SensitiveTargetAccessConstraintDefinition} that validates
     * that the {@code forCore} param matches the definition's
     * {@link SensitiveTargetAccessConstraintDefinition#isCore() isCore()} setting. The {@code forCore} param
     * should only be {@code true} for the kernel-defined classifications declared as constants in this class.
     */
    private SensitivityClassification(SensitiveTargetAccessConstraintDefinition definition, boolean forCore) {
        super(definition.isDefaultRequiresAccessPermission(), definition.isDefaultRequiresReadPermission(), definition.isDefaultRequiresWritePermission());
        this.core = definition.isCore();
        if (!this.core == forCore) {
            throw new IllegalArgumentException();
        }
        this.subsystem = definition.getSubsystemName();
        this.name = definition.getName();
    }

    /**
     * Constructor based on the management-api module's {@link SensitiveTargetAccessConstraintDefinition}.
     * Should not be used for {@link SensitiveTargetAccessConstraintDefinition#isCore() core} definitions.
     *
     * @param definition the definition on which this classification should be based
     *
     * @throws IllegalArgumentException if {@code definition} is {@link SensitiveTargetAccessConstraintDefinition#isCore() core}.
     */
    public SensitivityClassification(SensitiveTargetAccessConstraintDefinition definition) {
        this(definition, false);
    }

    public SensitivityClassification(String subsystem, String name, boolean accessDefault, boolean readDefault, boolean writeDefault) {
        super(accessDefault, readDefault, writeDefault);
        assert subsystem != null : "subsystem is null";
        assert name != null : "name is null";
        this.core = false;
        this.subsystem = subsystem;
        this.name = name;
    }

    public boolean isCore() {
        return core;
    }

    public String getSubsystem() {
        return subsystem;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SensitivityClassification that = (SensitivityClassification) o;

        return core == that.core && name.equals(that.name)
                && !(subsystem != null ? !subsystem.equals(that.subsystem) : that.subsystem != null);

    }

    @Override
    public int hashCode() {
        int result = (core ? 1 : 0);
        result = 31 * result + (subsystem != null ? subsystem.hashCode() : 0);
        result = 31 * result + name.hashCode();
        return result;
    }

    Key getKey() {
        return new Key();
    }

    class Key {

        private final SensitivityClassification sensitivity = SensitivityClassification.this;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Key thatKey = (Key) o;
            SensitivityClassification that = thatKey.sensitivity;

            return core == that.core && name.equals(that.name)
                    && !(subsystem != null ? !subsystem.equals(that.subsystem) : that.subsystem != null);

        }

        @Override
        public int hashCode() {
            int result = (core ? 1 : 0);
            result = 31 * result + (subsystem != null ? subsystem.hashCode() : 0);
            result = 31 * result + name.hashCode();
            return result;
        }

    }
}
