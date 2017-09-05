package org.eclipse.jdt.ls.debug.adapter.handler;

import java.util.Arrays;
import java.util.List;

import org.eclipse.jdt.ls.debug.DebugUtility;
import org.eclipse.jdt.ls.debug.adapter.AdapterUtils;
import org.eclipse.jdt.ls.debug.adapter.ErrorCode;
import org.eclipse.jdt.ls.debug.adapter.IDebugAdapterContext;
import org.eclipse.jdt.ls.debug.adapter.IDebugRequestHandler;
import org.eclipse.jdt.ls.debug.adapter.Messages.Response;
import org.eclipse.jdt.ls.debug.adapter.Requests.Arguments;
import org.eclipse.jdt.ls.debug.adapter.Requests.Command;
import org.eclipse.jdt.ls.debug.adapter.Requests.ContinueArguments;
import org.eclipse.jdt.ls.debug.adapter.Responses;
import org.eclipse.jdt.ls.debug.adapter.resource.disposer.IRequestHandlerResourceDisposer;
import org.eclipse.jdt.ls.debug.adapter.resource.disposer.ResumeThreadResourceDisposer;

import com.sun.jdi.ThreadReference;

public class ResumeRequestHandler implements IDebugRequestHandler {

    @Override
    public List<Command> getTargetCommands() {
        return Arrays.asList(Command.CONTINUE);
    }

    @Override
    public IRequestHandlerResourceDisposer getResourceDisposer() {
        return new ResumeThreadResourceDisposer();
    }

    @Override
    public void handle(Command command, Arguments arguments, Response response, IDebugAdapterContext context) {
        if (context.getDebugSession() == null) {
            AdapterUtils.setErrorResponse(response, ErrorCode.EMPTY_DEBUG_SESSION, "Debug Session doesn't exist.");
            return;
        }
        boolean allThreadsContinued = true;
        ThreadReference thread = DebugUtility.getThread(context.getDebugSession(),
                ((ContinueArguments) arguments).threadId);
        if (thread != null) {
            allThreadsContinued = false;
            thread.resume();
        } else {
            context.getDebugSession().resume();
        }
        response.body = new Responses.ContinueResponseBody(allThreadsContinued);

    }

}
