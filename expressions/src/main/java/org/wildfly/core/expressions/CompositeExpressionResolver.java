/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.wildfly.core.expressions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

/**
 * Resolves properties using a series of other {@code SimpleExpressionResolver}s.
 * Any default resolution provided by the other resolvers
 * will not be returned unless no other resolver provides a non-default resolution.
 *
 * @author John Bailey
 * @author Brian Stansberry (c) 2014 Red Hat Inc.
 */
class CompositeExpressionResolver implements SimpleExpressionResolver {
    private final Collection<SimpleExpressionResolver> expressionResolvers;
    final boolean hasJBossASExpressionSupport;

    CompositeExpressionResolver(SimpleExpressionResolver... resolvers) {
        this.expressionResolvers = new ArrayList<SimpleExpressionResolver>(resolvers.length);
        boolean jbossSupport = false;
        for (SimpleExpressionResolver ser : resolvers) {
            expressionResolvers.add(ser);
            if (!jbossSupport) {
                if (ser instanceof JBossASSimpleExpressionResolver) {
                    jbossSupport = true;
                } else if (ser instanceof CompositeExpressionResolver
                        && ((CompositeExpressionResolver) ser).hasJBossASExpressionSupport) {
                    jbossSupport = true;
                }
            }
        }
        this.hasJBossASExpressionSupport = jbossSupport;
    }

    @Override
    public ResolutionResult resolveExpressionContent(String expressionContent) {

        ResolutionResult value = null;
        ResolutionResult defaultValue = null;
        if (expressionResolvers != null) {
            for (Iterator<SimpleExpressionResolver> iter = expressionResolvers.iterator(); iter.hasNext() && value == null; ) {
                SimpleExpressionResolver.ResolutionResult rr = iter.next().resolveExpressionContent(expressionContent);
                if (rr != null) {
                    if (!rr.isDefault()) {
                        value = rr;
                    } else if (defaultValue == null) {
                        defaultValue = rr;
                    }
                }
            }
        }
        return value == null ? defaultValue : value;
    }
}
