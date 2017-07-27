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


import org.eclipse.jdt.ls.debug.adapter.Messages.Response;

import com.google.gson.JsonObject;

public interface IDebugAdapter {
    void dispatchRequest(Response response, String command, JsonObject args);

    void initialize(Response response, Requests.InitializeArguments arguments);

    void launch(Response response, Requests.LaunchArguments arguments);

    void attach(Response response, Requests.AttachArguments arguments);

    void disconnect(Response response);

    void setFunctionBreakpoints(Response response, Requests.SetFunctionBreakpointsArguments arguments);

    // NOTE: This method should never return a failure result, as this causes
    // the launch to be aborted half-way
    // through. Instead, failures should be returned as unverified breakpoints.
    void setBreakpoints(Response response, Requests.SetBreakpointArguments arguments);

    void setExceptionBreakpoints(Response response, Requests.SetExceptionBreakpointsArguments arguments);

    void resume(Response response, Requests.ContinueArguments arguments);

    void next(Response response, Requests.NextArguments arguments);

    void stepIn(Response response, Requests.StepInArguments arguments);

    void stepOut(Response response, Requests.StepOutArguments arguments);

    void pause(Response response, Requests.PauseArguments arguments);

    void threads(Response response);

    void stackTrace(Response response, Requests.StackTraceArguments arguments);

    void scopes(Response response, Requests.ScopesArguments arguments);

    void variables(Response response, Requests.VariablesArguments arguments);

    void setVariable(Response response, Requests.SetVariableArguments arguments);

    void source(Response response, Requests.SourceArguments arguments);

    void evaluate(Response response, Requests.EvaluateArguments arguments);
}
