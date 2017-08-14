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

package org.eclipse.jdt.ls.debug.adapter;

import java.util.Objects;

import com.sun.jdi.ObjectReference;
import com.sun.jdi.ThreadReference;

public class ThreadObjectReference {
    public ThreadReference thread;
    public ObjectReference object;

    @Override
    public String toString() {
        return this.object.toString();
    }

    @Override
    public int hashCode() {
        return this.object.hashCode() & this.thread.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ThreadObjectReference)) {
            return false;
        }
        final ThreadObjectReference other = (ThreadObjectReference) o;
        return (Objects.equals(this.thread, other.thread) && Objects.equals(this.object, other.object));
    }
}