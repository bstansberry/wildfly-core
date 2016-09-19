/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.domain.controller;

import org.jboss.as.controller.extension.ExtensionRegistry;
import org.jboss.as.controller.transform.Transformers;
import org.jboss.as.protocol.mgmt.ManagementChannelHandler;
import org.jboss.as.repository.HostFileRepository;

/**
 * Internal API for master host controller functionality.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public interface DomainController extends HostRegistrations {

    /**
     * Registers a slave Host Controller with this domain controller.
     *
     *
     * @param hostName the name of the slave host
     * @param handler  handler for communications with the host
     * @param transformers transformation handler for converting resources and operations to forms appropriate for the slave
     * @param remoteConnectionId long identifying this specific connection to the host, or {@code null} if the host did not provide such an id
     * @param registerProxyController {@code true} if a proxy controller should be registered for the host; {@code false}
     *                                             if the host is in --admin-only mode and should not be visible to outside users
     * @throws SlaveRegistrationException  if there is a problem registering the host
     */
    void registerRemoteHost(final String hostName, final ManagementChannelHandler handler, final Transformers transformers,
                            Long remoteConnectionId, boolean registerProxyController) throws SlaveRegistrationException;

    /**
     * Check if a Host Controller is already registered with this domain controller.
     *
     * @param id the name of the host controller
     * @return <code>true</code> if there is such a host controller registered, <code>false</code> otherwise
     */
    boolean isHostRegistered(String id);

    /**
     * Unregisters a previously registered Host Controller.
     *
     * @param id the name of the previously
     *           registered Host Controller
     * @param remoteConnectionId long identifying the specific connection to the host, or {@code null}. If {@code null}
     *                           the host's registration will be removed regardless of any remote connection id
     *                           that was provided at registration. If not {@code null}, the registration will only
     *                           be removed if the currently registered id matches the given id
     * @param cleanUnregistration flag indication whether the host was cleanly unregistered
     */
    void unregisterRemoteHost(final String id, Long remoteConnectionId, boolean cleanUnregistration);

    /**
     * Asynchronously ping the slave host with the given {@code hostName} to validate its connection.
     * @param hostName the name of the slave host
     */
    void pingRemoteHost(String hostName);

    /**
     * Get the extension registry.
     *
     * @return the extension registry
     */
    ExtensionRegistry getExtensionRegistry();

    /**
     * Gets the file repository backing this host controller
     *
     * @return the file repository
     */
    HostFileRepository getLocalFileRepository();
}
