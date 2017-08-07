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

public class DefaultTypeFormatter implements ITypeFormatter {
    private boolean showQualified;

    public DefaultTypeFormatter() {
        showQualified = true;
    }

    public DefaultTypeFormatter(boolean showQualified) {
        this.showQualified = showQualified;
    }

    @Override
    public String typeToString(Type type) {
        return trimTypeNameIfNeeded(type == null ? BaseFormatter.NULL : type.name());
    }

    public void setShowQualified(boolean showQualified) {
        this.showQualified = showQualified;
    }

    private String trimTypeNameIfNeeded(String type) {
        if (!showQualified) {
            return type;
        }
        if (type.indexOf('.') >= 0) {
            type = type.substring(type.lastIndexOf('.') + 1);
        }
        return type;
    }

}
