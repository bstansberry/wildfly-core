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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.dmr.ModelNode;
import org.wildfly.management.api.AddressElement;
import org.wildfly.management.api.ResourceAddress;
import org.wildfly.management.api.model.Resource;

/**
 * Bridges between the legacy resource API and the current one by wrapping a legacy resource
 * and implementing the current API.
 *
 * @author Brian Stansberry
 */
public final class LegacyWrapperBridgeResource implements BridgeResource, Resource  {

    private final org.jboss.as.controller.registry.Resource wrapped;
    private final PathElement relativePath;

    public LegacyWrapperBridgeResource(org.jboss.as.controller.registry.Resource.ResourceEntry wrapped) {
        this(wrapped, wrapped.getPathElement());
    }

    public LegacyWrapperBridgeResource(org.jboss.as.controller.registry.Resource wrapped, PathElement relativePath) {
        this.wrapped = wrapped;
        this.relativePath = relativePath;
    }

    @Override
    public Resource asResource() {
        return this;
    }

    @Override
    public org.jboss.as.controller.registry.Resource asLegacyResource() {
        return wrapped;
    }

    @Override
    public ModelNode getModel() {
        return wrapped.getModel();
    }

    @Override
    public boolean isModelDefined() {
        return wrapped.isModelDefined();
    }

    @Override
    public boolean isModelResolved() {
        return false;
    }

    @Override
    public boolean hasChild(AddressElement element) {
        return wrapped.hasChild(PathElement.pathElement(element));
    }

    @Override
    public Resource getChild(AddressElement element) {
        PathElement pe = PathElement.pathElement(element);
        org.jboss.as.controller.registry.Resource res = wrapped.getChild(pe);
        return res instanceof Resource ? (Resource) res : new LegacyWrapperBridgeResource(res, pe);
    }

    @Override
    public Resource requireChild(AddressElement element) {
        PathElement pe = PathElement.pathElement(element);
        org.jboss.as.controller.registry.Resource res = wrapped.requireChild(pe);
        return res instanceof Resource ? (Resource) res : new LegacyWrapperBridgeResource(res, pe);
    }

    @Override
    public boolean hasChildren(String childType) {
        return wrapped.hasChildren(childType);
    }

    @Override
    public Resource navigate(ResourceAddress address) {
        org.jboss.as.controller.registry.Resource res = wrapped.navigate(PathAddress.pathAddress(address));
        PathElement pe = address.size() == 0 ? null : PathElement.pathElement(address.getLastElement());
        return res instanceof Resource ? (Resource) res : new LegacyWrapperBridgeResource(res, pe);
    }

    @Override
    public Set<String> getChildTypes() {
        return wrapped.getChildTypes();
    }

    @Override
    public Set<String> getChildrenNames(String childType) {
        return wrapped.getChildrenNames(childType);
    }

    @Override
    public Set<Resource> getChildren(String childType) {
        Set<org.jboss.as.controller.registry.Resource.ResourceEntry> set = wrapped.getChildren(childType);
        if (set.isEmpty()) {
            return Collections.emptySet();
        }
        Set<Resource> result = new HashSet<>(set.size());
        for (org.jboss.as.controller.registry.Resource.ResourceEntry res : set) {
            if (res instanceof Resource) {
                result.add((Resource) res);
            } else {
                result.add(new LegacyWrapperBridgeResource(res));
            }
        }
        return Collections.unmodifiableSet(result);
    }

    @Override
    public Set<String> getOrderedChildTypes() {
        return wrapped.getOrderedChildTypes();
    }

    @Override
    public boolean isRuntime() {
        return wrapped.isRuntime();
    }

    @Override
    public boolean isProxy() {
        return wrapped.isProxy();
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override
    public LegacyWrapperBridgeResource clone() {
        return new LegacyWrapperBridgeResource(wrapped.clone(), relativePath);
    }

    @Override
    public String getName() {
        return relativePath == null ? null : relativePath.getValue();
    }

    @Override
    public AddressElement getAddressElement() {
        return relativePath == null ? null : relativePath.asAddressElement();
    }
}
