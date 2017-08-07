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

import com.sun.jdi.ArrayReference;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.Type;
import com.sun.jdi.Value;

public class ArrayFormatter extends ObjectFormatter {
    public ArrayFormatter() {

    }

    public ArrayFormatter(ITypeFormatter typeFormater) {
        setTypeFormatter(typeFormater);
    }


    @Override
    public boolean accept(Value value) {
        return value instanceof ArrayReference;
    }


    @Override
    public String valueOf(Value value) {
        Type type = value.type();
        StringBuilder sb = new StringBuilder();
        sb.append(typeFormatter == null ? type.name() :
            typeFormatter.typeToString(type).replaceFirst("\\[", "["
                    + toHexString(arrayLength(value))));
        sb.append(" (id=");
        sb.append(numbericToString(((ObjectReference) value).uniqueID()));
        sb.append(RIGHT_BRACE);
        return sb.toString();
    }

    private static int arrayLength(Value value) {
        return ((ArrayReference) value).length();
    }
}
