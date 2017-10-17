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

package org.jboss.as.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jboss.dmr.Property;
import org.wildfly.common.Assert;
import org.wildfly.management.api.AddressElement;

/**
 * An element of a path specification for matching operations with addresses.
 * @author Brian Stansberry
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public class PathElement {

    public static final String WILDCARD_VALUE = "*";
    private static final AddressElement[] EMPTY = new AddressElement[0];

    private final AddressElement wrapped;

    /**
     * Construct a new instance with a wildcard value.
     * @param key the path key to match
     * @return the new path element
     */
    public static PathElement pathElement(final String key) {
        return new PathElement(key);
    }

    /**
     * Construct a new instance.
     * @param key the path key to match
     * @param value the path value or wildcard to match
     * @return the new path element
     */
    public static PathElement pathElement(final String key, final String value) {
        return new PathElement(key, value);
    }

    /**
     * Construct a new instance based on a {@link AddressElement}.
     *
     * @param basis the basis. Cannot be {@code null}
     * @return the path element
     */
    public static PathElement pathElement(AddressElement basis) {
        return new PathElement(basis);
    }

    static List<AddressElement> getUnwrappedList(List<PathElement> wrapped) {
        if (wrapped == null || wrapped.size() == 0) {
            return Collections.emptyList();
        }
        List<AddressElement> result = new ArrayList<>(wrapped.size());
        for (PathElement pe : wrapped) {
            result.add(pe.wrapped);
        }
        return result;
    }

    static AddressElement[] getUnwrappedElements(PathElement... wrapped) {
        if (wrapped == null || wrapped.length == 0) {
            return EMPTY;
        }
        AddressElement[] result = new AddressElement[wrapped.length];
        for (int i = 0; i < wrapped.length; i++ ) {
            result[i] = wrapped[i].wrapped;
        }
        return result;
    }

    /**
     * Construct a new instance with a wildcard value.
     * @param key the path key to match
     */
    private PathElement(final String key) {
        this(key, WILDCARD_VALUE);
    }

    /**
     * Construct a new instance.
     * @param key the path key to match
     * @param value the path value or wildcard to match
     */
    private PathElement(final String key, final String value) {
        this.wrapped = AddressElement.pathElement(key, value);
    }

    PathElement(final AddressElement wrapped) {
        Assert.assertNotNull(wrapped);
        this.wrapped = wrapped;
    }

    /**
     * Get the path key.
     * @return the path key
     */
    public String getKey() {
        return wrapped.getKey();
    }

    /**
     * Get the path value.
     * @return the path value
     */
    public String getValue() {
        return wrapped.getValue();
    }

    /**
     * Determine whether the given property matches this element.
     * A property matches this element when property name and this key are equal,
     * values are equal or this element value is a wildcard.
     * @param property the property to check
     * @return {@code true} if the property matches
     */
    public boolean matches(Property property) {
        return wrapped.matches(property);
    }

    /**
     * Determine whether the given element matches this element.
     * An element matches this element when keys are equal, values are equal
     * or this element value is a wildcard.
     * @param pe the element to check
     * @return {@code true} if the element matches
     */
    public boolean matches(PathElement pe) {
        return wrapped.matches(pe.wrapped);
    }

    /**
     * Determine whether the value is the wildcard value.
     * @return {@code true} if the value is the wildcard value
     */
    public boolean isWildcard() {
        return wrapped.isWildcard();
    }

    public boolean isMultiTarget() {
        return wrapped.isMultiTarget();
    }

    public String[] getSegments() {
        return wrapped.getSegments();
    }

    public String[] getKeyValuePair(){
        return wrapped.getKeyValuePair();
    }

    /**
     * Provides a {@link AddressElement} representation of this element.
     * @return the address element representation. Will not be {@code null}
     */
    public final AddressElement asAddressElement() {
        return wrapped;
    }

    @Override
    public int hashCode() {
        return wrapped.hashCode();
    }

    /**
     * Determine whether this object is equal to another.
     * @param other the other object
     * @return {@code true} if they are equal, {@code false} otherwise
     */
    public boolean equals(Object other) {
        return other instanceof PathElement && equals((PathElement) other);
    }

    /**
     * Determine whether this object is equal to another.
     * @param other the other object
     * @return {@code true} if they are equal, {@code false} otherwise
     */
    public boolean equals(PathElement other) {
        return this == other || other != null && other.wrapped.equals(wrapped) ;
    }

    @Override
    public String toString() {
        return wrapped.toString();
    }

}
