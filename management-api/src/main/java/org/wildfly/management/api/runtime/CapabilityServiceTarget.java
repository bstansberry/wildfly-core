/*
 * JBoss, Home of Professional Open Source
 * Copyright 2017, Red Hat, Inc., and individual contributors as indicated
 * by the @authors tag.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wildfly.management.api.runtime;

import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.management.api.capability.RuntimeCapability;

/**
 * The target of a {@link ServiceBuilder} used for {@link RuntimeCapability capability} installations.
 * The {@link CapabilityServiceBuilder} to be installed on a target should be retrieved by calling
 * the {@code addCapability} method.
 *
 * @author Tomaz Cerar (c) 2017 Red Hat Inc.
 */
public interface CapabilityServiceTarget extends ServiceTarget {

    /**
     * Get a builder which can be used to add a service to this target.
     *
     * @param capability the capability that provides the service
     * @param service the service
     * @return the builder for the service
     */
    <T> CapabilityServiceBuilder<T> addCapability(final RuntimeCapability<?> capability, final Service<T> service) throws IllegalArgumentException;
}
