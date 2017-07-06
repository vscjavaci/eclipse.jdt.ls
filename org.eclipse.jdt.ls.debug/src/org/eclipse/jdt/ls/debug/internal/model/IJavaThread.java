/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Code copied from org.eclipse.jdt.debug.core.IJavaThread
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.ls.debug.internal.model;

import org.eclipse.jdt.ls.debug.internal.DebugException;

/**
 * A thread in a Java virtual machine.
 *
 * @see org.eclipse.debug.core.model.IThread
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 */
public interface IJavaThread extends IThread {

    /**
     * Status code indicating a request failed because a thread was not
     * suspended.
     */
    public static final int ERR_THREAD_NOT_SUSPENDED = 100;

    /**
     * Status code indicating a request to perform a message send failed because
     * a thread was already performing a message send.
     *
     * @see IJavaObject#sendMessage(String, String, IJavaValue[], IJavaThread,
     *      boolean)
     * @see IJavaClassType#sendMessage(String, String, IJavaValue[],
     *      IJavaThread)
     * @see IJavaClassType#newInstance(String, IJavaValue[], IJavaThread)
     */
    public static final int ERR_NESTED_METHOD_INVOCATION = 101;

    /**
     * Status code indicating a request to perform a message send failed because
     * a thread was not suspended by a step or breakpoint event. When a thread
     * is suspended explicitly via the <code>suspend()</code> method, it is not
     * able to perform method invocations (this is a JDI limitation).
     *
     * @see IJavaObject#sendMessage(String, String, IJavaValue[], IJavaThread,
     *      boolean)
     * @see IJavaClassType#sendMessage(String, String, IJavaValue[],
     *      IJavaThread)
     * @see IJavaClassType#newInstance(String, IJavaValue[], IJavaThread)
     */
    public static final int ERR_INCOMPATIBLE_THREAD_STATE = 102;

    /**
     * Returns whether this thread is a system thread.
     *
     * @return whether this thread is a system thread
     * @exception DebugException
     *                if this method fails. Reasons include:
     *                <ul>
     *                <li>Failure communicating with the VM. The
     *                DebugException's status code contains the underlying
     *                exception responsible for the failure.</li>
     *                </ul>
     */
    boolean isSystemThread() throws DebugException;


    /**
     * Returns whether this thread is currently performing an evaluation.
     *
     * @return whether this thread is currently performing an evaluation
     * @since 2.0
     */
    boolean isPerformingEvaluation();

    /**
     * Returns the name of the thread group this thread belongs to, or
     * <code>null</code> if none.
     *
     * @return thread group name, or <code>null</code> if none
     * @exception DebugException
     *                if this method fails. Reasons include:
     *                <ul>
     *                <li>Failure communicating with the VM. The
     *                DebugException's status code contains the underlying
     *                exception responsible for the failure.</li>
     *                </ul>
     */
    String getThreadGroupName() throws DebugException;

    /**
     * Returns a variable with the given name, or <code>null</code> if unable to
     * resolve a variable with the name, or if this thread is not currently
     * suspended.
     * <p>
     * Variable lookup works only when a thread is suspended. Lookup is
     * performed in all stack frames, in a top-down order, returning the first
     * successful match, or <code>null</code> if no match is found.
     * </p>
     *
     * @param variableName
     *            the name of the variable to search for
     * @return a variable, or <code>null</code> if none
     * @exception DebugException
     *                if this method fails. Reasons include:
     *                <ul>
     *                <li>Failure communicating with the VM. The
     *                DebugException's status code contains the underlying
     *                exception responsible for the failure.</li>
     *                </ul>
     */
    IJavaVariable findVariable(String variableName) throws DebugException;

    /**
     * Request to stops this thread with the given exception.<br>
     * The result will be the same as calling
     * java.lang.Thread#stop(java.lang.Throwable).<br>
     * If the thread is suspended when the method is called, the thread must be
     * resumed to complete the action.<br>
     *
     * <em>exception</em> must represent an exception.
     *
     * @param exception
     *            the exception to throw.
     * @exception DebugException
     *                if the request fails
     * @since 3.0
     * @see java.lang.Thread#stop(java.lang.Throwable)
     */
    public void stop(IJavaObject exception) throws DebugException;

    /**
     * Returns the thread group this thread belongs to or <code>null</code> if
     * none.
     *
     * @return thread group or <code>null</code>
     * @throws DebugException
     *             if the thread group cannot be computed
     * @since 3.2
     */
    public IJavaThreadGroup getThreadGroup() throws DebugException;

    /**
     * Returns whether this thread is a daemon thread.
     *
     * @return whether this thread is a daemon thread
     * @throws DebugException
     *             if an exception occurs while determining status
     * @since 3.3
     */
    public boolean isDaemon() throws DebugException;

    /**
     * Returns the number of frames in this thread.
     *
     * @return number of stack frames
     * @throws DebugException
     *             if an exception occurs while retrieving the count
     * @since 3.3
     */
    public int getFrameCount() throws DebugException;

    /**
     * Returns the object reference associated with this thread.
     *
     * @return thread object reference
     * @throws DebugException
     *             if unable to retrieve an object reference
     * @since 3.6
     */
    public IJavaObject getThreadObject() throws DebugException;

}
