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

public interface IFormatter {

    /**
     * Get the string representations for an object.
     *
     * @param value the value
     * @param props extra information for printing
     * @return the string representations.
     */
    String toString(Object value, Map<String, Object>props);
}
