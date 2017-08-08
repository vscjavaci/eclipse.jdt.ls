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

import java.util.Map;

public class NullObjectFormatter implements IValueFormatter {

    @Override
    public String toString(Object value, Map<String, Object> props) {
        return Constants.NULL_STRING;
    }

    @Override
    public boolean acceptType(Type type, Map<String, Object> props) {
        return type == null;
    }

    @Override
    public Value valueOf(String value, Type type, Map<String, Object> props) {
        if (value == null || Constants.NULL_STRING.equals(value)) {
            return null;
        }
        throw new UnsupportedOperationException("set value is not supported yet for type " + type.name());
    }

}
