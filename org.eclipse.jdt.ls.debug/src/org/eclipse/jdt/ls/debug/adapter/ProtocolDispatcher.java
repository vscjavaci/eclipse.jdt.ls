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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

import org.eclipse.jdt.ls.debug.DebugException;
import org.eclipse.jdt.ls.debug.IDebugSession;
import org.eclipse.jdt.ls.debug.adapter.Events.DebugEvent;
import org.eclipse.jdt.ls.debug.adapter.Messages.Request;
import org.eclipse.jdt.ls.debug.adapter.Messages.Response;
import org.eclipse.jdt.ls.debug.adapter.Requests.Arguments;
import org.eclipse.jdt.ls.debug.adapter.Requests.Command;
import org.eclipse.jdt.ls.debug.adapter.handler.InitializeRequestHandler;

public class ProtocolDispatcher implements IDebugAdapter, IDebugAdapterContext {
    private BiConsumer<Events.DebugEvent, Boolean> eventConsumer;
    private IProviderContext providerContext;
    private Map<Command, IDebugRequestHandler> requestHandlers = null;
    private IDebugSession session;
    private boolean isAttached = false;

    private boolean debuggerLinesStartAt1 = true;
    private boolean debuggerPathsAreUri = true;
    private boolean clientLinesStartAt1 = true;
    private boolean clientPathsAreUri = false;

    private AtomicInteger messageId = new AtomicInteger(1);

    /**
     * Constructor.
     */
    public ProtocolDispatcher(BiConsumer<Events.DebugEvent, Boolean> consumer, IProviderContext context) {
        this.eventConsumer = consumer;
        this.providerContext = context;
        this.requestHandlers = new HashMap<>();
        initialize();
    }

    private void initialize() {
        // Register request handlers.
        registerHandler(new InitializeRequestHandler());
    }

    private void registerHandler(IDebugRequestHandler handler) {
        for (Command command : handler.getTargetCommands()) {
            requestHandlers.put(command, handler);
        }
    }

    @Override
    public Response dispatchRequest(Request request) {
        Messages.Response response = new Messages.Response();
        response.request_seq = request.seq;
        response.command = request.command;
        response.success = true;

        Command command = Command.parse(request.command);
        IDebugRequestHandler handler = requestHandlers.get(command);
        try {
            if (handler != null) {
                Arguments arguments = JsonUtils.fromJson(request.arguments, command.getArgumentType());
                handler.handle(command, arguments, response, this);
            } else {
                throw new DebugException(String.format("Unrecognized request: { _request: %s }", request.command));
            }
        } catch (DebugException e) { // Error handling.
            String errorMessage = e.getMessage() != null ? e.getMessage() : e.toString();
            response.body = new Responses.ErrorResponseBody(new Types.Message(this.createMessageId(), errorMessage));
            response.message = errorMessage;
            response.success = false;
        }

        return response;
    }

    @Override
    public void setDebugSession(IDebugSession session) {
        this.session = session;
    }

    @Override
    public IDebugSession getDebugSession() {
        return this.session;
    }

    @Override
    public void sendEvent(DebugEvent event) {
        this.eventConsumer.accept(event, false);
    }

    @Override
    public void sendEventAsync(DebugEvent event) {
        this.eventConsumer.accept(event, true);
    }

    @Override
    public <T extends IProvider> T getProvider(Class<T> clazz)  {
        return providerContext.getProvider(clazz);
    }

    @Override
    public boolean isDebuggerLinesStartAt1() {
        return this.debuggerLinesStartAt1;
    }

    @Override
    public void setDebuggerLinesStartAt1(boolean debuggerLinesStartAt1) {
        this.debuggerLinesStartAt1 = debuggerLinesStartAt1;
    }

    @Override
    public boolean isDebuggerPathsAreUri() {
        return this.debuggerPathsAreUri;
    }

    @Override
    public void setDebuggerPathsAreUri(boolean debuggerPathsAreUri) {
        this.debuggerPathsAreUri = debuggerPathsAreUri;
    }

    @Override
    public boolean isClientLinesStartAt1() {
        return this.clientLinesStartAt1;
    }

    @Override
    public void setClientLinesStartAt1(boolean clientLinesStartAt1) {
        this.clientLinesStartAt1 = clientLinesStartAt1;
    }

    @Override
    public boolean isClientPathsAreUri() {
        return clientPathsAreUri;
    }

    @Override
    public void setClientPathsAreUri(boolean clientPathsAreUri) {
        this.clientPathsAreUri = clientPathsAreUri;
    }

    public boolean isAttached() {
        return isAttached;
    }

    public void setAttached(boolean attached) {
        this.isAttached = attached;
    }

    public int createMessageId() {
        return this.messageId.getAndIncrement();
    }
}
