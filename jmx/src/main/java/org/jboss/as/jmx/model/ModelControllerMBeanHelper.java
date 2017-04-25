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
package org.jboss.as.jmx.model;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ACCESS_MECHANISM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ATTRIBUTES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REPLY_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUEST_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.InvalidAttributeValueException;
import javax.management.JMRuntimeException;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.QueryEval;
import javax.management.QueryExp;
import javax.management.ReflectionException;

import org.jboss.as.controller.ExpressionResolver;
import org.jboss.as.controller.ModelController;
import org.jboss.as.controller.ModelController.OperationTransactionControl;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.registry.ImmutableManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.controller.registry.OperationEntry.Flag;
import org.jboss.as.core.security.AccessMechanism;
import org.jboss.as.jmx.logging.JmxLogger;
import org.jboss.as.jmx.model.ChildAddOperationFinder.ChildAddOperationEntry;
import org.jboss.as.jmx.model.ResourceAccessControlUtil.ResourceAccessControl;
import org.jboss.as.jmx.model.RootResourceIterator.ResourceAction;
import org.jboss.dmr.ModelNode;
import org.wildfly.common.Assert;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ModelControllerMBeanHelper {

    static final String CLASS_NAME = ModelController.class.getName();
    private static final String AUTHORIZED_ERROR = "WFLYCTL0313";

    private final MutabilityChecker mutabilityChecker;
    private final ModelController controller;
    private final ResourceAccessControlUtil accessControlUtil;
    private final PathAddress CORE_SERVICE_PLATFORM_MBEAN = PathAddress.pathAddress(PathElement.pathElement("core-service", "platform-mbean"));

    private final TypeConverters converters;
    private final ConfiguredDomains configuredDomains;
    private final String domain;
    private final ManagementModelIntegration.ManagementModelProvider managementModelProvider;

    ModelControllerMBeanHelper(TypeConverters converters, ConfiguredDomains configuredDomains, String domain,
                               ModelController controller, MutabilityChecker mutabilityChecker,
                               ManagementModelIntegration.ManagementModelProvider managementModelProvider) {
        this.converters = converters;
        this.configuredDomains = configuredDomains;
        this.domain = domain;
        this.controller = controller;
        this.accessControlUtil = new ResourceAccessControlUtil(controller);
        this.mutabilityChecker = mutabilityChecker;
        this.managementModelProvider = managementModelProvider;
    }

    int getMBeanCount() {
        return new RootResourceIterator<Integer>(accessControlUtil, getRootResourceAndRegistration().getResource(), new ResourceAction<Integer>() {
            int count;

            @Override
            public ObjectName onAddress(PathAddress address) {
                return isExcludeAddress(address) ? null : ObjectNameAddressUtil.createObjectName(domain, address);
            }

            public boolean onResource(ObjectName address) {
                count++;
                return true;
            }

            public Integer getResult() {
                return count;
            }
        }).iterate();
    }

    Set<ObjectInstance> queryMBeans(final MBeanServer mbeanServer, final ObjectName name, final QueryExp query) {
        ManagementModelIntegration.ResourceAndRegistration resourceAndReg = getRootResourceAndRegistration();
        Set<ObjectInstance> basic = new RootResourceIterator<Set<ObjectInstance>>(accessControlUtil, resourceAndReg.getResource(),
                new ObjectNameMatchResourceAction<Set<ObjectInstance>>(name, resourceAndReg.getRegistration()) {

            Set<ObjectInstance> set = new HashSet<ObjectInstance>();

            @Override
            public boolean onResource(ObjectName resourceName) {
                if (name == null || name.apply(resourceName)) {
                    set.add(new ObjectInstance(resourceName, CLASS_NAME));
                }
                return true;
            }

            @Override
            public Set<ObjectInstance> getResult() {
                if (set.size() == 1 && set.contains(ModelControllerMBeanHelper.createRootObjectInstance(domain))) {
                    return Collections.emptySet();
                }
                return set;
            }
        }).iterate();

        // Handle any 'query' outside the RootResourceIterator so if the query calls back
        // into us it's not a recursive kind of thing in the ModelController
        Set<ObjectInstance> result;
        if (query == null || basic.isEmpty()) {
            result = basic;
        } else {
            result = new HashSet<>(basic.size());
            for (ObjectInstance oi : basic) {

                MBeanServer oldServer = setQueryExpServer(query, mbeanServer);
                try {
                    if (query.apply(oi.getObjectName())) {
                        result.add(oi);
                    }
                } catch (Exception ignored) {
                    // we just don't add it
                } finally {
                    setQueryExpServer(query, oldServer);
                }
            }
        }
        return result;
    }

    Set<ObjectName> queryNames(MBeanServer mbeanServer, final ObjectName name, final QueryExp query) {
        ManagementModelIntegration.ResourceAndRegistration resourceAndReg = getRootResourceAndRegistration();
        Set<ObjectName> basic = new RootResourceIterator<Set<ObjectName>>(accessControlUtil, resourceAndReg.getResource(),
                new ObjectNameMatchResourceAction<Set<ObjectName>>(name, resourceAndReg.getRegistration()) {

            Set<ObjectName> set = new HashSet<ObjectName>();

            @Override
            public boolean onResource(ObjectName resourceName) {
                if (name == null || name.apply(resourceName)) {
                    set.add(resourceName);
                }
                return true;
            }

            @Override
            public Set<ObjectName> getResult() {
                if (set.size() == 1 && set.contains(ModelControllerMBeanHelper.createRootObjectName(domain))) {
                  return Collections.emptySet();
                }
                return set;
            }
        }).iterate();

        // Handle any 'query' outside the RootResourceIterator so if the query calls back
        // into us it's not a recursive kind of thing in the ModelController
        Set<ObjectName> result;
        if (query == null || basic.isEmpty()) {
            result = basic;
        } else {
            result = new HashSet<>(basic.size());
            for (ObjectName on : basic) {
                MBeanServer oldServer = setQueryExpServer(query, mbeanServer);
                try {
                    if (query.apply(on)) {
                        result.add(on);
                    }
                } catch (Exception ignored) {
                    // we just don't add it
                } finally {
                    setQueryExpServer(query, oldServer);
                }
            }
        }
        return result;
    }

    /**  Set the mbean server on the QueryExp and try and pass back any previously set one */
    private static MBeanServer setQueryExpServer(QueryExp query, MBeanServer toSet) {
        // We assume the QueryExp is a QueryEval subclass or uses the QueryEval thread local
        // mechanism to store any existing MBeanServer. If that's not the case we have no
        // way to access the old mbeanserver to let us restore it
        MBeanServer result = QueryEval.getMBeanServer();
        query.setMBeanServer(toSet);
        return result;
    }


    PathAddress resolvePathAddress(final ObjectName name) {
        return ObjectNameAddressUtil.resolvePathAddress(domain, getRootResourceAndRegistration().getResource(), name);
    }

    PathAddress resolvePathAddress(final ObjectName name, ManagementModelIntegration.ResourceAndRegistration reg) {
        return ObjectNameAddressUtil.resolvePathAddress(domain, reg.getResource(), name);
    }

    /**
     * Convert an ObjectName to a PathAddress.
     *
     * Patterns are supported: there may not be a resource at the returned PathAddress but a resource model <strong>MUST</strong>
     * must be registered.
     */

    PathAddress toPathAddress(final ObjectName name) {
        return ObjectNameAddressUtil.toPathAddress(domain, getRootResourceAndRegistration().getRegistration(), name);
    }

    MBeanInfo getMBeanInfo(final ObjectName name) throws InstanceNotFoundException {
        final ManagementModelIntegration.ResourceAndRegistration reg = getRootResourceAndRegistration();
        final PathAddress address = resolvePathAddress(name, reg);
        if (address == null) {
            throw JmxLogger.ROOT_LOGGER.mbeanNotFound(name);
        }
        final ResourceAccessControl accessControl = accessControlUtil.getResourceAccessWithInstanceNotFoundExceptionIfNotAccessible(name, address, true);
        return MBeanInfoFactory.createMBeanInfo(name, converters, configuredDomains, mutabilityChecker, address, getMBeanRegistration(address, reg));
    }

    Object getAttribute(final ObjectName name, final String attribute)  throws AttributeNotFoundException, InstanceNotFoundException, ReflectionException {
        final ManagementModelIntegration.ResourceAndRegistration reg = getRootResourceAndRegistration();
        final PathAddress address = resolvePathAddress(name, reg);
        if (address == null) {
            throw JmxLogger.ROOT_LOGGER.mbeanNotFound(name);
        }
        final ResourceAccessControl accessControl = accessControlUtil.getResourceAccessWithInstanceNotFoundExceptionIfNotAccessible(name, address, false);
        return getAttribute(reg, address, name, attribute, accessControl);
    }

    AttributeList getAttributes(ObjectName name, String[] attributes) throws InstanceNotFoundException, ReflectionException {
        final ManagementModelIntegration.ResourceAndRegistration reg = getRootResourceAndRegistration();
        final PathAddress address = resolvePathAddress(name, reg);
        if (address == null) {
            throw JmxLogger.ROOT_LOGGER.mbeanNotFound(name);
        }
        final ResourceAccessControl accessControl = accessControlUtil.getResourceAccessWithInstanceNotFoundExceptionIfNotAccessible(name, address, false);
        AttributeList list = new AttributeList();
        for (String attribute : attributes) {
            try {
                list.add(new Attribute(attribute, getAttribute(reg, address, name, attribute, accessControl)));
            } catch (AttributeNotFoundException e) {
                throw new ReflectionException(e);
            }
        }
        return list;
    }

    private Object getAttribute(final ManagementModelIntegration.ResourceAndRegistration reg, final PathAddress address, final ObjectName name, final String attribute, final ResourceAccessControl accessControl)  throws ReflectionException, AttributeNotFoundException, InstanceNotFoundException {
        final ImmutableManagementResourceRegistration registration = getMBeanRegistration(address, reg);
        final DescriptionProvider provider = registration.getModelDescription(PathAddress.EMPTY_ADDRESS);
        if (provider == null) {
            throw JmxLogger.ROOT_LOGGER.descriptionProviderNotFound(address);
        }
        final ModelNode description = provider.getModelDescription(null);
        final String attributeName = findAttributeName(description.get(ATTRIBUTES), attribute);

        if (!accessControl.isReadableAttribute(attributeName)) {
            throw JmxLogger.ROOT_LOGGER.notAuthorizedToReadAttribute(attributeName);
        }


        ModelNode op = new ModelNode();
        op.get(OP).set(READ_ATTRIBUTE_OPERATION);
        op.get(OP_ADDR).set(address.toModelNode());
        op.get(NAME).set(attributeName);
        ModelNode result = execute(op);
        String error = getFailureDescription(result);
        if (error != null) {
            throw new AttributeNotFoundException(error);
        }

        return converters.fromModelNode(description.require(ATTRIBUTES).require(attributeName), result.get(RESULT));
    }


    void setAttribute(ObjectName name, Attribute attribute) throws InstanceNotFoundException, AttributeNotFoundException, InvalidAttributeValueException {
        final ManagementModelIntegration.ResourceAndRegistration reg = getRootResourceAndRegistration();
        final PathAddress address = resolvePathAddress(name, reg);
        if (address == null) {
            throw JmxLogger.ROOT_LOGGER.mbeanNotFound(name);
        }
        final ResourceAccessControl accessControl = accessControlUtil.getResourceAccessWithInstanceNotFoundExceptionIfNotAccessible(name, address, false);
        setAttribute(reg, address, name, attribute, accessControl);

    }

    AttributeList setAttributes(ObjectName name, AttributeList attributes) throws InstanceNotFoundException, ReflectionException {
        final ManagementModelIntegration.ResourceAndRegistration reg = getRootResourceAndRegistration();
        final PathAddress address = resolvePathAddress(name, reg);
        if (address == null) {
            throw JmxLogger.ROOT_LOGGER.mbeanNotFound(name);
        }
        final ResourceAccessControl accessControl = accessControlUtil.getResourceAccessWithInstanceNotFoundExceptionIfNotAccessible(name, address, false);

        for (Attribute attribute : attributes.asList()) {
            try {
                setAttribute(reg, address, name, attribute, accessControl);
            } catch (JMRuntimeException e) {
                //Propagate the JMRuntimeException thrown from authorization
                throw e;
            } catch (Exception e) {
                throw JmxLogger.ROOT_LOGGER.cannotSetAttribute(e, attribute.getName());
            }
        }

        return attributes;
    }

    private void setAttribute(final ManagementModelIntegration.ResourceAndRegistration reg, final PathAddress address, final ObjectName name, final Attribute attribute, ResourceAccessControl accessControl)  throws InvalidAttributeValueException, AttributeNotFoundException, InstanceNotFoundException {
        final ImmutableManagementResourceRegistration registration = getMBeanRegistration(address, reg);
        final DescriptionProvider provider = registration.getModelDescription(PathAddress.EMPTY_ADDRESS);
        if (provider == null) {
            throw JmxLogger.ROOT_LOGGER.descriptionProviderNotFound(address);
        }
        final ModelNode description = provider.getModelDescription(null);
        final String attributeName = findAttributeName(description.get(ATTRIBUTES), attribute.getName());

        if (!mutabilityChecker.mutable(address)) {
            throw JmxLogger.ROOT_LOGGER.attributeNotWritable(attribute);
        }

        if (!accessControl.isWritableAttribute(attributeName)) {
            throw JmxLogger.ROOT_LOGGER.notAuthorizedToWriteAttribute(attributeName);
        }

        ModelNode op = new ModelNode();
        op.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
        op.get(OP_ADDR).set(address.toModelNode());
        op.get(NAME).set(attributeName);
        try {
            op.get(VALUE).set(converters.toModelNode(description.require(ATTRIBUTES).require(attributeName), attribute.getValue()));
        } catch (ClassCastException e) {
            throw JmxLogger.ROOT_LOGGER.invalidAttributeType(e, attribute.getName());
        }
        ModelNode result = execute(op);
        String error = getFailureDescription(result);
        if (error != null) {
            //Since read-resource-description does not know the parameters of the operation, i.e. if a vault expression is used or not,
            //check the error code
            //TODO add a separate authorize step where we check ourselves that the operation will pass authorization?
            if (isVaultExpression(attribute.getValue()) && error.contains(AUTHORIZED_ERROR)) {
                throw JmxLogger.ROOT_LOGGER.notAuthorizedToWriteAttribute(attributeName);
            }
            throw new InvalidAttributeValueException(error);
        }
    }

    ObjectInstance getObjectInstance(ObjectName name) throws InstanceNotFoundException {
        final PathAddress address = resolvePathAddress(name);
        if (address == null) {
            throw JmxLogger.ROOT_LOGGER.mbeanNotFound(name);
        }
        accessControlUtil.getResourceAccessWithInstanceNotFoundExceptionIfNotAccessible(name, address, false);
        return new ObjectInstance(name, CLASS_NAME);
    }

    Object invoke(ObjectName name, String operationName, Object[] params, String[] signature) throws InstanceNotFoundException, MBeanException, ReflectionException {
        Assert.checkNotNullParam("operationName", operationName);
        if (params == null) {
            params = new Object[0];
        }
        if (signature == null) {
            signature = new String[0];
        }
        if (params.length != signature.length) {
            throw JmxLogger.ROOT_LOGGER.differentLengths("params", "signature");
        }

        final ManagementModelIntegration.ResourceAndRegistration reg = getRootResourceAndRegistration();
        final PathAddress address = resolvePathAddress(name, reg);
        if (address == null) {
            throw JmxLogger.ROOT_LOGGER.mbeanNotFound(name);
        }
        final ImmutableManagementResourceRegistration registration = getMBeanRegistration(address, reg);

        String realOperationName = null;
        OperationEntry opEntry = registration.getOperationEntry(PathAddress.EMPTY_ADDRESS, operationName);
        if (opEntry != null) {
            realOperationName = operationName;
        } else {
            String opName = NameConverter.convertFromCamelCase(operationName);
            opEntry = registration.getOperationEntry(PathAddress.EMPTY_ADDRESS, opName);
            if (opEntry != null) {
                realOperationName = opName;
            }
        }

        if (opEntry == null) {
            //Brute force search in case the operation name is not standard format
            Map<String, OperationEntry> ops = registration.getOperationDescriptions(PathAddress.EMPTY_ADDRESS, false);
            for (Map.Entry<String, OperationEntry> entry : ops.entrySet()) {
                if (operationName.equals(NameConverter.convertToCamelCase(entry.getKey()))) {
                    opEntry = entry.getValue();
                    realOperationName = entry.getKey();
                    break;
                }
            }
        }


        if (opEntry == null) {
            ChildAddOperationEntry entry = ChildAddOperationFinder.findAddChildOperation(address, mutabilityChecker, reg.getRegistration().getSubModel(address), operationName);
            if (entry == null) {
                throw JmxLogger.ROOT_LOGGER.noOperationCalled(null, operationName, address);
            }
            PathElement element = entry.getElement();
            if (element.isWildcard()) {
                if (params.length == 0) {
                    throw JmxLogger.ROOT_LOGGER.wildcardNameParameterRequired();
                }
                element = PathElement.pathElement(element.getKey(), (String)params[0]);
                Object[] newParams = new Object[params.length - 1];
                System.arraycopy(params, 1, newParams, 0, newParams.length);
                params = newParams;
            }

            return invoke(entry.getOperationEntry(), ADD, address.append(element), params);
        }
        return invoke(opEntry, realOperationName, address, params);
    }

    private Object invoke(final OperationEntry entry, final String operationName, PathAddress address, Object[] params)  throws InstanceNotFoundException, MBeanException, ReflectionException {
        if (!mutabilityChecker.mutable(address) && !(entry.getFlags().contains(Flag.READ_ONLY) || entry.getFlags().contains(Flag.RUNTIME_ONLY))) {
            throw JmxLogger.ROOT_LOGGER.noOperationCalled(operationName);
        }

        ResourceAccessControl accessControl;
        if (operationName.equals("add")) {
            accessControl = accessControlUtil.getResourceAccess(address, true);
        } else {
            ObjectName objectName = ObjectNameAddressUtil.createObjectName(operationName, address);
            accessControl = accessControlUtil.getResourceAccessWithInstanceNotFoundExceptionIfNotAccessible(
                    objectName, address, true);
        }

        if (!accessControl.isExecutableOperation(operationName)) {
            throw JmxLogger.ROOT_LOGGER.notAuthorizedToExecuteOperation(operationName);
        }

        final ModelNode description = entry.getDescriptionProvider().getModelDescription(null);
        ModelNode op = new ModelNode();
        op.get(OP).set(operationName);
        op.get(OP_ADDR).set(address.toModelNode());
        if (params.length > 0) {
            ModelNode requestProperties = description.require(REQUEST_PROPERTIES);
            Set<String> keys = requestProperties.keys();
            if (keys.size() != params.length) {
                throw JmxLogger.ROOT_LOGGER.differentLengths("params", "description");
            }
            Iterator<String> it = requestProperties.keys().iterator();
            for (int i = 0 ; i < params.length ; i++) {
                String attributeName = it.next();
                ModelNode paramDescription = requestProperties.get(attributeName);
                op.get(attributeName).set(converters.toModelNode(paramDescription, params[i]));
            }
        }

        ModelNode result = execute(op);
        String error = getFailureDescription(result);
        if (error != null) {
            if (error.contains(AUTHORIZED_ERROR)) {
                for (Object param : params) {
                    //Since read-resource-description does not know the parameters of the operation, i.e. if a vault expression is used or not,
                    //check the error code
                    //TODO add a separate authorize step where we check ourselves that the operation will pass authorization?
                    if (isVaultExpression(param)) {
                        throw JmxLogger.ROOT_LOGGER.notAuthorizedToExecuteOperation(operationName);
                    }
                }
            }
            throw new ReflectionException(null, error);
        }

        if (!description.hasDefined(REPLY_PROPERTIES)) {
            return null;
        }
        //TODO we could have more than one reply property
        return converters.fromModelNode(description.get(REPLY_PROPERTIES), result.get(RESULT));
    }

    private ManagementModelIntegration.ResourceAndRegistration getRootResourceAndRegistration() {
        return managementModelProvider.getResourceAndRegistration();
    }

    private ModelNode execute(ModelNode op) {
        op.get(OPERATION_HEADERS, ACCESS_MECHANISM).set(AccessMechanism.JMX.toString());
        return controller.execute(op, null, OperationTransactionControl.COMMIT, null);
    }

    private ImmutableManagementResourceRegistration getMBeanRegistration(PathAddress address, ManagementModelIntegration.ResourceAndRegistration reg) throws InstanceNotFoundException {
        //TODO Populate MBeanInfo
        ImmutableManagementResourceRegistration resourceRegistration = reg.getRegistration().getSubModel(address);
        if (resourceRegistration == null) {
            throw JmxLogger.ROOT_LOGGER.registrationNotFound(address);
        }
        return resourceRegistration;
    }

    private String getFailureDescription(ModelNode result) {
        if (result.hasDefined(FAILURE_DESCRIPTION)) {
            return result.get(FAILURE_DESCRIPTION).toString();
        }
        return null;
    }

    private String findAttributeName(ModelNode attributes, String attributeName) throws AttributeNotFoundException{
        if (attributes.hasDefined(attributeName)) {
            return attributeName;
        }
        for (String key : attributes.keys()) {
            if (NameConverter.convertToCamelCase(key).equals(attributeName)) {
                return key;
            }
        }
        throw JmxLogger.ROOT_LOGGER.attributeNotFound(attributeName);
    }

    private boolean isExcludeAddress(PathAddress pathAddress) {
        return pathAddress.equals(CORE_SERVICE_PLATFORM_MBEAN);
    }

    private boolean isVaultExpression(Object value) {
        if (value != null && value.getClass() == String.class){
            String valueString = (String)value;
            if (ExpressionResolver.EXPRESSION_PATTERN.matcher(valueString).matches()) {
                return TypeConverters.VAULT_PATTERN.matcher(valueString).matches();
            }

        }
        return false;
    }

    public static ObjectName createRootObjectName(String domain) {
        try {
            return ObjectName.getInstance(domain, "management-root", "server");
        } catch (MalformedObjectNameException e) {
            throw new RuntimeException(e);
        }
    }

    private static ObjectInstance createRootObjectInstance(String domain) {
        return new ObjectInstance(createRootObjectName(domain), CLASS_NAME);
    }

    String getDomain() {
        return domain;
    }

    ImmutableManagementResourceRegistration getMBeanRegistration(ObjectName name) throws InstanceNotFoundException {
        final ManagementModelIntegration.ResourceAndRegistration reg = getRootResourceAndRegistration();
        final PathAddress address = resolvePathAddress(name, reg);
        return getMBeanRegistration(address, reg);
    }

    TypeConverters getConverters() {
        return converters;
    }

    private abstract class ObjectNameMatchResourceAction<T> implements ResourceAction<T> {

        private final ObjectName baseName;
        private final Map<String, String> properties;
        private final ObjectName domainOnlyName;
        private final boolean propertyListPattern;
        private final TypeTree typeTree;

        ObjectNameMatchResourceAction(ObjectName baseName, ImmutableManagementResourceRegistration rootMRR) {
            this.baseName = baseName;
            this.properties = baseName == null ? Collections.<String, String>emptyMap() : baseName.getKeyPropertyList();
            try {
                this.domainOnlyName = baseName == null ? null : ObjectName.getInstance(baseName.getDomain() + ":*");
            } catch (MalformedObjectNameException e) {
                throw new IllegalStateException(e);
            }
            this.propertyListPattern = baseName != null && baseName.isPropertyListPattern();
            // A property list pattern means we'd need to visit every resource including runtime-only,
            // which has unknown cost for runtime-only. So if there are properties that can limit what's
            // valid, use those and the MRR tree to first identify valid address patterns so we can
            // limit resource checking based on that
            this.typeTree = this.propertyListPattern && !properties.isEmpty() ? TypeTree.create(rootMRR, properties) : null;
        }

        @Override
        public ObjectName onAddress(PathAddress address) {
            if (isExcludeAddress(address)) {
                return null;
            }

            ObjectName result = null;
            ObjectName toMatch = ObjectNameAddressUtil.createObjectName(domain, address);
            if (baseName == null) {
                result = toMatch;
            } else if (address.size() == 0) {
                // We can't compare the ObjectName properties a la the final 'else' block,
                // because the special management=server property will not match
                // Just confirm correct domain
                if (domainOnlyName.apply(toMatch)) {
                    result = toMatch;
                }
            } else if (!propertyListPattern && address.size() >= properties.size()) {
                // We have same or more elements than our target has properties; let it do the match
                if (baseName.apply(toMatch)) {
                    result = toMatch;
                }
            } else {
                // Address may be a parent of an interesting address, so see if it matches all elements it has
                boolean matches = domainOnlyName.apply(toMatch) && (typeTree == null || typeTree.matches(address));
                if (matches) {
                    for (Map.Entry<String, String> entry : toMatch.getKeyPropertyList().entrySet()) {

                        String propertyValue = properties.get(entry.getKey());
                        if ((propertyValue == null && !propertyListPattern)
                                || (propertyValue != null
                                        && !entry.getValue().equals(propertyValue))
                                        && !baseName.isPropertyValuePattern(entry.getKey())) {
                            matches = false;
                            break;
                        }
                    }
                }
                if (matches) {
                    result = toMatch;
                }
            }
            return result;
        }
    }

    /**
     * Stripped down variety of an MRR tree that only stores path element information. Can prune itself
     * of branches where the keys in the branch addresses don't match all of the keys in a provided set
     * of ObjectName parameter keys.
     *
     * The idea here is if a JMX query uses an ObjectName with a property list pattern and a property value pattern
     * (see the ObjectNameclass javadoc for the meaning of those) then we can only know if a given branch may
     * match the pattern by navigating all the way to the leaves of the branch. Only then do we know whether a
     * particular property's key will match an element in a branch.
     *
     * So, what this does is create a matching MRR tree that only stores path element data, simultaneously
     * identifying all the leaves in the tree. Then we ask all leaves if their path includes
     * all the keys in the ObjectName properties, with leaves that do not pruning themselves from the tree,
     * with parent nodes pruning themselves if they have no children. When this is done, the tree only includes
     * branches that are useful for matching the ObjectName pattern.
     *
     * Once the tree is pruned, then resource PathAddresses can be matched against it with addresses
     * not corresponding to the tree identified as not matching the query. This avoids the need to load
     * resources, the cost of which may be high in the case of runtime-only resources.
     *
     * TODO the MRR.getChildAddresses and mrr.getSubModel calls we use here can be pretty expensive. More
     * efficient *might* be:
     *
     * 1) Add a method to MRR to have it provide all the leaves under its node in the tree.
     * Make that efficient internally (which is the key thing).
     * 2) Call that on the MRR root to get all the leaf MRRs and their addresses.
     * 3) Check those addresses against the ObjectName property keys, discarding mismatches.
     * 4) Build the "type tree" from the remnants. The tree build should be more efficient too since
     * the set of branches should be much smaller.
     */
    private static class TypeTree {

        static TypeTree create(ImmutableManagementResourceRegistration rootMRR, Map<String, String> properties) {
            assert rootMRR != null;
            assert properties != null;
            Set<TypeTree> leaves = new HashSet<>();
            TypeTree result = new TypeTree(rootMRR, leaves);
            result.setParents();
            for (TypeTree leaf : leaves) {
                leaf.prune(properties);
            }
            return result;
        }

        private TypeTree parent;
        private int level;
        private String objectNameType;
        private final PathElement pathElement;
        private final Set<TypeTree> children;
        private int childCount;

        private TypeTree(ImmutableManagementResourceRegistration mrr, Set<TypeTree> leaves) {
            PathAddress pa = mrr.getPathAddress();
            this.pathElement = pa.size() == 0 ? null : pa.getLastElement();
            // TODO if we could just get the leaves from the root MRR letting it calculate those internally,
            // that *could* be more efficient. MRR.getChildAddresses and mrr.getSubModel can be costly
            Set<PathElement> childElements = mrr.getChildAddresses(PathAddress.EMPTY_ADDRESS);
            if (childElements.size() == 0) {
                children = Collections.emptySet();
            } else {
                children = new HashSet<>(childElements.size());
                for (PathElement pe : childElements) {
                    ImmutableManagementResourceRegistration child = mrr.getSubModel(PathAddress.pathAddress(pe));
                    if (child != null && !child.isRemote()) {
                        TypeTree childTree = new TypeTree(child, leaves);
                        children.add(childTree);
                        if (childTree.childCount == 0) {
                            leaves.add(childTree);
                        }
                    }
                }
                childCount = children.size();
            }
        }

        void setParents()  {
            assert pathElement == null;
            setParent(null, -1);
        }

        private void setParent(TypeTree parent, int level) {
            this.parent = parent;
            this.level = level;
            int childLevel = level + 1;
            for (TypeTree child : children) {
                child.setParent(this, childLevel);
            }
        }

        void prune(Map<String, String> properties) {
            assert childCount == 0;
            Set<String> types = new HashSet<>(properties.keySet());
            match(types);
            // Any remaining types means this path is not a valid match
            if (!types.isEmpty() && parent != null) {
                parent.removeChild(this);
            }
        }

        private void match(Set<String> types) {
            if (pathElement != null) {
                if (objectNameType == null) {
                    objectNameType = ObjectNameAddressUtil.escapeKey(pathElement.getKey());
                }

                types.removeIf(objectNameType::equals);
                if (types.size() > 0 && parent != null) {
                    parent.match(types);
                }
            }
        }

        private void removeChild(TypeTree child) {
            if (children.remove(child)) {
                if (children.size() == 0 && parent != null) {
                    parent.removeChild(this);
                } else {
                    childCount--;
                }
            }
        }

        boolean matches(PathAddress address) {
            assert parent == null;
            int addrSize = address.size();
            if (addrSize == 0) {
                return true;
            }
            for (TypeTree child : children) {
                if (child.match(address, addrSize)) {
                    return true;
                }
            }
            return false;
        }

        private boolean match(PathAddress address, int addrSize) {
            assert level >= 0;
            // Skip the string check if there are no children and address is bigger than ours
            if (childCount == 0 && addrSize > level + 1 ) {
                return false;
            }
            PathElement matchElement = address.getElement(level);
            if (!pathElement.getKey().equals(matchElement.getKey()) || (!pathElement.isWildcard() && !pathElement.getValue().equals(matchElement.getValue()))) {
                return false;
            }
            if (addrSize == level + 1) {
                return true;
            }
            for (TypeTree child : children) {
                if (child.match(address, addrSize)) {
                    return true;
                }
            }
            return false;
        }
    }
}
