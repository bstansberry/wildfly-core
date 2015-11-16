/*
Copyright 2015 Red Hat, Inc.

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

package org.jboss.as.test.integration.management.util;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CHILDREN;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEPLOYMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INCLUDE_ALIASES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MODEL_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_DESCRIPTION_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RECURSIVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;

import java.io.IOException;
import java.io.PrintStream;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.junit.Assert;

/**
 * Utility to dump management API text descriptions to a CSV file.
 *
 * @author Brian Stansberry
 */
public class DescriptionDumper {

    public static void main(String[] args) {
        String pathString = getArg(args, 0, "/Users/bstansberry/Documents/EAP7/mgmt-api-descriptions-standalone.csv");
        String host = getArg(args, 1, "localhost");
        String port = getArg(args, 2, null);
        String protocol = getArg(args, 3, null);
        try (PrintStream ps = new PrintStream((Files.newOutputStream(Paths.get(pathString))));
                ModelControllerClient mcc = createClient(host, port, protocol)) {
            dumpDescriptions(mcc, ps);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String getArg(String[] args, int idx, String defaultVal) {
        return args == null || args.length <= idx ? defaultVal : args[idx];
    }

    private static ModelControllerClient createClient(String host, String portString, String protocol) throws UnknownHostException {
        int port = portString == null ? 9990 : Integer.parseInt(portString);
        if (protocol == null) {
            return ModelControllerClient.Factory.create(host, port);
        } else {
            return ModelControllerClient.Factory.create(protocol, host, port);
        }
    }

    private static void dumpDescriptions(ModelControllerClient mcc, PrintStream ps) throws IOException {

        final ModelNode operation = new ModelNode();
        operation.get(OP).set(READ_RESOURCE_DESCRIPTION_OPERATION);
        operation.get(OP_ADDR).setEmptyList();
        operation.get(RECURSIVE).set(true);
        operation.get(INCLUDE_ALIASES).set(true);

        final ModelNode result = mcc.execute(operation);
        Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());
        Assert.assertTrue(result.hasDefined(RESULT));

        PathAddress pa = PathAddress.pathAddress(DEPLOYMENT, "*");
        dumpResourceDescription(pa, result.get(RESULT, CHILDREN, DEPLOYMENT, MODEL_DESCRIPTION, "*"), ps);
    }

    private static void dumpResourceDescription(PathAddress pa, ModelNode resourceDescription, PrintStream ps) {
        String paString = pa.toCLIStyleString();
        String resDec = paString + "\tR\t\t" + resourceDescription.get(DESCRIPTION).asString();
        ps.println(resDec);
        if (resourceDescription.hasDefined(ModelDescriptionConstants.ATTRIBUTES)) {
            for (Property property : resourceDescription.get(ModelDescriptionConstants.ATTRIBUTES).asPropertyList()) {
                ModelNode attrdesc = property.getValue();
                String line = paString + "\tA\t" + property.getName() + "\t" + attrdesc.get(ModelDescriptionConstants.DESCRIPTION).asString();
                ps.println(line);
            }
        }

        if (resourceDescription.hasDefined(ModelDescriptionConstants.CHILDREN)) {
            List<Property> children = resourceDescription.get(ModelDescriptionConstants.CHILDREN).asPropertyList();
            // First write the text descriptions of the child types
            for (Property childTypeProp : children) {
                String childType = childTypeProp.getName();
                ModelNode childTypeDesc = childTypeProp.getValue();
                String line = paString + "\tC\t" + childType + "\t" + childTypeDesc.get(ModelDescriptionConstants.DESCRIPTION).asString();
                ps.println(line);
            }
            // Now recurse
            for (Property childTypeProp : children) {
                String childType = childTypeProp.getName();
                ModelNode childTypeDesc = childTypeProp.getValue();
                if (childTypeDesc.hasDefined(ModelDescriptionConstants.MODEL_DESCRIPTION)) {
                    for (Property childInstanceProp : childTypeDesc.get(ModelDescriptionConstants.MODEL_DESCRIPTION).asPropertyList()) {
                        PathAddress childAddress = pa.append(childType, childInstanceProp.getName());
                        dumpResourceDescription(childAddress, childInstanceProp.getValue(), ps);
                    }
                }
            }
        }

    }
}
