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
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.wildfly.management.api.AddressElement;
import org.wildfly.management.api.model.MutableResource;

/**
 * Bridges between the legacy resource API and the current one by wrapping a current resource
 * and implementing the legacy API.
 *
 * @author Brian Stansberry
 */
public class CurrentWrapperBridgeResource implements BridgeResource, Resource.ResourceEntry {

    private final org.wildfly.management.api.model.Resource wrapped;

    public CurrentWrapperBridgeResource(org.wildfly.management.api.model.Resource wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public org.wildfly.management.api.model.Resource asResource() {
        return wrapped;
    }

    @Override
    public Resource asLegacyResource() {
        return this;
    }

    @Override
    public ModelNode getModel() {
        return wrapped.getModel();
    }

    @Override
    public void writeModel(ModelNode newModel) {
        // Code written for the legacy API should not be manipulating current resources
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isModelDefined() {
        return wrapped.isModelDefined();
    }

    @Override
    public boolean hasChild(PathElement element) {
        return wrapped.hasChild(element.asAddressElement());
    }

    @Override
    public Resource getChild(PathElement element) {
        org.wildfly.management.api.model.Resource res = wrapped.getChild(element.asAddressElement());
        return res instanceof Resource ? (Resource) res : new CurrentWrapperBridgeResource(res);
    }

    @Override
    public Resource requireChild(PathElement element) {
        org.wildfly.management.api.model.Resource res = wrapped.requireChild(element.asAddressElement());
        return res instanceof Resource ? (Resource) res : new CurrentWrapperBridgeResource(res);
    }

    @Override
    public boolean hasChildren(String childType) {
        return wrapped.hasChildren(childType);
    }

    @Override
    public Resource navigate(PathAddress address) {
        org.wildfly.management.api.model.Resource res = wrapped.navigate(address.asResourceAddress());
        return res instanceof Resource ? (Resource) res : new CurrentWrapperBridgeResource(res);
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
    public Set<ResourceEntry> getChildren(String childType) {
        Set<org.wildfly.management.api.model.Resource> set = wrapped.getChildren(childType);
        if (set.isEmpty()) {
            return Collections.emptySet();
        }
        Set<ResourceEntry> result = new HashSet<>(set.size());
        for (org.wildfly.management.api.model.Resource res : set) {
            if (res instanceof ResourceEntry) {
                result.add((ResourceEntry) res);
            } else {
                result.add(new CurrentWrapperBridgeResource(res));
            }
        }
        return Collections.unmodifiableSet(result);
    }

    @Override
    public void registerChild(PathElement address, Resource resource) {
        if (wrapped instanceof MutableResource) {
            org.wildfly.management.api.model.Resource res =
                    resource instanceof org.wildfly.management.api.model.Resource
                            ? (org.wildfly.management.api.model.Resource) resource
                            : new LegacyWrapperBridgeResource(resource, address);
            ((MutableResource) wrapped).addChild(address.asAddressElement(), res);
        } else {
            throw new MutableResource.ImmutableTypeException(wrapped.getAddressElement(), address.getKey());
        }
    }

    @Override
    public void registerChild(PathElement address, int index, Resource resource) {
        if (wrapped instanceof MutableResource) {
            org.wildfly.management.api.model.Resource res =
                    resource instanceof org.wildfly.management.api.model.Resource
                        ? (org.wildfly.management.api.model.Resource) resource
                        : new LegacyWrapperBridgeResource(resource, address);
            ((MutableResource) wrapped).addChild(address.asAddressElement(), index, res);
        } else {
            throw new MutableResource.ImmutableTypeException(wrapped.getAddressElement(), address.getKey());
        }
    }

    @Override
    public Resource removeChild(PathElement address) {
        if (wrapped instanceof MutableResource) {
            org.wildfly.management.api.model.Resource removed = ((MutableResource) wrapped).removeChild(address.asAddressElement());
            if (removed == null || removed instanceof Resource) {
                return (Resource) removed;
            }
            return new CurrentWrapperBridgeResource(removed);
        } else {
            throw new MutableResource.ImmutableTypeException(wrapped.getAddressElement(), address.getKey());
        }
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
    public CurrentWrapperBridgeResource clone() {
        return new CurrentWrapperBridgeResource(wrapped.clone());
    }

    @Override
    public String getName() {
        return wrapped.getName();
    }

    @Override
    public PathElement getPathElement() {
        AddressElement ae = wrapped.getAddressElement();
        return ae == null ? null : PathElement.pathElement(ae);
    }
}
