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

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.jboss.dmr.ModelNode;
import org.wildfly.management.api.AddressElement;
import org.wildfly.management.api.ResourceAddress;
import org.wildfly.management.api.access.AccessConstraintDefinition;
import org.wildfly.management.api.model.Resource;
import org.wildfly.management.api.runtime.RuntimeReadHandler;
import org.wildfly.management.api.runtime.RuntimeUpdateHandler;

/**
 * Defining characteristics of an attribute in a {@link ResourceTypeDefinition}.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public final class AttributeDefinition {

    /** Flags to indicate special characteristics of an attribute */
    public enum Flag {
        /**
         * An attribute whose value is only stored in runtime services, and
         * isn't stored in the persistent configuration.
         */
        STORAGE_RUNTIME,
        /** An attribute that cannot be modified once the containing resource has been added */
        READ_ONLY,
        /** A read-only {@code Storage.RUNTIME} attribute whose value can be expected
         * to vary over time based on runtime activity */
        METRIC,
        /**
         * An attribute which does not require runtime MSC services to be read or written.
         * This flag can be used in conjunction with STORAGE_RUNTIME to specify that a runtime
         * attribute can work in the absence of runtime services.
         */
        RUNTIME_SERVICE_NOT_REQUIRED,
        /**
         * An attribute whose value is the same as the value part of the last {@link AddressElement element}
         * in its containing resource's {@link ResourceAddress}.
         */
        RESOURCE_NAME_ALIAS;

        private static final Map<EnumSet<Flag>, Set<Flag>> flagSets = new ConcurrentHashMap<>(16);
        public static Set<Flag> immutableSetOf(EnumSet<Flag> flags) {
            if (flags == null || flags.size() == 0) {
                return Collections.emptySet();
            }
            Set<Flag> result = flagSets.get(flags);
            if (result == null) {
                Set<Flag> immutable = Collections.unmodifiableSet(flags);
                Set<Flag> existing = flagSets.putIfAbsent(flags, immutable);
                result = existing == null ? immutable : existing;
            }

            return result;
        }
    }

    private static AccessConstraintDefinition[] ZERO_CONSTRAINTS = new AccessConstraintDefinition[0];

    private final ItemDefinition itemDefinition;
    private final Set<Flag> flags;
    private final RestartLevel restartLevel;
    private final boolean resourceOnly;
    private final List<AccessConstraintDefinition> accessConstraints;
    private final Boolean nilSignificant;
    private final RuntimeReadHandler readHandler;
    private final RuntimeUpdateHandler writeHandler;
    private final ModelNode undefinedMetricValue;

    private AttributeDefinition(Builder builder) {
        this.itemDefinition = builder.itemDefinition;
        this.flags = Flag.immutableSetOf(builder.flags);
        this.resourceOnly = builder.resourceOnly;
        this.accessConstraints = wrapConstraints(builder.accessConstraints);
        this.nilSignificant = builder.nullSignificant;
        this.readHandler = builder.readHandler;
        this.writeHandler = builder.writeHandler;
        ModelNode undefinedMetricValue = builder.undefinedMetricValue;
        if (undefinedMetricValue != null && undefinedMetricValue.isDefined()) {
            this.undefinedMetricValue = undefinedMetricValue;
            this.undefinedMetricValue.protect();
        } else {
            this.undefinedMetricValue = null;
        }
        // If we're read-only, restartLevel doesn't matter.
        // If we're writable, take a configured value, else no writeHandler implies reload required
        this.restartLevel = this.flags.contains(Flag.READ_ONLY)
                ? RestartLevel.NONE
                : (builder.restartLevel != null
                    ? builder.restartLevel
                    : (this.writeHandler == null ? RestartLevel.ALL_SERVICES : RestartLevel.NONE));
    }

    private static List<AccessConstraintDefinition> wrapConstraints(AccessConstraintDefinition[] accessConstraints) {
        if (accessConstraints == null || accessConstraints.length == 0) {
            return Collections.<AccessConstraintDefinition>emptyList();
        } else {
            return Collections.unmodifiableList(Arrays.asList(copyConstraints(accessConstraints)));
        }
    }

    /**
     * Get the basic definition of this attribute.
     *
     * @return the item definition. Will not be {@code null}
     */
    @SuppressWarnings("unused")
    public ItemDefinition getItemDefinition() {
        return itemDefinition;
    }

    /**
     * Gets the handler to use to read the attribute's value from runtime services, if one exists.
     * @return the handler. May return {@code null}
     */
    public RuntimeReadHandler getReadHandler() {
        return readHandler;
    }

    /**
     * Gets the handler to use to update runtime services when the attribute value is changed, if one exists.
     * @return the handler. May return {@code null}
     */
    public RuntimeUpdateHandler getWriteHandler() {
        return writeHandler;
    }

    /**
     * Gets what must be restarted in order to cause changes made to this attribute take effect in
     * the runtime.
     * @return the restart level. Will not return {@code null}
     */
    @SuppressWarnings("unused")
    public RestartLevel getRestartLevel() {
        return restartLevel;
    }

    /**
     * Gets whether an access control check is required to implicitly set an attribute to {@code undefined}
     * in a resource "add" operation. "Implicitly" setting an attribute refers to not providing a value for
     * it in the add operation, leaving the attribute in an undefined state. So, if a user attempts to
     * add a resource but does not define some attributes, a write permission check will be performed for
     * any attributes where this method returns {@code true}.
     * <p>
     * Generally this is {@code true} if {@link ItemDefinition#isRequired() undefined is allowed} and a
     * {@link ItemDefinition#getDefaultValue() default value} exists, although some instances may have a different setting.
     *
     * @return {@code true} if an {@code undefined} value is significant
     */
    @SuppressWarnings("unused")
    public boolean isNullSignificant() {
        if (nilSignificant != null) {
            return nilSignificant;
        }
        return itemDefinition.isNullSignificant();
    }

    /**
     * Gets a set of any {@link Flag flags} used
     * to indicate special characteristics of the attribute
     *
     * @return the flags. Will not be {@code null} but may be empty.
     */
    public Set<Flag> getFlags() {
        return flags;
    }

    /**
     * Show if attribute is resource only which means it wont be part of add operations but only present on resource
     * @return true is attribute is resource only
     */
    @SuppressWarnings("unused")
    public boolean isResourceOnly() {
        return resourceOnly;
    }

    public List<AccessConstraintDefinition> getAccessConstraints() {
        return accessConstraints;
    }

    /**
     * Gets the undefined metric value to use for the attribute if a value cannot be provided.
     *
     * @return the undefined metric value, or {@code null} if no undefined metric value was provided
     */
    @SuppressWarnings("unused")
    public ModelNode getUndefinedMetricValue() {
        return undefinedMetricValue;
    }

    private static AccessConstraintDefinition[] copyConstraints(AccessConstraintDefinition[] toCopy) {
        if (toCopy == null) {
            return null;
        }
        if (toCopy.length == 0){
            return ZERO_CONSTRAINTS;
        }
        AccessConstraintDefinition[] result = new AccessConstraintDefinition[toCopy.length];
        System.arraycopy(toCopy, 0, result, 0, toCopy.length);
        return result;
    }

    /**
     * Provides a builder API for creating an {@link AttributeDefinition}.
     *
     * @author Tomaz Cerar
     */
    public static final class Builder {

        /**
         * Creates a builder for an attribute based on the given item.
         * @param itemDefinition the item. Cannot be {@code null}
         */
        public static Builder of(final ItemDefinition itemDefinition) {
            return new Builder(itemDefinition);
        }

        private final ItemDefinition itemDefinition;
        private EnumSet<Flag> flags = EnumSet.noneOf(Flag.class);
        private RestartLevel restartLevel;
        private boolean resourceOnly = false;
        private AccessConstraintDefinition[] accessConstraints;
        private Boolean nullSignificant;
        private RuntimeReadHandler readHandler;
        private RuntimeUpdateHandler writeHandler;
        private ModelNode undefinedMetricValue;

        private Builder(final ItemDefinition itemDefinition) {
            this.itemDefinition = itemDefinition;
        }

        /**
         * Create the {@link AttributeDefinition}.
         * @return the attribute definition. Will not return {@code null}
         */
        public AttributeDefinition build() {
            return new AttributeDefinition(this);
        }

        /**
         * Marks the attribute as read-only, not modifiable via a {@code write-attribute} management operation. The
         * value of the attribute will be read from the {@link Resource#getModel() resource's model}.
         *
         * @return a builder that can be used to continue building the attribute definition
         */
        public Builder setReadOnly() {
            return setReadOnly(null);
        }

        /**
         * Marks the attribute as read-only, not modifiable via a {@code write-attribute} management operation, with an
         * optional handler for reading the attribute value from the runtime. If a {@code readHandler} is provided,
         * the attribute will be flagged as {@code runtime-only}. If no handler is provided, the value
         * of the attribute will be read from the {@link Resource#getModel() resource's model}.
         *
         * @param readHandler handler for reading the current value of the attribute from the runtime. May be {@code null}
         * @return a builder that can be used to continue building the attribute definition
         */
        public Builder setReadOnly(RuntimeReadHandler readHandler) {
            flags.add(Flag.READ_ONLY);
            this.readHandler = readHandler;
            if (readHandler != null) {
                flags.add(Flag.STORAGE_RUNTIME);
            }
            return this;
        }

        /**
         * Marks the attribute as read-write, modifiable via a {@code write-attribute} management operation.
         * Equivalent to {@link #setReadWrite(RuntimeReadHandler, RuntimeUpdateHandler) setReadWrite(null, null)};
         * see the description of that method for full details on behavior.
         *
         * @return a builder that can be used to continue building the attribute definition
         *
         * @deprecated this method is superfluous since the initial setting of a builder is the same as what this
         *             method configures. It's present in the API for conceptual clarity, but efficient code will skip
         *             calling it.
         */
        @Deprecated
        public Builder setReadWrite() {
            return setReadWrite(null, null);
        }

        /**
         * Marks the attribute as read-write, modifiable via a {@code write-attribute} management operation.
         * Equivalent to {@link #setReadWrite(RuntimeReadHandler, RuntimeUpdateHandler) setReadWrite(null, writeHandler)};
         * see the description of that method for full details on behavior.
         *
         * @param writeHandler handler for updating the current value of the metric from the runtime. May be {@code null}
         * @return a builder that can be used to continue building the attribute definition
         */
        @SuppressWarnings("unused")
        public Builder setReadWrite(RuntimeUpdateHandler writeHandler) {
            return setReadWrite(null, writeHandler);
        }

        /**
         * Marks the attribute as read-write, modifiable via a {@code write-attribute} management operation. If a
         * {@code readHandler} is provided, the attribute will be flagged as {@code runtime-only}. If no read handler is
         * provided, the value of the attribute will be read from the {@link Resource#getModel() resource's model}.
         * If no {@code writeHandler} , default handling will be based upon any setting of {@link #setRestartResourceServices()} ()}, {@link #setRestartAllServices()}
         * {@link #setRestartJVM()} or {@link #setRestartNone()}, with no setting at all treated the same as
         * {@link #setRestartAllServices()}. The default handling will vary based on the configured {@link RestartLevel}:
         * <ol>
         *     <li>{@link RestartLevel#NONE} -- no runtime update will be applied; the attribute only affects the model.</li>
         *     <li>{@link RestartLevel#ALL_SERVICES} -- the process will be put into {@code reload-required} state.</li>
         *     <li>{@link RestartLevel#JVM} -- the process will be put into {@code restart-required} state.</li>
         *     <li>{@link RestartLevel#RESOURCE_SERVICES} -- an invalid configuration; an {@code IllegalStateException} will be thrown by {@link #build()}.</li>
         * </ol>
         * <p>
         * If a handler is provided and the {@link #setRestartLevel(RestartLevel) restart level} is set to
         * {@link RestartLevel#RESOURCE_SERVICES}, the handler will not be invoked unless the operation included
         * the {@code allow-resource-service-restart} operation header with a value of {@code true}. If operation did
         * not include this the handler will not be invoked and the process will be put into {@code reload-required} state.
         *
         * @param readHandler handler for reading the current value of the attribute from the runtime. May be {@code null}
         * @param writeHandler handler for updating the runtime to reflect the current value of the attribute. May be {@code null}
         * @return a builder that can be used to continue building the attribute definition
         */
        @SuppressWarnings("WeakerAccess")
        public Builder setReadWrite(RuntimeReadHandler readHandler, RuntimeUpdateHandler writeHandler) {
            flags.remove(Flag.READ_ONLY);
            this.readHandler = readHandler;
            this.writeHandler = writeHandler;
            if (readHandler != null) {
                flags.add(Flag.STORAGE_RUNTIME);
            }
            return this;
        }

        /**
         * Marks the attribute as a {@link #setReadOnly() read-only}, runtime-only one
         * whose value can be expected to vary over time based on runtime activity. This expectation of varying
         * over time is what distinguishes a metric from other read-only, runtime-only attributes.
         *
         * @param readHandler handler for reading the current value of the metric from the runtime. Cannot be {@code null}
         * @return a builder that can be used to continue building the attribute definition
         */
        public Builder setMetric(RuntimeReadHandler readHandler) {
            return setMetric(readHandler, null);
        }

        /**
         * Marks the attribute as a {@link #setReadOnly() read-only}, runtime-only one
         * whose value can be expected to vary over time based on runtime activity.
         *
         * @param readHandler handler for reading the current value of the metric from the runtime. Cannot be {@code null}
         * @param undefinedMetricValue value to use for the metric if no runtime value is available (e.g. we are a
         *                             server running in admin-only mode). May be {@code null}
         * @return a builder that can be used to continue building the attribute definition
         */
        @SuppressWarnings("WeakerAccess")
        public Builder setMetric(RuntimeReadHandler readHandler, ModelNode undefinedMetricValue) {
            assert readHandler != null;
            setReadOnly(readHandler);
            flags.add(Flag.METRIC);
            this.undefinedMetricValue = undefinedMetricValue;
            return this;
        }

        /**
         * Marks the attribute as one that does not require runtime MSC services to be read or written.
         * This setting can be used in conjunction with providing a {@link RuntimeReadHandler} to
         * {@link #setReadOnly(RuntimeReadHandler)}, {@link #setMetric(RuntimeReadHandler)} or
         * {@link #setReadWrite(RuntimeReadHandler, RuntimeUpdateHandler)} to specify that a runtime
         * attribute can work in the absence of runtime services.
         *
         * @return a builder that can be used to continue building the attribute definition
         */
        public Builder setRuntimeServiceNotRequired() {
            flags.add(Flag.RUNTIME_SERVICE_NOT_REQUIRED);
            return this;
        }

        /**
         * Sets what must be restarted in order to cause changes made to a management resource to take effect in
         * the runtime.
         *
         * @param restartLevel the enum describing the restart level. May be {@code null}
         * @return a builder that can be used to continue building the attribute definition
         */
        @SuppressWarnings("WeakerAccess")
        public Builder setRestartLevel(RestartLevel restartLevel) {
            this.restartLevel = restartLevel;
            return this;
        }

        /**
         * Marks the attribute as one where a modification to the attribute can be applied to the runtime
         * with requiring a restart of any services.
         * <p>
         * Equivalent to {@code setRestartLevel(RestartLevel.NONE)}.
         *
         * @return a builder that can be used to continue building the attribute definition
         */
        @SuppressWarnings({"WeakerAccess", "unused"})
        public Builder setRestartNone() {
            return setRestartLevel(RestartLevel.NONE);
        }

        /**
         * Marks the attribute as one where a modification to the attribute can only be applied to the runtime via a
         * restart of all services (via the {@code reload} management operation), but does not require a full jvm restart.
         * <p>
         * Equivalent to {@code setRestartLevel(RestartLevel.ALL_SERVICES)}.
         *
         * @return a builder that can be used to continue building the attribute definition
         */
        public Builder setRestartAllServices() {
            return setRestartLevel(RestartLevel.ALL_SERVICES);
        }

        /**
         * Marks the attribute as one where a modification to the attribute can only be applied to the runtime via
         * a full jvm restart.
         * <p>
         * Equivalent to {@code setRestartLevel(RestartLevel.JVM)}.
         *
         * @return a builder that can be used to continue building the attribute definition
         */
        @SuppressWarnings({"unused", "WeakerAccess"})
        public Builder setRestartJVM() {
            return setRestartLevel(RestartLevel.JVM);
        }

        /**
         * Marks the attribute as one where a modification to the attribute can only be applied to the runtime via
         * a restart of services associated with the attribute's resource (and any services that directly or transitively
         * depend on those services), with the {@code restart-resource-services}
         * operation header required as part of the modifying operation in order to enable that service restart.
         * In the absence of that header the behavior of the attribute is the same as if the
         * {@link #setRestartAllServices()} setting had been applied.
         * <p>
         * Equivalent to {@code setRestartLevel(RestartLevel.RESOURCE_SERVICES)}.
         *
         * @return a builder that can be used to continue building the attribute definition
         */
        @SuppressWarnings({"unused", "WeakerAccess"})
        public Builder setRestartResourceServices() {
            return setRestartLevel(RestartLevel.RESOURCE_SERVICES);
        }

        /**
         * Marks this attribute as one whose value is the same as the value part of the last
         * {@link AddressElement element}
         * in its containing resource's {@link ResourceAddress}. The attribute
         * is also configured as {@link #setReadOnly() read-only} and as {@link #setResourceOnly() resource-only}.
         *
         * @return a builder that can be used to continue building the attribute definition
         */
        @SuppressWarnings({"unused", "WeakerAccess"})
        public Builder setResourceNameAlias() {
            flags.add(Flag.RESOURCE_NAME_ALIAS);
            flags.add(Flag.READ_ONLY);
            return setResourceOnly();
        }

        /**
         * Marks an attribute as only relevant to a resource, and not a valid parameter to an "add" operation that
         * creates that resource. By default, a resource's non-runtime-only attributes
         * are treated as valid parameters for an "add" operation; setting this turns that off.
         * Use {@link #setResourceNameAlias()} instead for the typical use case of
         * legacy "name" attributes that display the final value in the resource's address as an attribute.
         *
         * @return a builder that can be used to continue building the attribute definition
         *
         * @see #setResourceNameAlias()
         */
        public Builder setResourceOnly() {
            this.resourceOnly = true;
            return this;
        }

        /**
         * Sets access constraints to use with the attribute
         * @param accessConstraints the constraints
         * @return a builder that can be used to continue building the attribute definition
         */
        public Builder setAccessConstraints(AccessConstraintDefinition... accessConstraints) {
            this.accessConstraints = accessConstraints;
            return this;
        }

        /**
         * Adds an access constraint to the set used with the attribute
         * @param accessConstraint the constraint
         * @return a builder that can be used to continue building the attribute definition
         */
        public Builder addAccessConstraint(final AccessConstraintDefinition accessConstraint) {
            if (accessConstraints == null) {
                accessConstraints = new AccessConstraintDefinition[] {accessConstraint};
            } else {
                accessConstraints = Arrays.copyOf(accessConstraints, accessConstraints.length + 1);
                accessConstraints[accessConstraints.length - 1] = accessConstraint;
            }
            return this;
        }

        /**
         * Sets whether an access control check is required to implicitly set an attribute to {@code undefined}
         * in a resource "add" operation. "Implicitly" setting an attribute refers to not providing a value for
         * it in the add operation, leaving the attribute in an undefined state. If not set
         * the default value is whether the attribute {@link ItemDefinition#isRequired()} () is not required} and
         * has a {@link ItemDefinition#getDefaultValue() default value}.
         *
         * @param nullSignificant {@code true} if an undefined value is significant; {@code false} if it is not significant,
         *                                    even if a default value is configured
         * @return a builder that can be used to continue building the attribute definition
         */
        @SuppressWarnings("unused")
        public Builder setNullSignificant(boolean nullSignificant) {
            this.nullSignificant = nullSignificant;
            return this;
        }

    }
}
