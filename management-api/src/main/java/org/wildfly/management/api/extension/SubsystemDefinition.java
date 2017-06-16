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

package org.wildfly.management.api.extension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import org.jboss.staxmapper.XMLElementWriter;
import org.wildfly.management.api.SchemaVersion;
import org.wildfly.management.api.model.definition.ResourceTypeDefinition;

/**
 * Definition of a subsystem, encapsulating the {@link ResourceTypeDefinition} of its resource tree in the main
 * configuration model, one or more {@link SchemaVersion versions of its schema}, an optional definition of
 * a resource tree in the deployment section of a server's model, and an optional custom xml marshaller of its
 * current schema version.
 *
 * @author Brian Stansberry
 */
public final class SubsystemDefinition {

    public static final class Builder {

        /**
         * Creates a new builder for a subsystem with the given container definition and schema versions.
         * @param containerDefinition the container definition. Cannot be {@code null}
         * @param schemaVersions the schema versions, ordered by when they were released in final form
         * @return the builder
         */
        public static Builder of(ResourceTypeDefinition containerDefinition, SchemaVersion... schemaVersions) {
            return new Builder(containerDefinition, schemaVersions);
        }
        private final ResourceTypeDefinition containerDefinition;
        private final List<SchemaVersion> schemaVersions;
        private ResourceTypeDefinition deploymentDefinition;
        private Supplier<XMLElementWriter<SubsystemMarshallingContext>> marshallerSupplier;
        private XMLElementWriter<SubsystemMarshallingContext> marshaller;

        private Builder(ResourceTypeDefinition containerDefinition, SchemaVersion... schemaVersions) {
            this.containerDefinition = containerDefinition;
            this.schemaVersions = Arrays.asList(schemaVersions == null ? new SchemaVersion[0] : schemaVersions);
        }

        public Builder addSchemaVersion(SchemaVersion version) {
            schemaVersions.add(version);
            return this;
        }

        public Builder setDeploymentDefinition(ResourceTypeDefinition deploymentDefinition) {
            this.deploymentDefinition = deploymentDefinition;
            return this;
        }

        public Builder setMarshallerSupplier(Supplier<XMLElementWriter<SubsystemMarshallingContext>> marshallerSupplier) {
            if (marshallerSupplier != null) {
                this.marshaller = null;
            }
            this.marshallerSupplier = marshallerSupplier;
            return this;
        }

        public Builder setMarshaller(XMLElementWriter<SubsystemMarshallingContext> marshaller) {
            if (marshaller != null) {
                this.marshallerSupplier = null;
            }
            this.marshaller = marshaller;
            return this;
        }

        public SubsystemDefinition build() {
            return new SubsystemDefinition(this);
        }
    }

    private final ResourceTypeDefinition profileDefinition;
    private final ResourceTypeDefinition deploymentDefinition;
    private final List<SchemaVersion> schemaVersions;
    private final Supplier<XMLElementWriter<SubsystemMarshallingContext>> marshallerSupplier;
    private XMLElementWriter<SubsystemMarshallingContext> marshaller;

    private SubsystemDefinition(Builder builder) {
        this.profileDefinition = builder.containerDefinition;
        this.deploymentDefinition = builder.deploymentDefinition;
        this.schemaVersions = Collections.unmodifiableList(Arrays.asList(builder.schemaVersions.toArray(new SchemaVersion[builder.schemaVersions.size()])));
        this.marshallerSupplier = builder.marshallerSupplier;
        this.marshaller = builder.marshaller;
    }

    /**
     * Returns the definition of the subsystem's management model that would appear as a child of a server,
     * domain wide or host controller profile resource.
     *
     * @return the definition. Will not return {@code null}
     */
    public ResourceTypeDefinition getProfileDefinition() {
        return profileDefinition;
    }

    /**
     * Returns the definition of the subsystem's management model that would appear as a child of a server
     * deployment resource, if any.
     *
     * @return the definition. May be {@code null}
     */
    public ResourceTypeDefinition getDeploymentDefinition() {
        return deploymentDefinition;
    }

    /**
     * Gets the schema versions for the subsystem, ordered by when the versions were released in final form.
     * @return the schema versions. Will not be {@code null} or empty
     */
    public List<SchemaVersion> getSchemaVersions() {
        return schemaVersions;
    }

    /**
     * Gets the {@link XMLElementWriter} that should be used to marshal the subsystem's
     * configuration to XML. If marshaller is provided the kernel should use a standard marshaller
     * for the subsystem.
     *
     * @return the marshaller supplier. May be {@code null}
     */
    public XMLElementWriter<SubsystemMarshallingContext> getMarshaller() {
        return marshallerSupplier != null ? marshallerSupplier.get() : marshaller;
    }
}
