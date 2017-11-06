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

import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.wildfly.management.api.model.ResourceType;

/**
 * Type definition for a management resource type, allowing it to be viewed as either a
 * {@link ResourceType} or as a legacy {@link ManagementResourceRegistration}.
 *
 * @author Brian Stansberry
 */
public interface BridgeResourceType  {

    ManagementResourceRegistration asManagementResourceRegistration();

    ResourceType asResourceType();

}
