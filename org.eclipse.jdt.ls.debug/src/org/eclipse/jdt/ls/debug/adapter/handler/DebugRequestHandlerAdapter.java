package org.eclipse.jdt.ls.debug.adapter.handler;

import java.util.List;

import org.eclipse.jdt.ls.debug.DebugUtility;
import org.eclipse.jdt.ls.debug.adapter.IDebugAdapterContext;
import org.eclipse.jdt.ls.debug.adapter.IDebugRequestHandler;
import org.eclipse.jdt.ls.debug.adapter.Messages.Response;
import org.eclipse.jdt.ls.debug.adapter.Requests.Arguments;
import org.eclipse.jdt.ls.debug.adapter.Requests.ArgumentsWithThreadId;
import org.eclipse.jdt.ls.debug.adapter.Requests.Command;
import org.eclipse.jdt.ls.debug.adapter.resource.disposer.IThreadResourceDisposer;

import com.sun.jdi.ThreadReference;

public class DebugRequestHandlerAdapter implements IDebugRequestHandler {

    private IDebugRequestHandler inner;
    private IThreadResourceDisposer disposer;
    
    /**
     * constructor.
     * @param inner
     *             inner handler
     * @param disposer
     *                resource disposer
     */
    public DebugRequestHandlerAdapter(IDebugRequestHandler inner, IThreadResourceDisposer disposer) {
        if (inner == null) {
            throw new IllegalArgumentException("inner is empty");
        }
        this.inner = inner;
        this.disposer = disposer;
    }
    
    @Override
    public List<Command> getTargetCommands() {
        return inner.getTargetCommands();
    }

    @Override
    public void handle(Command command, Arguments arguments, Response response, IDebugAdapterContext context) {
        inner.handle(command, arguments, response, context);
        if (disposer != null && arguments instanceof ArgumentsWithThreadId) {
            long threadId = ((ArgumentsWithThreadId)arguments).threadId;
            ThreadReference current = DebugUtility.getThread(context.getDebugSession(), threadId);
            if (current != null) {
                disposer.dispose(current, context);
            }
        }
    }

}
