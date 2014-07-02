/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

import org.jboss.as.controller.capability.RuntimeCapability;

/**
 * Registry of {@link org.jboss.as.controller.capability.RuntimeCapability capabilities} available in the runtime.
 *
 * @author Brian Stansberry (c) 2014 Red Hat Inc.
 */
public interface CapabilityRegistry {

    /**
     * Registers a capability with the system. Any {@link org.jboss.as.controller.capability.RuntimeCapability#getRequirements() requirements}
     * associated with the capability will be recorded as requirements.
     *
     * @param capability  the capability. Cannot be {@code null}
     */
    void registerCapability(RuntimeCapability capability);

    /**
     * Registers an additional requirement a capability has beyond what it was aware of when {@code capability}
     * was passed to {@link #registerCapability(org.jboss.as.controller.capability.RuntimeCapability)}. Used for cases
     * where a capability optionally depends on another capability, and whether or not that requirement is needed is
     * not known when the capability is first registered.
     *
     * @param required the name of the required capability. Cannot be {@code null}
     * @param dependent the capability that requires the other capability. Cannot be {@code null}
     */
    void registerAdditionalCapabilityRequirement(String required, RuntimeCapability dependent);

    /**
     * Remove a previously registered requirement for a capability.
     *
     * @param required the name of the no longer required capability
     * @param dependent the capability that no longer has the requirement
     */
    void removeCapabilityRequirement(String required, RuntimeCapability dependent);

    /**
     * Remove a previously registered requirement for a capability.
     *
     * @param capability the capability
     */
    void removeCapability(RuntimeCapability capability);

    /**
     * Gets whether a capability with the given name is registered.
     * @param capabilityName the name of the capability. Cannot be {@code null}
     * @return {@code true} if there is a capability with the given name registered
     *
     */
    boolean hasCapability(String capabilityName);

    <T> T getCapabilityRuntimeAPI(String capabilityName, Class<T> apiType);
}
