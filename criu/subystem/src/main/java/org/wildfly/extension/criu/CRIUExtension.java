/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.criu;

import static org.jboss.as.controller.ModelController.CheckpointIntegration.CheckpointStrategy.RELOAD_TO_MODEL;
import static org.jboss.as.controller.ModelController.CheckpointIntegration.CheckpointStrategy.SUSPEND_RESUME;
import static org.wildfly.extension.criu.CRIUIntegration.FEATURE_STABILITY;

import org.jboss.as.controller.AbstractWriteAttributeHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelController;
import org.jboss.as.controller.ModelController.CheckpointIntegration.CheckpointStrategy;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.PersistentResourceXMLDescription;
import org.jboss.as.controller.PersistentSubsystemSchema;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.ResourceRegistration;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.SubsystemModel;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.SubsystemSchema;
import org.jboss.as.controller.access.AuthorizationResult;
import org.jboss.as.controller.access.constraint.SensitivityClassification;
import org.jboss.as.controller.access.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.as.controller.descriptions.ParentResourceDescriptionResolver;
import org.jboss.as.controller.descriptions.SubsystemResourceDescriptionResolver;
import org.jboss.as.controller.logging.ControllerLogger;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.xml.VersionedNamespace;
import org.jboss.as.version.Stability;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.staxmapper.IntVersion;
import org.wildfly.subsystem.SubsystemConfiguration;
import org.wildfly.subsystem.SubsystemExtension;
import org.wildfly.subsystem.SubsystemPersistence;
import org.wildfly.subsystem.resource.ManagementResourceRegistrar;
import org.wildfly.subsystem.resource.ManagementResourceRegistrationContext;
import org.wildfly.subsystem.resource.ResourceDescriptor;
import org.wildfly.subsystem.resource.SubsystemResourceDefinitionRegistrar;

/**
 * The extension class for the WildFly CRIU subsystem.
 *
 * @author <a href="mailto:brian.stansberry@redhat.com">Brian Stansberry</a>
 */
public final class CRIUExtension extends SubsystemExtension<CRIUExtension.CRIUSubsystemSchema> {

    public CRIUExtension() {
        super(SubsystemConfiguration.of(CRIUSubsystemRegistrar.NAME, CRIUSubsystemModel.CURRENT,
                        CRIUSubsystemRegistrar::new), SubsystemPersistence.of(CRIUSubsystemSchema.CURRENT));
    }

    @Override
    public Stability getStability() {
        return /*FEATURE_STABILITY*/ Stability.DEFAULT; // TODO change to experimental when wildfly-galleon-plugin can deal with that
    }

    /**
     * Model for the 'criu' subsystem.
     */
    public enum CRIUSubsystemModel implements SubsystemModel {
        VERSION_1_0_0(1, 0, 0),
        ;

        static final CRIUSubsystemModel CURRENT = VERSION_1_0_0;

        private final ModelVersion version;

        CRIUSubsystemModel(int major, int minor, int micro) {
            this.version = ModelVersion.create(major, minor, micro);
        }

        @Override
        public ModelVersion getVersion() {
            return this.version;
        }
    }

    /**
     * Schema for the 'criu' subsystem.
     */
    public enum CRIUSubsystemSchema  implements PersistentSubsystemSchema<CRIUSubsystemSchema> {

        VERSION_1_0_EXPERIMENTAL(1, 0, Stability.EXPERIMENTAL),
        ;

        static final CRIUSubsystemSchema CURRENT = VERSION_1_0_EXPERIMENTAL;

        private final VersionedNamespace<IntVersion, CRIUSubsystemSchema> namespace;

        CRIUSubsystemSchema(int major, int minor, Stability stability) {
            this.namespace = SubsystemSchema.createSubsystemURN(CRIUSubsystemRegistrar.NAME, stability, new IntVersion(major, minor));
        }

        @Override
        public VersionedNamespace<IntVersion, CRIUSubsystemSchema> getNamespace() {
            return this.namespace;
        }

        @Override
        public Stability getStability() {
            return FEATURE_STABILITY;
        }

        @Override
        public PersistentResourceXMLDescription getXMLDescription() {
            PersistentResourceXMLDescription.Factory factory = PersistentResourceXMLDescription.factory(this);
            return factory.builder(CRIUSubsystemRegistrar.PATH)
                    .addAttribute(CRIUSubsystemRegistrar.DEFAULT_CHECKPOINT_STRATEGY)
                    .build();
        }
    }

    /**
     * Registrar for the 'criu' subsystem root resource.
     */
    static final class CRIUSubsystemRegistrar implements SubsystemResourceDefinitionRegistrar {

        static final String NAME = "criu";
        static final PathElement PATH = SubsystemResourceDefinitionRegistrar.pathElement(NAME);
        static final ParentResourceDescriptionResolver RESOLVER = new SubsystemResourceDescriptionResolver(NAME, CRIUSubsystemRegistrar.class);

        private static final SensitiveTargetAccessConstraintDefinition CHECKPOINT_CONSTRAINT = new SensitiveTargetAccessConstraintDefinition(
                new SensitivityClassification(NAME, "checkpoint", false, false, true));

        private static final AttributeDefinition DEFAULT_CHECKPOINT_STRATEGY = SimpleAttributeDefinitionBuilder.create("default-checkpoint-strategy", ModelType.STRING)
                .setRequired(false)
                .setAllowExpression(true)
                .setAllowedValues(RELOAD_TO_MODEL.toString(), SUSPEND_RESUME.toString()) // note we don't expose currently unimplemented RELOAD_TO_DEPLOYMENT_INIT
                .setDefaultValue(new ModelNode(CheckpointStrategy.RELOAD_TO_MODEL.toString()))
                .setAccessConstraints(SensitiveTargetAccessConstraintDefinition.JVM, CHECKPOINT_CONSTRAINT)
                .build();

        private static final AttributeDefinition CHECKPOINT_SUPPORTED = SimpleAttributeDefinitionBuilder.create("checkpoint-supported", ModelType.BOOLEAN)
                .addFlag(AttributeAccess.Flag.STORAGE_RUNTIME)
                .setAccessConstraints(SensitiveTargetAccessConstraintDefinition.JVM, CHECKPOINT_CONSTRAINT)
                .build();

        private static final AttributeDefinition TRIGGER_CHECKPOINT_STRATEGY = SimpleAttributeDefinitionBuilder.create("strategy", ModelType.STRING)
                .setRequired(false)
                .setAllowedValues(RELOAD_TO_MODEL.toString(), SUSPEND_RESUME.toString()) // note we don't expose currently unimplemented RELOAD_TO_DEPLOYMENT_INIT
                .build();
        private static final OperationDefinition CHECKPOINT = new SimpleOperationDefinitionBuilder("checkpoint", RESOLVER)
                .setParameters(TRIGGER_CHECKPOINT_STRATEGY)
                .setAccessConstraints(SensitiveTargetAccessConstraintDefinition.JVM, CHECKPOINT_CONSTRAINT)
                .build();

        @Override
        public ManagementResourceRegistration register(SubsystemRegistration parent, ManagementResourceRegistrationContext context) {
            ManagementResourceRegistration registration = parent.registerSubsystemModel(ResourceDefinition.builder(ResourceRegistration.of(PATH), RESOLVER).build());
            CRIUExecutor criuExecutor = CRIUIntegration.getInstalledExecutor();
            ResourceDescriptor descriptor = ResourceDescriptor.builder(RESOLVER)
                    .addAttribute(DEFAULT_CHECKPOINT_STRATEGY, new AbstractWriteAttributeHandler<>() {

                        @Override
                        protected boolean applyUpdateToRuntime(OperationContext context, ModelNode operation,
                                                               String attributeName, ModelNode resolvedValue,
                                                               ModelNode currentValue, HandbackHolder<Object> handbackHolder) {
                            criuExecutor.setDefaultCheckpointStrategy(CheckpointStrategy.fromString(resolvedValue.asString()));
                            return false;
                        }

                        @Override
                        protected void revertUpdateToRuntime(OperationContext context, ModelNode operation,
                                                             String attributeName, ModelNode valueToRestore,
                                                             ModelNode valueToRevert, Object handback) {
                            criuExecutor.setDefaultCheckpointStrategy(CheckpointStrategy.fromString(valueToRestore.asString()));
                        }
                    }).build();
            ManagementResourceRegistrar.of(descriptor).register(registration);
            if (context.isRuntimeOnlyRegistrationValid()) {
                registration.registerReadOnlyAttribute(CHECKPOINT_SUPPORTED, (ctx, op) -> criuExecutor.isCheckpointingSupported());
                registration.registerOperationHandler(CHECKPOINT, (ctx, op) -> triggerCheckpoint(ctx, op, criuExecutor));
            }
            return registration;
        }

        private void triggerCheckpoint(OperationContext context, ModelNode op, CRIUExecutor criuExecutor) throws OperationFailedException {
            AuthorizationResult authorizationResult = context.authorizeOperation(op);
            if (authorizationResult.getDecision().equals(AuthorizationResult.Decision.DENY)) {
                throw ControllerLogger.ACCESS_LOGGER.unauthorized("checkpoint", context.getCurrentAddress(), authorizationResult.getExplanation());
            }
            String strategy = TRIGGER_CHECKPOINT_STRATEGY.resolveModelAttribute(context, op).asStringOrNull();
            if (strategy == null) {
                ModelNode model = context.readResource(PathAddress.EMPTY_ADDRESS).getModel();
                strategy = DEFAULT_CHECKPOINT_STRATEGY.resolveModelAttribute(context, model).asString();
            }
            criuExecutor.triggerCheckpoint(ModelController.CheckpointIntegration.CheckpointStrategy.fromString(strategy));
        }
    }
}
