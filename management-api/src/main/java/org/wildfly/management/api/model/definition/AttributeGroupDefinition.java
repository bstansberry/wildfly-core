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

import org.wildfly.management.api.SchemaVersion;

/**
 * Definition of a conceptually related group of attributes.
 *
 * @author Brian Stansberry
 */
public final class AttributeGroupDefinition {

    private final String name;
    private final AttributeDefinition[] attributes;
    private final boolean nonPersistent;
    private final SchemaVersion since;

    private AttributeGroupDefinition(Builder builder) {
        this.name = builder.name;
        assert  builder.attributes.length > 0;
        this.attributes = builder.attributes;
        this.nonPersistent = builder.nonPersistent;
        this.since = builder.since;
    }

    public String getName() {
        return name;
    }

    public boolean isNonPersistent() {
        return nonPersistent;
    }

    public SchemaVersion getSince() {
        return since;
    }

    /** Builder for creating an {@link org.wildfly.management.api.model.definition.AttributeGroupDefinition} */
    public static final class Builder {

        /**
         * Creates a builder for an attribute group with the given name.
         * @param name the name of the attribute group. Cannot be {@code null}
         * @return  a builder that can be used to continue building the attribute group definition
         */
        public static Builder of(String name) {
            return new Builder(name);
        }

        /**
         * Creates a builder for an attribute group initially configured the same as an existing group.
         * @param basis the existing group. Cannot be {@code null}
         * @return  a builder that can be used to continue building the attribute group definition
         */
        public static Builder of(AttributeGroupDefinition basis) {
            return new Builder(null, basis);
        }

        /**
         * Creates a builder for an attribute group initially configured the same as an existing group.
         * @param name the name of the attribute group, or {@code null} if the name should be taken from {@code basis}
         * @param basis the existing group. Cannot be {@code null}
         * @return  a builder that can be used to continue building the attribute group definition
         */
        public static Builder of(String name, AttributeGroupDefinition basis) {
            return new Builder(name);
        }

        private final String name;
        private boolean nonPersistent;
        private SchemaVersion since;
        private AttributeDefinition[] attributes = ResourceTypeDefinition.Builder.EMPTY_ATTRS;

        private Builder(String name) {
            this.name = name;
        }

        private Builder(String name, AttributeGroupDefinition basis) {
            this.name = name == null ? basis.name : name;
            this.nonPersistent = basis.nonPersistent;
            this.since = basis.since;
            this.attributes = basis.attributes;
        }

        /**
         * Builds the {@link AttributeGroupDefinition}.
         * @return the definition. Will not return {@code null}
         */
        public AttributeGroupDefinition build() {
            return new AttributeGroupDefinition(this);
        }

        /**
         * Sets the attributes that are associated with this group.
         * @param attributes the attributes. Cannot be {@code null} or empty
         * @return a builder that can be used to continue building the resource type definition
         */
        public Builder setAttributes(AttributeDefinition... attributes) {
            assert attributes != null;
            assert attributes.length > 0;
            this.attributes = attributes;
            return this;
        }

        public Builder setNonPersistent(boolean nonPersistent) {
            this.nonPersistent = nonPersistent;
            return this;
        }

        public Builder setSince(SchemaVersion since) {
            this.since = since;
            return this;
        }

    }
}
