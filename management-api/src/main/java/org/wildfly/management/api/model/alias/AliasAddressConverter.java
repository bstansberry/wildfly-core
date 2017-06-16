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

package org.wildfly.management.api.model.alias;

import org.wildfly.management.api.PathAddress;

/**
 * Performs resource address conversions for management resources that are aliases
 * for other resources.
 *
 * @author Brian Stansberry
 */
@FunctionalInterface
public interface AliasAddressConverter {

    /**
     * Convert the alias address to the target address.
     *
     * @param aliasAddress the alias address. Will not be {@code null}
     * @param aliasContext context that can be used
     * @return the target address
     */
     PathAddress convertToTargetAddress(PathAddress aliasAddress, AliasContext aliasContext);
}
