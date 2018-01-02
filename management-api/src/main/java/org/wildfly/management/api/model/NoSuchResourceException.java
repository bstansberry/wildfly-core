/*
Copyright 2018 Red Hat, Inc.

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

import org.jboss.dmr.ModelNode;
import org.wildfly.management.api.AddressElement;
import org.wildfly.management.api.OperationClientException;
import org.wildfly.management.api.ResourceAddress;
import org.wildfly.management.api._private.ControllerLoggerDuplicate;

/**
 * An {@link OperationClientException} variant that can be thrown by {@link Resource#requireChild(AddressElement)} and
 * {@link Resource#navigate(ResourceAddress)} implementations to indicate a client error when invoking a
 * management operation.
 */
public final class NoSuchResourceException extends OperationClientException {

    private static final long serialVersionUID = -2409240663987141424L;

    public NoSuchResourceException(AddressElement childPath) {
        this(ControllerLoggerDuplicate.ROOT_LOGGER.childResourceNotFound(childPath));
    }

    public NoSuchResourceException(String message) {
        super(message);
    }

    @Override
    public ModelNode getFailureDescription() {
        return new ModelNode(getLocalizedMessage());
    }

    @Override
    public String toString() {
        return super.toString() + " [ " + getFailureDescription() + " ]";
    }
}
