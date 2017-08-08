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

public class NonNumericPrimitiveFormatter implements IValueFormatter {

    @Override
    public String toString(Object value, Map<String, Object> props) {
        return value == null ? Constants.NULL_STRING : value.toString();
    }

    @Override
    public boolean acceptType(Type type, Map<String, Object> props) {
        if (type == null) {
            return false;
        }
        char signature0 = type.signature().charAt(0);
        return signature0 == Constants.BOOLEAN || signature0 == Constants.DOUBLE
                || signature0 == Constants.FLOAT
                || signature0 == Constants.CHAR;
    }

    @Override
    public Value valueOf(String value, Type type, Map<String, Object> props) {
        VirtualMachine vm = type.virtualMachine();
        char signature0 = type.signature().charAt(0);
        if (signature0 == Constants.FLOAT) {
            return vm.mirrorOf(Float.parseFloat(value));
        } else if (signature0 == Constants.DOUBLE) {
            return vm.mirrorOf(Double.parseDouble(value));
        } else if (signature0 == Constants.CHAR) {
            return vm.mirrorOf((char)value.charAt(0));
        }  else if (signature0 == Constants.BOOLEAN) {
            return vm.mirrorOf(Boolean.parseBoolean(value));
        }
        throw new UnsupportedOperationException(
                String.format("%s is not supported by NonNumericPrimitiveFormatter.", type.name()));
    }
}
