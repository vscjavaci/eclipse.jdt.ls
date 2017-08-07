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

import com.sun.jdi.ObjectReference;
import com.sun.jdi.StringReference;
import com.sun.jdi.Value;

import org.apache.commons.lang3.StringUtils;


public class StringObjectFormatter extends BaseFormatter {

    private static final int DEFAULT_MAX_STRING_LENGTH = 100;
    private int maxStringLength = DEFAULT_MAX_STRING_LENGTH;

    /**
     * Set the max displayed string length for <code>String</code> objects.
     *
     * @param maxStringLength the max displayed string length
     */
    public void setMaxStringLength(int maxStringLength) {
        this.maxStringLength = maxStringLength;
    }

    @Override
    public boolean accept(Value value) {
        return value instanceof StringReference;
    }

    @Override
    public String valueOf(Value value) {
        StringBuilder sb = new StringBuilder();
        sb.append(QUOTE);
        sb.append(StringUtils.abbreviate(((StringReference) value).value(),
                this.maxStringLength));
        sb.append(QUOTE);
        sb.append(" (id=");
        sb.append(numbericToString(((ObjectReference) value).uniqueID()));
        sb.append(RIGHT_BRACE);
        return sb.toString();
    }
}
