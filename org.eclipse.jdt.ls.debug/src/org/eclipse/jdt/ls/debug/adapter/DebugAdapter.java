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
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdi.Bootstrap;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.ls.debug.DebugEvent;
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

    private String cwd;
    private String[] sourcePath;
    private boolean shutdown = false;
    private IDebugSession debugSession;
    private BreakpointManager breakpointManager;
    private AtomicInteger nextBreakpointId = new AtomicInteger(1);
    private List<Disposable> eventSubscriptions;
    
    private IdCollection<StackFrame> frameCollection = new IdCollection<>();
    private IdCollection<String> sourceCollection = new IdCollection<>();
    private HashMap<Long, Events.StoppedEvent> stoppedEventsByThread = new HashMap<>();

    /**
     * Constructor.
     */
    public DebugAdapter(Consumer<Events.DebugEvent> consumer) {
        this.eventConsumer = consumer;
        this.breakpointManager = new BreakpointManager();
        this.eventSubscriptions = new ArrayList<>();
    }

    @Override
    public Messages.Response dispatchRequest(Messages.Request request) {
        if (this.shutdown) {
            return new Messages.Response(request.seq, request.command, true);
        }

        Responses.ResponseBody responseBody = null;
        JsonObject arguments = request.arguments != null ? request.arguments : new JsonObject();
        Logger.logInfo("Dispatch command:" + request.command);

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

                case "disconnect":
                    responseBody = disconnect();
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
                        responseBody = new Responses.ErrorResponseBody(new Types.Message(1009, String.format("%s: property '%s' is missing, null, or empty",
                                "variables", "variablesReference"), null));
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
                        responseBody = new Responses.ErrorResponseBody(new Types.Message(1106, String.format("%s: property '%s' is missing, null, or empty",
                                "setVariable", "variablesReference"), null));
                    } else if (setVarArguments.name == null) {
                        responseBody = new Responses.ErrorResponseBody(new Types.Message(1106,
                                String.format("%s: property '%s' is missing, null, or empty", "setVariable", "name"), null));
                    } else {
                        responseBody = setVariable(setVarArguments);
                    }
                    break;

                case "source":
                    Requests.SourceArguments sourceArguments = JsonUtils.fromJson(arguments, Requests.SourceArguments.class);
                    if (sourceArguments.sourceReference == -1) {
                        responseBody = new Responses.ErrorResponseBody(new Types.Message(1010,
                                String.format("%s: property '%s' is missing, null, or empty", "source", "sourceReference"),
                                null));
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
                        // FunctionBreakpoint[] breakpoints =
                        // mapper.readValue(args.getString("breakpoints"),
                        // FunctionBreakpoint[].class);
                        // return SetFunctionBreakpoints(breakpoints);
                    }
                    responseBody = new Responses.ErrorResponseBody(new Types.Message(1012, String.format("%s: property '%s' is missing, null, or empty",
                            "setFunctionBreakpoints", "breakpoints"), null));
                    break;

                case "evaluate":
                    Requests.EvaluateArguments evaluateArguments = JsonUtils.fromJson(arguments,
                            Requests.EvaluateArguments.class);
                    if (evaluateArguments.expression == null) {
                        responseBody = new Responses.ErrorResponseBody(new Types.Message(1013,
                                String.format("%s: property '%s' is missing, null, or empty", "evaluate", "expression"),
                                null));
                    } else {
                        responseBody = evaluate(evaluateArguments);
                    }
                    break;

                default:
                    responseBody = new Responses.ErrorResponseBody(new Types.Message(1014, String.format("unrecognized request: { _request: %s }",
                            request.command), null));
                }
        } catch (Exception e) {
            Logger.logException("DebugSession dispatch exception", e);
            responseBody = new Responses.ErrorResponseBody(new Types.Message(1104, e.getMessage(), null));
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
        caps.supportsDelayedStackTraceLoading = true;
        return new Responses.InitializeResponseBody(caps);
    }

    private Responses.ResponseBody launch(Requests.LaunchArguments arguments) {
        this.cwd = arguments.cwd;
        String mainClass = arguments.startupClass;
        if (mainClass.endsWith(".java")) {
            mainClass = mainClass.substring(0, mainClass.length() - 5);
        }

        String classpath;
        try {
            IJavaProject project = AdapterUtils.getJavaProject(arguments.projectName, mainClass);
            classpath = AdapterUtils.computeClassPath(project);
            classpath = classpath.replaceAll("\\\\", "/");
        } catch (CoreException e) {
            Logger.logException("Failed to resolve classpath.", e);
            return new Responses.ErrorResponseBody(new Types.Message(3001, "Cannot launch jvm.", null));
        }

        if (arguments.sourcePath == null || arguments.sourcePath.length == 0) {
            this.sourcePath = new String[] { cwd };
        } else {
            this.sourcePath = new String[arguments.sourcePath.length];
            System.arraycopy(arguments.sourcePath, 0, this.sourcePath, 0, arguments.sourcePath.length);
        }

        Logger.logInfo("Launch JVM with main class \"" + mainClass + "\", -classpath \"" + classpath + "\"");
        
        try {
            this.debugSession = DebugUtility.launch(Bootstrap.virtualMachineManager(), mainClass, classpath);
        } catch (IOException | IllegalConnectorArgumentsException | VMStartException e) {
            Logger.logException("Launching debuggee vm exception", e);
            return new Responses.ErrorResponseBody(new Types.Message(3001, "Cannot launch jvm.", null));
        }

        return new Responses.ResponseBody();
    }

    private Responses.ResponseBody attach(Requests.AttachArguments arguments) {
        return new Responses.ResponseBody();
    }

    /**
     * VS Code terminates a debug session with the disconnect request.
     */
    private Responses.ResponseBody disconnect() {
        this.eventSubscriptions.forEach(subscription -> {
            subscription.dispose();
        });
        
        this.sendEvent(new Events.TerminatedEvent());
        return new Responses.ResponseBody();
    }

    /**
     * VS Code sends a configurationDone request to indicate the end of configuration sequence.
     */
    private Responses.ResponseBody configurationDone() {
        this.debugSession.start();
        this.eventSubscriptions.add(this.debugSession.eventHub().events().subscribe(debugEvent -> {
            handleEvent(debugEvent);
        }));
        // The configuration sequence has done, then resume VM.
        this.debugSession.resume();
        return new Responses.ResponseBody();
    }

    private Responses.ResponseBody setFunctionBreakpoints(Requests.SetFunctionBreakpointsArguments arguments) {
        return new Responses.ResponseBody();
    }

    private Responses.ResponseBody setBreakpoints(Requests.SetBreakpointArguments arguments) {
        int size = arguments.breakpoints.length;
        int[] debuggerLines = this.convertClientLineToDebugger(arguments.lines);
        List<Types.Breakpoint> res = new ArrayList<>(size);
        String[] fqns = AdapterUtils.getFullQualifiedName(arguments.source, debuggerLines);
        IBreakpoint[] newBreakpoints = new IBreakpoint[size];
        for (int i = 0; i < size; i++) {
            newBreakpoints[i] = this.debugSession.createBreakpoint(fqns[i], debuggerLines[i]);
        }
        IBreakpoint[] breakpoints = this.breakpointManager.addBreakpoints(arguments.source.path, newBreakpoints, arguments.sourceModified);
        for (int i = 0; i < size; i++) {
            if (newBreakpoints[i] == breakpoints[i] && breakpoints[i].className() != null) {
                breakpoints[i].install().thenAccept(bp -> {
                    int id = (int) bp.getProperty("id");
                    boolean verified = (boolean) bp.getProperty("verified");
                    int lineNumber = bp.lineNumber();
                    Events.BreakpointEvent bpEvent = new Events.BreakpointEvent("new", new Types.Breakpoint(id, verified, lineNumber, ""));
                    sendEvent(bpEvent);
                });
                breakpoints[i].putProperty("id", this.nextBreakpointId.getAndIncrement());
            }
            res.add(new Types.Breakpoint((int) breakpoints[i].getProperty("id"), (boolean) breakpoints[i].getProperty("verified"), this.convertDebuggerLineToClient(breakpoints[i].lineNumber()), ""));
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
            this.stoppedEventsByThread.remove(arguments.threadId);
            thread.resume();
        }
        if (allThreadsContinued) {
            this.debugSession.resume();
        }
        return new Responses.ContinueResponseBody(allThreadsContinued);
    }

    private Responses.ResponseBody next(Requests.NextArguments arguments) {
        return new Responses.ResponseBody();
    }

    private Responses.ResponseBody stepIn(Requests.StepInArguments arguments) {
        return new Responses.ResponseBody();
    }

    private Responses.ResponseBody stepOut(Requests.StepOutArguments arguments) {
        return new Responses.ResponseBody();
    }

    private Responses.ResponseBody pause(Requests.PauseArguments arguments) {
        return new Responses.ResponseBody();
    }

    private Responses.ResponseBody threads() {
        if (this.shutdown) {
            return new Responses.ThreadsResponseBody(new ArrayList<Types.Thread>());
        }
        ArrayList<Types.Thread> threads = new ArrayList<>();
        for (ThreadReference thread : this.safeGetAllThreads()) {
            threads.add(new Types.Thread(thread.uniqueID(), "Thread [" + thread.name() + "]"));
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
                    int frameId = this.frameCollection.create(stackFrame);
                    Location location = stackFrame.location();
                    Method method = location.method();
                    Types.Source source = getAdapterSource(location);
                    Types.StackFrame newFrame = new Types.StackFrame(frameId, method.name(),
                            source, this.convertDebuggerLineToClient(location.lineNumber()), 0);
                    result.add(newFrame);
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
        String source = AdapterUtils.getSource(uri);
        return new Responses.SourceResponseBody(source);
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
            debugEvent.consumed = true;
            debugEvent.shouldResume &= false;
        } else if (event instanceof VMDeathEvent || event instanceof VMDisconnectEvent) {
            if (!this.shutdown) {
                Events.ExitedEvent exitedEvent = new Events.ExitedEvent(0);
                Events.TerminatedEvent terminatedEvent = new Events.TerminatedEvent();
                this.sendEvent(exitedEvent);
                this.sendEvent(terminatedEvent);
            }
            this.shutdown = true;
            debugEvent.consumed = true;
            debugEvent.shouldResume &= false;
        } else if (event instanceof ThreadStartEvent) {
            ThreadReference startThread = ((ThreadStartEvent) event).thread();
            Events.ThreadEvent threadEvent = new Events.ThreadEvent("started", startThread.uniqueID());
            this.sendEvent(threadEvent);
            debugEvent.consumed = true;
            debugEvent.shouldResume &= true;
        } else if (event instanceof ThreadDeathEvent) {
            ThreadReference deathThread = ((ThreadDeathEvent) event).thread();
            Events.ThreadEvent threadDeathEvent = new Events.ThreadEvent("exited", deathThread.uniqueID());
            this.stoppedEventsByThread.remove(deathThread.uniqueID());
            this.sendEvent(threadDeathEvent);
            for (Events.StoppedEvent stoppedEvent : this.stoppedEventsByThread.values()) {
                this.sendEvent(stoppedEvent);
            }
            debugEvent.consumed = true;
            debugEvent.shouldResume &= true;
        } else if (event instanceof BreakpointEvent) {
            BreakpointEvent bpEvent = (BreakpointEvent) event;
            ThreadReference bpThread = bpEvent.thread();
            Location bpLocation = bpEvent.location();
            try {
                Types.Source adapterSource = getAdapterSource(bpLocation);
                Events.StoppedEvent stopevent = new Events.StoppedEvent("breakpoint",
                        adapterSource, this.convertDebuggerLineToClient(bpLocation.lineNumber()),
                        0, "", bpThread.uniqueID());
                this.stoppedEventsByThread.put(bpThread.uniqueID(), stopevent);
                this.sendEvent(stopevent);
            } catch (AbsentInformationException | URISyntaxException e) {
                Logger.logException("Get breakpoint info exception", e);
            }
            debugEvent.consumed = true;
            debugEvent.shouldResume &= false;
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

    private Types.Source getAdapterSource(Location location) throws URISyntaxException, AbsentInformationException {
        Types.Source source = null;
        String uri = null;
        String name = location.sourceName();
        try {
            uri = AdapterUtils.getURI(location);
        } catch (JavaModelException e) {
            // do nothing.
        }

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
}
