/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Code copied from org.eclipse.debug.core.model.IDebugElement
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.ls.debug.internal.model;

/**
 * A debug element represents an artifact in a program being debugged.
 * <p>
 * Some methods on debug elements require communication with the target program.
 * Such methods may throw a <code>DebugException</code> with a status code of
 * <code>TARGET_REQUEST_FAILED</code> when unable to complete a request due to a
 * failure on the target. Methods that require communication with the target
 * program or require the target to be in a specific state (for example,
 * suspended), are declared as such.
 * </p>
 * <p>
 * Clients may implement this interface.
 * </p>
 */
public interface IDebugElement {

    /**
     * Returns the debug target this element is contained in.
     *
     * @return the debug target this element is contained in
     */
    public IDebugTarget getDebugTarget();
}
