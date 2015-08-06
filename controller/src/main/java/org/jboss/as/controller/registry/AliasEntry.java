/*
* JBoss, Home of Professional Open Source.
* Copyright 2011, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.controller.registry;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;

/**
 * Entry describing the relationship between an {@link ManagementResourceRegistration#isAlias() alias resource
 * registration} and the primary resource registration of the alias.
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public abstract class AliasEntry {

    private final ManagementResourceRegistration target;
    private volatile PathAddress aliasAddress;

    protected AliasEntry(final ManagementResourceRegistration target) {
        this.target = target;
    }

    /**
     * Only to be called by the parent resource registration when this alias entry is first
     * {@link ManagementResourceRegistration#registerAlias(PathElement, AliasEntry) registered}.
     * @param aliasAddress the address of the resource registration created upon registration of this entry
     *
     * @deprecated unnecessary as the same value is used as the param to {@link #convertToTargetAddress(PathAddress)}
     */
    @Deprecated
    public final void setAliasAddress(PathAddress aliasAddress) {
        assert this.aliasAddress == null || this.aliasAddress.equals(aliasAddress);
        this.aliasAddress = aliasAddress;
    }

    /** @deprecated implementations should use the address passed as a param to {@link #convertToTargetAddress(PathAddress)},
     * which will be the same value if this method is not overridden  */
    @Deprecated
    protected PathAddress getAliasAddress() {
        return aliasAddress;
    }

    public PathAddress getTargetAddress() {
        return target.getPathAddress();
    }

    public abstract PathAddress convertToTargetAddress(PathAddress address);
}
