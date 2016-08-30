/*
Copyright 2016 Red Hat, Inc.

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

package org.jboss.as.server.controller.resources;

import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleOperationDefinition;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.repository.ContentRepository;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.as.server.deployment.DeploymentAddHandler;
import org.jboss.as.server.deployment.DeploymentDeployHandler;
import org.jboss.as.server.deployment.DeploymentRedeployHandler;
import org.jboss.as.server.deployment.DeploymentRemoveHandler;
import org.jboss.as.server.deployment.DeploymentUndeployHandler;
import org.jboss.as.server.services.security.AbstractVaultReader;

/**
 * Definition of a system-controlled deployment.
 *
 * @author Brian Stansberry
 */
public class SystemDeploymentResourceDefinition extends DeploymentResourceDefinition {

    private static final PathElement PATH = PathElement.pathElement(ModelDescriptionConstants.SYSTEM_DEPLOYMENT);

    private final AbstractVaultReader vaultReader;

    private SystemDeploymentResourceDefinition(Parameters parameters, AbstractVaultReader vaultReader) {
        super(DeploymentResourceParent.SERVER, parameters);
        this.vaultReader = vaultReader;
    }

    public static SystemDeploymentResourceDefinition create(ContentRepository contentRepository, AbstractVaultReader vaultReader, ServerEnvironment serverEnvironment) {
        Parameters parameters = new Parameters(PATH, DeploymentAttributes.DEPLOYMENT_RESOLVER)
                .setAddHandler(DeploymentAddHandler.create(contentRepository, vaultReader))
                .setRemoveHandler(new DeploymentRemoveHandler(contentRepository, vaultReader));
        return new SystemDeploymentResourceDefinition(parameters, vaultReader);
    }

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        super.registerOperations(resourceRegistration);
        resourceRegistration.registerOperationHandler(cloneAsPrivate(DeploymentAttributes.DEPLOY_DEFINITION), new DeploymentDeployHandler(vaultReader));
        resourceRegistration.registerOperationHandler(cloneAsPrivate(DeploymentAttributes.UNDEPLOY_DEFINITION), new DeploymentUndeployHandler(vaultReader));
        resourceRegistration.registerOperationHandler(cloneAsPrivate(DeploymentAttributes.REDEPLOY_DEFINITION), new DeploymentRedeployHandler(vaultReader));
     }

    @Override
    protected void registerAddOperation(ManagementResourceRegistration registration, OperationStepHandler handler, OperationEntry.Flag... flags) {
        registration.registerOperationHandler(cloneAsPrivate(DeploymentAttributes.SERVER_DEPLOYMENT_ADD_DEFINITION), handler);
    }

    @Override
    protected void registerRemoveOperation(ManagementResourceRegistration registration, AbstractRemoveStepHandler handler, OperationEntry.Flag... flags) {

        OperationDefinition opDef = new SimpleOperationDefinitionBuilder(ModelDescriptionConstants.REMOVE, DeploymentAttributes.DEPLOYMENT_RESOLVER)
                .withFlags(flags)
                .setPrivateEntry()
                .build();
        registration.registerOperationHandler(opDef, handler);
    }

    private static OperationDefinition cloneAsPrivate(SimpleOperationDefinition toClone) {
        return new SimpleOperationDefinitionBuilder(toClone).setPrivateEntry().build();
    }
}
