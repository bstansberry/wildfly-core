/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site=> http=>//www.fsf.org.
 */

package org.wildfly.test;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.dmr.ModelNode;

/**
 * TODO class javadoc.
 *
 * @author Brian Stansberry
 */
public class DatasourceRunner {

    public static void main(String[] args) {
        new DatasourceRunner().run();
    }

    private void run() {
        try (ModelControllerClient client = getModelControllerClient()) {
            long totalElapsed = 0;
            for (int j = 0; j < 8; j++) {
                ModelNode steps = new ModelNode();
                for (int i = (j * 1000) + 1; i <= (j * 1000) + 1000; i++) {
                    PathElement pe = PathElement.pathElement("xa-data-source", "datasource" + i);
                    BASE_PES[2] = TOKEN_PES[2] = TOKEN1_PES[2] = pe;
                    MAIN_ADD.get("address").set(PathAddress.pathAddress(BASE_PES).toModelNode());
                    steps.add(MAIN_ADD.clone());
                    PROP_1.get("address").set(PathAddress.pathAddress(TOKEN_PES).toModelNode());
                    steps.add(PROP_1.clone());
                    PROP_2.get("address").set(PathAddress.pathAddress(TOKEN1_PES).toModelNode());
                    steps.add(PROP_2.clone());
                }
                COMPOSITE.get("steps").set(steps);
                System.out.println("Executing " + j);
                long start = System.nanoTime();
                ModelNode response = client.execute(COMPOSITE);
                long elapsed = System.nanoTime() - start;
                totalElapsed += elapsed;
                int count = (j * 1000) + 1000;
                System.out.println(count + " elapsed: " + elapsed + " (" + TimeUnit.NANOSECONDS.toMillis(elapsed) + ") total: " + totalElapsed + " (" + TimeUnit.NANOSECONDS.toMillis(totalElapsed) + ")  average: " + (totalElapsed / count) + " (" + TimeUnit.NANOSECONDS.toMillis((totalElapsed / count)) + ")");
                if (!"success".equals(response.get("outcome").asString())) {
                    throw new IllegalStateException(response.asString());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();  //TODO
        }
    }

    private ModelControllerClient getModelControllerClient() throws UnknownHostException {
        return ModelControllerClient.Factory.create("remote", "localhost", 9999);
    }

    static final PathElement PROFILE_PE = PathElement.pathElement("profile", "full-ha");
    static final PathElement SUBS_PE = PathElement.pathElement("subsystem", "datasources");
    static final PathElement TOKEN_PE = PathElement.pathElement("xa-datasource-properties", "token");
    static final PathElement TOKEN1_PE = PathElement.pathElement("xa-datasource-properties", "token1");
    static final PathElement[] BASE_PES = { PROFILE_PE, SUBS_PE, null};
    static final PathElement[] TOKEN_PES = { PROFILE_PE, SUBS_PE, null, TOKEN_PE};
    static final PathElement[] TOKEN1_PES = { PROFILE_PE, SUBS_PE, null, TOKEN1_PE};
    static final ModelNode COMPOSITE = Util.createEmptyOperation("composite", PathAddress.EMPTY_ADDRESS);
    static final ModelNode MAIN_ADD  = ModelNode.fromString("{  " +
            "\"operation\" => \"add\"," +
            "    \"xa-datasource-class\" => \"token\"," +
            "    \"driver-name\" => \"token\"," +
            "    \"url-delimiter\" => \"token\"," +
            "    \"url-selector-strategy-class-name\" => \"token\"," +
            "    \"new-connection-sql\" => \"string\"," +
            "    \"transaction-isolation\" => \"TRANSACTION_SERIALIZABLE\"," +
            "    \"is-same-rm-override\" => false," +
            "    \"interleaving\" => true," +
            "    \"no-tx-separate-pools\" => true," +
            "    \"pad-xid\" => false," +
            "    \"wrap-xa-resource\" => false," +
            "    \"min-pool-size\" => 200," +
            "    \"max-pool-size\" => 200," +
            "    \"pool-prefill\" => false," +
            "    \"pool-use-strict-min\" => true," +
            "    \"flush-strategy\" => \"EntirePool\"," +
            "    \"allow-multiple-users\" => true," +
            "    \"user-name\" => \"token\"," +
            "    \"password\" => \"token\"," +
            "    \"security-domain\" => undefined," +
            "    \"reauth-plugin-class-name\" => \"token\"," +
            "    \"reauth-plugin-properties\" => {" +
            "\"token\" => \"token\"" +
            "},   " +
            "\"valid-connection-checker-class-name\" => \"token\"," +
            "   \"valid-connection-checker-properties\" => {          " +
            "    \"token\" => \"token\", " +
            "    \"token1\" => \"token1\"" +
            " }," +
            "\"check-valid-connection-sql\" => \"string\"," +
            " \"validate-on-match\" => false,    " +
            "    \"background-validation\" => false," +
            "    \"background-validation-millis\" => 200," +
            "    \"use-fast-fail\" => true,  " +
            "    \"stale-connection-checker-class-name\" => \"token\"," +
            "    \"stale-connection-checker-properties\" => {" +
            " \"token\" => \"token\"" +
            " }," +
            "   \"exception-sorter-class-name\" => \"token\"," +
            "       \"exception-sorter-properties\" => {" +
            "   \"token\" => \"token\"" +
            " },     " +
            " \"blocking-timeout-wait-millis\" => 200, " +
            "    \"idle-timeout-minutes\" => 200, " +
            "    \"set-tx-query-timeout\" => true," +
            "    \"query-timeout\" => 200," +
            "    \"use-try-lock\" => 200," +
            "    \"allocation-retry\" => 200," +
            "    \"allocation-retry-wait-millis\" => 200," +
            "    \"xa-resource-timeout\" => 200," +
            "    \"track-statements\" => \"NOWARN\"," +
            "    \"prepared-statements-cache-size\" => 200," +
            "    \"share-prepared-statements\" => true," +
            "    \"recover-credential\" => undefined," +
            "    \"recover-plugin-class-name\" => \"token\"," +
            "    \"recover-plugin-properties\" => [{" +
            "     \"token\" => \"token\"" +
            " }],  " +
            "  \"no-recovery\" => false," +
            "      \"jndi-name\" => \"java:jboss/ENTRANDB\", " +
            "      \"pool-name\" => \"ENTRANDB10\"," +
            "      \"enabled\" => true," +
            "      \"use-java-context\" => true," +
            "      \"spy\" => false," +
            "      \"use-ccm\" => true," +
            "      \"statistics-enabled\" => false" +
            " }");
    static final ModelNode PROP_1 = ModelNode.fromString(" { " +
            "   \"operation\" => \"add\"," +
            "       \"value\" => \"per turbine\"" +
            "}");
    static final ModelNode PROP_2 = ModelNode.fromString("{ " +
            "    \"operation\" => \"add\"," +
            "         \"value\" => \"per turbine1\" " +
            " }");
}
