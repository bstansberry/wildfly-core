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

package org.wildfly.management.api.model;

import java.util.Set;

import org.jboss.dmr.ModelNode;
import org.wildfly.management.api.AddressElement;
import org.wildfly.management.api.ResourceAddress;

/**
 * {@link Resource} implementation that simply delegates to another
 * {@link Resource}. Intended as a convenience class to allow overriding
 * of standard behaviors.
 *
 * @author Brian Stansberry
 */
public class DelegatingResource implements Resource {

    /**
     * Provides a delegate for use by a {@code DelegatingResource}.
     * Does not need to provide the same delegate for every call, allowing a copy-on-write
     * semantic for the underlying {@code Resource}.
     */
    public interface ResourceDelegateProvider {
        /**
         * Gets the delegate.
         * @return the delegate. Cannot return {@code null}
         */
        Resource getDelegateResource();
    }

    private final ResourceDelegateProvider delegateProvider;

    /**
     * Creates a new DelegatingResource with a fixed delegate.
     *
     * @param delegate the delegate. Cannot be {@code null}
     */
    public DelegatingResource(final Resource delegate) {
        this(new ResourceDelegateProvider() {
            @Override
            public Resource getDelegateResource() {
                return delegate;
            }
        });
    }

    /**
     * Creates a new DelegatingResource with a possibly changing delegate.
     *
     * @param delegateProvider provider of the delegate. Cannot be {@code null}
     */
    public DelegatingResource(ResourceDelegateProvider delegateProvider) {
        assert delegateProvider != null;
        assert delegateProvider.getDelegateResource() != null;
        this.delegateProvider = delegateProvider;
    }

    @Override
    public String getName() {
        return getDelegate().getName();
    }

    @Override
    public AddressElement getAddressElement() {
        return getDelegate().getAddressElement();
    }

    @Override
    public ModelNode getModel() {
        return getDelegate().getModel();
    }

    @Override
    public boolean isModelDefined() {
        return getDelegate().isModelDefined();
    }

    @Override
    public boolean isModelResolved() {
        return getDelegate().isModelResolved();
    }

    @Override
    public boolean hasChild(AddressElement element) {
        return getDelegate().hasChild(element);
    }

    @Override
    public Resource getChild(AddressElement element) {
        return getDelegate().getChild(element);
    }

    @Override
    public Resource requireChild(AddressElement element) {
        return getDelegate().requireChild(element);
    }

    @Override
    public boolean hasChildren(String childType) {
        return getDelegate().hasChildren(childType);
    }

    @Override
    public Resource navigate(ResourceAddress address) {
        return getDelegate().navigate(address);
    }

    @Override
    public Set<String> getChildTypes() {
        return getDelegate().getChildTypes();
    }

    @Override
    public Set<String> getChildrenNames(String childType) {
        return getDelegate().getChildrenNames(childType);
    }

    @Override
    public Set<Resource> getChildren(String childType) {
        return getDelegate().getChildren(childType);
    }

    @Override
    public Set<String> getOrderedChildTypes() {
        return getDelegate().getOrderedChildTypes();
    }

    @Override
    public boolean isRuntime() {
        return getDelegate().isRuntime();
    }

    @Override
    public boolean isProxy() {
        return getDelegate().isProxy();
    }

    @SuppressWarnings({"CloneDoesntCallSuperClone"})
    @Override
    public Resource clone() {
        return getDelegate().clone();
    }

    private Resource getDelegate() {
        return this.delegateProvider.getDelegateResource();
    }
}
