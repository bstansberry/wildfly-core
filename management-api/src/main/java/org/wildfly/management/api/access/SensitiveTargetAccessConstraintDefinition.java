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

/**
 * {@link AccessConstraintDefinition} for access control constraints related to whether a target or action
 * is security-sensitive.
 *
 * @author Brian Stansberry (c) 2013 Red Hat Inc.
 */
public final class SensitiveTargetAccessConstraintDefinition implements AccessConstraintDefinition {

    public static final SensitiveTargetAccessConstraintDefinition ACCESS_CONTROL = new SensitiveTargetAccessConstraintDefinition("access-control", true, true, true);
    public static final SensitiveTargetAccessConstraintDefinition AUTHENTICATION_CLIENT_REF = new SensitiveTargetAccessConstraintDefinition("authentication-client-ref", true, true, true);
    public static final SensitiveTargetAccessConstraintDefinition AUTHENTICATION_FACTORY_REF = new SensitiveTargetAccessConstraintDefinition("authentication-factory-ref", true, true, true);
    public static final SensitiveTargetAccessConstraintDefinition CREDENTIAL = new SensitiveTargetAccessConstraintDefinition("credential", false, true, true);
    public static final SensitiveTargetAccessConstraintDefinition DOMAIN_CONTROLLER = new SensitiveTargetAccessConstraintDefinition("domain-controller", false, false, true);
    public static final SensitiveTargetAccessConstraintDefinition DOMAIN_NAMES = new SensitiveTargetAccessConstraintDefinition("domain-names", false, false, true);
    public static final SensitiveTargetAccessConstraintDefinition ELYTRON_SECURITY_DOMAIN_REF = new SensitiveTargetAccessConstraintDefinition("elytron-security-domain-ref", true, true, true);
    public static final SensitiveTargetAccessConstraintDefinition EXTENSIONS = new SensitiveTargetAccessConstraintDefinition("extensions", false, false, true);
    public static final SensitiveTargetAccessConstraintDefinition JVM = new SensitiveTargetAccessConstraintDefinition("jvm", false, false, true);
    public static final SensitiveTargetAccessConstraintDefinition MANAGEMENT_INTERFACES = new SensitiveTargetAccessConstraintDefinition("management-interfaces", false, false, true);
    public static final SensitiveTargetAccessConstraintDefinition MODULE_LOADING = new SensitiveTargetAccessConstraintDefinition("module-loading", false, false, true);
    public static final SensitiveTargetAccessConstraintDefinition PATCHING = new SensitiveTargetAccessConstraintDefinition("patching", false, false, true);
    public static final SensitiveTargetAccessConstraintDefinition READ_WHOLE_CONFIG = new SensitiveTargetAccessConstraintDefinition("read-whole-config", false, true, true);
    public static final SensitiveTargetAccessConstraintDefinition SECURITY_DOMAIN = new SensitiveTargetAccessConstraintDefinition("security-domain", true, true, true);
    public static final SensitiveTargetAccessConstraintDefinition SECURITY_DOMAIN_REF = new SensitiveTargetAccessConstraintDefinition("security-domain-ref", true, true, true);
    public static final SensitiveTargetAccessConstraintDefinition SECURITY_REALM = new SensitiveTargetAccessConstraintDefinition("security-realm", true, true, true);
    public static final SensitiveTargetAccessConstraintDefinition SECURITY_REALM_REF = new SensitiveTargetAccessConstraintDefinition("security-realm-ref", true, true, true);
    public static final SensitiveTargetAccessConstraintDefinition SECURITY_VAULT = new SensitiveTargetAccessConstraintDefinition("security-vault", false, true, true);
    public static final SensitiveTargetAccessConstraintDefinition SERVER_SSL = new SensitiveTargetAccessConstraintDefinition("server-ssl", true, true, true);
    public static final SensitiveTargetAccessConstraintDefinition SERVICE_CONTAINER = new SensitiveTargetAccessConstraintDefinition("service-container", false, false, true);
    public static final SensitiveTargetAccessConstraintDefinition SOCKET_BINDING_REF = new SensitiveTargetAccessConstraintDefinition("socket-binding-ref", false, false, false);
    public static final SensitiveTargetAccessConstraintDefinition SOCKET_CONFIG = new SensitiveTargetAccessConstraintDefinition("socket-config", false, false, true);
    public static final SensitiveTargetAccessConstraintDefinition SNAPSHOTS = new SensitiveTargetAccessConstraintDefinition("snapshots", false, false, false);
    public static final SensitiveTargetAccessConstraintDefinition SSL_REF = new SensitiveTargetAccessConstraintDefinition("ssl-ref", true, true, true);
    public static final SensitiveTargetAccessConstraintDefinition SYSTEM_PROPERTY = new SensitiveTargetAccessConstraintDefinition("system-property", false, false, true);

    private final boolean core;
    private final String subsystem;
    private final String name;
    /** If {@code true} access (awareness) is considered sensitive by default*/
    private final boolean defaultRequiresAccessPermission;
    /** If {@code true} reading is considered sensitive by default*/
    private final boolean defaultRequiresReadPermission;
    /** If {@code true} writing is considered sensitive by default*/
    private final boolean defaultRequiresWritePermission;


    private SensitiveTargetAccessConstraintDefinition(String name, boolean accessDefault, boolean readDefault, boolean writeDefault) {
        this.core = true;
        this.subsystem = null;
        this.name = name;
        this.defaultRequiresAccessPermission = accessDefault;
        this.defaultRequiresReadPermission = readDefault;
        this.defaultRequiresWritePermission = writeDefault;
    }

    public SensitiveTargetAccessConstraintDefinition(String subsystem, String name, boolean accessDefault, boolean readDefault, boolean writeDefault) {
        assert subsystem != null : "subsystem is null";
        assert name != null : "name is null";
        this.core = false;
        this.subsystem = subsystem;
        this.name = name;
        this.defaultRequiresAccessPermission = accessDefault;
        this.defaultRequiresReadPermission = readDefault;
        this.defaultRequiresWritePermission = writeDefault;
    }

    public boolean isCore() {
        return core;
    }

    @Override
    public String getSubsystemName() {
        return subsystem;
    }

    public String getName() {
        return name;
    }

    @Override
    public String getType() {
        return "sensitive";
    }

    public boolean isDefaultRequiresAccessPermission() {
        return defaultRequiresAccessPermission;
    }

    public boolean isDefaultRequiresReadPermission() {
        return defaultRequiresReadPermission;
    }

    public boolean isDefaultRequiresWritePermission() {
        return defaultRequiresWritePermission;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SensitiveTargetAccessConstraintDefinition that = (SensitiveTargetAccessConstraintDefinition) o;

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
