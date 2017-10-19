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

package org.jboss.as.controller.registry.bridge;

import org.wildfly.management.api.model.Resource;

/**
 * Provides a view of a management resource either via the legacy {@link org.jboss.as.controller.registry.Resource} API
 * or the current {@link Resource} API.
 *
 * @author Brian Stansberry
 */
public interface BridgeResource extends Cloneable {

    /**
     * Provide a current API view  of the resource.
     * @return the current API view. Will not be {@code null}
     */
    Resource asResource();

    /**
     * Provide a legacy API view of the resource.
     * @return the legacy API view. Will not be {@code null}
     */
    org.jboss.as.controller.registry.Resource asLegacyResource();

    /**
     * Creates and returns a copy of this resource.
     *
     * @return the clone. Will not return {@code null}
     */
    BridgeResource clone();
}
