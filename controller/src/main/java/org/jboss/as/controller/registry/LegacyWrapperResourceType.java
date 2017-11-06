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

package org.jboss.as.controller.registry;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.wildfly.management.api.AddressElement;
import org.wildfly.management.api.ResourceAddress;
import org.wildfly.management.api.model.ResourceType;

/**
 * Partial implementation of {@link ResourceType} that wraps a legacy {@link ImmutableManagementResourceRegistration}
 * and implements as many methods as possible by invoking equivalent methods on the wrapped object.
 *
 * @author Brian Stansberry
 */
abstract class LegacyWrapperResourceType implements ResourceType {

    private final ImmutableManagementResourceRegistration legacy;

    LegacyWrapperResourceType(ImmutableManagementResourceRegistration legacy) {
        this.legacy = legacy;
    }

    @Override
    public ResourceAddress getPathAddress() {
        return legacy.getPathAddress().asResourceAddress();
    }

    @Override
    public org.wildfly.management.api.ProcessType getProcessType() {
        org.jboss.as.controller.ProcessType legacyPT = legacy.getProcessType();
        return legacyPT.asNonLegacyProcessType();
    }

    @Override
    public boolean isRuntimeOnly() {
        return legacy.isRuntimeOnly();
    }

    @Override
    public boolean isRemote() {
        return legacy.isRemote();
    }

    @Override
    public boolean isAlias() {
        return legacy.isAlias();
    }

    @Override
    public Set<String> getAttributeNames() {
        return legacy.getAttributeNames(PathAddress.EMPTY_ADDRESS);
    }

    @Override
    public Set<String> getAttributeNames(ResourceAddress address) {
        return legacy.getAttributeNames(PathAddress.pathAddress(address));
    }

    @Override
    public Set<String> getChildNames() {
        return legacy.getChildNames(PathAddress.EMPTY_ADDRESS);
    }

    @Override
    public Set<String> getChildNames(ResourceAddress address) {
        return legacy.getChildNames(PathAddress.pathAddress(address));
    }

    @Override
    public Set<AddressElement> getChildAddresses(ResourceAddress address) {
        Set<PathElement> pes = legacy.getChildAddresses(PathAddress.pathAddress(address));
        if (pes.isEmpty()) {
            return Collections.emptySet();
        }
        Set<AddressElement> result = new LinkedHashSet<>(pes.size());
        for (PathElement pe : pes) {
            result.add(pe.asAddressElement());
        }
        return Collections.unmodifiableSet(result);
    }

    @Override
    public boolean isOrderedChildResource() {
        return legacy.isOrderedChildResource();
    }

    @Override
    public Set<String> getOrderedChildTypes() {
        return legacy.getOrderedChildTypes();
    }

    @Override
    public Set<org.wildfly.management.api.capability.RuntimeCapability> getCapabilities() {
        Set<org.jboss.as.controller.capability.RuntimeCapability> legacySet = legacy.getCapabilities();
        if (legacySet.isEmpty()) {
            return Collections.emptySet();
        }
        Set<org.wildfly.management.api.capability.RuntimeCapability> result = new LinkedHashSet<>(legacySet.size());
        for (org.jboss.as.controller.capability.RuntimeCapability rc : legacySet) {
            result.add(rc.asNonLegacyCapability());
        }
        return Collections.unmodifiableSet(result);
    }

    @Override
    public Set<org.wildfly.management.api.capability.RuntimeCapability> getIncorporatingCapabilities() {
        Set<org.jboss.as.controller.capability.RuntimeCapability> legacySet = legacy.getIncorporatingCapabilities();
        if (legacySet.isEmpty()) {
            return Collections.emptySet();
        }
        Set<org.wildfly.management.api.capability.RuntimeCapability> result = new LinkedHashSet<>(legacySet.size());
        for (org.jboss.as.controller.capability.RuntimeCapability rc : legacySet) {
            result.add(rc.asNonLegacyCapability());
        }
        return Collections.unmodifiableSet(result);
    }
}
