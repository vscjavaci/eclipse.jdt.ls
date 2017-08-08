/*******************************************************************************
* Copyright (c) 2017 Microsoft Corporation and others.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*     Microsoft Corporation - initial API and implementation
*******************************************************************************/

package org.eclipse.jdt.ls.debug.adapter.formatter;

import com.sun.jdi.Type;
import com.sun.jdi.Value;
import com.sun.jdi.VirtualMachine;

import java.util.Map;

public class HexicalNumericFormatter implements IValueFormatter {
    /**
     * Get the string representations for an object.
     *
     * @param value the value
     * @param props extra information for printing
     * @return the string representations.
     */
    public String toString(Object value, Map<String, Object> props) {
        return numbericToString(Long.parseLong(value.toString()), true);
    }

    @Override
    public Value valueOf(String value, Type type, Map<String, Object> props) {
        VirtualMachine vm = type.virtualMachine();
        char signature0 = type.signature().charAt(0);
        if (signature0 == Constants.LONG) {
            return vm.mirrorOf(stringToLong(value));
        } else if (signature0 == Constants.INT) {
            return vm.mirrorOf((int)stringToLong(value));
        } else if (signature0 == Constants.SHORT) {
            return vm.mirrorOf((short)stringToLong(value));
        } else if (signature0 == Constants.BYTE) {
            return vm.mirrorOf((byte)stringToLong(value));
        }
        throw new UnsupportedOperationException(
                String.format("%s is not supported by HexicalNumericFormatter.", type.name()));
    }


    /**
     * The conditional function for this formatter.
     *
     * @param type the JDI type
     * @return whether or not this formatter is expected to work on this value.
     */
    @Override
    public boolean acceptType(Type type, Map<String, Object> props) {
        if (type == null) {
            return false;
        }
        char signature0 = type.signature().charAt(0);
        return (signature0 == Constants.LONG
                || signature0 == Constants.INT
                || signature0 == Constants.SHORT
                || signature0 == Constants.BYTE)
                && containsHexFormat(props);
    }

    static boolean containsHexFormat(Map<String, Object> props) {
        return props.containsKey(Constants.HEX_FORMAT)
                && Boolean.parseBoolean(String.valueOf(props.get(Constants.HEX_FORMAT)));
    }

    static String numbericToString(long longValue, boolean hexFormat) {
        return hexFormat ? Constants.HEX_PREFIX + Long.toHexString(longValue) : Long.toString(longValue);
    }

    static String numbericToString(int intValue, boolean hexFormat) {
        return hexFormat ? Constants.HEX_PREFIX + Integer.toHexString(intValue) : Integer.toString(intValue);
    }

    private static long stringToLong(String longValue) {
        if (longValue.startsWith(Constants.HEX_PREFIX)) {
            return Long.valueOf(longValue.substring(2), 16);
        }
        throw new IllegalArgumentException(
                String.format("%s is not a valid hex number, it should start with %s", longValue, Constants.HEX_PREFIX));
    }
}
