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

package org.wildfly.management.api.model.definition;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.jboss.dmr.ModelNode;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2012 Red Hat Inc.
 */
class DefaultAttributeMarshaller extends AttributeMarshaller {

    @Override
    public void marshallAsAttribute(final ItemDefinition attribute, final ModelNode resourceModel, final boolean marshallDefault, final XMLStreamWriter writer) throws XMLStreamException {
        if (isMarshallable(attribute, resourceModel, marshallDefault)) {
            writer.writeAttribute(attribute.getXmlName(), this.asString(resourceModel.get(attribute.getName())));
        }
    }

    @Override
    public void marshallAsElement(final ItemDefinition attribute, final ModelNode resourceModel, final boolean marshallDefault, final XMLStreamWriter writer) throws XMLStreamException {
        if (isMarshallable(attribute, resourceModel, marshallDefault)) {
            writer.writeStartElement(attribute.getXmlName());
            String content = this.asString(resourceModel.get(attribute.getName()));
            if (content.indexOf('\n') > -1) {
                // Multiline content. Use the overloaded variant that staxmapper will format
                writer.writeCharacters(content);
            } else {
                // Staxmapper will just output the chars without adding newlines if this is used
                char[] chars = content.toCharArray();
                writer.writeCharacters(chars, 0, chars.length);
            }
            writer.writeEndElement();
        }
    }

    @Override
    public boolean isMarshallableAsElement() {
        return false;
    }

    protected String asString(ModelNode value) {
        return value.asString();
    }
}
