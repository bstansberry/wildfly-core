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

package org.wildfly.management.api.model.definition;

import org.jboss.dmr.ModelType;
import org.wildfly.management.api.model.validation.ModelTypeValidator;

/**
 * {@link ItemDefinition} for items whose values are lists with elements that are of a simple ModelType.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 * @author Richard Achmatowicz (c) 2012 RedHat Inc.
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a>
 *
 * @see SimpleListItemDefinition for an alternative that allows a higher degree of configurability.
 */
public class PrimitiveListItemDefinition extends ListItemDefinition {
    private final ModelType valueType;

    PrimitiveListItemDefinition(final ListItemDefinition.Builder builder, ModelType valueType) {
        super(builder);
        this.valueType = valueType;
    }

    /** Builder for a {@link PrimitiveListItemDefinition}. */
    public static class Builder extends ListItemDefinition.Builder<Builder, PrimitiveListItemDefinition> {

        public static Builder of(final String name, final ModelType valueType) {
            return new Builder(name, valueType);
        }

        public static Builder of(final PrimitiveListItemDefinition basis) {
            return new Builder(null, basis);
        }

        public static Builder of(final String name, final PrimitiveListItemDefinition basis) {
            return new Builder(name, basis);
        }

        private final ModelType valueType;

        Builder(final String name, final ModelType valueType) {
            super(name);
            this.valueType = valueType;
            setElementValidator(new ModelTypeValidator(valueType));
        }

        Builder(final String name, final PrimitiveListItemDefinition basis) {
            super(basis);
            this.valueType = basis.valueType;
        }

        @Override
        public PrimitiveListItemDefinition build() {
            return new PrimitiveListItemDefinition(this, valueType);
        }
    }
}
