/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.criu;

import static org.wildfly.extension.criu.CRIUExtension.CRIUSubsystemRegistrar.NAME;
import static org.wildfly.extension.criu.CRIUExtension.CRIUSubsystemSchema.CURRENT;

import java.util.EnumSet;

import org.jboss.as.subsystem.test.AbstractSubsystemSchemaTest;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class CRIUSubsystemTestCase  extends AbstractSubsystemSchemaTest<CRIUExtension.CRIUSubsystemSchema> {

    @Parameters
    public static Iterable<CRIUExtension.CRIUSubsystemSchema> parameters() {
        return EnumSet.allOf(CRIUExtension.CRIUSubsystemSchema.class);
    }

    public CRIUSubsystemTestCase(CRIUExtension.CRIUSubsystemSchema schema) {
        super(NAME, new CRIUExtension(), schema, CURRENT);
    }
}
