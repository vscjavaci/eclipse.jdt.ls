/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Code copied from org.eclipse.jdt.debug.core.IJavaStackFrame
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.ls.debug.internal.model;

import java.util.List;

import org.eclipse.jdt.ls.debug.internal.DebugException;

/**
 * A stack frame in a thread on a Java virtual machine.
 *
 * @see org.eclipse.debug.core.model.IStackFrame
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 */
public interface IJavaStackFrame extends IStackFrame, IJavaModifiers {

    /**
     * Status code indicating a stack frame is invalid. A stack frame becomes
     * invalid when the thread containing the stack frame resumes. A stack frame
     * may or may not be valid if the thread subsequently suspends, depending on
     * the location where the thread suspends.
     *
     * @since 3.1
     */
    public static final int ERR_INVALID_STACK_FRAME = 130;

    /**
     * Returns whether the method associated with this stack frame is a
     * constructor.
     *
     * @return whether this stack frame is associated with a constructor
     * @exception DebugException
     *                if this method fails. Reasons include:
     *                <ul>
     *                <li>Failure communicating with the VM. The
     *                DebugException's status code contains the underlying
     *                exception responsible for the failure.</li>
     *                <li>This stack frame is no longer valid. That is, the
     *                thread containing this stack frame has since been resumed.
     *                </li>
     *                </ul>
     */
    public boolean isConstructor() throws DebugException;

    /**
     * Returns whether the method associated with this stack frame has been
     * declared as native.
     *
     * @return whether this stack frame has been declared as native
     * @exception DebugException
     *                if this method fails. Reasons include:
     *                <ul>
     *                <li>Failure communicating with the VM. The
     *                DebugException's status code contains the underlying
     *                exception responsible for the failure.</li>
     *                <li>This stack frame is no longer valid. That is, the
     *                thread containing this stack frame has since been resumed.
     *                </li>
     *                </ul>
     */
    public boolean isNative() throws DebugException;

    /**
     * Returns whether the method associated with this stack frame is a static
     * initializer.
     *
     * @return whether this stack frame is a static initializer
     * @exception DebugException
     *                if this method fails. Reasons include:
     *                <ul>
     *                <li>Failure communicating with the VM. The
     *                DebugException's status code contains the underlying
     *                exception responsible for the failure.</li>
     *                <li>This stack frame is no longer valid. That is, the
     *                thread containing this stack frame has since been resumed.
     *                </li>
     *                </ul>
     */
    public boolean isStaticInitializer() throws DebugException;

    /**
     * Returns the fully qualified name of the type that declares the method
     * associated with this stack frame.
     *
     * @return declaring type name
     * @exception DebugException
     *                if this method fails. Reasons include:
     *                <ul>
     *                <li>Failure communicating with the VM. The
     *                DebugException's status code contains the underlying
     *                exception responsible for the failure.</li>
     *                <li>This stack frame is no longer valid. That is, the
     *                thread containing this stack frame has since been resumed.
     *                </li>
     *                </ul>
     */
    public String getDeclaringTypeName() throws DebugException;

    /**
     * Returns the fully qualified name of the type that is the receiving object
     * associated with this stack frame
     *
     * @return receiving type name
     * @exception DebugException
     *                if this method fails. Reasons include:
     *                <ul>
     *                <li>Failure communicating with the VM. The
     *                DebugException's status code contains the underlying
     *                exception responsible for the failure.</li>
     *                <li>This stack frame is no longer valid. That is, the
     *                thread containing this stack frame has since been resumed.
     *                </li>
     *                </ul>
     */
    public String getReceivingTypeName() throws DebugException;

    /**
     * Returns the JNI signature for the method this stack frame is associated
     * with.
     *
     * @return signature
     * @exception DebugException
     *                if this method fails. Reasons include:
     *                <ul>
     *                <li>Failure communicating with the VM. The
     *                DebugException's status code contains the underlying
     *                exception responsible for the failure.</li>
     *                <li>This stack frame is no longer valid. That is, the
     *                thread containing this stack frame has since been resumed.
     *                </li>
     *                </ul>
     */
    public String getSignature() throws DebugException;

    /**
     * Returns a list of fully qualified type names of the arguments for the
     * method associated with this stack frame.
     *
     * @return argument type names, or an empty list if this method has no
     *         arguments
     * @exception DebugException
     *                if this method fails. Reasons include:
     *                <ul>
     *                <li>Failure communicating with the VM. The
     *                DebugException's status code contains the underlying
     *                exception responsible for the failure.</li>
     *                <li>This stack frame is no longer valid. That is, the
     *                thread containing this stack frame has since been resumed.
     *                </li>
     *                </ul>
     */
    public List<String> getArgumentTypeNames() throws DebugException;

    /**
     * Returns the name of the method associated with this stack frame
     *
     * @return method name
     * @exception DebugException
     *                if this method fails. Reasons include:
     *                <ul>
     *                <li>Failure communicating with the VM. The
     *                DebugException's status code contains the underlying
     *                exception responsible for the failure.</li>
     *                <li>This stack frame is no longer valid. That is, the
     *                thread containing this stack frame has since been resumed.
     *                </li>
     *                </ul>
     */
    public String getMethodName() throws DebugException;

    /**
     * Returns the local, static, or "this" variable with the given name, or
     * <code>null</code> if unable to resolve a variable with the name.
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
     *                <li>This stack frame is no longer valid. That is, the
     *                thread containing this stack frame has since been resumed.
     *                </li>
     *                </ul>
     */
    public IJavaVariable findVariable(String variableName) throws DebugException;

    /**
     * Returns the line number of the instruction pointer in this stack frame
     * that corresponds to the line in the associated source element in the
     * specified stratum, or <code>-1</code> if line number information is
     * unavailable.
     *
     * @param stratum
     *            the stratum to use.
     * @return line number of instruction pointer in this stack frame, or
     *         <code>-1</code> if line number information is unavailable
     * @exception DebugException
     *                if this method fails. Reasons include:
     *                <ul>
     *                <li>Failure communicating with the debug target. The
     *                DebugException's status code contains the underlying
     *                exception responsible for the failure.</li>
     *                </ul>
     *
     * @since 3.0
     */
    public int getLineNumber(String stratum) throws DebugException;

    /**
     * Returns the source name debug attribute associated with the declaring
     * type of this stack frame, or <code>null</code> if the source name debug
     * attribute not present.
     *
     * @return source name debug attribute, or <code>null</code>
     * @exception DebugException
     *                if this method fails. Reasons include:
     *                <ul>
     *                <li>Failure communicating with the VM. The
     *                DebugException's status code contains the underlying
     *                exception responsible for the failure.</li>
     *                <li>This stack frame is no longer valid. That is, the
     *                thread containing this stack frame has since been resumed.
     *                </li>
     *                </ul>
     */
    public String getSourceName() throws DebugException;

    /**
     * Returns the source name debug attribute associated with the declaring
     * type of this stack frame in the specified stratum, or <code>null</code>
     * if the source name debug attribute not present.
     *
     * @param stratum
     *            the stratum to use.
     * @return source name debug attribute, or <code>null</code>
     * @exception DebugException
     *                if this method fails. Reasons include:
     *                <ul>
     *                <li>Failure communicating with the VM. The
     *                DebugException's status code contains the underlying
     *                exception responsible for the failure.</li>
     *                <li>This stack frame is no longer valid. That is, the
     *                thread containing this stack frame has since been resumed.
     *                </li>
     *                </ul>
     *
     * @since 3.0
     */
    public String getSourceName(String stratum) throws DebugException;

    /**
     * Returns the source path debug attribute associated with this stack frame
     * in the specified stratum, or <code>null</code> if the source path is not
     * known.
     *
     * @param stratum
     *            the stratum to use.
     * @return source path debug attribute, or <code>null</code>
     * @exception DebugException
     *                if this method fails. Reasons include:
     *                <ul>
     *                <li>Failure communicating with the VM. The
     *                DebugException's status code contains the underlying
     *                exception responsible for the failure.</li>
     *                <li>This stack frame is no longer valid. That is, the
     *                thread containing this stack frame has since been resumed.
     *                </li>
     *                </ul>
     * @since 3.0
     */
    public String getSourcePath(String stratum) throws DebugException;

    /**
     * Returns the source path debug attribute associated with this stack frame,
     * or <code>null</code> if the source path is not known.
     *
     * @return source path debug attribute, or <code>null</code>
     * @exception DebugException
     *                if this method fails. Reasons include:
     *                <ul>
     *                <li>Failure communicating with the VM. The
     *                DebugException's status code contains the underlying
     *                exception responsible for the failure.</li>
     *                <li>This stack frame is no longer valid. That is, the
     *                thread containing this stack frame has since been resumed.
     *                </li>
     *                </ul>
     * @since 3.0
     */
    public String getSourcePath() throws DebugException;

    /**
     * Returns a collection of local variables that are visible at the current
     * point of execution in this stack frame. The list includes arguments.
     *
     * @return collection of locals and arguments
     * @exception DebugException
     *                if this method fails. Reasons include:
     *                <ul>
     *                <li>Failure communicating with the VM. The
     *                DebugException's status code contains the underlying
     *                exception responsible for the failure.</li>
     *                <li>This stack frame is no longer valid. That is, the
     *                thread containing this stack frame has since been resumed.
     *                </li>
     *                </ul>
     * @since 2.0
     */
    public IJavaVariable[] getLocalVariables() throws DebugException;

    /**
     * Returns a reference to the receiver of the method associated with this
     * stack frame, or <code>null</code> if this stack frame represents a static
     * method.
     *
     * @return 'this' object, or <code>null</code>
     * @exception DebugException
     *                if this method fails. Reasons include:
     *                <ul>
     *                <li>Failure communicating with the VM. The
     *                DebugException's status code contains the underlying
     *                exception responsible for the failure.</li>
     *                <li>This stack frame is no longer valid. That is, the
     *                thread containing this stack frame has since been resumed.
     *                </li>
     *                </ul>
     */
    public IJavaObject getThis() throws DebugException;

    /**
     * Returns the type in which this stack frame's method is declared.
     *
     * @return the type in which this stack frame's method is declared
     * @exception DebugException
     *                if this method fails. Reasons include:
     *                <ul>
     *                <li>Failure communicating with the VM. The
     *                DebugException's status code contains the underlying
     *                exception responsible for the failure.</li>
     *                <li>This stack frame is no longer valid. That is, the
     *                thread containing this stack frame has since been resumed.
     *                </li>
     *                </ul>
     * @since 3.1
     */
    public IJavaReferenceType getReferenceType() throws DebugException;

}
