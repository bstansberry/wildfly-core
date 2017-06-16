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

package org.wildfly.management.api.model.definition;

/**
 * Describes what must be restarted in order to cause changes made to a management resource to take effect in
 * the runtime. Example changes would be invoking an {@code add} or {@code remove} operation to add or remove the
 * resource, or invoking the {@code write-attribute} or {@code undefine-attribute} operations to modify one of its
 * attributes.
 *
 * @author Brian Stansberry
 */
public enum RestartLevel {

    /** Nothing needs to be restarted; changes take effect without any restart.  */
    NONE,
    /**
     * Services provided by the resource must be restarted for the change to take effect,
     * along with those that depend on those services, directly or transitively. If this level
     * is supported, the services will only be restarted if the management operation making the
     * change includes the {@code allow-resource-service-restart} operation header with a value of {@code true}.
     * Otherwise, a process {@code reload} must be done for the change to take effect.*/
    RESOURCE_SERVICES,
    /**
     * A restart of all managed services must be done (via a {@code reload} for the change to take effect, but
     * a full process restart is not necessary.
     */
    ALL_SERVICES,
    /** The entire JVM must be shut down and a new JVM launched for the change to take effect. */
    JVM
}
