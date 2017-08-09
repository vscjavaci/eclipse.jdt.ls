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

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.ls.debug.internal.Logger;

import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.Location;
import com.sun.jdi.Method;
import com.sun.jdi.StackFrame;
import com.sun.jdi.ThreadReference;

public class VariableDebugAdapter {

    private final DebugAdapter parent;
    private final IdCollection<StackFrame> frameCollection = new IdCollection<>();

    public VariableDebugAdapter(DebugAdapter debugAdapter) {
        this.parent = debugAdapter;
    }

    public void reset() {
        this.frameCollection.reset();
    }

   Responses.ResponseBody stackTrace(Requests.StackTraceArguments arguments) {
        List<Types.StackFrame> result = new ArrayList<>();
        if (arguments.startFrame < 0 || arguments.levels < 0) {
            return new Responses.StackTraceResponseBody(result, 0);
        }
        ThreadReference thread = parent.getThread(arguments.threadId);
        int frameCount = 0;
        if (thread != null) {
            try {
                List<StackFrame> stackFrames = thread.frames();
                frameCount = stackFrames.size();
                if (arguments.startFrame >= stackFrames.size()) {
                    return new Responses.StackTraceResponseBody(result, 0);
                }
                if (arguments.levels == 0) {
                    arguments.levels = stackFrames.size() - arguments.startFrame;
                } else {
                    arguments.levels = Math.min(stackFrames.size() - arguments.startFrame, arguments.levels);
                }

                for (int i = 0; i < arguments.levels; i++) {
                    StackFrame stackFrame = stackFrames.get(arguments.startFrame + i);
                    Types.StackFrame clientStackFrame = this.convertDebuggerStackFrameToClient(stackFrame);
                    result.add(clientStackFrame);
                }
            } catch (IncompatibleThreadStateException | AbsentInformationException | URISyntaxException e) {
                Logger.logException("DebugSession#stackTrace exception", e);
            }
        }
        return new Responses.StackTraceResponseBody(result, frameCount);
    }

    Responses.ResponseBody scopes(Requests.ScopesArguments arguments) {
        List<Types.Scope> scopes = new ArrayList<>();
        scopes.add(new Types.Scope("Local", 1000000 + arguments.frameId, false));
        return new Responses.ScopesResponseBody(scopes);
    }

    Responses.ResponseBody variables(Requests.VariablesArguments arguments) {
        List<Types.Variable> list = new ArrayList<>();
        return new Responses.VariablesResponseBody(list);
    }

    Responses.ResponseBody setVariable(Requests.SetVariableArguments arguments) {
        return new Responses.ResponseBody();
    }

    private Types.StackFrame convertDebuggerStackFrameToClient(StackFrame stackFrame)
            throws URISyntaxException, AbsentInformationException {
        int frameId = this.frameCollection.create(stackFrame);
        Location location = stackFrame.location();
        Method method = location.method();
        Types.Source clientSource = parent.convertDebuggerSourceToClient(location);
        return new Types.StackFrame(frameId, method.name(), clientSource,
                parent.convertDebuggerLineToClient(location.lineNumber()), 0);
    }
}
