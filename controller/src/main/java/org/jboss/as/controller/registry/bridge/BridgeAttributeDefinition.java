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

package org.jboss.as.controller.registry.bridge;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jboss.as.controller.AbstractAttributeDefinitionBuilder;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.access.management.AccessConstraintDefinition;
import org.jboss.as.controller.access.management.ApplicationTypeAccessConstraintDefinition;
import org.jboss.as.controller.access.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.wildfly.management.api.model.definition.AttributeGroupDefinition;
import org.wildfly.management.api.model.definition.ItemDefinition;

/**
 * Provides a legacy {@link AttributeDefinition} view of a
 * {@link org.wildfly.management.api.model.definition.AttributeDefinition}.
 *
 * @author Brian Stansberry
 */
public final class BridgeAttributeDefinition extends AttributeDefinition {

    public static final class Builder extends AbstractAttributeDefinitionBuilder<Builder, BridgeAttributeDefinition> {

        public static Builder of(org.wildfly.management.api.model.definition.AttributeDefinition basis) {
            return new Builder(basis);
        }

        public static Builder of(org.wildfly.management.api.model.definition.AttributeDefinition basis, AttributeGroupDefinition groupDefinition) {
            Builder result = new Builder(basis);
            result.setAttributeGroup(groupDefinition.getName());
            return result;
        }

        private final org.wildfly.management.api.model.definition.AttributeDefinition basis;

        private Builder(org.wildfly.management.api.model.definition.AttributeDefinition basis) {
            super(basis.getItemDefinition().getName(), basis.getItemDefinition().getType());
            this.basis = basis;
        }

        @Override
        public BridgeAttributeDefinition build() {
            return new BridgeAttributeDefinition(this);
        }

        @Override
        protected ItemDefinition.Builder createItemDefinitionBuilder() {
            return basis.getItemDefinition().getBuilderToCopy();
        }
    }

    private final org.wildfly.management.api.model.definition.AttributeDefinition wrapped;

    private BridgeAttributeDefinition(Builder builder) {
        super(builder);
        this.wrapped = builder.basis;
    }

    @Override
    public List<AccessConstraintDefinition> getAccessConstraints() {
        List<org.wildfly.management.api.access.AccessConstraintDefinition> nonLegacy = wrapped.getAccessConstraints();
        if (nonLegacy.isEmpty()) {
            return Collections.emptyList();
        }
        List<AccessConstraintDefinition> result = new ArrayList<>(nonLegacy.size());
        for (org.wildfly.management.api.access.AccessConstraintDefinition acd : nonLegacy) {
            if (acd instanceof org.wildfly.management.api.access.SensitiveTargetAccessConstraintDefinition) {
                result.add(SensitiveTargetAccessConstraintDefinition.forDefinition((org.wildfly.management.api.access.SensitiveTargetAccessConstraintDefinition) acd));
            } else {
                result.add(ApplicationTypeAccessConstraintDefinition.forDefinition((org.wildfly.management.api.access.ApplicationTypeAccessConstraintDefinition) acd));
            }
        }
        return Collections.unmodifiableList(result);
    }

    @Override
    public ModelNode getUndefinedMetricValue() {
        return wrapped.getUndefinedMetricValue();
    }

    @Override
    public boolean isResourceOnly() {
        return wrapped.isResourceOnly();
    }

    @Override
    public Set<AttributeAccess.Flag> getImmutableFlags() {
        AttributeAccess.Flag restartFlag = AttributeAccess.Flag.fromRestartLevel(wrapped.getRestartLevel());
        Set<org.wildfly.management.api.model.definition.AttributeDefinition.Flag> flagSet = wrapped.getFlags();
        if (flagSet.isEmpty()) {
            return Collections.singleton(restartFlag);
        }
        Set<AttributeAccess.Flag> result = new HashSet<>(flagSet.size() + 1);
        result.add(restartFlag);
        for (org.wildfly.management.api.model.definition.AttributeDefinition.Flag flag : flagSet) {
            AttributeAccess.Flag oldFlag = AttributeAccess.Flag.fromNonLegacyFlag(flag);
            if (oldFlag != null) {
                result.add(oldFlag);
            }
        }
        return Collections.unmodifiableSet(result);
    }

    @Override
    protected Boolean getNilSignificant() {
        return wrapped.isNullSignificant();
    }
}
