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

package org.wildfly.management.api.runtime;

import org.wildfly.management.api.model.definition.ResourceDescriptionResolver;

/**
 * Type definition of a management resource.
 *
 * @author Brian Stansberry
 */
public final class OverrideResourceDefinition {

    public static final class Builder {

        public static Builder of(String name, ResourceDescriptionResolver descriptionResolver) {
            return new Builder(name, descriptionResolver);
        }

        private final String name;
        private final ResourceDescriptionResolver descriptionResolver;

        private Builder(String name, ResourceDescriptionResolver descriptionResolver) {
            this.name = name;
            this.descriptionResolver = descriptionResolver;
        }

        public OverrideResourceDefinition build() {
            return new OverrideResourceDefinition(this);
        }
    }

    private final String name;
    private final ResourceDescriptionResolver descriptionResolver;

    private OverrideResourceDefinition(Builder builder) {
        this.name = builder.name;
        this.descriptionResolver = builder.descriptionResolver;
    }
}
