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

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public abstract class AliasEntry implements Cloneable {

    private volatile PathAddress aliasAddress;
    private final PathAddress targetAddress;

    protected AliasEntry(final ManagementResourceRegistration target) {
        this(target.getPathAddress());
    }

    protected AliasEntry(final PathAddress targetAddress) {
        this.targetAddress = targetAddress;
    }

    void setAliasAddress(PathAddress aliasAddress) {
        this.aliasAddress = aliasAddress;
    }

    protected PathAddress getAliasAddress() {
        return aliasAddress;
    }

    public PathAddress getTargetAddress() {
        return targetAddress;
    }

    public abstract PathAddress convertToTargetAddress(PathAddress address);

    @Override
    public AliasEntry clone() {
        try {
            return (AliasEntry) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

}
