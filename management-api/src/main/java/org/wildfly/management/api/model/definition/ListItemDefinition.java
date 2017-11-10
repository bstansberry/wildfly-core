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
import org.wildfly.management.api.model.validation.ParameterValidator;

/**
 * Defining characteristics of an {@link ItemDefinition item} whose {@link ItemDefinition#getType() type}
 * is {@link ModelType#LIST}.
 *
 * @author Brian Stansberry
 */
public abstract class ListItemDefinition<ELEMENT extends ItemDefinition> extends CollectionItemDefinition<ELEMENT> {

    private final boolean allowDuplicates;

    ListItemDefinition(ListItemDefinition.Builder<?, ?, ELEMENT> builder) {
        super(builder);
        this.allowDuplicates = builder.isAllowDuplicates();
    }

    @Override
    public final boolean isAllowDuplicates() {
        return allowDuplicates;
    }

    /** Builder for creating a {@link ListItemDefinition} */
    public abstract static class Builder<BUILDER extends Builder, ITEM extends ListItemDefinition<ELEMENT>, ELEMENT extends ItemDefinition>
            extends CollectionItemDefinition.Builder<BUILDER, ITEM, ELEMENT> {

        private Boolean allowDuplicates;

        Builder(String attributeName, ELEMENT elementDefinition) {
            super(attributeName, ModelType.LIST, elementDefinition);
            this.setAttributeParser(AttributeParser.STRING_LIST);
        }

        Builder(ITEM basis) {
            this(null, basis);
        }

        Builder(String name, ITEM basis) {
            super(name, basis);
            this.allowDuplicates = basis.isAllowDuplicates();
            this.setAttributeParser(AttributeParser.STRING_LIST);
        }

        /**
         * Sets an overall validator for the list. This is only relevant to operation parameter
         * use cases. The item definition produced by this builder will directly enforce the item's
         * {@link ItemDefinition#isRequired()} () required} and
         * {@link ItemDefinition#isAllowExpression() allow expression} settings, so the given {@code validator}
         * need not concern itself with those validations.
         * <p>
         * <strong>Usage note:</strong> Providing a validator should be limited to atypical custom validation cases.
         * Standard validation against the item's definition (i.e. checking for correct type, size within
         * min and max, presence of duplicates and validity of each element) will be automatically handled without
         * requiring provision of a validator.
         *
         * @param validator the validator. {@code null} is allowed
         * @return a builder that can be used to continue building the item definition
         */
        @SuppressWarnings({"WeakerAccess", "UnusedReturnValue", "unused"})
        public final BUILDER setListValidator(ParameterValidator validator) {
            return super.setCollectionValidator(validator);
        }

        /**
         * Sets whether duplicate elements in the list are allowed. If set to {@code false}, the list
         * functions similarly to an ordered set.
         *
         * @param allowDuplicates {@code false} if duplicates are not allowed
         * @return a builder that can be used to continue building the item definition
         */
        @SuppressWarnings({"unchecked", "unused"})
        public final BUILDER setAllowDuplicates(boolean allowDuplicates) {
            this.allowDuplicates = allowDuplicates;
            return (BUILDER) this;
        }

        private boolean isAllowDuplicates() {
            return allowDuplicates == null || allowDuplicates;
        }
    }
}
