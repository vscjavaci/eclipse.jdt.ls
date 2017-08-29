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

public enum ErrorCode {
    UNKNOWN_FAILURE,
    UNRECOGNIZED_REQUEST_FAILURE,
    LAUNCH_FAILURE,
    ATTACH_FAILURE,
    ARGUMENT_MISSING,
    SET_BREAKPOINT_FAILURE,
    SET_EXCEPTIONBREAKPOINT_FAILURE,
    GET_STACKTRACE_FAILURE,
    GET_VARIABLE_FAILURE,
    SET_VARIABLE_FAILURE,
    EVALUATE_FAILURE,
    EMPTY_DEBUG_SESSION
}
