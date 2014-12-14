/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

/**
 * Replace any expressions found within the provided text.
 *
 * @author Brian Stansberry (c) 2014 Red Hat Inc.
 */
public interface ExpressionReplacer {

    /**
     * Replace any expressions found within the text provided with the resolve value of the expression.
     *
     * @param text Text within which expressions are to be replaced
     * @return the text with expressions replaced
     */
    String replaceExpressions(final String text);


    class Factory {

        public static final ExpressionReplacer DEFAULT_REPLACER = resolvingReplacer(SystemPropertyResolver.INSTANCE);

        /**
         * Return an {@code ExpressionReplacer} that uses the provided {@code SimpleExpressionResolver} to resolve any
         * expressions found in the text. The returned replacer searches for strings beginning with the string "${"
         * and ending with the char '}', and passes the value within to the given {@code resolver}. The replacer
         * supports arbitrarily nested expressions, finding the inner-most expressions and resolving those before
         * using the resolved values to compose the outer expressions. The replacer also supports recursive resolution,
         * so if a resolved value is itself in the form of an expression, that expression will in turn be resolved.
         *
         * @param resolvers The resolvers used for any expressions being replaced. Cannot be {@code null} or empty
         * @return the replacer. Will not be {@code null}
         */
        public static ExpressionReplacer resolvingReplacer(final SimpleExpressionResolver... resolvers) {
            assert resolvers != null && resolvers.length > 0;
            if (resolvers.length > 1) {
                return new DefaultExpressionReplacer(new CompositeExpressionResolver(resolvers));
            }
            return new DefaultExpressionReplacer(resolvers[0]);
        }

    }
}
