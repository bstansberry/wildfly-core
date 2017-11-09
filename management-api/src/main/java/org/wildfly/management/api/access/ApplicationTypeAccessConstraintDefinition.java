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
 * is related to deployments.
 *
 * @author Brian Stansberry (c) 2013 Red Hat Inc.
 */
public final class ApplicationTypeAccessConstraintDefinition implements AccessConstraintDefinition {

    public static final ApplicationTypeAccessConstraintDefinition DEPLOYMENT = new ApplicationTypeAccessConstraintDefinition("deployment", true);

    private final boolean core;

    private final String subsystem;
    private final String name;
    private final boolean application;

    public ApplicationTypeAccessConstraintDefinition(String subsystem, String name) {
        this(subsystem, name, false);
    }

    public ApplicationTypeAccessConstraintDefinition(String subsystem, String name, boolean application) {
        assert subsystem != null : "subsystem is null";
        assert name != null : "name is null";
        this.subsystem = subsystem;
        this.name = name;
        this.application = application;
        this.core = false;
    }

    private ApplicationTypeAccessConstraintDefinition(String name, boolean application) {
        this.core = true;
        this.subsystem = null;
        this.name = name;
        this.application = application;
    }

    @Override
    public String getType() {
        return "application";
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isCore() {
        return core;
    }

    @Override
    public String getSubsystemName() {
        return core ? null : subsystem;
    }

    public boolean isDefaultApplication() {
        return application;
    }

    @Override
    public int hashCode() {
        int result = (core ? 1 : 0);
        result = 31 * result + (subsystem != null ? subsystem.hashCode() : 0);
        result = 31 * result + name.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        ApplicationTypeAccessConstraintDefinition that = (ApplicationTypeAccessConstraintDefinition) obj;

        return core == that.core && name.equals(that.name)
                && !(subsystem != null ? !subsystem.equals(that.subsystem) : that.subsystem != null);
    }
}
