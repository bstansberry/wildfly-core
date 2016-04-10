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

package org.jboss.as.host.controller.jgroups;

/**
 * Constants used in the inter-process protocol for the JBoss Remoting based
 * JGroups transport.
 *
 * @author Brian Stansberry
 */
final class JGroupsProtocol {

    static final String CHANNEL_NAME = "jgroups";

    static final byte JGROUPS_CONNECT = 0x5a;
    static final byte OK = 0x5b;
    static final byte JGROUPS_MESSAGE = 0x5c;
}
