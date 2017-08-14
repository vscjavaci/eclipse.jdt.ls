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

package org.eclipse.jdt.ls.debug.adapter.variables;

import java.util.Objects;

import com.sun.jdi.StackFrame;

public class StackFrameScope {
    public StackFrame stackFrame;
    public String scope;
    
    @Override
    public String toString() {
        return String.format("%s %s", String.valueOf(stackFrame), scope);
    }

    @Override
    public int hashCode() {
        return this.stackFrame.hashCode() & this.scope.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof StackFrameScope)) {
            return false;
        }
        final StackFrameScope other = (StackFrameScope) o;
        return (Objects.equals(this.stackFrame, other.stackFrame) && Objects.equals(this.scope, other.scope));
    }
}
