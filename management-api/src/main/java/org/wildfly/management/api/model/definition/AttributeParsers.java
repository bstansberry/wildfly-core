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

import static javax.xml.stream.XMLStreamConstants.END_ELEMENT;
import static org.wildfly.management.api.model.definition.AttributeParsers.ObjectParser.parseEmbeddedElement;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.xml.stream.XMLStreamException;

import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.wildfly.management.api.OperationClientException;

/**
 * @author Tomaz Cerar (c) 2015 Red Hat Inc.
 */
public interface AttributeParsers {

    abstract class MapParser extends AttributeParser {
        private final String wrapperElement;
        private final String elementName;
        private final boolean wrapElement;


        protected MapParser(String wrapperElement, String elementName, boolean wrapElement) {
            this.wrapperElement = wrapperElement;
            this.elementName = elementName == null ? "property" : elementName;
            this.wrapElement = wrapElement;
        }

        public MapParser(String wrapperElement, boolean wrapElement) {
            this(wrapperElement, "property", wrapElement);
        }

        public MapParser(boolean wrapElement) {
            this(null, null, wrapElement);
        }

        public MapParser(String wrapperElement) {
            this(wrapperElement, null, true);
        }

        public MapParser() {
            this(null, "property", true);
        }

        @Override
        public boolean isParseAsElement() {
            return true;
        }

        @Override
        public String getXmlName(ItemDefinition attribute) {
            return wrapElement ? wrapperElement != null ? wrapperElement : attribute.getXmlName() : elementName;
        }

        @Override
        public void parseElement(ItemDefinition attribute, XMLExtendedStreamReader reader, ModelNode operation) throws XMLStreamException {
            String wrapper = wrapperElement == null ? attribute.getName() : wrapperElement;
            assert attribute instanceof MapItemDefinition;
            MapItemDefinition mapAttribute = (MapItemDefinition) attribute;

            operation.get(attribute.getName()).setEmptyObject();//create empty attribute to address WFCORE-1448
            if (wrapElement) {
                if (!reader.getLocalName().equals(wrapper)) {
                    throw ParseUtils.unexpectedElement(reader, Collections.singleton(wrapper));
                } else {
                    // allow empty properties list
                    if (reader.nextTag() == END_ELEMENT) {
                        return;
                    }
                }
            }

            do {
                if (elementName.equals(reader.getLocalName())) {
                    //real parsing happens
                    parseSingleElement(mapAttribute, reader, operation);
                } else {
                    throw ParseUtils.unexpectedElement(reader, Collections.singleton(elementName));
                }

            } while (reader.hasNext() && reader.nextTag() != END_ELEMENT && reader.getLocalName().equals(elementName));

            if (wrapElement) {
                // To exit the do loop either we hit an END_ELEMENT or a START_ELEMENT not for 'elementName'
                // The latter means a bad document
                if (reader.getEventType() != END_ELEMENT) {
                    throw ParseUtils.unexpectedElement(reader, Collections.singleton(elementName));
                }
            }
        }

        public abstract void parseSingleElement(MapItemDefinition attribute, XMLExtendedStreamReader reader, ModelNode operation) throws XMLStreamException;

    }


    class PropertiesParser extends MapParser {

        public PropertiesParser(String wrapperElement, String elementName, boolean wrapElement) {
            super(wrapperElement, elementName, wrapElement);
        }

        public PropertiesParser(String wrapperElement, boolean wrapElement) {
            this(wrapperElement, "property", wrapElement);
        }

        public PropertiesParser(boolean wrapElement) {
            this(null, null, wrapElement);
        }

        public PropertiesParser(String wrapperElement) {
            this(wrapperElement, null, true);
        }

        public PropertiesParser() {
            this(null, "property", true);
        }

        public void parseSingleElement(MapItemDefinition attribute, XMLExtendedStreamReader reader, ModelNode operation) throws XMLStreamException {
            final String[] array = ParseUtils.requireAttributes(reader, "name", "value");

            ModelNode paramVal = ParseUtils.parseAttributeValue(array[1], attribute.isAllowExpression(), attribute.getType());
            try {
                ItemDefinitionValidator.validateItem(attribute.getElementDefinition(), paramVal);
            } catch (OperationClientException e) {
                throw new XMLStreamException(e.getFailureDescription().toString(), reader.getLocation());
            }
            operation.get(attribute.getName()).get(array[0]).set(paramVal);
            ParseUtils.requireNoContent(reader);
        }
    }

    static class ObjectMapParser extends MapParser {
        private final String keyAttributeName; //name of attribute to use for "key"

        public ObjectMapParser(String wrapperElement, String elementName, boolean wrapElement, String keyAttributeName) {
            super(wrapperElement, elementName, wrapElement);
            this.keyAttributeName = keyAttributeName == null ? "key" : keyAttributeName;
        }

        public ObjectMapParser() {
            this(null, "property", true, null);
        }

        public ObjectMapParser(boolean wrapElement) {
            this(null, null, wrapElement, null);
        }

        public ObjectMapParser(String elementName, boolean wrapElement) {
            this(null, elementName, wrapElement, null);
        }

        @Override
        public void parseSingleElement(MapItemDefinition attribute, XMLExtendedStreamReader reader, ModelNode operation) throws XMLStreamException {

            assert attribute instanceof ObjectMapItemDefinition;
            assert attribute.getParser().isParseAsElement();

            ObjectMapItemDefinition map = ((ObjectMapItemDefinition) attribute);
            ObjectTypeItemDefinition objectType = map.getElementDefinition();
            ParseUtils.requireAttributes(reader, keyAttributeName);
            String key = reader.getAttributeValue(null, keyAttributeName);
            ModelNode op = operation.get(attribute.getName(), key);
            parseEmbeddedElement(objectType, reader, op, keyAttributeName);
            ParseUtils.requireNoContent(reader);
        }
    }


    static class ObjectParser extends AttributeParser {
        @Override
        public boolean isParseAsElement() {
            return true;
        }

        @Override
        public void parseElement(ItemDefinition attribute, XMLExtendedStreamReader reader, ModelNode operation) throws XMLStreamException {
            assert attribute instanceof ObjectTypeItemDefinition;

            if (operation.hasDefined(attribute.getName())) {
                throw ParseUtils.unexpectedElement(reader);
            }
            if (attribute.getXmlName().equals(reader.getLocalName())) {
                ObjectTypeItemDefinition objectType = ((ObjectTypeItemDefinition) attribute);
                ModelNode op = operation.get(attribute.getName());
                op.setEmptyObject();
                parseEmbeddedElement(objectType, reader, op);
            } else {
                throw ParseUtils.unexpectedElement(reader, Collections.singleton(attribute.getXmlName()));
            }
            ParseUtils.requireNoContent(reader);
        }

        static void parseEmbeddedElement(ObjectTypeItemDefinition attribute, XMLExtendedStreamReader reader, ModelNode op, String... additionalExpectedAttributes) throws XMLStreamException {
            ItemDefinition[] valueTypes = attribute.getValueTypes();

            Map<String, ItemDefinition> attributes = Arrays.asList(valueTypes).stream()
                    .collect(Collectors.toMap(ItemDefinition::getXmlName, Function.identity()));

            Map<String, ItemDefinition> attributeElements = Arrays.asList(valueTypes).stream()
                                   .filter(attributeDefinition -> attributeDefinition.getParser().isParseAsElement())
                    .collect(Collectors.toMap(a -> a.getParser().getXmlName(a) , Function.identity()));


            for (int i = 0; i < reader.getAttributeCount(); i++) {
                String attributeName = reader.getAttributeLocalName(i);
                String value = reader.getAttributeValue(i);
                if (attributes.containsKey(attributeName)) {
                    ItemDefinition def = attributes.get(attributeName);
                    AttributeParser parser = def.getParser();
                    assert parser != null;
                    parser.parseAndSetParameter(def, value, op, reader);
                } else if (Arrays.binarySearch(additionalExpectedAttributes, attributeName) < 0) {
                    throw ParseUtils.unexpectedAttribute(reader, i, attributes.keySet());
                }
            }
            // Check if there are also element attributes inside a group
            if (!attributeElements.isEmpty()) {
                while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
                    String attrName = reader.getLocalName();
                    if (attributeElements.containsKey(attrName)) {
                        ItemDefinition ad = attributeElements.get(reader.getLocalName());
                        ad.getParser().parseElement(ad, reader, op);
                    } else {
                        throw ParseUtils.unexpectedElement(reader, attributeElements.keySet());
                    }
                }
            }

        }

    }

    class WrappedObjectListParser extends AttributeParser {
        @Override
        public boolean isParseAsElement() {
            return true;
        }


        ObjectTypeItemDefinition getObjectType(ItemDefinition attribute) {
            assert attribute instanceof ObjectListItemDefinition;
            ObjectListItemDefinition list = ((ObjectListItemDefinition) attribute);
            return list.getElementDefinition();
        }

        @Override
        public void parseElement(ItemDefinition attribute, XMLExtendedStreamReader reader, ModelNode operation) throws XMLStreamException {
            assert attribute instanceof ObjectListItemDefinition;

            ObjectListItemDefinition list = ((ObjectListItemDefinition) attribute);
            ObjectTypeItemDefinition objectType = list.getElementDefinition();


            ModelNode listValue = new ModelNode();
            listValue.setEmptyList();
            while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
                if (objectType.getXmlName().equals(reader.getLocalName())) {
                    ModelNode op = listValue.add();
                    parseEmbeddedElement(objectType, reader, op);
                } else {
                    throw ParseUtils.unexpectedElement(reader, Collections.singleton(objectType.getXmlName()));
                }
                if (!reader.isEndElement()) {
                    ParseUtils.requireNoContent(reader);
                }
            }
            operation.get(attribute.getName()).set(listValue);
        }
    }

    class UnWrappedObjectListParser extends WrappedObjectListParser {

        @Override
        public String getXmlName(ItemDefinition attribute) {
            return getObjectType(attribute).getXmlName();
        }

        @Override
        public void parseElement(ItemDefinition attribute, XMLExtendedStreamReader reader, ModelNode operation) throws XMLStreamException {
            ObjectTypeItemDefinition objectType = getObjectType(attribute);

            ModelNode listValue = operation.get(attribute.getName());
            if (!listValue.isDefined()){
                listValue.setEmptyList();
            }
            String xmlName = objectType.getXmlName();
            if (xmlName.equals(reader.getLocalName())) {
                ModelNode op = listValue.add();
                parseEmbeddedElement(objectType, reader, op);
            } else {
                throw ParseUtils.unexpectedElement(reader, Collections.singleton(xmlName));
            }
            if (!reader.isEndElement()) {
                ParseUtils.requireNoContent(reader);
            }
        }
    }


    AttributeParser PROPERTIES_WRAPPED = new PropertiesParser();
    AttributeParser PROPERTIES_UNWRAPPED = new PropertiesParser(false);

    AttributeParser OBJECT_MAP_WRAPPED = new ObjectMapParser();
    AttributeParser OBJECT_MAP_UNWRAPPED = new ObjectMapParser(false);

    AttributeParser WRAPPED_OBJECT_LIST_PARSER = new WrappedObjectListParser();
    AttributeParser UNWRAPPED_OBJECT_LIST_PARSER = new UnWrappedObjectListParser();

    static AttributeParser getObjectMapAttributeParser(String keyElementName) {
        return new ObjectMapParser(null, null, true, keyElementName);
    }

    static AttributeParser getObjectMapAttributeParser(String elementName, String keyElementName, boolean wrapElement) {
        return new ObjectMapParser(null, elementName, wrapElement, keyElementName);
    }

    static AttributeParser getObjectMapAttributeParser(String elementName, boolean wrapElement) {
        return new ObjectMapParser(null, elementName, wrapElement, null);
    }

    static AttributeParser getObjectMapAttributeParser(String wrapperElementName, boolean wrapElement, String elementName, String keyElementName) {
        return new ObjectMapParser(wrapperElementName, elementName, wrapElement, keyElementName);
    }


}
