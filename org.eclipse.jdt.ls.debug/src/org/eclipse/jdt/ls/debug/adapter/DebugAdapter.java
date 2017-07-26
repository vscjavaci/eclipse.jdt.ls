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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdi.Bootstrap;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.ls.debug.DebugEvent;
import org.eclipse.jdt.ls.debug.DebugUtility;
import org.eclipse.jdt.ls.debug.IBreakpoint;
import org.eclipse.jdt.ls.debug.IDebugSession;
import org.eclipse.jdt.ls.debug.adapter.DispatcherProtocol.IResponder;
import org.eclipse.jdt.ls.debug.adapter.Results.DebugResult;
import org.eclipse.jdt.ls.debug.adapter.Results.SetBreakpointsResponseBody;
import org.eclipse.jdt.ls.debug.adapter.Types.Capabilities;
import org.eclipse.jdt.ls.debug.internal.JavaDebuggerServerPlugin;
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
    protected boolean debuggerLinesStartAt1;
    protected boolean debuggerPathsAreURI;
    protected boolean clientLinesStartAt1 = true;
    protected boolean clientPathsAreURI = true;

    private String cwd;
    private String[] sourcePath;
    private IResponder responder;
    private boolean shutdown = false;
    private IDebugSession debugSession;
    private BreakpointManager breakpointManager;
    private AtomicInteger nextBreakpointId = new AtomicInteger(1);
    private List<Disposable> eventSubscriptions;
    
    private IdCollection<StackFrame> frameCollection = new IdCollection<>();
    private IdCollection<String> sourceCollection = new IdCollection<>();
    private HashMap<Long, Events.StoppedEvent> stoppedEventsByThread = new HashMap<>();

    /**
     * Constructs a DebugSession instance.
     */
    public DebugAdapter(boolean debuggerLinesStartAt1, boolean debuggerPathsAreURI, IResponder responder) {
        this.debuggerLinesStartAt1 = debuggerLinesStartAt1;
        this.debuggerPathsAreURI = debuggerPathsAreURI;
        this.responder = responder;
        this.breakpointManager = new BreakpointManager();
        this.eventSubscriptions = new ArrayList<>();
    }

    @Override
    public DebugResult dispatch(String command, JsonObject args) {
        if (this.shutdown) {
            return new DebugResult();
        }
        Logger.logInfo("Dispatch command:" + command);
        try {
            switch (command) {
            case "initialize":
                return initialize(JsonUtils.fromJson(args, Requests.InitializeArguments.class));

            case "launch":
                return launch(JsonUtils.fromJson(args, Requests.LaunchArguments.class));

            case "attach":
                return attach(JsonUtils.fromJson(args, Requests.AttachArguments.class));

            case "disconnect":
                return disconnect();

            case "configurationDone":
                return configurationDone();

            case "next":
                return next(JsonUtils.fromJson(args, Requests.NextArguments.class));

            case "continue":
                return resume(JsonUtils.fromJson(args, Requests.ContinueArguments.class));

            case "stepIn":
                return stepIn(JsonUtils.fromJson(args, Requests.StepInArguments.class));

            case "stepOut":
                return stepOut(JsonUtils.fromJson(args, Requests.StepOutArguments.class));

            case "pause":
                return pause(JsonUtils.fromJson(args, Requests.PauseArguments.class));

            case "stackTrace":
                return stackTrace(JsonUtils.fromJson(args, Requests.StackTraceArguments.class));

            case "scopes":
                return scopes(JsonUtils.fromJson(args, Requests.ScopesArguments.class));

            case "variables":
                Requests.VariablesArguments arguments = JsonUtils.fromJson(args, Requests.VariablesArguments.class);
                if (arguments.variablesReference == -1) {
                    return new DebugResult(1009, String.format("%s: property '%s' is missing, null, or empty",
                            "variables", "variablesReference"), null);
                }
                return variables(arguments);

            case "setVariable":
                Requests.SetVariableArguments setVarArguments = JsonUtils.fromJson(args,
                        Requests.SetVariableArguments.class);
                if (setVarArguments.value == null) {
                    // Just exit out of editing if we're given an empty
                    // expression.
                    return new DebugResult();
                }
                if (setVarArguments.variablesReference == -1) {
                    return new DebugResult(1106, String.format("%s: property '%s' is missing, null, or empty",
                            "setVariable", "variablesReference"), null);
                }
                if (setVarArguments.name == null) {
                    return new DebugResult(1106,
                            String.format("%s: property '%s' is missing, null, or empty", "setVariable", "name"), null);
                }
                return setVariable(setVarArguments);

            case "source":
                Requests.SourceArguments sourceArguments = JsonUtils.fromJson(args, Requests.SourceArguments.class);
                if (sourceArguments.sourceReference == -1) {
                    return new DebugResult(1010,
                            String.format("%s: property '%s' is missing, null, or empty", "source", "sourceReference"),
                            null);
                }
                return source(sourceArguments);

            case "threads":
                return threads();

            case "setBreakpoints":
                Requests.SetBreakpointArguments setBreakpointArguments = JsonUtils.fromJson(args,
                        Requests.SetBreakpointArguments.class);
                SetBreakpointsResponseBody body = setBreakpoints(setBreakpointArguments);
                return new DebugResult(body);

            case "setExceptionBreakpoints":
                return setExceptionBreakpoints(
                        JsonUtils.fromJson(args, Requests.SetExceptionBreakpointsArguments.class));

            case "setFunctionBreakpoints":
                Requests.SetFunctionBreakpointsArguments setFuncBreakpointArguments = JsonUtils.fromJson(args,
                        Requests.SetFunctionBreakpointsArguments.class);
                if (setFuncBreakpointArguments.breakpoints != null) {
                    // FunctionBreakpoint[] breakpoints =
                    // mapper.readValue(args.getString("breakpoints"),
                    // FunctionBreakpoint[].class);
                    // return SetFunctionBreakpoints(breakpoints);
                }
                return new DebugResult(1012, String.format("%s: property '%s' is missing, null, or empty",
                        "setFunctionBreakpoints", "breakpoints"), null);

            case "evaluate":
                Requests.EvaluateArguments evaluateArguments = JsonUtils.fromJson(args,
                        Requests.EvaluateArguments.class);
                if (evaluateArguments.expression == null) {
                    return new DebugResult(1013,
                            String.format("%s: property '%s' is missing, null, or empty", "evaluate", "expression"),
                            null);
                }
                return evaluate(evaluateArguments);

            default:
                return new DebugResult(1014, "unrecognized request: {_request}",
                        JsonUtils.fromJson("{ _request:" + command + "}", JsonObject.class));
            }
        } catch (Exception e) {
            Logger.logException("DebugSession dispatch exception", e);
        }
        return null;
    }

    @Override
    public DebugResult initialize(Requests.InitializeArguments arguments) {
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
        Capabilities caps = new Capabilities();
        caps.supportsConfigurationDoneRequest = true;
        // caps.supportsFunctionBreakpoints = true;
        // caps.supportsSetVariable = true;
        // caps.supportsConditionalBreakpoints = false;
        // caps.supportsEvaluateForHovers = true;
        caps.supportsDelayedStackTraceLoading = true;
        caps.exceptionBreakpointFilters = new ArrayList<>();
        DebugResult result = new DebugResult(new Results.InitializeResponseBody(caps));
        result.add(new Events.InitializedEvent());
        return result;
    }

    @Override
    public DebugResult launch(Requests.LaunchArguments arguments) {
        this.cwd = arguments.cwd;
        String mainClass = arguments.startupClass;
        if (mainClass.endsWith(".java")) {
            mainClass = mainClass.substring(0, mainClass.length() - 5);
        }

        String classpath;
        try {
            IJavaProject project = getJavaProject(arguments.projectName, mainClass);
            classpath = AdapterUtils.computeClassPath(project);
            classpath = classpath.replaceAll("\\\\", "/");
        } catch (CoreException e) {
            Logger.logException("Failed to resolve classpath.", e);
            return new DebugResult(3001, "Cannot launch jvm.", null);
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
            return new DebugResult(3001, "Cannot launch jvm.", null);
        }
        return new DebugResult();
    }
    
    private IJavaProject getJavaProject(String projectName, String mainClass) throws CoreException {
        // if type exists in multiple projects, debug configuration need provide project name.
        if (projectName != null) {
            return AdapterUtils.getJavaProjectFromName(projectName);
        } else {
            List<IJavaProject> projects = AdapterUtils.getJavaProjectFromType(mainClass);
            if (projects.size() == 0 || projects.size() > 1) {
                throw new CoreException(new Status(IStatus.ERROR, JavaDebuggerServerPlugin.PLUGIN_ID, "project count is zero or more than one."));
            }
            return projects.get(0);
        }
    }

    @Override
    public DebugResult attach(Requests.AttachArguments arguments) {
        return null;
    }

    /**
     * VS Code terminates a debug session with the disconnect request.
     */
    @Override
    public DebugResult disconnect() {
        this.eventSubscriptions.forEach(subscription -> {
            subscription.dispose();
        });
        return new DebugResult(new Events.TerminatedEvent());
    }
    
    /**
     * VS Code sends a configurationDone request to indicate the end of configuration sequence.
     */
    public DebugResult configurationDone() {
        this.debugSession.start();
        this.eventSubscriptions.add(this.debugSession.eventHub().events().subscribe(debugEvent -> {
            handleEvent(debugEvent);
        }));
        // The configuration sequence has done, then resume VM.
        this.debugSession.resume();
        return new DebugResult();
    }

    @Override
    public DebugResult setFunctionBreakpoints(Requests.SetFunctionBreakpointsArguments arguments) {
        return null;
    }

    @Override
    public SetBreakpointsResponseBody setBreakpoints(Requests.SetBreakpointArguments arguments) {
        int size = arguments.breakpoints.length;
        List<Types.Breakpoint> res = new ArrayList<>(size);
        String[] fqns = AdapterUtils.getFullQualifiedName(arguments.source, arguments.breakpoints);
        IBreakpoint[] candidate = new IBreakpoint[size];
        for (int i = 0; i < size; i++) {
            candidate[i] = this.debugSession.createBreakpoint(fqns[i], arguments.breakpoints[i].line);
        }
        IBreakpoint[] breakpoints = this.breakpointManager.addBreakpoints(arguments.source.path, candidate, arguments.sourceModified);
        for (int i = 0; i < size; i++) {
            if (candidate[i] == breakpoints[i] && breakpoints[i].className() != null) {
                breakpoints[i].install();
                breakpoints[i].putProperty("id", this.nextBreakpointId.getAndIncrement());
            }
            res.add(new Types.Breakpoint((int) breakpoints[i].getProperty("id"), true, breakpoints[i].lineNumber(), ""));
        }
        return new SetBreakpointsResponseBody(res);
    }

    @Override
    public DebugResult setExceptionBreakpoints(Requests.SetExceptionBreakpointsArguments arguments) {
        return null;
    }

    @Override
    public DebugResult resume(Requests.ContinueArguments arguments) {
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
        return new DebugResult(new Results.ContinueResponseBody(allThreadsContinued));
    }

    @Override
    public DebugResult next(Requests.NextArguments arguments) {
        return new DebugResult();
    }

    @Override
    public DebugResult stepIn(Requests.StepInArguments arguments) {
        return new DebugResult();
    }

    @Override
    public DebugResult stepOut(Requests.StepOutArguments arguments) {
        return new DebugResult();
    }

    @Override
    public DebugResult pause(Requests.PauseArguments arguments) {
        return null;
    }

    @Override
    public DebugResult threads() {
        if (this.shutdown) {
            return new DebugResult(new Results.ThreadsResponseBody(new ArrayList<Types.Thread>()));
        }
        ArrayList<Types.Thread> threads = new ArrayList<>();
        for (ThreadReference thread : this.safeGetAllThreads()) {
            threads.add(new Types.Thread(thread.uniqueID(), "Thread [" + thread.name() + "]"));
        }
        return new DebugResult(new Results.ThreadsResponseBody(threads));
    }

    @Override
    public DebugResult stackTrace(Requests.StackTraceArguments arguments) {
        List<Types.StackFrame> result = new ArrayList<>();
        if (arguments.startFrame < 0 || arguments.levels < 0) {
            return new DebugResult(new Results.StackTraceResponseBody(result, 0));
        }
        ThreadReference thread = getThread(arguments.threadId);
        if (thread != null) {
            try {
                List<StackFrame> stackFrames = thread.frames();
                if (arguments.startFrame >= stackFrames.size()) {
                    return new DebugResult(new Results.StackTraceResponseBody(result, 0));
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
                            source, location.lineNumber(), 0);
                    result.add(newFrame);
                }
            } catch (IncompatibleThreadStateException | AbsentInformationException | URISyntaxException e) {
                Logger.logException("DebugSession#stackTrace exception", e);
            }
        }
        return new DebugResult(new Results.StackTraceResponseBody(result, result.size()));
    }

    @Override
    public DebugResult scopes(Requests.ScopesArguments arguments) {
        List<Types.Scope> scps = new ArrayList<>();
        scps.add(new Types.Scope("Local", 1000000 + arguments.frameId, false));
        return new DebugResult(new Results.ScopesResponseBody(scps));
    }

    @Override
    public DebugResult variables(Requests.VariablesArguments arguments) {
        List<Types.Variable> list = new ArrayList<>();
        return new DebugResult(new Results.VariablesResponseBody(list));
    }

    @Override
    public DebugResult setVariable(Requests.SetVariableArguments arguments) {
        return null;
    }

    @Override
    public DebugResult source(Requests.SourceArguments arguments) {
        int sourceReference = arguments.sourceReference;
        String uri = sourceCollection.get(sourceReference);
        String source = AdapterUtils.getSource(uri);
        return new DebugResult(new Results.SourceResponseBody(source));
    }

    @Override
    public DebugResult evaluate(Requests.EvaluateArguments arguments) {
        return null;
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

    protected int convertDebuggerLineToClient(int line) {
        if (this.debuggerLinesStartAt1) {
            return this.clientLinesStartAt1 ? line : line - 1;
        } else {
            return this.clientLinesStartAt1 ? line + 1 : line;
        }
    }

    protected int convertClientLineToDebugger(int line) {
        if (this.debuggerLinesStartAt1) {
            return this.clientLinesStartAt1 ? line : line + 1;
        } else {
            return this.clientLinesStartAt1 ? line - 1 : line;
        }
    }

    protected int convertDebuggerColumnToClient(int column) {
        return column;
    }

    protected String convertDebuggerPathToClient(String path) {
        if (this.debuggerPathsAreURI) {
            if (this.clientPathsAreURI) {
                return path;
            } else {
                URI uri = Paths.get(path).toUri();
                return uri.getPath();
            }
        } else {
            if (this.clientPathsAreURI) {
                return Paths.get(path).toUri().getPath();

            } else {
                return path;
            }
        }
    }

    protected String convertClientPathToDebugger(String clientPath) {
        if (clientPath == null) {
            return null;
        }

        if (this.debuggerPathsAreURI) {
            if (this.clientPathsAreURI) {
                return clientPath;
            } else {
                return Paths.get(clientPath).toUri().getPath();
            }
        } else {
            if (this.clientPathsAreURI) {
                return Paths.get(clientPath).toUri().getPath();
            } else {
                return clientPath;
            }
        }
    }

    private void handleEvent(DebugEvent debugEvent) {
        Event event = debugEvent.event;
        if (event instanceof VMStartEvent) {
            debugEvent.consumed = true;
            debugEvent.shouldResume &= false;
        } else if (event instanceof VMDeathEvent || event instanceof VMDisconnectEvent) {
            if (!this.shutdown) {
                Events.ExitedEvent exitedEvent = new Events.ExitedEvent(0);
                Events.TerminatedEvent terminatedEvent = new Events.TerminatedEvent();
                this.responder.addEvent(exitedEvent.type, exitedEvent);
                this.responder.addEvent(terminatedEvent.type, terminatedEvent);
            }
            this.shutdown = true;
            debugEvent.consumed = true;
            debugEvent.shouldResume &= false;
        } else if (event instanceof ThreadStartEvent) {
            ThreadReference startThread = ((ThreadStartEvent) event).thread();
            Events.ThreadEvent threadEvent = new Events.ThreadEvent("started", startThread.uniqueID());
            this.responder.addEvent(threadEvent.type, threadEvent);
            debugEvent.consumed = true;
            debugEvent.shouldResume &= true;
        } else if (event instanceof ThreadDeathEvent) {
            ThreadReference deathThread = ((ThreadDeathEvent) event).thread();
            Events.ThreadEvent threadDeathEvent = new Events.ThreadEvent("exited", deathThread.uniqueID());
            this.stoppedEventsByThread.remove(deathThread.uniqueID());
            this.responder.addEvent(threadDeathEvent.type, threadDeathEvent);
            for (Events.StoppedEvent stoppedEvent : this.stoppedEventsByThread.values()) {
                this.responder.addEvent(stoppedEvent.type, stoppedEvent);
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
                        adapterSource, bpLocation.lineNumber(), 0,
                        "", bpThread.uniqueID());
                this.stoppedEventsByThread.put(bpThread.uniqueID(), stopevent);
                this.responder.addEvent(stopevent.type, stopevent);
            } catch (AbsentInformationException | URISyntaxException e) {
                Logger.logException("Get breakpoint info exception", e);
            }
            debugEvent.consumed = true;
            debugEvent.shouldResume &= false;
        }
    }
}
