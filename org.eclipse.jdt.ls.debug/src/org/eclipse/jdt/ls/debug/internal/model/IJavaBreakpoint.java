/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Code copied from org.eclipse.jdt.debug.core.IJavaBreakpoint
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.ls.debug.internal.model;

import org.eclipse.jdt.ls.debug.internal.CoreException;

/**
 * A breakpoint specific to the Java debug model. A Java breakpoint supports:
 * <ul>
 * <li>a hit count</li>
 * <li>a suspend policy that determines if the entire VM or a single thread is
 * suspended when hit</li>
 * <li>a thread filter to restrict a breakpoint to a specific thread within a VM
 * </li>
 * <li>an installed property that indicates a breakpoint was successfully
 * installed in a VM</li>
 * </ul>
 *
 * @since 2.0
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 */
public interface IJavaBreakpoint extends IBreakpoint {

    /**
     * Suspend policy constant indicating a breakpoint will suspend the target
     * VM when hit.
     */
    public static final int SUSPEND_VM = 1;

    /**
     * Default suspend policy constant indicating a breakpoint will suspend only
     * the thread in which it occurred.
     */
    public static final int SUSPEND_THREAD = 2;

    /**
     * Returns the fully qualified name of the type this breakpoint is located
     * in, or <code>null</code> if this breakpoint is not located in a specific
     * type - for example, a pattern breakpoint.
     *
     * @return the fully qualified name of the type this breakpoint is located
     *         in, or <code>null</code>
     * @exception CoreException
     *                if unable to access the property from this breakpoint's
     *                underlying marker
     */
    public String getTypeName() throws CoreException;

    /**
     * Returns this breakpoint's hit count or, -1 if this breakpoint does not
     * have a hit count.
     *
     * @return this breakpoint's hit count, or -1
     * @exception CoreException
     *                if unable to access the property from this breakpoint's
     *                underlying marker
     */
    public int getHitCount() throws CoreException;

    /**
     * Sets the hit count attribute of this breakpoint. If this breakpoint is
     * currently disabled and the hit count is set greater than -1, this
     * breakpoint is automatically enabled.
     *
     * @param count
     *            the new hit count
     * @exception CoreException
     *                if unable to set the property on this breakpoint's
     *                underlying marker
     */
    public void setHitCount(int count) throws CoreException;

    /**
     * Sets whether all threads in the target VM will be suspended when this
     * breakpoint is hit. When <code>SUSPEND_VM</code> the target VM is
     * suspended, and when <code>SUSPEND_THREAD</code> only the thread in which
     * this breakpoint occurred is suspended.
     *
     * @param suspendPolicy
     *            one of <code>SUSPEND_VM</code> or <code>SUSPEND_THREAD</code>
     * @exception CoreException
     *                if unable to set the property on this breakpoint's
     *                underlying marker
     */
    public void setSuspendPolicy(int suspendPolicy) throws CoreException;

    /**
     * Returns the suspend policy used by this breakpoint, one of
     * <code>SUSPEND_VM</code> or <code>SUSPEND_THREAD</code>.
     *
     * @return one of <code>SUSPEND_VM</code> or <code>SUSPEND_THREAD</code>
     * @exception CoreException
     *                if unable to access the property from this breakpoint's
     *                underlying marker
     */
    public int getSuspendPolicy() throws CoreException;

}
