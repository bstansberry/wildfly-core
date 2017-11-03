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

package org.jboss.as.controller.capability;

import java.util.Collections;
import java.util.Set;

import org.jboss.as.controller.PathAddress;

/**
 * Base class for RuntimeCapability because for some reason removing this kind of
 * base class leads to type erasure complaints re: iterating over the string from getRequirements()
 * if the calling code uses <pre>RuntimeCapability cap</pre> instead of <pre>RuntimeCapability<?> cap</pre>.
 *
 * @author Brian Stansberry
 */
abstract class CapabilityBase implements Capability {

    @Override
    public final String getName() {
        return getCapability().getName();
    }

    @Override
    public final Set<String> getRequirements() {
        return getCapability().getRequirements();
    }

    @Override
    public final Set<String> getOptionalRequirements() {
        return Collections.emptySet();
    }

    @Override
    public final Set<String> getRuntimeOnlyRequirements() {
        return Collections.emptySet();
    }

    @Override
    public final Set<String> getDynamicRequirements() {
        return Collections.emptySet();
    }

    @Override
    public final Set<String> getDynamicOptionalRequirements() {
        return Collections.emptySet();
    }

    @Override
    public final boolean isDynamicallyNamed() {
        return getCapability().isDynamicallyNamed();
    }

    @Override
    public final String getDynamicName(String dynamicNameElement) {
        return getCapability().getDynamicName(dynamicNameElement);
    }

    @Override
    public final String getDynamicName(PathAddress address) {
        return getCapability().getDynamicName(address.asResourceAddress());
    }


    abstract org.wildfly.management.api.capability.Capability getCapability();
}
