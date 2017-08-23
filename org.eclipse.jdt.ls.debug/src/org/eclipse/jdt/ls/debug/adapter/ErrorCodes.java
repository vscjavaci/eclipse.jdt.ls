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

public final class ErrorCodes {
    public static final int UNKNOWN_FAILURE = 1000;
    public static final int UNRECOGNIZED_REQUEST_FAILURE = 1001;
    public static final int LAUNCH_FAILURE = 1002;
    public static final int ATTACH_FAILURE = 1003;
    public static final int ARGUMENT_MISSING = 1004;
    public static final int SET_BREAKPOINT_FAILURE = 1005;
    public static final int SET_EXCEPTIONBREAKPOINT_FAILURE = 1006;
    public static final int GET_STACKTRACE_FAILURE = 1007;
    public static final int GET_VARIABLE_FAILURE = 1008;
}
