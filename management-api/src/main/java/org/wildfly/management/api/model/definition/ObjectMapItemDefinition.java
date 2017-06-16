/*
 * JBoss, Home of Professional Open Source
 * Copyright 2016, Red Hat, Inc., and individual contributors as indicated
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

package org.wildfly.management.api.model.definition;

import org.jboss.dmr.ModelType;

/**
 * {@link ItemDefinition} for items that are maps with keys of {@link ModelType#STRING} and values that
 * are {@link ObjectTypeItemDefinition object items}
 *
 * @author Tomaz Cerar
 * @since 11.0
 */
public class ObjectMapItemDefinition extends MapItemDefinition {
    private final ObjectTypeItemDefinition valueType;

    private ObjectMapItemDefinition(final Builder builder) {
        super(builder);
        this.valueType = builder.valueType;
    }

    public final ObjectTypeItemDefinition getValueType() {
        return valueType;
    }

    /** Builder for an {@link ObjectMapItemDefinition}. */
    public static final class Builder extends MapItemDefinition.Builder<Builder, ObjectMapItemDefinition> {

        public static Builder of(final String name, final ObjectTypeItemDefinition valueType) {
            return new Builder(name, valueType);
        }

        public static Builder of(final ObjectMapItemDefinition basis) {
            return new Builder(null, basis);
        }

        public static Builder of(final String name, final ObjectMapItemDefinition basis) {
            return new Builder(name, basis);
        }

        private final ObjectTypeItemDefinition valueType;

        private Builder(final String name, final ObjectTypeItemDefinition valueType) {
            super(name);
            this.valueType = valueType;
            setElementValidator(valueType.getValidator());
            setAttributeParser(AttributeParsers.OBJECT_MAP_WRAPPED);
            setAttributeMarshaller(AttributeMarshaller.OBJECT_MAP_MARSHALLER);
        }

        private Builder(final String name, final ObjectMapItemDefinition basis) {
            super(name, basis);
            this.valueType = basis.valueType;
        }

        @Override
        public ObjectMapItemDefinition build() {
            return new ObjectMapItemDefinition(this);
        }
    }


}
