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

package org.eclipse.jdt.ls.debug.internal.adapter;

import org.eclipse.jdt.ls.debug.internal.adapter.Results.DebugResult;
import org.eclipse.jdt.ls.debug.internal.adapter.Results.SetBreakpointsResponseBody;

import com.google.gson.JsonObject;

public interface IDebugSession {
	DebugResult Dispatch(String command, JsonObject args);

	DebugResult Initialize(Requests.InitializeArguments arguments);
	DebugResult Launch(Requests.LaunchArguments arguments);
	DebugResult Attach(Requests.AttachArguments arguments);
	DebugResult Disconnect();

	DebugResult SetFunctionBreakpoints(Requests.SetFunctionBreakpointsArguments arguments);

	// NOTE: This method should never return a failure result, as this causes the launch to be aborted half-way
	// through. Instead, failures should be returned as unverified breakpoints.
	SetBreakpointsResponseBody SetBreakpoints(Requests.SetBreakpointArguments arguments);
	DebugResult SetExceptionBreakpoints(Requests.SetExceptionBreakpointsArguments arguments);

	DebugResult Continue(Requests.ContinueArguments arguments);
	DebugResult Next(Requests.NextArguments arguments);
	DebugResult StepIn(Requests.StepInArguments arguments);
	DebugResult StepOut(Requests.StepOutArguments arguments);
	DebugResult Pause(Requests.PauseArguments arguments);

	DebugResult Threads();
	DebugResult StackTrace(Requests.StackTraceArguments arguments);
	DebugResult Scopes(Requests.ScopesArguments arguments);
	DebugResult Variables(Requests.VariablesArguments arguments);
	DebugResult SetVariable(Requests.SetVariableArguments arguments);
	DebugResult Source(Requests.SourceArguments arguments);

	DebugResult Evaluate(Requests.EvaluateArguments arguments);
}
