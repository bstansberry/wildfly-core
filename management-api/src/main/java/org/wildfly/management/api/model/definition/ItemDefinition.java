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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.management.api.SchemaVersion;
import org.wildfly.management.api.capability.RuntimeCapability;
import org.wildfly.management.api.model.validation.BytesValidator;
import org.wildfly.management.api.model.validation.ModelTypeValidator;
import org.wildfly.management.api.model.validation.ParameterValidator;
import org.wildfly.management.api.model.validation.StringLengthValidator;

/**
 * Common characteristics of an {@link AttributeDefinition attribute} in a {@link ResourceTypeDefinition},
 * a parameter or reply value type item in an {@link OperationDefinition}, or a field within a
 * {@link ModelType#OBJECT} variant of any of the above.
 * <p>
 * <strong>Design note:</strong> Attributes, operation parameters, reply value types and fields therein
 * share many common definitional aspects. A class like this one could conceivably be a base for definition
 * classes for each of those. However, in addition to those kinds of relationships, items can also be defined
 * differently based on whether their values hold simple model nodes, complex nodes or various types of collections.
 * Attributes, operation parameters, reply value types and fields therein can all have each of these as their value,
 * and having different classes for all the permutations is too much. So instead the design is to have
 * subclasses of {@link ItemDefinition} for the simple, complex and collection cases, and then, if the usage
 * cases require it, have an unrelated type like {@link AttributeDefinition} wrap the item definition and expose
 * separate definition elements for its unique cases.
 * <p>
 * Following from this, {@link ItemDefinition itself} may expose definitional properties that are not relevant to all
 * uses. However, they should be relevant to at least two uses. Note also that in typical use, the {@code add}
 * operation for a resource exposes parameters that match attributes of the resource. To make defining this kind
 * of resource simple, we encourage the reuse of the same {@link ItemDefinition} for both. So, an
 * item definition that is wrapped by an {@link AttributeDefinition} may include definition settings, like
 * a {@link ParameterCorrector} or a {@link ParameterValidator}, that are only relevant to an operation parameter.
 *
 * @author Brian Stansberry
 */
public class ItemDefinition {

    private final String name;
    private final String xmlName;
    private final ModelType type;
    private final boolean required;
    private final boolean allowExpression;
    private final ModelNode defaultValue;
    private final MeasurementUnit measurementUnit;
    private final String[] alternatives;
    private final String[] requires;
    private final Long min;
    private final Long max;
    private final Integer minSize;
    private final Integer maxSize;
    private final ModelNode[] allowedValues;
    private final ParameterCorrector valueCorrector;
    private final ParameterValidator validator;
    private final AttributeMarshaller attributeMarshaller;
    private final DeprecationData deprecationData;
    private final AttributeParser parser;
    private final CapabilityReferenceRecorder referenceRecorder;
    private final Map<String, ModelNode> arbitraryDescriptors;
    private final String aliasFor;
    private final SchemaVersion[] since;

    ItemDefinition(Builder<?, ?> toCopy) {
        this.name = toCopy.name;
        String xmlName = toCopy.xmlName;
        this.xmlName = xmlName == null ? name : xmlName;
        this.type = toCopy.type;
        this.required = toCopy.required;
        this.allowExpression = toCopy.allowExpression;
        AttributeParser parser = toCopy.parser;
        this.parser = parser != null ? parser : AttributeParser.SIMPLE;
        ModelNode defaultValue = toCopy.defaultValue;
        if (defaultValue != null && defaultValue.isDefined()) {
            this.defaultValue = defaultValue;
            this.defaultValue.protect();
        } else {
            this.defaultValue = null;
        }
        this.measurementUnit = toCopy.measurementUnit;
        this.alternatives = toCopy.alternatives;
        this.requires = toCopy.requires;
        this.valueCorrector = toCopy.corrector;
        this.min = toCopy.min;
        this.max = toCopy.max;
        this.minSize = toCopy.getMinSize();
        this.maxSize = toCopy.getMaxSize();
        this.validator = wrapValidator(toCopy.getValidator(), required, alternatives, allowExpression,
                type, minSize, maxSize);
        AttributeMarshaller marshaller = toCopy.attributeMarshaller;
        this.attributeMarshaller = marshaller != null ? marshaller : AttributeMarshaller.SIMPLE;
        this.deprecationData = toCopy.deprecated;
        this.allowedValues = toCopy.allowedValues;
        this.referenceRecorder = toCopy.referenceRecorder;
        Map<String, ModelNode> arbitraryDescriptors = toCopy.arbitraryDescriptors;
        if (arbitraryDescriptors != null && !arbitraryDescriptors.isEmpty()) {
            if (arbitraryDescriptors.size() == 1) {
                Map.Entry<String, ModelNode> entry = arbitraryDescriptors.entrySet().iterator().next();
                this.arbitraryDescriptors = Collections.singletonMap(entry.getKey(), entry.getValue());
            } else {
                this.arbitraryDescriptors = Collections.unmodifiableMap(new HashMap<>(arbitraryDescriptors));
            }
        } else {
            this.arbitraryDescriptors = Collections.emptyMap();
        }
        this.aliasFor = toCopy.aliasFor;
        this.since = toCopy.since;
    }

    private static ParameterValidator wrapValidator(ParameterValidator toWrap, boolean required,
                                                    String[] alternatives, boolean allowExpression, ModelType type,
                                                    Integer minSize, Integer maxSize) {
        NillableOrExpressionParameterValidator result = null;
        boolean hasAlternatives = alternatives != null && alternatives.length > 0;
        boolean nullOK = !required || hasAlternatives;
        if (toWrap == null) {
            if (type == ModelType.STRING) {
                // If sizing was specified, use it. If unspecified use defaults we've used since early AS 7
                int min = minSize == null ? 1 : minSize;
                int max = maxSize == null ? Integer.MAX_VALUE : maxSize;
                toWrap = new StringLengthValidator(min, max);
            } else if (type == ModelType.BYTES) {
                // If sizing was specified, use it. If unspecified use defaults equivalent to no min or max
                int min = minSize == null ? 0 : minSize;
                int max = maxSize == null ? Integer.MAX_VALUE : maxSize;
                toWrap = new BytesValidator(min, max); // don't need to allow null here; the wrapper will deal with null
            } else {
                toWrap = new ModelTypeValidator(type);
            }
        } else if (toWrap instanceof NillableOrExpressionParameterValidator) {
            // Avoid re-wrapping
            NillableOrExpressionParameterValidator current = (NillableOrExpressionParameterValidator) toWrap;
            if (allowExpression == current.isAllowExpression() &&
                    nullOK == current.getAllowNull()) {
                result = current;
            } else {
                toWrap = current.getDelegate();
            }
        }
        if (result == null) {
            result = new NillableOrExpressionParameterValidator(toWrap, nullOK, allowExpression);
        }

        return result;
    }

    /**
     * The item's name in the management model.
     *
     * @return the name. Will not be {@code null}
     */
    public final String getName() {
        return name;
    }

    /**
     * The item's name in the xml configuration. Not relevant for operation parameters and reply value types.
     *
     * @return the name. Will not be {@code null}, although it may not be relevant
     */
    public final String getXmlName() {
        return xmlName;
    }

    /**
     * The expected {@link ModelType type} of the {@link ModelNode} that holds the
     * item data.
     * @return the type. Will not be {@code null}
     */
    public final ModelType getType() {
        return type;
    }

    /**
     * Whether a {@link ModelNode} holding the value of this item can be
     * {@link ModelType#UNDEFINED} when all other items in the same context
     * that are {@link #getAlternatives() alternatives} of this item are undefined.
     * <p>
     * In a valid model an item that is required must be undefined if any alternative
     * is defined, so this method should not be used for checking if it is valid for
     * the item ever to have an undefined value. Use {@link #isNillable()} for that.
     *
     * @return {@code true} if an {@code undefined} ModelNode is invalid in the absence of
     *         alternatives; {@code false} if not
     */
    public final boolean isRequired() {
        return required;
    }

    /**
     * Whether a {@link ModelNode} holding the value of this item can be
     * {@link ModelType#UNDEFINED} in any situation. An item that ordinarily is
     * {@link #isRequired() required} may still be undefined in a given model if an
     * {@link #getAlternatives() alternative item} is defined.
     * <p>
     * This is equivalent to {@code !isRequired() || (getAlternatives() != null && getAlternatives().length > 0)}.
     *
     * @return {@code true} if an {@code undefined} ModelNode is valid; {@code false} if not
     */
    public final boolean isNillable() {
        return !required || (alternatives != null && alternatives.length > 0);
    }

    /**
     * Whether a {@link ModelNode} holding the value of this item can be
     * {@link ModelType#EXPRESSION}.
     *
     * @return {@code true} if an {@code expression ModelNode} is valid; {@code false} if not
     */
    public final boolean isAllowExpression() {
        return allowExpression;
    }

    /**
     * Gets the default value to use for the item if a value was not provided.
     * Not relevant for operation parameters and reply value types.
     *
     * @return the default value, or {@code null} if no defined value was provided
     */
    public final ModelNode getDefaultValue() {
        return defaultValue;
    }

    /**
     * The unit of measure in which an item with a numerical value is expressed.
     *
     * @return the measurement unit, or {@code null} if none is relevant
     */
    @SuppressWarnings("WeakerAccess")
    public final MeasurementUnit getMeasurementUnit() {
        return measurementUnit;
    }

    /**
     * Gets the corrector used to correct values before checking that they comply with the item's definition.
     *
     * @return the corrector. May be {@code null}
     */
    @SuppressWarnings("WeakerAccess")
    public final ParameterCorrector getCorrector() {
        return valueCorrector;
    }

    /**
     * Gets the validator used to validate that values comply with the item's definition.
     *
     * @return the validator. Will not be {@code null}
     */
    public ParameterValidator getValidator() {
        return validator;
    }

    /**
     * Gets the names of other items whose value must be {@code undefined} if this item's value is
     * defined, and vice versa.
     *
     * @return the alternative item names, or {@code null} if there are no such items
     */
    public final String[] getAlternatives() {
        return alternatives;
    }

    /**
     * Gets the names of other items whose value must not be {@code undefined} if this item's value is
     * defined.
     *
     * @return the required item names, or {@code null} if there are no such items
     */
    public final String[] getRequires() {
        return requires;
    }

    /**
     * Gets the legal values for the item, or an empty list if there is no fixed set of legal values.
     * @return the allowed values. Will not return {@code null}
     */
    public final List<ModelNode> getAllowedValues() {
        if (allowedValues == null) {
            return Collections.emptyList();
        }
        return Arrays.asList(this.allowedValues);
    }

    /**
     * Gets the minimum value for this item, if there is one. Only relevant for items whose value is a numeric
     * {@link #getType() type}.
     *
     * @return the minimum value, or {@code null} if no such value is configured
     */
    public Long getMin() {
        return min;
    }

    /**
     * Gets the maximum value for this item, if there is one. Only relevant for items whose value is a numeric
     * {@link #getType() type}.
     *
     * @return the maximum value, or {@code null} if no such value is configured
     */
    public Long getMax() {
        return max;
    }

    /**
     * Convenience method that gets the minimum value for this item, if there is one, but constrained to a valid int
     * range. Only relevant for items whose value is a numeric {@link #getType() type}. This method will convert any
     * value returned by {@link #getMin()} that is less than {@link Integer#MIN_VALUE} to {@link Integer#MIN_VALUE}.
     * It will make no attempt to determine whether that is a reasonable thing to do, e.g. by checking this item's
     * {@link #getType() type}, so it is the responsibility of the caller to check whether constraining to an int range
     * is appropriate.
     *
     * @return the minimum value, or {@code null} if no such value is configured
     *
     * @throws IllegalStateException if {@link #getMin()} returns a value greater than {@link Integer#MAX_VALUE}
     */
    @SuppressWarnings("unused")
    public Integer getMinAsInteger() {
        if (min == null) {
            return null;
        } else {
            long minLong = min;
            if (minLong < Integer.MIN_VALUE) {
                return Integer.MIN_VALUE;
            } else if (minLong > Integer.MAX_VALUE) {
                throw new IllegalStateException();
            } else {
                return (int) minLong;
            }
        }
    }

    /**
     * Convenience method that gets the maximum value for this item, if there is one, but constrained to a valid int
     * range. Only relevant for items whose value is a numeric {@link #getType() type}. This method will convert any
     * value returned by {@link #getMax()} that is greater than {@link Integer#MAX_VALUE} to {@link Integer#MAX_VALUE}.
     * It will make no attempt to determine whether that is a reasonable thing to do, e.g. by checking this item's
     * {@link #getType() type}, so it is the responsibility of the caller to check whether constraining to an int range
     * is appropriate.
     *
     * @return the maximum value, or {@code null} if no such value is configured
     *
     * @throws IllegalStateException if {@link #getMax()} returns a value less than {@link Integer#MIN_VALUE}
     */
    @SuppressWarnings("unused")
    public Integer getMaxAsInteger() {
        if (min == null) {
            return null;
        } else {
            long minLong = min;
            if (minLong > Integer.MAX_VALUE) {
                return Integer.MAX_VALUE;
            } else if (minLong > Integer.MIN_VALUE) {
                throw new IllegalStateException();
            } else {
                return (int) minLong;
            }
        }
    }

    /**
     * Gets the minimum size for this item, if there is one. Only relevant for items whose value is a
     * {@link #getType() type} that can logically have a size, e.g. {@code LIST}, {@code STRING}, {@code BYTES}
     * or an {@code OBJECT} that represents a map.
     *
     * @return the minimum size, or {@code null} if no such value is configured
     */
    public Integer getMinSize() {
        return minSize;
    }

    /**
     * Gets the maximum size for this item, if there is one. Only relevant for items whose value is a
     * {@link #getType() type} that can logically have a size, e.g. {@code LIST}, {@code STRING}, {@code BYTES}
     * or an {@code OBJECT} that represents a map.
     *
     * @return the maximum size, or {@code null} if no such value is configured
     */
    public Integer getMaxSize() {
        return maxSize;
    }

    /**
     * Gets any arbitrary descriptors that provide extra information about the item.
     * @return a map of descriptor name to the value of that descriptor. Will not return {@code null}
     */
    @SuppressWarnings("WeakerAccess")
    public final Map<String, ModelNode> getArbitraryDescriptors() {
        return arbitraryDescriptors;
    }

    /**
     * Gets a parser that can be used to parse this item's value to XML.
     * Not relevant for operation parameters and reply value types.
     * @return the attribute parser. Will not return {@code null}
     */
    public final AttributeParser getParser() {
        return parser;
    }

    /**
     * Gets a marshaller that can be used to persist this item's value to XML.
     * Not relevant for operation parameters and reply value types.
     * @return the attribute marshaller. Will not return {@code null}
     */
    public final AttributeMarshaller getMarshaller() {
        return attributeMarshaller;
    }

    /**
     * If the item is deprecated, provides information about said deprecation.
     * @return the deprecation data, or {@code null} if the item is not deprecated
     */
    public final DeprecationData getDeprecationData() {
        return deprecationData;
    }

    /**
     * If this item is an alias for another item in the same context, provides the name of that item.
     * @return the item name or {@code null} if the item is not an alias
     */
    @SuppressWarnings("unused")
    public final String getAliasFor() {
        return aliasFor;
    }

    /**
     * Gets the recorder of a capability reference, if this item refers to a capability.
     * @return the recorder or {@code null} if this item is not a capability reference
     */
    public CapabilityReferenceRecorder getReferenceRecorder(){
        return referenceRecorder;
    }

    final boolean isNullSignificant() {
        return !required && defaultValue != null && defaultValue.isDefined();
    }

    /**
     * Builder for creating an {@link ItemDefinition}.
     * @param <BUILDER> the type of the builder
     * @param <ITEM> the specific type of the {@link ItemDefinition} that will be produced.
     */
    @SuppressWarnings("unchecked")
    public abstract static class Builder<BUILDER extends Builder, ITEM extends ItemDefinition> {

        private final String name;
        private final ModelType type;
        private String xmlName;
        private boolean required = true;
        private boolean allowExpression;
        private ModelNode defaultValue;
        private MeasurementUnit measurementUnit;
        private String[] alternatives;
        private String[] requires;
        private ModelNode[] allowedValues;
        private ParameterCorrector corrector;
        private ParameterValidator validator;
        private Long min;
        private Long max;
        private Integer minSize;
        private Integer maxSize;
        private AttributeMarshaller attributeMarshaller = null;
        private DeprecationData deprecated = null;
        private AttributeParser parser;
        private CapabilityReferenceRecorder referenceRecorder;
        private Map<String, ModelNode> arbitraryDescriptors = null;
        private String aliasFor;
        private SchemaVersion[] since;

        Builder(final String itemName, final ModelType type) {
            this.name = itemName;
            this.type = type;
            this.xmlName = name;
        }

        Builder(final String name, final ItemDefinition basis) {
            this.name = name != null ? name : basis.getName();
            this.xmlName = basis.getXmlName();
            this.defaultValue = basis.getDefaultValue();
            this.type = basis.getType();
            this.required = basis.isRequired();
            this.allowExpression = basis.isAllowExpression();
            this.measurementUnit = basis.getMeasurementUnit();
            this.corrector = basis.getCorrector();
            this.validator = basis.getValidator();
            this.alternatives = basis.getAlternatives();
            this.requires = basis.getRequires();
            this.attributeMarshaller = basis.getMarshaller();
            this.deprecated = basis.getDeprecationData();
            this.parser = basis.getParser();
            if (basis.getAllowedValues().size() > 0) {
                List<ModelNode> basisAllowedValues = basis.getAllowedValues();
                this.allowedValues = basisAllowedValues.toArray(new ModelNode[basisAllowedValues.size()]);
            }
            if(!basis.getArbitraryDescriptors().isEmpty()) {
                this.arbitraryDescriptors = new HashMap<>(basis.getArbitraryDescriptors());
            }
            this.referenceRecorder = basis.referenceRecorder;
            this.aliasFor = basis.aliasFor;
            this.since = basis.since;
        }

        /**
         * Create the {@link ItemDefinition}
         * @return the item definition. Will not return {@code null}
         */
        public abstract ITEM build();

        /**
         * Sets the {@link SchemaVersion schema versions} when this item first appeared in the management API.
         * @param since the versions when the item first appeared. Use multiple versions if the item appeared on
         *              more than one branch of the version tree.
         * @return a builder that can be used to continue building the item definition
         */
        public final BUILDER setSince(SchemaVersion... since) {
            this.since = since;
            return (BUILDER) this;
        }

        /**
         * Sets the {@link ItemDefinition#getXmlName() xml name} for the item, which is only needed
         * if the name used for the item is different from its ordinary
         * {@link ItemDefinition#getName() name in the model}. If not set the default value is the name
         * passed to the builder constructor.
         *
         * @param xmlName the xml name. {@code null} is allowed
         * @return a builder that can be used to continue building the item definition
         */
        public BUILDER setXmlName(String xmlName) {
            this.xmlName = xmlName == null ? this.name : xmlName;
            return (BUILDER) this;
        }

        /**
         * Sets whether the item should {@link ItemDefinition#isRequired() require a defined value}
         * in the absence of {@link #setAlternatives(String...) alternatives}.
         * If not set the default value is {@code true}.
         *
         * @param required {@code true} if undefined values should not be allowed in the absence of alternatives
         * @return a builder that can be used to continue building the item definition
         */
        public final BUILDER setRequired(boolean required) {
            this.required = required;
            return (BUILDER) this;
        }

        /**
         * Sets a {@link ItemDefinition#getDefaultValue() default value} to use for the item if no
         * user-provided value is available.
         * @param defaultValue the default value, or {@code null} if no default should be used
         * @return a builder that can be used to continue building the item definition
         */
        public final BUILDER setDefaultValue(ModelNode defaultValue) {
            this.defaultValue = (defaultValue == null || !defaultValue.isDefined()) ? null : defaultValue;
            return (BUILDER) this;
        }

        /**
         * Sets a {@link ParameterCorrector} to use to adjust any user provided values
         * before validation occurs. This is only relevant to operation parameter uses.
         * @param corrector the corrector. May be {@code null}
         * @return a builder that can be used to continue building the item definition
         */
        public final BUILDER setCorrector(ParameterCorrector corrector) {
            this.corrector = corrector;
            return (BUILDER) this;
        }

        /**
         * Sets the validator that should be used to validate item values. The resulting item definition
         * will wrap this validator in one that enforces the item's
         * {@link ItemDefinition#isRequired()} () required} and
         * {@link ItemDefinition#isAllowExpression() allow expression} settings, so the given {@code validator}
         * need not concern itself with those validations. This is only relevant to operation parameter uses.
         * @param validator the validator. {@code null} is allowed
         * @return a builder that can be used to continue building the item definition
         */
        public BUILDER setValidator(ParameterValidator validator) {
            this.validator = validator;
            return (BUILDER) this;
        }

        /**
         * Sets {@link ItemDefinition#getAlternatives() names of alternative items} that should not
         * be defined if this item is defined.
         * @param alternatives the item names
         * @return a builder that can be used to continue building the item definition
         */
        public final BUILDER setAlternatives(String... alternatives) {
            this.alternatives = alternatives;
            return (BUILDER) this;
        }

        /**
         * Adds {@link ItemDefinition#getAlternatives() names of alternative items} that should not
         * be defined if this item is defined.
         * @param alternatives the item names
         * @return a builder that can be used to continue building the item definition
         */
        @SuppressWarnings("unused")
        public final BUILDER addAlternatives(String... alternatives) {
            if (this.alternatives == null) {
                this.alternatives = alternatives;
            } else {
                String[] newAlternatives = Arrays.copyOf(this.alternatives, this.alternatives.length + alternatives.length);
                System.arraycopy(alternatives, 0, newAlternatives, this.alternatives.length, alternatives.length);
                this.alternatives = newAlternatives;
            }
            return (BUILDER) this;
        }

        /**
         * Sets the {@link ItemDefinition#getArbitraryDescriptors() arbitrary descriptors}.
         * @param arbitraryDescriptors the arbitrary descriptor map.
         * @return a builder that can be used to continue building the item definition
         */
        @SuppressWarnings({"unused", "UnusedReturnValue"})
        public final BUILDER setArbitraryDescriptors(Map<String, ModelNode> arbitraryDescriptors) {
            this.arbitraryDescriptors = arbitraryDescriptors;
            return (BUILDER) this;
        }

        /**
         * Adds {@link ItemDefinition#getArbitraryDescriptors() arbitrary descriptors}.
         * @param arbitraryDescriptor the arbitrary descriptor name.
         * @param value the value of the arbitrary descriptor.
         * @return a builder that can be used to continue building the item definition
         */
        @SuppressWarnings("unused")
        public final BUILDER addArbitraryDescriptor(String arbitraryDescriptor, ModelNode value) {
            if (this.arbitraryDescriptors == null) {
                this.arbitraryDescriptors = Collections.singletonMap(arbitraryDescriptor, value);
            } else {
                if (this.arbitraryDescriptors.size() == 1) {
                    this.arbitraryDescriptors = new HashMap<>(this.arbitraryDescriptors);
                }
                arbitraryDescriptors.put(arbitraryDescriptor, value);
            }
            return (BUILDER) this;
        }

        /**
         * Sets {@link ItemDefinition#getRequires() names of required items} that must
         * be defined if this item is defined.
         * @param requires the item names
         * @return a builder that can be used to continue building the item definition
         */
        public final BUILDER setRequires(String... requires) {
            this.requires = requires;
            return (BUILDER) this;
        }

        /**
         * Sets a maximum size for a collection-type item or one whose value is a string or byte[].
         * The value represents the maximum number of elements in the collection, or the maximum length of
         * the string or array. <strong>It does not represent a maximum value for a numeric item and should
         * not be configured for numeric items.</strong>
         * @param maxSize the maximum size
         * @return a builder that can be used to continue building the item definition
         */
        public final BUILDER setMaxSize(final int maxSize) {
            this.maxSize = maxSize;
            return (BUILDER) this;
        }

        /**
         * Sets a minimum size description for a collection-type item or one whose value is a string or byte[].
         * The value represents the minimum number of elements in the collection, or the minimum length of
         * the string or array. <strong>It does not represent a minimum value for a numeric item and should
         * not be configured for numeric items.</strong>
         * @param minSize the minimum size
         * @return a builder that can be used to continue building the item definition
         */
        public final BUILDER setMinSize(final int minSize) {
            this.minSize = minSize;
            return (BUILDER) this;
        }

        /**
         * Marks the item as deprecated since the given API version. This is equivalent to calling
         * {@link #setDeprecated(SchemaVersion, boolean)} with the {@code notificationUseful} parameter
         * set to {@code true}.
         *
         * @param since the API version, with the API being the one (core or a subsystem) in which the item was first deprecated
         * @return a builder that can be used to continue building the item definition
         */
        public final BUILDER setDeprecated(SchemaVersion since) {
            return setDeprecated(since, true);
        }

        /**
         * Marks the item as deprecated since the given API version, with the ability to configure that
         * notifications to the user (e.g. via a log message) about deprecation of the item should not be emitted.
         * Notifying the user should only be done if the user can take some action in response. Advising that
         * something will be removed in a later release is not useful if there is no alternative in the
         * current release. If the {@code notificationUseful} param is {@code true} the text
         * description of the item deprecation available from the {@code read-resource-description}
         * or {@code read-operation-description} (for operation parameters) management operation should
         * provide useful information about how the user can avoid using the item.
         *
         * @param since the API version, with the API being the one (core or a subsystem) in which the item was first deprecated
         * @param notificationUseful whether actively advising the user about the deprecation is useful
         *
         * @return a builder that can be used to continue building the item definition
         */
        public final BUILDER setDeprecated(SchemaVersion since, boolean notificationUseful) {
            this.deprecated = new DeprecationData(since, notificationUseful);
            return (BUILDER) this;
        }

        /**
         * Marks this item as an alias for another item that appears in the same context.
         *
         * @param aliasFor name of the item for which this item is an alias
         * @return a builder that can be used to continue building the item definition
         */
        @SuppressWarnings("unused")
        public final BUILDER setAliasFor(String aliasFor) {
            this.aliasFor = aliasFor;
            return (BUILDER) this;
        }

        /**
         * Records that this item's value represents a reference to an instance of a
         * {@link RuntimeCapability#isDynamicallyNamed() dynamic capability}.
         * <p>
         * This method is a convenience method equivalent to calling
         * {@link #setCapabilityReference(CapabilityReferenceRecorder)}
         * passing in a {@link CapabilityReferenceRecorder.DefaultCapabilityReferenceRecorder}
         * constructed using the parameters passed to this method.
         *
         * @param referencedCapability the name of the dynamic capability the dynamic portion of whose name is
         *                             represented by the item's value
         * @param dependentCapability  the capability that depends on {@code referencedCapability}
         * @return the builder
         */
        public final BUILDER setCapabilityReference(String referencedCapability, RuntimeCapability<?> dependentCapability) {
            return setCapabilityReference(referencedCapability, dependentCapability.getName(), dependentCapability.isDynamicallyNamed());
        }

        /**
         * Records that this item's value represents a reference to an instance of a
         * {@link RuntimeCapability#isDynamicallyNamed() dynamic capability}.
         * <p>
         * This method is a convenience method equivalent to calling
         * {@link #setCapabilityReference(CapabilityReferenceRecorder)}
         * passing in a {@link CapabilityReferenceRecorder.ContextDependencyRecorder}
         * constructed using the parameters passed to this method.
         * <p>
         * <strong>NOTE:</strong> This method of recording capability references is only suitable for use in items
         * only used in resources that themselves expose a single capability.
         * If your resource exposes more than single capability you should use {@link #setCapabilityReference(RuntimeCapability, String, ItemDefinition...)} variant
         * When the capability requirement is registered, the dependent capability will be that capability.
         *
         * @param referencedCapability the name of the dynamic capability the dynamic portion of whose name is
         *                             represented by the item's value
         * @return the builder
         */
        public final BUILDER setCapabilityReference(String referencedCapability) {
            referenceRecorder = new CapabilityReferenceRecorder.ContextDependencyRecorder(referencedCapability);
            return (BUILDER) this;
        }

        /**
         * Records that this item's value represents a reference to an instance of a
         * {@link RuntimeCapability#isDynamicallyNamed() dynamic capability}.
         * <p>
         * This method is a convenience method equivalent to calling * {@link #setCapabilityReference(CapabilityReferenceRecorder)}
         * passing in a {@link CapabilityReferenceRecorder.CompositeAttributeDependencyRecorder}
         * constructed using the parameters passed to this method.
         * <p>
         * <strong>NOTE:</strong> This method of recording capability references is only suitable for use in items
         * only used in resources that themselves expose a single capability.
         * If your resource exposes more than single capability, you should use {@link #setCapabilityReference(RuntimeCapability, String, ItemDefinition...)}
         * When the capability requirement
         * is registered, the dependent capability will be that capability.
         *
         * @param referencedCapability the name of the dynamic capability the dynamic portion of whose name is
         *                             represented by the item's value
         *  @param dependantAttributes item from same resource that will be used to derive multiple dynamic parts for the dependant capability
         * @return the builder
         */
        public final BUILDER setCapabilityReference(String referencedCapability, ItemDefinition... dependantAttributes) {
            referenceRecorder = new CapabilityReferenceRecorder.CompositeAttributeDependencyRecorder(referencedCapability, dependantAttributes);
            return (BUILDER) this;
        }

        /**
         * Records that this item's value represents a reference to an instance of a
         * {@link RuntimeCapability#isDynamicallyNamed() dynamic capability}.
         * <p>
         * This method is a convenience method equivalent to calling {@link #setCapabilityReference(CapabilityReferenceRecorder)}
         * passing in a {@link CapabilityReferenceRecorder.CompositeAttributeDependencyRecorder}
         * constructed using the parameters passed to this method.
         * <p>
         * When the capability requirement is registered, the dependent capability will be that capability.
         *
         * @param capability requirement capability
         * @param referencedCapability the name of the dynamic capability the dynamic portion of whose name is
         *                             represented by the item's value
         * @param dependantFields items on resource which will be used for registering capability reference, can be multiple.
         * @return the builder
         */
        public final BUILDER setCapabilityReference(RuntimeCapability capability, String referencedCapability, ItemDefinition... dependantFields) {
            referenceRecorder = new CapabilityReferenceRecorder.CompositeAttributeDependencyRecorder(capability, referencedCapability, dependantFields);
            return (BUILDER) this;
        }

        /**
         * Records that this item's value represents a reference to an instance of a
         * {@link RuntimeCapability#isDynamicallyNamed() dynamic capability}.
         * <p>
         * This method is a convenience method equivalent to calling
         * {@link #setCapabilityReference(CapabilityReferenceRecorder)}
         * passing in a {@link CapabilityReferenceRecorder.DefaultCapabilityReferenceRecorder}
         * constructed using the parameters passed to this method.
         *
         * @param referencedCapability the name of the dynamic capability the dynamic portion of whose name is
         *                             represented by the item's value
         * @param dependentCapability  the name of the capability that depends on {@code referencedCapability}
         * @param dynamicDependent     {@code true} if {@code dependentCapability} is a dynamic capability, the dynamic
         *                             portion of which comes from the name of the resource with which
         *                             the item is associated
         * @return the builder
         */
        public final BUILDER setCapabilityReference(String referencedCapability, String dependentCapability, boolean dynamicDependent) {
            referenceRecorder = new CapabilityReferenceRecorder.DefaultCapabilityReferenceRecorder(referencedCapability, dependentCapability, dynamicDependent);
            return (BUILDER) this;
        }

        /**
         * Records that this item's value represents a reference to an instance of a
         * {@link RuntimeCapability#isDynamicallyNamed() dynamic capability} and assigns the
         * object that should be used to handle adding and removing capability requirements.
         *
         * @param referenceRecorder recorder to handle adding and removing capability requirements. May be {@code null}
         * @return the builder
         */
        public final BUILDER setCapabilityReference(CapabilityReferenceRecorder referenceRecorder) {
            this.referenceRecorder = referenceRecorder;
            return (BUILDER)this;
        }

        public BUILDER setAllowExpression(boolean allowExpression) {
            this.allowExpression = allowExpression;
            return (BUILDER) this;
        }

        public BUILDER setAllowedValues(ModelNode ... allowedValues) {
            assert allowedValues!= null;
            this.allowedValues = allowedValues;
            return (BUILDER) this;
        }

        public BUILDER setAllowedValues(String ... allowedValues) {
            assert allowedValues!= null;
            this.allowedValues = new ModelNode[allowedValues.length];
            for (int i = 0; i < allowedValues.length; i++) {
                this.allowedValues[i] = new ModelNode(allowedValues[i]);
            }
            return (BUILDER) this;
        }

        public BUILDER setAllowedValues(int ... allowedValues) {
            assert allowedValues!= null;
            this.allowedValues = new ModelNode[allowedValues.length];
            for (int i = 0; i < allowedValues.length; i++) {
                this.allowedValues[i] = new ModelNode(allowedValues[i]);
            }
            return (BUILDER) this;
        }

        public BUILDER setMeasurementUnit(MeasurementUnit unit) {
            this.measurementUnit = unit;
            return (BUILDER) this;
        }


        BUILDER setAttributeMarshaller(AttributeMarshaller marshaller) {
            this.attributeMarshaller = marshaller;
            return (BUILDER) this;
        }

        BUILDER setAttributeParser(AttributeParser parser) {
            this.parser = parser;
            return (BUILDER) this;
        }

        BUILDER setMin(long min) {
            this.min = min;
            return (BUILDER) this;
        }

        BUILDER setMax(long max) {
            this.max = max;
            return (BUILDER) this;
        }

        String getName() {
            return name;
        }

        String getXmlName() {
            return xmlName;
        }

        boolean isAllowExpression() {
            return allowExpression;
        }

        Integer getMinSize() {
            return minSize;
        }

        Integer getMaxSize() {
            return maxSize;
        }

        ParameterValidator getValidator() {
            return validator;
        }

        AttributeMarshaller getAttributeMarshaller() {
            return attributeMarshaller;
        }

        AttributeParser getParser() {
            return parser;
        }
    }
}
