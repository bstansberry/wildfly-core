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

import org.jboss.dmr.ModelNode;
import org.jboss.as.controller.ParameterCorrector;

/**
 * Wraps a legacy {@link ParameterCorrector} in an implementation that also supports
 * the non-legacy {@link org.wildfly.management.api.model.definition.ParameterCorrector}.
 *
 * @author Brian Stansberry
 */
public final class BridgeParameterCorrector implements ParameterCorrector, org.wildfly.management.api.model.definition.ParameterCorrector {

    private final ParameterCorrector legacyBridged;
    private final org.wildfly.management.api.model.definition.ParameterCorrector modernBridged;

    public BridgeParameterCorrector(ParameterCorrector bridged) {
        assert bridged != null;
        this.legacyBridged = bridged;
        this.modernBridged = null;
    }

    public BridgeParameterCorrector(org.wildfly.management.api.model.definition.ParameterCorrector bridged) {
        assert bridged != null;
        this.modernBridged = bridged;
        this.legacyBridged = null;
    }

    @Override
    public ModelNode correct(ModelNode newValue, ModelNode currentValue) {
        assert legacyBridged != null || modernBridged != null;
        return legacyBridged == null ? modernBridged.correct(newValue, currentValue) : legacyBridged.correct(newValue, currentValue);
    }
}
