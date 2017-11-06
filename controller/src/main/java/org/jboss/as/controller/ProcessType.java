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

package org.jboss.as.controller;

/**
 * Holds the possible process types. This is used to identify what type of server we are running in.
 * {@link Extension}s can use this information to decide whether certain resources, operations or attributes
 * need to be present. {@link OperationStepHandler}s can use this to determine how to handle operations.
 */
public enum ProcessType {
    DOMAIN_SERVER(org.wildfly.management.api.ProcessType.DOMAIN_SERVER),
    EMBEDDED_SERVER(org.wildfly.management.api.ProcessType.EMBEDDED_SERVER),
    STANDALONE_SERVER(org.wildfly.management.api.ProcessType.STANDALONE_SERVER),
    HOST_CONTROLLER(org.wildfly.management.api.ProcessType.HOST_CONTROLLER),
    EMBEDDED_HOST_CONTROLLER(org.wildfly.management.api.ProcessType.EMBEDDED_HOST_CONTROLLER),
    APPLICATION_CLIENT(org.wildfly.management.api.ProcessType.APPLICATION_CLIENT),
    SELF_CONTAINED(org.wildfly.management.api.ProcessType.SELF_CONTAINED);

    private final org.wildfly.management.api.ProcessType wrapped;

    ProcessType(final org.wildfly.management.api.ProcessType wrapped) {
        this.wrapped = wrapped;
    }

    /**
     * Returns true if the process is one of the server variants.
     *
     * @return Returns <tt>true</tt> if the process is a server. Returns <tt>false</tt> otherwise.
     */
    public boolean isServer() {
        return wrapped.isServer();
    }

    /**
     * Returns true if the process is a host controller,
     *
     * @return Returns <tt>true</tt> if the process is a hostcontroller. Returns <tt>false</tt> otherwise.
     */
    public boolean isHostController() {
        return !isServer();
    }

    /**
     * Returns true if the process is a managed domain process.
     *
     * @return Returns <tt>true</tt> if the process is a managed domain process. Returns <tt>false</tt> otherwise.
     */
    public boolean isManagedDomain() {
        return wrapped.isManagedDomain();
    }

    /**
     * Gets the underlying non-legacy representation of this process type.
     * @return the process type. Will not return {@code null}
     */
    public org.wildfly.management.api.ProcessType asNonLegacyProcessType() {
        return wrapped;
    }
}
