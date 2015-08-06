package org.jboss.as.controller.transform;

import org.jboss.as.controller.NoopOperationStepHandler;
import org.jboss.as.controller.descriptions.NonResolvingResourceDescriptionResolver;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;

/**
 * @author Tomaz Cerar
 * @created 19.12.11 20:04
 */
public class RootSubsystemResource extends SimpleResourceDefinition {


    public static final RootSubsystemResource INSTANCE = new RootSubsystemResource();

    private RootSubsystemResource() {
        super(PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, "test-subsystem"),
                new NonResolvingResourceDescriptionResolver(),
                NoopOperationStepHandler.WITHOUT_RESULT,
                ReloadRequiredRemoveStepHandler.INSTANCE);
    }
}
