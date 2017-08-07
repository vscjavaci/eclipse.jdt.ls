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

public abstract class BaseFormatter implements IValueFormatter {
    public static final String NULL = "null";
    protected static final String SQUARE_BRACKET = "[]";
    protected static final char LEFT_SQUARE_BRACKET = '[';
    protected static final char RIGHT_SQUARE_BRACKET = ']';
    protected static final char LEFT_BRACE = '(';
    protected static final char RIGHT_BRACE = ')';
    protected static final char QUOTE = '\"';
    protected static final char SPACE = ' ';
    protected boolean hexFormat = true;

    public void setHexFormat(boolean hexFormat) {
        this.hexFormat = hexFormat;
    }

    @Override
    public Value valueFrom(String value, Type type) {
        throw new UnsupportedOperationException("set value is not supported yet.");
    }


    protected String numbericToString(long longValue) {
        return hexFormat ? "0x" + Long.toHexString(longValue) : Long.toString(longValue);
    }

    protected String toHexString(int longValue) {
        return hexFormat ? "0x" + Integer.toHexString(longValue) : Integer.toString(longValue);
    }
}
