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

import com.sun.jdi.ByteValue;
import com.sun.jdi.DoubleValue;
import com.sun.jdi.FloatValue;
import com.sun.jdi.IntegerValue;
import com.sun.jdi.LongValue;
import com.sun.jdi.ShortValue;
import com.sun.jdi.Value;

public class NumbericFormatter extends BaseFormatter {
    @Override
    public boolean accept(Value value) {
        return value instanceof ByteValue
                || value instanceof ShortValue
                || value instanceof IntegerValue
                || value instanceof LongValue
                || value instanceof FloatValue
                || value instanceof DoubleValue;
    }

    @Override
    public String valueOf(Value value) {
        // float/double should not be formatted with hex format.
        if (value instanceof FloatValue
                || value instanceof DoubleValue) {
            return value.toString();
        }
        return numbericToString(Long.valueOf(value.toString()));
    }
}
