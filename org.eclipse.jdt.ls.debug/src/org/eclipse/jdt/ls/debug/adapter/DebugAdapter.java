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

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.eclipse.jdt.ls.debug.DebugEvent;
import org.eclipse.jdt.ls.debug.DebugException;
import org.eclipse.jdt.ls.debug.DebugUtility;
import org.eclipse.jdt.ls.debug.IBreakpoint;
import org.eclipse.jdt.ls.debug.IDebugSession;
import org.eclipse.jdt.ls.debug.internal.Logger;

import com.google.gson.JsonObject;
import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.Location;
import com.sun.jdi.Method;
import com.sun.jdi.StackFrame;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.VMDisconnectedException;
import com.sun.jdi.connect.IllegalConnectorArgumentsException;
import com.sun.jdi.connect.VMStartException;
import com.sun.jdi.event.BreakpointEvent;
import com.sun.jdi.event.Event;
import com.sun.jdi.event.StepEvent;
import com.sun.jdi.event.ThreadDeathEvent;
import com.sun.jdi.event.ThreadStartEvent;
import com.sun.jdi.event.VMDeathEvent;
import com.sun.jdi.event.VMDisconnectEvent;
import com.sun.jdi.event.VMStartEvent;

import io.reactivex.disposables.Disposable;

public class DebugAdapter implements IDebugAdapter {
    private Consumer<Events.DebugEvent> eventConsumer;

    private boolean debuggerLinesStartAt1 = true;
    private boolean debuggerPathsAreURI = false;
    private boolean clientLinesStartAt1 = true;
    private boolean clientPathsAreURI = false;

    private Requests.LaunchArguments launchArguments;
    private String cwd;
    private String[] sourcePath;
    private IDebugSession debugSession;
    private BreakpointManager breakpointManager;
    private List<Disposable> eventSubscriptions;
    private IProviderContext context;
    
    private IdCollection<StackFrame> frameCollection = new IdCollection<>();
    private IdCollection<String> sourceCollection = new IdCollection<>();
    private AtomicInteger messageId = new AtomicInteger(1);

    /**
     * Constructor.
     */
    public DebugAdapter(Consumer<Events.DebugEvent> consumer, IProviderContext context) {
        this.eventConsumer = consumer;
        this.breakpointManager = new BreakpointManager();
        this.eventSubscriptions = new ArrayList<>();
        this.context = context;
    }

    @Override
    public Messages.Response dispatchRequest(Messages.Request request) {
        Responses.ResponseBody responseBody = null;
        JsonObject arguments = request.arguments != null ? request.arguments : new JsonObject();

        try {
            switch (request.command) {
                case "initialize":
                    responseBody = initialize(JsonUtils.fromJson(arguments, Requests.InitializeArguments.class));
                    break;

                case "launch":
                    responseBody = launch(JsonUtils.fromJson(arguments, Requests.LaunchArguments.class));
                    break;

                case "attach":
                    responseBody = attach(JsonUtils.fromJson(arguments, Requests.AttachArguments.class));
                    break;

                case "restart":
                    responseBody = restart(JsonUtils.fromJson(arguments, Requests.RestartArguments.class));
                    break;

                case "disconnect":
                    responseBody = disconnect(JsonUtils.fromJson(arguments, Requests.DisconnectArguments.class));
                    break;

                case "configurationDone":
                    responseBody = configurationDone();
                    break;

                case "next":
                    responseBody = next(JsonUtils.fromJson(arguments, Requests.NextArguments.class));
                    break;

                case "continue":
                    responseBody = resume(JsonUtils.fromJson(arguments, Requests.ContinueArguments.class));
                    break;

                case "stepIn":
                    responseBody = stepIn(JsonUtils.fromJson(arguments, Requests.StepInArguments.class));
                    break;

                case "stepOut":
                    responseBody = stepOut(JsonUtils.fromJson(arguments, Requests.StepOutArguments.class));
                    break;

                case "pause":
                    responseBody = pause(JsonUtils.fromJson(arguments, Requests.PauseArguments.class));
                    break;

                case "stackTrace":
                    responseBody = stackTrace(JsonUtils.fromJson(arguments, Requests.StackTraceArguments.class));
                    break;

                case "scopes":
                    responseBody = scopes(JsonUtils.fromJson(arguments, Requests.ScopesArguments.class));
                    break;

                case "variables":
                    Requests.VariablesArguments varArguments = JsonUtils.fromJson(arguments, Requests.VariablesArguments.class);
                    if (varArguments.variablesReference == -1) {
                        responseBody = new Responses.ErrorResponseBody(
                                this.convertDebuggerMessageToClient("VariablesRequest: property 'variablesReference' is missing, null, or empty"));
                    } else {
                        responseBody = variables(varArguments);
                    }
                    break;

                case "setVariable":
                    Requests.SetVariableArguments setVarArguments = JsonUtils.fromJson(arguments,
                            Requests.SetVariableArguments.class);
                    if (setVarArguments.value == null) {
                        // Just exit out of editing if we're given an empty expression.
                        responseBody = new Responses.ResponseBody();
                    } else if (setVarArguments.variablesReference == -1) {
                        responseBody = new Responses.ErrorResponseBody(
                                this.convertDebuggerMessageToClient("SetVariablesRequest: property 'variablesReference' is missing, null, or empty"));
                    } else if (setVarArguments.name == null) {
                        responseBody = new Responses.ErrorResponseBody(
                                this.convertDebuggerMessageToClient("SetVariablesRequest: property 'name' is missing, null, or empty"));
                    } else {
                        responseBody = setVariable(setVarArguments);
                    }
                    break;

                case "source":
                    Requests.SourceArguments sourceArguments = JsonUtils.fromJson(arguments, Requests.SourceArguments.class);
                    if (sourceArguments.sourceReference == -1) {
                        responseBody = new Responses.ErrorResponseBody(
                                this.convertDebuggerMessageToClient("SourceRequest: property 'sourceReference' is missing, null, or empty"));
                    } else {                        
                        responseBody = source(sourceArguments);
                    }
                    break;

                case "threads":
                    responseBody = threads();
                    break;

                case "setBreakpoints":
                    Requests.SetBreakpointArguments setBreakpointArguments = JsonUtils.fromJson(arguments,
                            Requests.SetBreakpointArguments.class);
                    responseBody = setBreakpoints(setBreakpointArguments);
                    break;

                case "setExceptionBreakpoints":
                    responseBody = setExceptionBreakpoints(JsonUtils.fromJson(arguments, Requests.SetExceptionBreakpointsArguments.class));
                    break;

                case "setFunctionBreakpoints":
                    Requests.SetFunctionBreakpointsArguments setFuncBreakpointArguments = JsonUtils.fromJson(arguments,
                            Requests.SetFunctionBreakpointsArguments.class);
                    if (setFuncBreakpointArguments.breakpoints != null) {
                        responseBody = setFunctionBreakpoints(setFuncBreakpointArguments);
                    } else {
                        responseBody = new Responses.ErrorResponseBody(
                                this.convertDebuggerMessageToClient("SetFunctionBreakpointsRequest: property 'breakpoints' is missing, null, or empty"));
                    }
                    break;

                case "evaluate":
                    Requests.EvaluateArguments evaluateArguments = JsonUtils.fromJson(arguments,
                            Requests.EvaluateArguments.class);
                    if (evaluateArguments.expression == null) {
                        responseBody = new Responses.ErrorResponseBody(
                                this.convertDebuggerMessageToClient("EvaluateRequest: property 'expression' is missing, null, or empty"));
                    } else {
                        responseBody = evaluate(evaluateArguments);
                    }
                    break;

                default:
                    responseBody = new Responses.ErrorResponseBody(
                            this.convertDebuggerMessageToClient(String.format("unrecognized request: { _request: %s }", request.command)));
                }
        } catch (Exception e) {
            Logger.logException("DebugSession dispatch exception", e);
            responseBody = new Responses.ErrorResponseBody(
                    this.convertDebuggerMessageToClient(e.getMessage() != null ? e.getMessage() : e.toString()));
        }

        Messages.Response response = new Messages.Response();
        response.request_seq = request.seq;
        response.command = request.command;
        return setBody(response, responseBody);
    }

    /* ======================================================*/
    /* Invoke different dispatch logic for different request */
    /* ======================================================*/

    private Responses.ResponseBody initialize(Requests.InitializeArguments arguments) {
        this.clientLinesStartAt1 = arguments.linesStartAt1;
        String pathFormat = arguments.pathFormat;
        if (pathFormat != null) {
            switch (pathFormat) {
                case "uri":
                    this.clientPathsAreURI = true;
                    break;
                default:
                    this.clientPathsAreURI = false;
                }
        }
        // Send an InitializedEvent
        this.sendEvent(new Events.InitializedEvent());

        Types.Capabilities caps = new Types.Capabilities();
        caps.supportsConfigurationDoneRequest = true;
        caps.supportsRestartRequest = true;
        caps.supportTerminateDebuggee = true;
        return new Responses.InitializeResponseBody(caps);
    }

    private Responses.ResponseBody launch(Requests.LaunchArguments arguments) {
        // Need cache the launch json because VSCode doesn't resend the launch json at the RestartRequest.
        this.launchArguments = arguments;
        try {
            this.launchDebugSession(arguments);
        } catch (DebugException e) {
            return new Responses.ErrorResponseBody(
                    this.convertDebuggerMessageToClient("Cannot launch debuggee vm: " + e.getMessage()));
        }
        return new Responses.ResponseBody();
    }

    private Responses.ResponseBody attach(Requests.AttachArguments arguments) {
        return new Responses.ResponseBody();
    }

    private Responses.ResponseBody restart(Requests.RestartArguments arguments) {
        // Shutdown the old debug session.
        this.shutdownDebugSession(true);
        // Launch new debug session.
        try {
            this.launchDebugSession(this.launchArguments);
        } catch (DebugException e) {
            return new Responses.ErrorResponseBody(
                    this.convertDebuggerMessageToClient("Cannot restart debuggee vm: " + e.getMessage()));
        }
        // See VSCode bug 28175 (https://github.com/Microsoft/vscode/issues/28175).
        // Need send a ContinuedEvent to clean up the old debugger's call stacks.
        this.sendEvent(new Events.ContinuedEvent(true));
        // Send an InitializedEvent to ask VSCode to restore the existing breakpoints.
        this.sendEvent(new Events.InitializedEvent());
        return new Responses.ResponseBody();
    }

    /**
     * VS Code terminates a debug session with the disconnect request.
     */
    private Responses.ResponseBody disconnect(Requests.DisconnectArguments arguments) {
        this.shutdownDebugSession(arguments.terminateDebuggee);
        return new Responses.ResponseBody();
    }

    /**
     * VS Code sends a configurationDone request to indicate the end of configuration sequence.
     */
    private Responses.ResponseBody configurationDone() {
        this.eventSubscriptions.add(this.debugSession.eventHub().events().subscribe(debugEvent -> {
            handleEvent(debugEvent);
        }));
        this.debugSession.start();
        return new Responses.ResponseBody();
    }

    private Responses.ResponseBody setFunctionBreakpoints(Requests.SetFunctionBreakpointsArguments arguments) {
        return new Responses.ResponseBody();
    }

    private Responses.ResponseBody setBreakpoints(Requests.SetBreakpointArguments arguments) {
        List<Types.Breakpoint> res = new ArrayList<>();
        IBreakpoint[] toAdds = this.convertClientBreakpointsToDebugger(arguments.source.path, arguments.lines);
        IBreakpoint[] added = this.breakpointManager.setBreakpoints(arguments.source.path, toAdds, arguments.sourceModified);
        for (int i = 0; i < arguments.breakpoints.length; i++) {
            // For newly added breakpoint, should install it to debuggee first.
            if (toAdds[i] == added[i] && added[i].className() != null) {
                added[i].install().thenAccept(bp -> {
                    Events.BreakpointEvent bpEvent = new Events.BreakpointEvent("new", this.convertDebuggerBreakpointToClient(bp));
                    sendEvent(bpEvent);
                });
            }
            res.add(this.convertDebuggerBreakpointToClient(added[i]));
        }

        return new Responses.SetBreakpointsResponseBody(res);
    }

    private Responses.ResponseBody setExceptionBreakpoints(Requests.SetExceptionBreakpointsArguments arguments) {
        return new Responses.ResponseBody();
    }

    private Responses.ResponseBody resume(Requests.ContinueArguments arguments) {
        boolean allThreadsContinued = true;
        ThreadReference thread = getThread(arguments.threadId);
        if (thread != null) {
            allThreadsContinued = false;
            thread.resume();
        } else {
            this.debugSession.resume();
        }
        return new Responses.ContinueResponseBody(allThreadsContinued);
    }

    private Responses.ResponseBody next(Requests.NextArguments arguments) {
        ThreadReference thread = getThread(arguments.threadId);
        if (thread != null) {
            DebugUtility.stepOver(thread, this.debugSession.eventHub());
        }
        return new Responses.ResponseBody();
    }

    private Responses.ResponseBody stepIn(Requests.StepInArguments arguments) {
        ThreadReference thread = getThread(arguments.threadId);
        if (thread != null) {
            DebugUtility.stepInto(thread, this.debugSession.eventHub());
        }
        return new Responses.ResponseBody();
    }

    private Responses.ResponseBody stepOut(Requests.StepOutArguments arguments) {
        ThreadReference thread = getThread(arguments.threadId);
        if (thread != null) {
            DebugUtility.stepOut(thread, this.debugSession.eventHub());
        }
        return new Responses.ResponseBody();
    }

    private Responses.ResponseBody pause(Requests.PauseArguments arguments) {
        ThreadReference thread = getThread(arguments.threadId);
        if (thread != null) {
            thread.suspend();
            this.sendEvent(new Events.StoppedEvent("pause", arguments.threadId));
        } else {
            this.debugSession.suspend();
            this.sendEvent(new Events.StoppedEvent("pause", arguments.threadId, true));
        }
        return new Responses.ResponseBody();
    }

    private Responses.ResponseBody threads() {
        ArrayList<Types.Thread> threads = new ArrayList<>();
        for (ThreadReference thread : this.safeGetAllThreads()) {
            Types.Thread clientThread = this.convertDebuggerThreadToClient(thread);
            threads.add(clientThread);
        }
        return new Responses.ThreadsResponseBody(threads);
    }

    private Responses.ResponseBody stackTrace(Requests.StackTraceArguments arguments) {
        List<Types.StackFrame> result = new ArrayList<>();
        if (arguments.startFrame < 0 || arguments.levels < 0) {
            return new Responses.StackTraceResponseBody(result, 0);
        }
        ThreadReference thread = getThread(arguments.threadId);
        if (thread != null) {
            try {
                List<StackFrame> stackFrames = thread.frames();
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
        return new Responses.StackTraceResponseBody(result, result.size());
    }

    private Responses.ResponseBody scopes(Requests.ScopesArguments arguments) {
        List<Types.Scope> scps = new ArrayList<>();
        scps.add(new Types.Scope("Local", 1000000 + arguments.frameId, false));
        return new Responses.ScopesResponseBody(scps);
    }

    private Responses.ResponseBody variables(Requests.VariablesArguments arguments) {
        List<Types.Variable> list = new ArrayList<>();
        return new Responses.VariablesResponseBody(list);
    }

    private Responses.ResponseBody setVariable(Requests.SetVariableArguments arguments) {
        return new Responses.ResponseBody();
    }

    private Responses.ResponseBody source(Requests.SourceArguments arguments) {
        int sourceReference = arguments.sourceReference;
        String uri = sourceCollection.get(sourceReference);
        String contents = this.convertDebuggerSourceToClient(uri);
        return new Responses.SourceResponseBody(contents);
    }

    private Responses.ResponseBody evaluate(Requests.EvaluateArguments arguments) {
        return new Responses.ResponseBody();
    }

    /* ======================================================*/
    /* Dispatch logic End */
    /* ======================================================*/

    // This is a global event handler to handle the JDI Event from Virtual Machine.
    private void handleEvent(DebugEvent debugEvent) {
        Event event = debugEvent.event;
        if (event instanceof VMStartEvent) {
            // do nothing.
        } else if (event instanceof VMDeathEvent) {
            this.sendEvent(new Events.ExitedEvent(0));
        } else if (event instanceof VMDisconnectEvent) {
            this.sendEvent(new Events.TerminatedEvent());
            // Terminate eventHub thread.
            try {
                this.debugSession.eventHub().close();
            } catch (Exception e) {
                // do nothing.
            }
        } else if (event instanceof ThreadStartEvent) {
            ThreadReference startThread = ((ThreadStartEvent) event).thread();
            Events.ThreadEvent threadEvent = new Events.ThreadEvent("started", startThread.uniqueID());
            this.sendEvent(threadEvent);
        } else if (event instanceof ThreadDeathEvent) {
            ThreadReference deathThread = ((ThreadDeathEvent) event).thread();
            Events.ThreadEvent threadDeathEvent = new Events.ThreadEvent("exited", deathThread.uniqueID());
            this.sendEvent(threadDeathEvent);
        } else if (event instanceof BreakpointEvent) {
            ThreadReference bpThread = ((BreakpointEvent) event).thread();
            this.sendEvent(new Events.StoppedEvent("breakpoint", bpThread.uniqueID()));
            debugEvent.shouldResume = false;
        } else if (event instanceof StepEvent) {
            ThreadReference stepThread = ((StepEvent) event).thread();
            this.sendEvent(new Events.StoppedEvent("step", stepThread.uniqueID()));
            debugEvent.shouldResume = false;
        }
    }

    private Messages.Response setBody(Messages.Response response, Responses.ResponseBody body) {
        response.body = body;
        if (body instanceof Responses.ErrorResponseBody) {
            response.success = false;
            Types.Message error = ((Responses.ErrorResponseBody) body).error;
            if (error.format != null) {
                response.message = error.format;
            } else {
                response.message = "Error response body";              
            }
        } else {
            response.success = true;
            if (body instanceof Responses.InitializeResponseBody) {
                response.body = ((Responses.InitializeResponseBody) body).body;
            }
        }
        return response;
      }

    private void sendEvent(Events.DebugEvent event) {
        this.eventConsumer.accept(event);
    }

    private void launchDebugSession(Requests.LaunchArguments arguments) throws DebugException {
        this.cwd = arguments.cwd;
        String mainClass = arguments.startupClass;
        String classpath = arguments.classpath;
        if (arguments.sourcePath == null || arguments.sourcePath.length == 0) {
            this.sourcePath = new String[] { cwd };
        } else {
            this.sourcePath = new String[arguments.sourcePath.length];
            System.arraycopy(arguments.sourcePath, 0, this.sourcePath, 0, arguments.sourcePath.length);
        }

        Logger.logInfo("Launch JVM with main class \"" + mainClass + "\", -classpath \"" + classpath + "\"");

        try {
            this.debugSession = DebugUtility.launch(context.getVirtualMachineManagerProvider().getVirtualMachineManager(), mainClass, classpath);
        } catch (IOException | IllegalConnectorArgumentsException | VMStartException e) {
            Logger.logException("Launching debuggee vm exception", e);
            throw new DebugException("Launching debuggee vm exception \"" + e.getMessage() + "\"", e);
        }
    }

    private void shutdownDebugSession(boolean terminateDebuggee) {
        // Unsubscribe event handler.
        this.eventSubscriptions.forEach(subscription -> {
            subscription.dispose();
        });
        this.eventSubscriptions.clear();
        this.breakpointManager.reset();
        this.frameCollection.reset();
        this.sourceCollection.reset();
        if (this.debugSession.process().isAlive()) {
            if (terminateDebuggee) {
                this.debugSession.terminate();
            } else {
                this.debugSession.detach();
            }
        }
    }

    private ThreadReference getThread(int threadId) {
        for (ThreadReference thread : this.safeGetAllThreads()) {
            if (thread.uniqueID() == threadId) {
                return thread;
            }
        }
        return null;
    }

    private List<ThreadReference> safeGetAllThreads() {
        try {
            return this.debugSession.allThreads();
        } catch (VMDisconnectedException ex) {
            return new ArrayList<>();
        }
    }

    private int convertDebuggerLineToClient(int line) {
        if (this.debuggerLinesStartAt1) {
            return this.clientLinesStartAt1 ? line : line - 1;
        } else {
            return this.clientLinesStartAt1 ? line + 1 : line;
        }
    }

    private int convertClientLineToDebugger(int line) {
        if (this.debuggerLinesStartAt1) {
            return this.clientLinesStartAt1 ? line : line + 1;
        } else {
            return this.clientLinesStartAt1 ? line - 1 : line;
        }
    }

    private int[] convertClientLineToDebugger(int[] lines) {
        int[] newLines = new int[lines.length];
        for (int i = 0; i < lines.length; i++) {
            newLines[i] = convertClientLineToDebugger(lines[i]);
        }
        return newLines;
    }

    private Types.Breakpoint convertDebuggerBreakpointToClient(IBreakpoint breakpoint) {
        int id = (int) breakpoint.getProperty("id");
        boolean verified = breakpoint.getProperty("verified") != null ? (boolean) breakpoint.getProperty("verified") : false;
        int lineNumber = this.convertDebuggerLineToClient(breakpoint.lineNumber());
        return new Types.Breakpoint(id, verified, lineNumber, "");
    }

    private IBreakpoint[] convertClientBreakpointsToDebugger(String sourceFile, int[] lines) {
        int[] debuggerLines = this.convertClientLineToDebugger(lines);
        String[] fqns = context.getSourceLookUpProvider().getFullyQualifiedName(sourceFile, debuggerLines, null);
        IBreakpoint[] breakpoints = new IBreakpoint[lines.length];
        for (int i = 0; i < lines.length; i++) {
            breakpoints[i] = this.debugSession.createBreakpoint(fqns[i], debuggerLines[i]);
        }
        return breakpoints;
    }

    private Types.Source convertDebuggerSourceToClient(Location location) throws URISyntaxException, AbsentInformationException {
        Types.Source source = null;
        String uri = context.getSourceLookUpProvider().getSourceFileURI(location.declaringType().name());
        String name = location.sourceName();

        if (uri != null && uri.startsWith("jdt://")) {
            int sourceReference = sourceCollection.create(uri);
            source = new Types.Source(name, uri, sourceReference);
        } else if (uri != null) {
            source = new Types.Source(name, Paths.get(new URI(uri)).toString(), 0);
        } else {
            String originalSourcePath = location.sourcePath();
            String sourcepath = AdapterUtils.sourceLookup(this.sourcePath, originalSourcePath);
            if (sourcepath == null) {
                sourcepath = Paths.get(this.cwd, originalSourcePath).toString();
            }
            source = new Types.Source(name, sourcepath, 0);
        }
        return source;
    }

    private String convertDebuggerSourceToClient(String uri) {
        return context.getSourceLookUpProvider().getSourceContents(uri);
    }

    private Types.Thread convertDebuggerThreadToClient(ThreadReference thread) {
        return new Types.Thread(thread.uniqueID(), "Thread [" + thread.name() + "]");
    }

    private Types.StackFrame convertDebuggerStackFrameToClient(StackFrame stackFrame)
            throws URISyntaxException, AbsentInformationException {
        int frameId = this.frameCollection.create(stackFrame);
        Location location = stackFrame.location();
        Method method = location.method();
        Types.Source clientSource = this.convertDebuggerSourceToClient(location);
        return new Types.StackFrame(frameId, method.name(), clientSource,
                this.convertDebuggerLineToClient(location.lineNumber()), 0);
    }

    private Types.Message convertDebuggerMessageToClient(String message) {
        return new Types.Message(this.messageId.getAndIncrement(), message);
    }
}
