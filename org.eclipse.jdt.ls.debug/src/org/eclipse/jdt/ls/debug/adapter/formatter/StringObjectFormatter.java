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

import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.sun.jdi.StringReference;
import com.sun.jdi.Type;
import com.sun.jdi.Value;

public class StringObjectFormatter extends AbstractFormatter implements IValueFormatter {
	private static final int DEFAULT_MAX_STRING_LENGTH = 100;
	private static final String STRING_OBJECT_TEMPLATE = "\"%s\" (id=%s)";
    private int maxStringLength = DEFAULT_MAX_STRING_LENGTH;

    /**
     * Set the max displayed string length for <code>String</code> objects.
     *
     * @param maxStringLength
     *            the max displayed string length
     */
    public void setMaxStringLength(int maxStringLength) {
        this.maxStringLength = maxStringLength;
    }

    @Override
    public String toString(Object value, Map<String, Object> props) {
        return String.format(STRING_OBJECT_TEMPLATE, 
                StringUtils.abbreviate(((StringReference) value).value(), this.maxStringLength), 
                HexicalNumericFormatter.numbericToString(
                        ((StringReference) value).uniqueID(),
                        HexicalNumericFormatter.containsHexFormat(props)));
    }

    @Override
    public boolean acceptType(Type type, Map<String, Object> props) {
        return type != null && (type.signature().charAt(0) == STRING
                || type.signature().equals(STRING_SIGNATURE));
    }

    @Override
    public Value valueOf(String value, Type type, Map<String, Object> props) {
        if (value == null || NULL_STRING.equals(value)) {
            return null;
        }
        if (value.length() >= 2
                && value.startsWith(QUOTE_STRING)
                && value.endsWith(QUOTE_STRING)) {
            return type.virtualMachine().mirrorOf(StringUtils.substring(value, 1, -1));
        }
        return type.virtualMachine().mirrorOf(value);
    }
}
