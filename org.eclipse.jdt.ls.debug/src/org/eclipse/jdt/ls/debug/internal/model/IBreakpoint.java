/*******************************************************************************
 * Copyright (c) 2000, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Code copied from org.eclipse.debug.core.model.IBreakpoint
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.ls.debug.internal.model;

import org.eclipse.jdt.ls.debug.internal.DebugException;

public interface IBreakpoint {

    /**
     * Returns whether this breakpoint is currently registered with the
     * breakpoint manager.
     *
     * @return whether this breakpoint is currently registered with the
     *         breakpoint manager
     * @exception DebugException
     *                if unable to access the associated attribute on this
     *                breakpoint's underlying marker
     */
    public boolean isRegistered() throws DebugException;

    /**
     * Sets whether this breakpoint is currently registered with the breakpoint
     * manager.
     *
     * @param registered
     *            whether this breakpoint is registered with the breakpoint
     *            manager
     * @exception DebugException
     *                if unable to set the associated attribute on this
     *                breakpoint's underlying marker.
     */
    public void setRegistered(boolean registered) throws DebugException;

}
