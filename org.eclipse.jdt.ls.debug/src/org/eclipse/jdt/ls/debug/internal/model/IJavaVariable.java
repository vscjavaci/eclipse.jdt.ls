/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Code copied from org.eclipse.jdt.debug.core.IJavaVariable
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.ls.debug.internal.model;

import org.eclipse.jdt.ls.debug.internal.DebugException;

/**
 * A local variable, field slot, or receiver (this) in a Java virtual machine.
 *
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 */
public interface IJavaVariable extends IVariable, IJavaModifiers {

    /**
     * Returns the JNI-style signature for the declared type of this variable,
     * or <code>null</code> if the type associated with the signature is not yet
     * loaded in the target VM.
     *
     * @return signature, or <code>null</code> if not accessible
     * @exception DebugException
     *                if this method fails. Reasons include:
     *                <ul>
     *                <li>Failure communicating with the VM. The
     *                DebugException's status code contains the underlying
     *                exception responsible for the failure.</li>
     *                <li>The type associated with the signature is not yet
     *                loaded</li>
     *                </ul>
     */
    public String getSignature() throws DebugException;

    /**
     * Returns the generic signature as defined in the JVM specification for the
     * declared type of this variable, or <code>null</code> if the type
     * associated with the signature is not yet loaded in the target VM. Returns
     * the same value as #getSignature() if the declared type of this variable
     * is not a generic type.
     *
     * @return generic signature, or <code>null</code> if not accessible
     * @exception DebugException
     *                if this method fails. Reasons include:
     *                <ul>
     *                <li>Failure communicating with the VM. The
     *                DebugException's status code contains the underlying
     *                exception responsible for the failure.</li>
     *                <li>The type associated with the signature is not yet
     *                loaded</li>
     *                </ul>
     * @since 3.1
     */
    public String getGenericSignature() throws DebugException;

    /**
     * Returns the declared type of this variable.
     *
     * @return the declared type of this variable
     * @exception DebugException
     *                if this method fails. Reasons include:
     *                <ul>
     *                <li>Failure communicating with the VM. The
     *                DebugException's status code contains the underlying
     *                exception responsible for the failure.</li>
     *                <li>The type associated with the signature is not yet
     *                loaded</li>
     *                </ul>
     * @since 2.0
     */
    public IJavaType getJavaType() throws DebugException;

    /**
     * Returns whether this variable is local.
     *
     * @return whether this variable is a local variable
     * @exception DebugException
     *                if this method fails. Reasons include:
     *                <ul>
     *                <li>Failure communicating with the VM. The
     *                DebugException's status code contains the underlying
     *                exception responsible for the failure.</li>
     *                </ul>
     * @since 2.1
     */
    public boolean isLocal() throws DebugException;

}
