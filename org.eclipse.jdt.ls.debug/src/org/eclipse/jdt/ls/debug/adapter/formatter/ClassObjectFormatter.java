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

import com.sun.jdi.ClassObjectReference;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.Type;
import com.sun.jdi.Value;

public class ClassObjectFormatter extends ObjectFormatter {

    public ClassObjectFormatter() {

    }

    public ClassObjectFormatter(ITypeFormatter typeFormater) {
        setTypeFormatter(typeFormater);
    }

    @Override
    public boolean accept(Value value) {
        return value instanceof ClassObjectReference;
    }

    @Override
    public String valueOf(Value value) {
        Type type = value.type();
        StringBuilder sb = new StringBuilder();
        sb.append(typeFormatter == null ? type.name() : typeFormatter.typeToString(type));
        Type classType = ((ClassObjectReference) value).reflectedType();
        sb.append(LEFT_BRACE);
        sb.append(typeFormatter == null ? classType.name() : typeFormatter.typeToString(classType));
        sb.append(RIGHT_BRACE);
        sb.append(SPACE);

        sb.append(" (id=");
        sb.append(numbericToString(((ObjectReference) value).uniqueID()));
        sb.append(RIGHT_BRACE);
        return sb.toString();
    }
}
