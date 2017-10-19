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

package org.jboss.as.controller;

import org.jboss.as.controller.registry.DelegatingResource;
import org.jboss.as.controller.registry.bridge.BridgeResource;
import org.wildfly.management.api.model.Resource;

/**
 * {@link BridgeResource} that works by delegating to a potentially changing delegate
 * provided by a {@link ResourceDelegateProvider}.
 *
 * @author Brian Stansberry
 */
final class DelegatingBridgeResource implements BridgeResource {

    /**
     * Provides a delegate for use by a {@code DelegatingResource}.
     * Does not need to provide the same delegate for every call, allowing a copy-on-write
     * semantic for the underlying {@code Resource}.
     */
    interface ResourceDelegateProvider {
        /**
         * Gets the delegate.
         * @return the delegate. Cannot return {@code null}
         */
        BridgeResource getDelegateResource();
    }

    private final ResourceDelegateProvider delegateProvider;


    /**
     * Creates a new DelegatingResource with a possibly changing delegate.
     *
     * @param delegateProvider provider of the delegate. Cannot be {@code null}
     */
    DelegatingBridgeResource(ResourceDelegateProvider delegateProvider) {
        assert delegateProvider != null;
        assert delegateProvider.getDelegateResource() != null;
        this.delegateProvider = delegateProvider;
    }

    @Override
    public Resource asResource() {
        org.wildfly.management.api.model.DelegatingResource.ResourceDelegateProvider provider =
                new org.wildfly.management.api.model.DelegatingResource.ResourceDelegateProvider() {
            @Override
            public Resource getDelegateResource() {
                return getDelegate().asResource();
            }
        };
        return new org.wildfly.management.api.model.DelegatingResource(provider);
    }

    @Override
    public org.jboss.as.controller.registry.Resource asLegacyResource() {
        DelegatingResource.ResourceDelegateProvider provider = new DelegatingResource.ResourceDelegateProvider() {
            @Override
            public org.jboss.as.controller.registry.Resource getDelegateResource() {
                return getDelegate().asLegacyResource();
            }
        };
        return new DelegatingResource(provider);
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override
    public BridgeResource clone() {
        return getDelegate().clone();
    }

    private BridgeResource getDelegate() {
        return this.delegateProvider.getDelegateResource();
    }
}
