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

package org.wildfly.management.api.extension;

import org.wildfly.management.api.ProcessType;
import org.wildfly.management.api.RunningMode;

/**
 * The context for registering a new extension.
 *
 * @author Brian Stansberry (c) 2017 Red Hat Inc.
 */
public interface ExtensionContext {

    /**
     * The various types of contexts in which an {@link Extension} can be asked to initialize.
     */
    enum ContextType {
        /** The {@code Extension} will be used to extend the functionality of a server instance */
        SERVER,
        /** The {@code Extension} will be for use in domain-wide profiles managed by a Host Controller.*/
        DOMAIN,
        /** The {@code Extension} will be used to extend the functionality of a Host Controller */
        HOST_CONTROLLER
    }

    /** Gets the type of this context.*/
    ContextType getType();

    /**
     * Gets the type of the current process.
     * @return the current process type. Will not be {@code null}
     */
    ProcessType getProcessType();

    /**
     * Gets the current running mode of the process.
     * @return the current running mode. Will not be {@code null}
     */
    RunningMode getRunningMode();

    /**
     * Register a new subsystem type. If the subsystem registration is deemed invalid by the time the
     * extension registration is complete, the subsystem registration will be ignored, and an
     * error message will be logged.
     * <p>
     * The new subsystem registration <em>must</em> register a handler and description for the
     * {@code add} operation at its root address.  The new subsystem registration <em>must</em> register a
     * {@code remove} operation at its root address.
     *
     * @param definition the definition of the subsystem
     *
     * @throws IllegalStateException if a subsystem with the same name has already been registered
     */
    void registerSubsystem(SubsystemDefinition definition);
}
