/*
 * JBoss, Home of Professional Open Source
 * Copyright 2015, Red Hat, Inc., and individual contributors as indicated
 * by the @authors tag.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wildfly.management.api.model.definition;

import java.util.List;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.wildfly.management.api._private.ControllerLoggerDuplicate;

/**
 * @author Tomaz Cerar (c) 2015 Red Hat Inc.
 */
public interface AttributeMarshallers {


    abstract class MapAttributeMarshaller extends AttributeMarshaller {
        final String wrapperElement;
        protected final String elementName;
        final boolean wrapElement;

        MapAttributeMarshaller(String wrapperElement, String elementName, boolean wrapElement) {
            this.wrapperElement = wrapperElement;
            this.elementName = elementName == null ? "property" : elementName;
            this.wrapElement = wrapElement;
        }


        @Override
        public void marshallAsElement(ItemDefinition attribute, ModelNode resourceModel, boolean marshallDefault, XMLStreamWriter writer) throws XMLStreamException {

            resourceModel = resourceModel.get(attribute.getName());
            if (!resourceModel.isDefined()) {
                // nothing to do
                return;
            }

            String wrapper = wrapperElement == null ? attribute.getName() : wrapperElement;
            List<ModelNode> elementList = resourceModel.asList();
            if (elementList.isEmpty()) {
                if (wrapElement) {
                    writer.writeEmptyElement(wrapper);
                } else {
                    // This is a subtle programming error, where the xml schema doesn't
                    // prevent ambiguity between the 'null' and 'empty collection' cases,
                    // or a defined but empty ModelNode isn't valid but there's no
                    // AttributeDefinition validation preventing that being accepted.
                    // TODO We can look into possibly throwing an exception here, but
                    // for now to be conservative and avoid regressions I'm just sticking
                    // with existing behavior and marshalling nothing. I'll log a DEBUG
                    // though in the off chance it's helpful if this happens.
                    ControllerLoggerDuplicate.ROOT_LOGGER.debugf("%s found ambigous empty value for unwrapped property %s",
                            getClass().getSimpleName(), attribute.getName());
                }
                // No elements to marshal, so we're done
                return;
            }

            if (wrapElement) {
                writer.writeStartElement(wrapper);
            }
            for (ModelNode property : elementList) {
                marshallSingleElement(attribute, property, marshallDefault, writer);
            }
            if (wrapElement) {
                writer.writeEndElement();
            }
        }

        @Override
        public void marshallAsAttribute(ItemDefinition attribute, ModelNode resourceModel, boolean marshallDefault, XMLStreamWriter writer) throws XMLStreamException {
            marshallAsElement(attribute, resourceModel, marshallDefault, writer);
        }

        @Override
        public boolean isMarshallableAsElement() {
            return true;
        }

        public abstract void marshallSingleElement(ItemDefinition attribute, ModelNode property, boolean marshallDefault, XMLStreamWriter writer) throws XMLStreamException;

    }


    class PropertiesAttributeMarshaller extends MapAttributeMarshaller {

        PropertiesAttributeMarshaller(String wrapperElement, String elementName, boolean wrapElement) {
            super(wrapperElement, elementName, wrapElement);
        }

        PropertiesAttributeMarshaller(String wrapperElement, boolean wrapElement) {
            this(wrapperElement, null, wrapElement);
        }

        PropertiesAttributeMarshaller() {
            this(null, null, true);
        }

        @Override
        public void marshallSingleElement(ItemDefinition attribute, ModelNode property, boolean marshallDefault, XMLStreamWriter writer) throws XMLStreamException {
            writer.writeEmptyElement(elementName);
            writer.writeAttribute("name", property.asProperty().getName());
            writer.writeAttribute("value", property.asProperty().getValue().asString());
        }
    }


    class ObjectMapAttributeMarshaller extends MapAttributeMarshaller {
        final String keyAttributeName; //name of attribute to use for "key"

        public ObjectMapAttributeMarshaller(String wrapperElement, String elementName, boolean wrapElement, String keyAttributeName) {
            super(wrapperElement, elementName, wrapElement);
            this.keyAttributeName = keyAttributeName == null ? "key" : keyAttributeName;
        }

        public ObjectMapAttributeMarshaller(String wrapperElement, String elementName, boolean wrapElement) {
            this(wrapperElement, elementName, wrapElement, null);
        }

        public ObjectMapAttributeMarshaller(String wrapperElement, boolean wrapElement) {
            this(wrapperElement, null, wrapElement);
        }

        public ObjectMapAttributeMarshaller(String keyAttributeName) {
            this(null, null, true, keyAttributeName);
        }

        public ObjectMapAttributeMarshaller() {
            this(null, null, true);
        }

        @Override
        public void marshallSingleElement(ItemDefinition attribute, ModelNode property, boolean marshallDefault, XMLStreamWriter writer) throws XMLStreamException {
            ObjectMapItemDefinition map = ((ObjectMapItemDefinition) attribute);
            ItemDefinition[] valueTypes = map.getValueType().getValueTypes();
            writer.writeEmptyElement(elementName);
            Property p = property.asProperty();
            writer.writeAttribute(keyAttributeName, p.getName());
            for (ItemDefinition valueType : valueTypes) {
                valueType.getMarshaller().marshall(valueType, p.getValue(), false, writer);
            }
        }
    }

    class SimpleListAttributeMarshaller extends AttributeMarshaller {
        private final boolean wrap;

        SimpleListAttributeMarshaller(boolean wrap) {
            this.wrap = wrap;
        }

        @Override
        public void marshallAsElement(ItemDefinition attribute, ModelNode resourceModel, boolean marshallDefault, XMLStreamWriter writer) throws XMLStreamException {
            assert attribute instanceof SimpleListItemDefinition;
            SimpleListItemDefinition attr = (SimpleListItemDefinition) attribute;
            if (resourceModel.hasDefined(attribute.getName())) {
                if (wrap) {
                    writer.writeStartElement(attribute.getXmlName());
                }
                for (ModelNode handler : resourceModel.get(attribute.getName()).asList()) {
                    attr.getValueType().getMarshaller().marshallAsElement(attribute, handler, true, writer);
                }
                if (wrap) {
                    writer.writeEndElement();
                }
            }
        }
    }

    static AttributeMarshaller getObjectMapAttributeMarshaller(String keyElementName) {
        return new ObjectMapAttributeMarshaller(null, null, true, keyElementName);
    }

    static AttributeMarshaller getObjectMapAttributeMarshaller(String elementName, String keyElementName, boolean wrapElement) {
        return new ObjectMapAttributeMarshaller(null, elementName, wrapElement, keyElementName);
    }

    static AttributeMarshaller getObjectMapAttributeMarshaller(String elementName, boolean wrapElement) {
        return new ObjectMapAttributeMarshaller(null, elementName, wrapElement, null);
    }

    static AttributeMarshaller getObjectMapAttributeMarshaller(String wrapperElementName, boolean wrapElement, String elementName, String keyElementName) {
        return new ObjectMapAttributeMarshaller(wrapperElementName, elementName, wrapElement, keyElementName);
    }

    static AttributeMarshaller getSimpleListMarshaller(boolean wrapper) {
        return new SimpleListAttributeMarshaller(wrapper);
    }
}
