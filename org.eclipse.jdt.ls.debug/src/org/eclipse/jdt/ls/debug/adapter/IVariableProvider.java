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

import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.StackFrame;
import com.sun.jdi.Value;

import java.util.List;

/**
 * The interface for variable provider, implementations of this interface
 * will provide the ability to convert JDI value to display string.
 */
public interface IVariableProvider {
    /**
     * Test whether the value has referenced objects.
     *
     * @param value the value.
     * @return true if this value is reference objects.
     */
    boolean hasReferences(Value value);

    /**
     * Get display text of the value.
     *
     * @param value the value.
     * @return the display text of the value
     */
    String valueToString(Value value);

    /**
     * Get display name of type of the value.
     * @param value the value
     * @return display name of type of the value.
     */
    String valueTypeString(Value value);

    /**
     * Get the variables of the object.
     *
     * @param obj the object
     * @return the variable list
     * @throws AbsentInformationException when there is any error in retrieving information
     */
    List<JavaVariable> listFieldVariables(ObjectReference obj) throws AbsentInformationException;

    /**
     * Get the local variables of an stack frame.
     *
     * @param stackFrame the stack frame
     * @return local variable list
     * @throws AbsentInformationException when there is any error in retrieving information
     */
    List<JavaVariable> listLocalVariables(StackFrame stackFrame) throws AbsentInformationException;

    /**
     * Get the this variable of an stack frame.
     *
     * @param stackFrame the stack frame
     * @return this variable
     */
    JavaVariable getThisVariable(StackFrame stackFrame);

    /**
     * Get the static variable of an stack frame.
     *
     * @param stackFrame the stack frame
     * @return the static variable of an stack frame.
     */
    List<JavaVariable> listStaticVariables(StackFrame stackFrame);
}
