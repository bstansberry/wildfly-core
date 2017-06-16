/*
 *
 *  * JBoss, Home of Professional Open Source.
 *  * Copyright 2012, Red Hat, Inc., and individual contributors
 *  * as indicated by the @author tags. See the copyright.txt file in the
 *  * distribution for a full listing of individual contributors.
 *  *
 *  * This is free software; you can redistribute it and/or modify it
 *  * under the terms of the GNU Lesser General Public License as
 *  * published by the Free Software Foundation; either version 2.1 of
 *  * the License, or (at your option) any later version.
 *  *
 *  * This software is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  * Lesser General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU Lesser General Public
 *  * License along with this software; if not, write to the Free
 *  * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 *  * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 *
 */

package org.wildfly.management.api.model.definition;

import static org.wildfly.management.api.model.definition.OperationDefinition.Flag.immutableSetOf;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.jboss.dmr.ModelType;
import org.wildfly.management.api.SchemaVersion;
import org.wildfly.management.api.access.AccessConstraintDefinition;
import org.wildfly.management.api.model.ResourceType;

/**
 * Defining characteristics of operation exposed by a {@link ResourceType}
 *
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a>
 */
public final class OperationDefinition {

    public enum EntryType {
        PUBLIC, PRIVATE
    }

    /** Flags to indicate special characteristics of an operation */
    public enum Flag {
        /** Operation only reads, does not modify */
        READ_ONLY,
        /** The operation modifies the configuration and can be applied to the runtime without requiring a restart */
        RESTART_NONE,
        /** The operation modifies the configuration but can only be applied to the runtime via a full jvm restart */
        RESTART_JVM,
        /** The operation modifies the configuration but can only be applied to the runtime via a restart of all services;
         *  however it does not require a full jvm restart */
        RESTART_ALL_SERVICES,
        /** The operation modifies the configuration but can only be applied to the runtime via a restart of services,
         *  associated with the affected resource, but does not require a restart of all services or a full jvm restart */
        RESTART_RESOURCE_SERVICES,
        /** A domain or host-level operation that should be pushed to the servers even if the default behavior
         *  would indicate otherwise */
        DOMAIN_PUSH_TO_SERVERS,
        /** A host-level operation that should only be executed on the HostController and not on the servers,
         * even if the default behavior would indicate otherwise */
        HOST_CONTROLLER_ONLY,
        /** A domain-level operation that should only be executed on the master HostController and not on the slaves,
         * even if the default behavior would indicate otherwise */
        MASTER_HOST_CONTROLLER_ONLY,
        /** Operations with this flag do not affect the persistent configuration model. The main intention for
         * this is to only make RUNTIME_ONLY methods on domain mode servers visible to end users. */
        RUNTIME_ONLY,
        /** Operations with this flag do not appear in management API description output but still can be invoked
         *  by external callers.  This is meant for operations that were not meant to be part of the supported external
         *  management API but users may have learned of them. Such ops should be evaluated for inclusion as normally
         *  described ops, or perhaps should be marked with {@link EntryType#PRIVATE} and external use thus disabled.
         *  This can also be used for ops that are invoked internally on one domain process by another domain process
         *  but where it's not possible for the caller to suppress the caller-type=user header from the op, making
         *  use of {@link EntryType#PRIVATE} not workable. */
        HIDDEN;

        private static final EnumSet<Flag> NONE = EnumSet.noneOf(Flag.class);
        private static final Map<EnumSet<Flag>, Set<Flag>> flagSets = new ConcurrentHashMap<>(16);
        public static Set<Flag> immutableSetOf(EnumSet<Flag> flags) {
            EnumSet<Flag> baseSet;
            if (flags == null) {
                baseSet = NONE;
            } else {
                baseSet = flags;
            }
            Set<Flag> result = flagSets.get(baseSet);
            if (result == null) {
                Set<Flag> immutable = Collections.unmodifiableSet(baseSet);
                Set<Flag> existing = flagSets.putIfAbsent(baseSet, immutable);
                result = existing == null ? immutable : existing;
            }

            return result;
        }

    }

    private final String name;
    private final ResourceDescriptionResolver resolver;
    private final ResourceDescriptionResolver attributeResolver;
    private final EntryType entryType;
    private final Set<Flag> flags;
    private final ItemDefinition[] parameters;
    private final ModelType replyType;
    private final ModelType replyValueType;
    private final boolean replyAllowNull;
    private final DeprecationData deprecationData;
    private final ItemDefinition[] replyParameters;
    private final List<AccessConstraintDefinition> accessConstraints;

    OperationDefinition(OperationDefinitionBuilder builder) {
        this.name = builder.name;
        this.resolver = builder.resolver;
        this.attributeResolver = builder.attributeResolver;
        this.entryType = builder.entryType;
        this.flags = immutableSetOf(builder.flags);
        this.parameters = builder.parameters;
        this.replyType = builder.replyType;
        this.replyValueType = builder.replyValueType;
        this.replyAllowNull = builder.replyAllowNull;
        this.deprecationData = builder.deprecationData;
        this.replyParameters = builder.replyParameters;
        if (builder.accessConstraints == null) {
            this.accessConstraints = Collections.emptyList();
        } else {
            this.accessConstraints = Collections.unmodifiableList(Arrays.asList(builder.accessConstraints));
        }

    }

    public String getName() {
        return name;
    }

    public EntryType getEntryType() {
        return entryType;
    }

    /**
     * Gets an immutable set of any {@link Flag flags} associated with the operation.
     * @return the flags. Will not return {@code null} be may be empty
     */
    public Set<Flag> getFlags() {
        return flags;
    }

    public ItemDefinition[] getParameters() {
        return parameters;
    }

    public ModelType getReplyType() {
        return replyType;
    }

    /**
     * Only required if the reply type is some form of collection.
     */
    public ModelType getReplyValueType() {
        return replyValueType;
    }

    public List<AccessConstraintDefinition> getAccessConstraints() {
        return accessConstraints;
    }

    public DeprecationData getDeprecationData() {
        return deprecationData;
    }

    public boolean isReplyAllowNull() {
        return replyAllowNull;
    }

    public ItemDefinition[] getReplyParameters() {
        return replyParameters;
    }

    /**
     * Gets the resolver for localized text descriptions of the operations.
     *
     * @return the resolver. Will not return {@code null}
     */
    public ResourceDescriptionResolver getResolver() {
        return resolver;
    }

    /**
     * Gets the resolver for localized text descriptions of the operation's parameters or reply.
     *
     * @return the resolver. Will not return {@code null}
     */
    public ResourceDescriptionResolver getAttributeResolver() {
        return attributeResolver == null ? resolver : attributeResolver;
    }

    public static class OperationDefinitionBuilder {
        private static final ItemDefinition[] NO_ATTRIBUTES = new ItemDefinition[0];

        private ResourceDescriptionResolver resolver;
        private ResourceDescriptionResolver attributeResolver;
        private String name;
        private OperationDefinition.EntryType entryType = OperationDefinition.EntryType.PUBLIC;
        private EnumSet<OperationDefinition.Flag> flags = EnumSet.noneOf(OperationDefinition.Flag.class);
        private ItemDefinition[] parameters = NO_ATTRIBUTES;
        private ModelType replyType;
        private ModelType replyValueType;
        private boolean replyAllowNull;
        private DeprecationData deprecationData = null;
        private ItemDefinition[] replyParameters = NO_ATTRIBUTES;
        private AccessConstraintDefinition[] accessConstraints;

        public OperationDefinitionBuilder(String name, ResourceDescriptionResolver resolver) {
            this.name = name;
            this.resolver = resolver;
        }


        public OperationDefinition build() {
            if (attributeResolver == null) {
                attributeResolver = resolver;
            }
            return new OperationDefinition(this);
        }

        private static EnumSet<OperationDefinition.Flag> getFlagsSet(OperationDefinition.Flag... vararg) {
            EnumSet<OperationDefinition.Flag> result = EnumSet.noneOf(OperationDefinition.Flag.class);
            if (vararg != null && vararg.length > 0) {
                Collections.addAll(result, vararg);
            }
            return result;
        }

        public OperationDefinitionBuilder setEntryType(OperationDefinition.EntryType entryType) {
            this.entryType = entryType;
            return this;
        }

        public OperationDefinitionBuilder setPrivateEntry() {
            this.entryType = OperationDefinition.EntryType.PRIVATE;
            return this;
        }

        public OperationDefinitionBuilder withFlags(EnumSet<OperationDefinition.Flag> flags) {
            this.flags.addAll(flags);
            return this;
        }

        public OperationDefinitionBuilder withFlags(OperationDefinition.Flag... flags) {
            this.flags.addAll(getFlagsSet(flags));
            return this;
        }

        public OperationDefinitionBuilder withFlag(OperationDefinition.Flag flag) {
            this.flags.add(flag);
            return this;
        }

        public OperationDefinitionBuilder setRuntimeOnly() {
            return withFlag(OperationDefinition.Flag.RUNTIME_ONLY);
        }

        public OperationDefinitionBuilder setReadOnly() {
            return withFlag(OperationDefinition.Flag.READ_ONLY);
        }

        public OperationDefinitionBuilder setParameters(ItemDefinition... parameters) {//todo add validation for same param name
            this.parameters = parameters;
            return this;
        }

        public OperationDefinitionBuilder addParameter(ItemDefinition parameter) {
            int i = parameters.length;
            parameters = Arrays.copyOf(parameters, i + 1);
            parameters[i] = parameter;
            return this;
        }

        public OperationDefinitionBuilder setReplyType(ModelType replyType) {
            this.replyType = replyType;
            return this;
        }

        public OperationDefinitionBuilder setReplyValueType(ModelType replyValueType) {
            this.replyValueType = replyValueType;
            return this;
        }

        public OperationDefinitionBuilder allowReturnNull() {
            this.replyAllowNull = true;
            return this;
        }

        /**
         * Marks the operation as deprecated since the given API version. This is equivalent to calling
         * {@link #setDeprecated(SchemaVersion, boolean)} with the {@code notificationUseful} parameter
         * set to {@code true}.
         * @param since the API version, with the API being the one (core or a subsystem) in which the attribute is used
         * @return a builder that can be used to continue building the attribute definition
         */
        public OperationDefinitionBuilder setDeprecated(SchemaVersion since) {
            return setDeprecated(since, true);
        }

        /**
         * Marks the attribute as deprecated since the given API version, with the ability to configure that
         * notifications to the user (e.g. via a log message) about deprecation of the operation should not be emitted.
         * Notifying the user should only be done if the user can take some action in response. Advising that
         * something will be removed in a later release is not useful if there is no alternative in the
         * current release. If the {@code notificationUseful} param is {@code true} the text
         * description of the operation deprecation available from the {@code read-operation-description}
         * management operation should provide useful information about how the user can avoid using
         * the operation.
         *
         * @param since the API version, with the API being the one (core or a subsystem) in which the attribute is used
         * @param notificationUseful whether actively advising the user about the deprecation is useful
         * @return a builder that can be used to continue building the attribute definition
         */
        public OperationDefinitionBuilder setDeprecated(SchemaVersion since, boolean notificationUseful) {
            this.deprecationData = new DeprecationData(since, notificationUseful);
            return this;
        }

        public OperationDefinitionBuilder setReplyParameters(ItemDefinition... replyParameters) {
            this.replyParameters = replyParameters;
            return this;
        }

        public OperationDefinitionBuilder setAttributeResolver(ResourceDescriptionResolver resolver) {
            this.attributeResolver = resolver;
            return this;
        }

        public OperationDefinitionBuilder setAccessConstraints(AccessConstraintDefinition... accessConstraints) {
            this.accessConstraints = accessConstraints;
            return this;
        }

        public OperationDefinitionBuilder addAccessConstraint(final AccessConstraintDefinition accessConstraint) {
            if (accessConstraints == null) {
                accessConstraints = new AccessConstraintDefinition[] {accessConstraint};
            } else {
                accessConstraints = Arrays.copyOf(accessConstraints, accessConstraints.length + 1);
                accessConstraints[accessConstraints.length - 1] = accessConstraint;
            }
            return this;
        }

    }
}
