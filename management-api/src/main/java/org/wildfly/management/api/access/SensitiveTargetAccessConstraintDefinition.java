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
