/*
 * Copyright 2016 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wildfly.management.api.model.validation;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jboss.dmr.ModelNode;
import org.wildfly.management.api.OperationClientException;
import org.wildfly.management.api._private.ControllerLoggerDuplicate;

/**
 *
 * Validates that a String value can resolve to a subnet format based on class SubnetUtils in Apache Commons Net
 *
 * @author wangc based on work of @author rwinston@apache.org
 *
 */
@SuppressWarnings("unused")
public final class SubnetValidator implements ParameterValidator, MinMaxValidator {

    private static final String IP_ADDRESS = "(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})";
    private static final String SLASH_FORMAT = IP_ADDRESS + "/(\\d{1,3})";
    private static final Pattern cidrPattern = Pattern.compile(SLASH_FORMAT);
    private static final int NBITS = 32;

    public static final SubnetValidator INSTANCE = new SubnetValidator();

    @Override
    public void validateParameter(String parameterName, ModelNode value) throws OperationClientException {
        String subnet = value.asString();
        try {
            calculate(subnet);
        } catch (IllegalArgumentException e) {
            throw ControllerLoggerDuplicate.ROOT_LOGGER.invalidSubnetFormat(subnet, parameterName);
        }
    }

    @Override
    public Long getMin() {
        return 1L;
    }

    @Override
    public Long getMax() {
        return MAX_INT;
    }

    /*
     * Initialize the internal fields from the supplied CIDR mask
     */
    private void calculate(String mask) {
        Matcher matcher = cidrPattern.matcher(mask);

        if (matcher.matches()) {

            // IPv4 address parts must be 0-255
            for (int i = 1; i <= 4; ++i) {
                rangeCheck(Integer.parseInt(matcher.group(i)), 255);
            }

            // subnet mask parts must be 0-32
            rangeCheck(Integer.parseInt(matcher.group(5)), NBITS);
        } else {
            throw new IllegalArgumentException("Could not parse [" + mask + "]");
        }
    }

    /*
     * Convenience function to check integer boundaries. Checks if a value x is in the range [begin,end]. Returns x if it is in
     * range, throws an exception otherwise.
     */
    private void rangeCheck(int value, int end) {
        if (value < 0 || value > end) { // (begin,end]
            throw new IllegalArgumentException("Value [" + value + "] not in range [0," + end + "]");
        }
    }
}
