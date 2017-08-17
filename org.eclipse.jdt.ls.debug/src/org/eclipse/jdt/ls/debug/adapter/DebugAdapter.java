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

import static org.eclipse.jdt.ls.debug.adapter.formatter.TypeIdentifiers.BOOLEAN;
import static org.eclipse.jdt.ls.debug.adapter.formatter.TypeIdentifiers.BYTE;
import static org.eclipse.jdt.ls.debug.adapter.formatter.TypeIdentifiers.CHAR;
import static org.eclipse.jdt.ls.debug.adapter.formatter.TypeIdentifiers.DOUBLE;
import static org.eclipse.jdt.ls.debug.adapter.formatter.TypeIdentifiers.FLOAT;
import static org.eclipse.jdt.ls.debug.adapter.formatter.TypeIdentifiers.INT;
import static org.eclipse.jdt.ls.debug.adapter.formatter.TypeIdentifiers.LONG;
import static org.eclipse.jdt.ls.debug.adapter.formatter.TypeIdentifiers.SHORT;
import static org.eclipse.jdt.ls.debug.adapter.formatter.TypeIdentifiers.STRING_SIGNATURE;

import com.google.gson.JsonObject;
import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.ArrayReference;
import com.sun.jdi.ArrayType;
import com.sun.jdi.ClassLoaderReference;
import com.sun.jdi.ClassNotLoadedException;
import com.sun.jdi.ClassType;
import com.sun.jdi.Field;
import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.InvalidTypeException;
import com.sun.jdi.LocalVariable;
import com.sun.jdi.Location;
import com.sun.jdi.Method;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.StackFrame;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.Type;
import com.sun.jdi.TypeComponent;
import com.sun.jdi.VMDisconnectedException;
import com.sun.jdi.Value;
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
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.ls.debug.DebugEvent;
import org.eclipse.jdt.ls.debug.DebugException;
import org.eclipse.jdt.ls.debug.DebugUtility;
import org.eclipse.jdt.ls.debug.IBreakpoint;
import org.eclipse.jdt.ls.debug.IDebugSession;
import org.eclipse.jdt.ls.debug.adapter.Requests.StackTraceArguments;
import org.eclipse.jdt.ls.debug.adapter.formatter.IValueFormatter;
import org.eclipse.jdt.ls.debug.adapter.formatter.NumericFormatEnum;
import org.eclipse.jdt.ls.debug.adapter.formatter.NumericFormatter;
import org.eclipse.jdt.ls.debug.adapter.formatter.SimpleTypeFormatter;
import org.eclipse.jdt.ls.debug.adapter.headless.ClassPathEntry;
import org.eclipse.jdt.ls.debug.adapter.headless.utility.CompileUtils;
import org.eclipse.jdt.ls.debug.adapter.variables.IVariableFormatter;
import org.eclipse.jdt.ls.debug.adapter.variables.JdiObjectProxy;
import org.eclipse.jdt.ls.debug.adapter.variables.SetValueFunction;
import org.eclipse.jdt.ls.debug.adapter.variables.StackFrameScope;
import org.eclipse.jdt.ls.debug.adapter.variables.ThreadObjectReference;
import org.eclipse.jdt.ls.debug.adapter.variables.Variable;
import org.eclipse.jdt.ls.debug.adapter.variables.VariableFormatterFactory;
import org.eclipse.jdt.ls.debug.adapter.variables.VariableUtils;
import org.eclipse.jdt.ls.debug.internal.Logger;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

public class DebugAdapter implements IDebugAdapter {
    private BiConsumer<Events.DebugEvent, Boolean> eventConsumer;

    private boolean debuggerLinesStartAt1 = true;
    private boolean debuggerPathsAreUri = true;
    private boolean clientLinesStartAt1 = true;
    private boolean clientPathsAreUri = false;

    private boolean isAttached = false;

    private String cwd;
    private String[] sourcePath;
    private IDebugSession debugSession;
    private BreakpointManager breakpointManager;
    private List<Disposable> eventSubscriptions;
    private IProviderContext context;
    private VariableRequestHandler variableRequestHandler;
    private IdCollection<String> sourceCollection = new IdCollection<>();
    private AtomicInteger messageId = new AtomicInteger(1);

    /**
     * Constructor.
     */
    public DebugAdapter(BiConsumer<Events.DebugEvent, Boolean> consumer, IProviderContext context) {
        this.eventConsumer = consumer;
        this.breakpointManager = new BreakpointManager();
        this.eventSubscriptions = new ArrayList<>();
        this.context = context;
        this.variableRequestHandler = new VariableRequestHandler(VariableFormatterFactory.createVariableFormatter(),
                true, false, true);
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
            // When there are uncaught exception during dispatching, send an error response back and terminate debuggee.
            responseBody = new Responses.ErrorResponseBody(
                    this.convertDebuggerMessageToClient(e.getMessage() != null ? e.getMessage() : e.toString()));
            this.sendEventLater(new Events.TerminatedEvent());
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
                    this.clientPathsAreUri = true;
                    break;
                default:
                    this.clientPathsAreUri = false;
            }
        }
        // Send an InitializedEvent
        this.sendEventLater(new Events.InitializedEvent());

        Types.Capabilities caps = new Types.Capabilities();
        caps.supportsConfigurationDoneRequest = true;
        caps.supportsHitConditionalBreakpoints = true;
        caps.supportTerminateDebuggee = true;
        return new Responses.InitializeResponseBody(caps);
    }

    private Responses.ResponseBody launch(Requests.LaunchArguments arguments) {
        try {
            this.isAttached = false;
            this.launchDebugSession(arguments);
        } catch (DebugException e) {
            // When launching failed, send a TerminatedEvent to tell DA the debugger would exit.
            this.sendEventLater(new Events.TerminatedEvent());
            return new Responses.ErrorResponseBody(
                    this.convertDebuggerMessageToClient("Cannot launch debuggee vm: " + e.getMessage()));
        }
        return new Responses.ResponseBody();
    }

    private Responses.ResponseBody attach(Requests.AttachArguments arguments) {
        try {
            this.isAttached = true;
            this.attachDebugSession(arguments);
        } catch (DebugException e) {
            // When attaching failed, send a TerminatedEvent to tell DA the debugger would exit.
            this.sendEventLater(new Events.TerminatedEvent());
            return new Responses.ErrorResponseBody(
                    this.convertDebuggerMessageToClient(e.getMessage()));
        }
        return new Responses.ResponseBody();
    }

    /**
     * VS Code terminates a debug session with the disconnect request.
     */
    private Responses.ResponseBody disconnect(Requests.DisconnectArguments arguments) {
        this.shutdownDebugSession(arguments.terminateDebuggee && !this.isAttached);
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
        String clientPath = arguments.source.path;
        if (AdapterUtils.isWindows()) {
            // VSCode may send drive letters with inconsistent casing which will mess up the key
            // in the BreakpointManager. See https://github.com/Microsoft/vscode/issues/6268
            // Normalize the drive letter casing. Note that drive letters
            // are not localized so invariant is safe here.
            String drivePrefix = FilenameUtils.getPrefix(clientPath);
            if (drivePrefix != null && drivePrefix.length() >= 2
                    && Character.isLowerCase(drivePrefix.charAt(0)) && drivePrefix.charAt(1) == ':') {
                drivePrefix = drivePrefix.substring(0, 2); // d:\ is an illegal regex string, convert it to d:
                clientPath = clientPath.replaceFirst(drivePrefix, drivePrefix.toUpperCase());
            }
        }
        String sourcePath = clientPath;
        if (arguments.source.sourceReference != 0 && this.sourceCollection.get(arguments.source.sourceReference) != null) {
            sourcePath = this.sourceCollection.get(arguments.source.sourceReference);
        } else {
            sourcePath = this.convertClientPathToDebugger(clientPath);
        }

        // When breakpoint source path is null or an invalid file path, send an ErrorResponse back.
        if (sourcePath == null) {
            return new Responses.ErrorResponseBody(this.convertDebuggerMessageToClient(
                    String.format("Failed to setBreakpoint. Reason: '%s' is an invalid path.", arguments.source.path)));
        }
        try {
            List<Types.Breakpoint> res = new ArrayList<>();
            IBreakpoint[] toAdds = this.convertClientBreakpointsToDebugger(sourcePath, arguments.breakpoints);
            IBreakpoint[] added = this.breakpointManager.setBreakpoints(sourcePath, toAdds, arguments.sourceModified);
            for (int i = 0; i < arguments.breakpoints.length; i++) {
                // For newly added breakpoint, should install it to debuggee first.
                if (toAdds[i] == added[i] && added[i].className() != null) {
                    added[i].install().thenAccept(bp -> {
                        Events.BreakpointEvent bpEvent = new Events.BreakpointEvent("new", this.convertDebuggerBreakpointToClient(bp));
                        sendEventLater(bpEvent);
                    });
                } else if (toAdds[i].hitCount() != added[i].hitCount() && added[i].className() != null) {
                    // Update hitCount condition.
                    added[i].setHitCount(toAdds[i].hitCount());
                }
                res.add(this.convertDebuggerBreakpointToClient(added[i]));
            }
            return new Responses.SetBreakpointsResponseBody(res);
        } catch (DebugException e) {
            return new Responses.ErrorResponseBody(this.convertDebuggerMessageToClient(
                    String.format("Failed to setBreakpoint. Reason: '%s'", e.getMessage())));
        }
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
            checkThreadRunningAndRecycleIds(thread);
        } else {
            this.debugSession.resume();
            this.variableRequestHandler.recyclableAllObject();
        }
        return new Responses.ContinueResponseBody(allThreadsContinued);
    }

    private Responses.ResponseBody next(Requests.NextArguments arguments) {
        ThreadReference thread = getThread(arguments.threadId);
        if (thread != null) {
            DebugUtility.stepOver(thread, this.debugSession.eventHub());
            checkThreadRunningAndRecycleIds(thread);
        }
        return new Responses.ResponseBody();
    }

    private Responses.ResponseBody stepIn(Requests.StepInArguments arguments) {
        ThreadReference thread = getThread(arguments.threadId);
        if (thread != null) {
            DebugUtility.stepInto(thread, this.debugSession.eventHub());
            checkThreadRunningAndRecycleIds(thread);
        }
        return new Responses.ResponseBody();
    }

    private Responses.ResponseBody stepOut(Requests.StepOutArguments arguments) {
        ThreadReference thread = getThread(arguments.threadId);
        if (thread != null) {
            DebugUtility.stepOut(thread, this.debugSession.eventHub());
            checkThreadRunningAndRecycleIds(thread);
        }
        return new Responses.ResponseBody();
    }

    private Responses.ResponseBody pause(Requests.PauseArguments arguments) {
        ThreadReference thread = getThread(arguments.threadId);
        if (thread != null) {
            thread.suspend();
            this.sendEventLater(new Events.StoppedEvent("pause", arguments.threadId));
        } else {
            this.debugSession.suspend();
            this.sendEventLater(new Events.StoppedEvent("pause", arguments.threadId, true));
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
        try {
            return this.variableRequestHandler.stackTrace(arguments);
        } catch (IncompatibleThreadStateException | AbsentInformationException | URISyntaxException e) {
            return new Responses.ErrorResponseBody(this.convertDebuggerMessageToClient(
                    String.format("Failed to get stackTrace. Reason: '%s'", e.getMessage())));
        }
    }

    private Responses.ResponseBody scopes(Requests.ScopesArguments arguments) {
        return this.variableRequestHandler.scopes(arguments);
    }

    private Responses.ResponseBody variables(Requests.VariablesArguments arguments) {
        try {
            return this.variableRequestHandler.variables(arguments);
        } catch (AbsentInformationException e) {
            return new Responses.ErrorResponseBody(this.convertDebuggerMessageToClient(
                    String.format("Failed to get variables. Reason: '%s'", e.getMessage())));
        }
    }

    private Responses.ResponseBody setVariable(Requests.SetVariableArguments arguments) {
        return this.variableRequestHandler.setVariable(arguments);
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
            this.sendEventLater(new Events.ExitedEvent(0));
        } else if (event instanceof VMDisconnectEvent) {
            this.sendEventLater(new Events.TerminatedEvent());
            // Terminate eventHub thread.
            try {
                this.debugSession.eventHub().close();
            } catch (Exception e) {
                // do nothing.
            }
        } else if (event instanceof ThreadStartEvent) {
            ThreadReference startThread = ((ThreadStartEvent) event).thread();
            Events.ThreadEvent threadEvent = new Events.ThreadEvent("started", startThread.uniqueID());
            this.sendEventLater(threadEvent);
        } else if (event instanceof ThreadDeathEvent) {
            ThreadReference deathThread = ((ThreadDeathEvent) event).thread();
            Events.ThreadEvent threadDeathEvent = new Events.ThreadEvent("exited", deathThread.uniqueID());
            this.sendEventLater(threadDeathEvent);
        } else if (event instanceof BreakpointEvent) {
            ThreadReference bpThread = ((BreakpointEvent) event).thread();
            this.sendEventLater(new Events.StoppedEvent("breakpoint", bpThread.uniqueID()));
            debugEvent.shouldResume = false;
        } else if (event instanceof StepEvent) {
            ThreadReference stepThread = ((StepEvent) event).thread();
            this.sendEventLater(new Events.StoppedEvent("step", stepThread.uniqueID()));
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

    /**
     * Send event to DA immediately.
     * @see ProtocolServer#sendEvent(String,Object)
     */
    private void sendEvent(Events.DebugEvent event) {
        this.eventConsumer.accept(event, false);
    }

    /**
     * Send event to DA after the current dispatching request is resolved.
     * @see ProtocolServer#sendEventLater(String,Object)
     */
    private void sendEventLater(Events.DebugEvent event) {
        this.eventConsumer.accept(event, true);
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
            ProcessConsole debuggeeConsole = new ProcessConsole(this.debugSession.process(), "Debuggee");
            debuggeeConsole.onStdout((output) -> {
                // When DA receives a new OutputEvent, it just shows that on Debug Console and doesn't affect the DA's dispatching workflow.
                // That means the debugger can send OutputEvent to DA at any time.
                sendEvent(Events.OutputEvent.createStdoutOutput(output));
            });
            debuggeeConsole.onStderr((err) -> {
                sendEvent(Events.OutputEvent.createStderrOutput(err));
            });
            debuggeeConsole.start();
        } catch (IOException | IllegalConnectorArgumentsException | VMStartException e) {
            Logger.logException("Launching debuggee vm exception", e);
            throw new DebugException("Launching debuggee vm exception \"" + e.getMessage() + "\"", e);
        }
    }

    private void attachDebugSession(Requests.AttachArguments arguments) throws DebugException {
        this.cwd = arguments.cwd;
        if (arguments.sourcePath == null || arguments.sourcePath.length == 0) {
            this.sourcePath = new String[] { cwd };
        } else {
            this.sourcePath = new String[arguments.sourcePath.length];
            System.arraycopy(arguments.sourcePath, 0, this.sourcePath, 0, arguments.sourcePath.length);
        }

        try {
            this.debugSession = DebugUtility.attach(context.getVirtualMachineManagerProvider().getVirtualMachineManager(),
                    arguments.hostName, arguments.port, arguments.attachTimeout);
        } catch (IOException | IllegalConnectorArgumentsException e) {
            Logger.logException("Failed to attach to remote debuggee vm. Reason: " + e.getMessage(), e);
            throw new DebugException("Failed to attach to remote debuggee vm. Reason: " + e.getMessage(), e);
        }
    }

    private void shutdownDebugSession(boolean terminateDebuggee) {
        this.eventSubscriptions.clear();
        this.breakpointManager.reset();
        this.variableRequestHandler.recyclableAllObject();
        this.sourceCollection.reset();
        if (this.debugSession != null) {
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

    private String convertClientPathToDebugger(String clientPath) {
        if (clientPath == null) {
            return null;
        }

        if (this.debuggerPathsAreUri) {
            if (this.clientPathsAreUri) {
                return clientPath;
            } else {
                try {
                    return Paths.get(clientPath).toUri().toString();
                } catch (InvalidPathException e) {
                    return null;
                }
            }
        } else {
            if (this.clientPathsAreUri) {
                try {
                    return Paths.get(new URI(clientPath)).toString();
                } catch (URISyntaxException | IllegalArgumentException
                        | FileSystemNotFoundException | SecurityException e) {
                    return null;
                }
            } else {
                return clientPath;
            }
        }
    }

    private String convertDebuggerPathToClient(String debuggerPath) {
        if (debuggerPath == null) {
            return null;
        }

        if (this.debuggerPathsAreUri) {
            if (this.clientPathsAreUri) {
                return debuggerPath;
            } else {
                try {
                    return Paths.get(new URI(debuggerPath)).toString();
                } catch (URISyntaxException | IllegalArgumentException
                        | FileSystemNotFoundException | SecurityException e) {
                    return null;
                }
            }
        } else {
            if (this.clientPathsAreUri) {
                try {
                    return Paths.get(debuggerPath).toUri().toString();
                } catch (InvalidPathException e) {
                    return null;
                }
            } else {
                return debuggerPath;
            }
        }
    }

    private Types.Breakpoint convertDebuggerBreakpointToClient(IBreakpoint breakpoint) {
        int id = (int) breakpoint.getProperty("id");
        boolean verified = breakpoint.getProperty("verified") != null ? (boolean) breakpoint.getProperty("verified") : false;
        int lineNumber = this.convertDebuggerLineToClient(breakpoint.lineNumber());
        return new Types.Breakpoint(id, verified, lineNumber, "");
    }

    private IBreakpoint[] convertClientBreakpointsToDebugger(String sourceFile, Types.SourceBreakpoint[] sourceBreakpoints) throws DebugException {
        int[] lines = Arrays.asList(sourceBreakpoints).stream().map(sourceBreakpoint -> {
            return sourceBreakpoint.line;
        }).mapToInt(line -> line).toArray();
        int[] debuggerLines = this.convertClientLineToDebugger(lines);
        String[] fqns = context.getSourceLookUpProvider().getFullyQualifiedName(sourceFile, debuggerLines, null);
        IBreakpoint[] breakpoints = new IBreakpoint[lines.length];
        for (int i = 0; i < lines.length; i++) {
            int hitCount = 0;
            try {
                hitCount = Integer.parseInt(sourceBreakpoints[i].hitCondition);
            } catch (NumberFormatException e) {
                hitCount = 0; // If hitCount is an illegal number, ignore hitCount condition.
            }
            breakpoints[i] = this.debugSession.createBreakpoint(fqns[i], debuggerLines[i], hitCount);
        }
        return breakpoints;
    }

    private Types.Source convertDebuggerSourceToClient(Location location) throws URISyntaxException {
        String fullyQualifiedName = location.declaringType().name();
        String sourceName = "";
        String relativeSourcePath = "";
        try {
            // When the .class file doesn't contain source information in meta data,
            // invoking Location#sourceName() would throw AbsentInformationException.
            sourceName = location.sourceName();
            relativeSourcePath = location.sourcePath();
        } catch (AbsentInformationException e) {
            String enclosingType = AdapterUtils.parseEnclosingType(fullyQualifiedName);
            sourceName = enclosingType.substring(enclosingType.lastIndexOf('.') + 1) + ".java";
            relativeSourcePath = enclosingType.replace('.', '/') + ".java";
        }
        String uri = context.getSourceLookUpProvider().getSourceFileURI(fullyQualifiedName, relativeSourcePath);
        // If the source lookup engine cannot find the source file, then lookup it in the source directories specified by user.
        if (uri == null) {
            String absoluteSourcepath = AdapterUtils.sourceLookup(this.sourcePath, relativeSourcePath);
            if (absoluteSourcepath == null) {
                absoluteSourcepath = Paths.get(this.cwd, relativeSourcePath).toString();
            }
            uri = Paths.get(absoluteSourcepath).toUri().toString();
        }
        String clientPath = this.convertDebuggerPathToClient(uri);
        if (uri.startsWith("file:")) {
            return new Types.Source(sourceName, clientPath, 0);
        } else {
            return new Types.Source(sourceName, clientPath, this.sourceCollection.create(uri));
        }
    }

    private String convertDebuggerSourceToClient(String uri) {
        return context.getSourceLookUpProvider().getSourceContents(uri);
    }

    private Types.Thread convertDebuggerThreadToClient(ThreadReference thread) {
        return new Types.Thread(thread.uniqueID(), "Thread [" + thread.name() + "]");
    }

    private Types.StackFrame convertDebuggerStackFrameToClient(StackFrame stackFrame, int frameId)
            throws URISyntaxException, AbsentInformationException {
        Location location = stackFrame.location();
        Method method = location.method();
        Types.Source clientSource = this.convertDebuggerSourceToClient(location);
        return new Types.StackFrame(frameId, method.name(), clientSource,
                this.convertDebuggerLineToClient(location.lineNumber()), 0);
    }

    private Types.Message convertDebuggerMessageToClient(String message) {
        return new Types.Message(this.messageId.getAndIncrement(), message);
    }

    private void checkThreadRunningAndRecycleIds(ThreadReference thread) {
        if (allThreadRunning()) {
            this.variableRequestHandler.recyclableAllObject();
        } else {
            this.variableRequestHandler.recyclableThreads(thread);
        }
    }
    
    private boolean allThreadRunning() {
        return !safeGetAllThreads().stream().anyMatch(ThreadReference::isSuspended);
    }

    private class VariableRequestHandler {
        private IVariableFormatter variableFormatter;
        private RecyclableObjectPool<Long, Object> objectPool;
        
        public VariableRequestHandler(IVariableFormatter variableFormatter, boolean showStaticVariables,
                               boolean hexFormat, boolean showQualified) {
            this.objectPool = new RecyclableObjectPool<>();
            this.variableFormatter = variableFormatter;
        }
        
        public void recyclableAllObject() {
            this.objectPool.removeAllObjects();
        }

        public void recyclableThreads(ThreadReference thread) {
            this.objectPool.removeObjectsByOwner(thread.uniqueID());
        }

        Responses.ResponseBody stackTrace(StackTraceArguments arguments)
                throws IncompatibleThreadStateException, AbsentInformationException, URISyntaxException {
            List<Types.StackFrame> result = new ArrayList<>();
            if (arguments.startFrame < 0 || arguments.levels < 0) {
                return new Responses.StackTraceResponseBody(result, 0);
            }
            ThreadReference thread = getThread(arguments.threadId);
            int totalFrames = 0;
            if (thread != null) {
                totalFrames = thread.frameCount();
                if (totalFrames <= arguments.startFrame) {
                    return new Responses.StackTraceResponseBody(result, totalFrames);
                }
                try {
                    List<StackFrame> stackFrames = arguments.levels == 0
                            ? thread.frames(arguments.startFrame, totalFrames - arguments.startFrame)
                            : thread.frames(arguments.startFrame,
                            Math.min(totalFrames - arguments.startFrame, arguments.levels));
                    for (int i = 0; i < arguments.levels; i++) {
                        StackFrame stackFrame = stackFrames.get(arguments.startFrame + i);
                        int frameId = this.objectPool.addObject(stackFrame.thread().uniqueID(),
                                new JdiObjectProxy<>(stackFrame));
                        Types.StackFrame clientStackFrame = convertDebuggerStackFrameToClient(stackFrame, frameId);
                        result.add(clientStackFrame);
                    }
                } catch (IndexOutOfBoundsException ex) {
                    // ignore if stack frames overflow
                    return new Responses.StackTraceResponseBody(result, totalFrames);
                }
            }
            return new Responses.StackTraceResponseBody(result, totalFrames);
        }

        Responses.ResponseBody scopes(Requests.ScopesArguments arguments) {
            List<Types.Scope> scopes = new ArrayList<>();
            JdiObjectProxy<StackFrame> stackFrameProxy = (JdiObjectProxy<StackFrame>)this.objectPool.getObjectById(arguments.frameId);
            if (stackFrameProxy == null) {
                return new Responses.ScopesResponseBody(scopes);
            }
            StackFrameScope localScope = new StackFrameScope(stackFrameProxy.getProxiedObject(), "Local");
            scopes.add(new Types.Scope(
                    localScope.getScope(), this.objectPool.addObject(stackFrameProxy.getProxiedObject()
                    .thread().uniqueID(), localScope), false));

            return new Responses.ScopesResponseBody(scopes);
        }


        Responses.ResponseBody variables(Requests.VariablesArguments arguments) throws AbsentInformationException {
            Map<String, Object> options = new HashMap<>();
            // TODO: when vscode protocol support customize settings of value format, showQualified should be one of the options.
            boolean showStaticVariables = true;
            boolean showQualified = true;
            if (arguments.format != null && arguments.format.hex) {
                options.put(NumericFormatter.NUMERIC_FORMAT_OPTION, NumericFormatEnum.HEX);
            }
            if (showQualified) {
                options.put(SimpleTypeFormatter.QUALIFIED_CLASS_NAME_OPTION, showQualified);
            }
            
            List<Types.Variable> list = new ArrayList<>();
            List<Variable> variables;
            Object obj = this.objectPool.getObjectById(arguments.variablesReference);
            ThreadReference thread;
            if (obj instanceof StackFrameScope) {
                StackFrame frame = ((StackFrameScope) obj).getStackFrame();
                thread = frame.thread();
                variables = VariableUtils.listLocalVariables(frame);
                Variable thisVariable = VariableUtils.getThisVariable(frame);
                if (thisVariable != null) {
                    variables.add(thisVariable);
                }
                if (showStaticVariables && frame.location().method().isStatic()) {
                    variables.addAll(VariableUtils.listStaticVariables(frame));
                }
            } else if (obj instanceof ThreadObjectReference) {
                ObjectReference currentObj = ((ThreadObjectReference) obj).getObject();
                thread = ((ThreadObjectReference) obj).getThread();

                if (arguments.count > 0) {
                    variables = VariableUtils.listFieldVariables(currentObj, arguments.start, arguments.count);
                } else {
                    variables = VariableUtils.listFieldVariables(currentObj, showStaticVariables);
                }

            } else {
                throw new IllegalArgumentException(String
                        .format("VariablesRequest: Invalid variablesReference %d.", arguments.variablesReference));
            }
            // find variable name duplicates
            Set<String> duplicateNames = getDuplicateNames(variables.stream().map(var -> var.name)
                    .collect(Collectors.toList()));
            Map<Variable, String> variableNameMap = new HashMap<>();
            if (!duplicateNames.isEmpty()) {
                Map<String, List<Variable>> duplicateVars =
                        variables.stream()
                                .filter(var -> duplicateNames.contains(var.name))
                                .collect(Collectors.groupingBy(var -> var.name, Collectors.toList()));

                duplicateVars.forEach((k, duplicateVariables) -> {
                    Set<String> declarationTypeNames = new HashSet<>();
                    boolean declarationTypeNameConflict = false;
                    // try use type formatter to resolve name conflict
                    for (Variable javaVariable : duplicateVariables) {
                        Type declarationType = javaVariable.getDeclaringType();
                        if (declarationType != null) {
                            String declarationTypeName = this.variableFormatter.typeToString(declarationType, options);
                            String compositeName = String.format("%s (%s)", javaVariable.name, declarationTypeName);
                            if (!declarationTypeNames.add(compositeName)) {
                                declarationTypeNameConflict = true;
                                break;
                            }
                            variableNameMap.put(javaVariable, compositeName);
                        }
                    }
                    // if there are duplicate names on declaration types, use fully qualified name
                    if (declarationTypeNameConflict) {
                        for (Variable javaVariable : duplicateVariables) {
                            Type declarationType = javaVariable.getDeclaringType();
                            if (declarationType != null) {
                                variableNameMap.put(javaVariable, String.format("%s (%s)", javaVariable.name, declarationType.name()));
                            }
                        }
                    }
                });
            }
            for (Variable javaVariable : variables) {
                Value value = javaVariable.value;
                String name = javaVariable.name;
                if (variableNameMap.containsKey(javaVariable)) {
                    name = variableNameMap.get(javaVariable);
                }
                int referenceId = 0;
                if (value instanceof ObjectReference && VariableUtils.hasChildren(value, showStaticVariables)) {
                    ThreadObjectReference threadObjectReference = new ThreadObjectReference(thread, (ObjectReference) value);
                    referenceId = this.objectPool.addObject(thread.uniqueID(), threadObjectReference);
                }
                Types.Variable typedVariables = new Types.Variable(name, variableFormatter.valueToString(value, options),
                        variableFormatter.typeToString(value == null ? null : value.type(), options), referenceId, null);
                if (javaVariable.value instanceof ArrayReference) {
                    typedVariables.indexedVariables = ((ArrayReference) javaVariable.value).length();
                }
                list.add(typedVariables);
            }
            return new Responses.VariablesResponseBody(list);
        }

        Responses.ResponseBody setVariable(Requests.SetVariableArguments arguments) {
            Map<String, Object> options = new HashMap<>();
            // TODO: when vscode protocol support customize settings of value format, showQualified should be one of the options.
            boolean showStaticVariables = true;
            boolean showQualified = true;
            if (arguments.format != null && arguments.format.hex) {
                options.put(NumericFormatter.NUMERIC_FORMAT_OPTION, NumericFormatEnum.HEX);
            }
            if (showQualified) {
                options.put(SimpleTypeFormatter.QUALIFIED_CLASS_NAME_OPTION, showQualified);
            }

            Object obj = this.objectPool.getObjectById(arguments.variablesReference);
            ThreadReference thread;
            String name = arguments.name;
            Value newValue = null;
            String belongToClass = null;
            if (arguments.name.contains("(")) {
                name = arguments.name.substring(0, arguments.name.indexOf('(')).trim();
                belongToClass = arguments.name.substring(arguments.name.indexOf('(') + 1, arguments.name.indexOf(')'))
                        .trim();
            }
            try {
                if (obj instanceof StackFrameScope) {
                    if (arguments.name.equals("this")) {
                        throw new UnsupportedOperationException("SetVariableRequest: 'This' variable cannot be changed.");
                    }
                    StackFrame frame = ((StackFrameScope) obj).getStackFrame();
                    thread = frame.thread();
                    LocalVariable variable = frame.visibleVariableByName(name);
                    if (StringUtils.isBlank(belongToClass) && variable != null) {
                        newValue = this.setFrameValue(frame, variable, arguments.value, options);
                    } else {
                        if (showStaticVariables && frame.location().method().isStatic()) {
                            ReferenceType type = frame.location().declaringType();
                            if (StringUtils.isBlank(belongToClass)) {
                                Field field = type.fieldByName(name);
                                newValue = setStaticFieldValue(type, field, arguments.name, arguments.value, options);
                            } else {
                                if (frame.location().method().isStatic() && showStaticVariables) {
                                    newValue = setFieldValueWithConflict(null, type.allFields(), name, belongToClass,
                                            arguments.value, options);
                                }
                            }

                        } else {
                            throw new UnsupportedOperationException(
                                    String.format("SetVariableRequest: Variable %s cannot be found.", arguments.name));
                        }
                    }
                } else if (obj instanceof ThreadObjectReference) {
                    ObjectReference currentObj = ((ThreadObjectReference) obj).getObject();
                    thread = ((ThreadObjectReference) obj).getThread();
                    if (currentObj instanceof ArrayReference) {
                        ArrayReference array = (ArrayReference) currentObj;
                        Type eleType = ((ArrayType) array.referenceType()).componentType();
                        newValue = setArrayValue(array, eleType, Integer.parseInt(arguments.name), arguments.value, options);
                    } else {
                        if (StringUtils.isBlank(belongToClass)) {
                            Field field = currentObj.referenceType().fieldByName(name);
                            if (field != null) {
                                if (field.isStatic()) {
                                    newValue = this.setStaticFieldValue(currentObj.referenceType(), field,
                                            arguments.name, arguments.value, options);
                                } else {
                                    newValue = this.setObjectFieldValue(currentObj, field, arguments.name,
                                            arguments.value, options);
                                }
                            } else {
                                throw new IllegalArgumentException(
                                        String.format("SetVariableRequest: Variable %s cannot be found.", arguments.name));
                            }
                        } else {
                            newValue = setFieldValueWithConflict(currentObj, currentObj.referenceType().allFields(),
                                    name, belongToClass, arguments.value, options);
                        }
                    }
                } else {
                    throw new IllegalArgumentException(
                            String.format("SetVariableRequest: Variable %s cannot be found.", arguments.name));
                }
            } catch (IllegalArgumentException | AbsentInformationException | InvalidTypeException
                    | UnsupportedOperationException | ClassNotLoadedException e) {
                return new Responses.ErrorResponseBody(convertDebuggerMessageToClient(e.getMessage()));
            }
            int referenceId = getReferenceId(thread, newValue, showStaticVariables);

            int indexedVariables = 0;
            if (newValue instanceof ArrayReference) {
                indexedVariables = ((ArrayReference) newValue).length();
            }
            return new Responses.SetVariablesResponseBody(
                    this.variableFormatter.typeToString(newValue == null ? null : newValue.type(), options), // type
                    this.variableFormatter.valueToString(newValue, options), // value,
                    referenceId, indexedVariables);

        }

        private Value setValueProxy(Type type, String value, SetValueFunction setValueFunc, Map<String, Object> options)
                throws ClassNotLoadedException, InvalidTypeException {
            IValueFormatter formatter = getFormatterForModification(type, options);
            Value newValue = formatter.valueOf(value, type, options);
            setValueFunc.apply(newValue);
            return newValue;
        }

        private IValueFormatter getFormatterForModification(Type type, Map<String, Object> options) {
            char signature0 = type.signature().charAt(0);

            if (signature0 == LONG || signature0 == INT || signature0 == SHORT || signature0 == BYTE || signature0 == FLOAT
                    || signature0 == DOUBLE || signature0 == BOOLEAN || signature0 == CHAR
                    || type.signature().equals(STRING_SIGNATURE)) {
                return this.variableFormatter.getValueFormatter(type, options);
            }
            throw new UnsupportedOperationException(String.format("Set value for type %s is not supported.", type.name()));
        }

        private Value setStaticFieldValue(Type declaringType, Field field, String name, String value, Map<String, Object> options)
                throws ClassNotLoadedException, InvalidTypeException {
            if (field.isFinal()) {
                throw new UnsupportedOperationException(
                        String.format("SetVariableRequest: Final field %s cannot be changed.", name));
            }
            if (!(declaringType instanceof ClassType)) {
                throw new UnsupportedOperationException(
                        String.format("SetVariableRequest: Field %s in interface cannot be changed.", name));
            }
            return setValueProxy(field.type(), value, newValue -> ((ClassType) declaringType).setValue(field, newValue), options);
        }

        private Value setFrameValue(StackFrame frame, LocalVariable localVariable, String value, Map<String, Object> options)
                throws ClassNotLoadedException, InvalidTypeException {
            return setValueProxy(localVariable.type(), value, newValue -> frame.setValue(localVariable, newValue), options);
        }

        private Value setObjectFieldValue(ObjectReference obj, Field field, String name, String value, Map<String, Object> options)
                throws ClassNotLoadedException, InvalidTypeException {
            if (field.isFinal()) {
                throw new UnsupportedOperationException(
                        String.format("SetVariableRequest: Final field %s cannot be changed.", name));
            }
            return setValueProxy(field.type(), value, newValue -> obj.setValue(field, newValue), options);
        }

        private Value setArrayValue(ArrayReference array, Type eleType, int index, String value, Map<String, Object> options)
                throws ClassNotLoadedException, InvalidTypeException {
            return setValueProxy(eleType, value, newValue -> array.setValue(index, newValue), options);
        }

        private Value setFieldValueWithConflict(ObjectReference obj, List<Field> fields, String name, String belongToClass,
                                                String value, Map<String, Object> options) throws ClassNotLoadedException, InvalidTypeException {
            Field field;
            // first try to resolve filed by fully qualified name
            List<Field> narrowedFields = fields.stream().filter(TypeComponent::isStatic)
                    .filter(t -> t.name().equals(name) && t.declaringType().name().equals(belongToClass))
                    .collect(Collectors.toList());
            if (narrowedFields.isEmpty()) {
                // second try to resolve filed by formatted name
                narrowedFields = fields.stream().filter(TypeComponent::isStatic)
                        .filter(t -> t.name().equals(name)
                                && this.variableFormatter.typeToString(t.declaringType(), options).equals(belongToClass))
                        .collect(Collectors.toList());
            }
            if (narrowedFields.size() == 1) {
                field = narrowedFields.get(0);
            } else {
                throw new UnsupportedOperationException(String.format("SetVariableRequest: Name conflicted for %s.", name));
            }
            return field.isStatic() ? setStaticFieldValue(field.declaringType(), field, name, value, options)
                    : this.setObjectFieldValue(obj, field, name, value, options);

        }

        private int getReferenceId(ThreadReference thread, Value value, boolean includeStatic) {
            if (value instanceof ObjectReference && VariableUtils.hasChildren(value, includeStatic)) {
                ThreadObjectReference threadObjectReference = new ThreadObjectReference(thread, (ObjectReference)value);
                return this.objectPool.addObject(thread.uniqueID(), threadObjectReference);
            }
            return 0;
        }


        private Set<String> getDuplicateNames(Collection<String> list) {
            Set<String> result = new HashSet<>();
            Set<String> set = new HashSet<>();

            for (String item : list) {
                if (!set.contains(item)) {
                    set.add(item);
                } else {
                    result.add(item);
                }
            }
            return result;
        }
    }
}