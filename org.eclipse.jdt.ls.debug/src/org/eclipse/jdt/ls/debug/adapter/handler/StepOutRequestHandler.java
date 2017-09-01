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
import org.eclipse.jdt.ls.debug.adapter.Requests.StepOutArguments;
import org.eclipse.jdt.ls.debug.adapter.resource.disposer.IRequestHandlerResourceDisposer;
import org.eclipse.jdt.ls.debug.adapter.resource.disposer.OnDemandThreadResourceDisposer;

import com.sun.jdi.ThreadReference;

public class StepOutRequestHandler implements IDebugRequestHandler {

    @Override
    public List<Command> getTargetCommands() {
        return Arrays.asList(Command.STEPOUT);
    }

    @Override
    public IRequestHandlerResourceDisposer getResourceDisposer() {
        return new OnDemandThreadResourceDisposer();
    }

    @Override
    public void handle(Command command, Arguments arguments, Response response, IDebugAdapterContext context) {
        if (context.getDebugSession() == null) {
            AdapterUtils.setErrorResponse(response, ErrorCode.EMPTY_DEBUG_SESSION, "Debug Session doesn't exist.");
            return;
        }
        ThreadReference thread = DebugUtility.getThread(context.getDebugSession(),
                ((StepOutArguments) arguments).threadId);
        if (thread != null) {
            DebugUtility.stepOut(thread, context.getDebugSession().eventHub());
        }

    }

}
