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
