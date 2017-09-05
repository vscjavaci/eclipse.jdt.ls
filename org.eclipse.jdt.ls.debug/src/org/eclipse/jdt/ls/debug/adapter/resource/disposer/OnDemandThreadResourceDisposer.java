package org.eclipse.jdt.ls.debug.adapter.resource.disposer;

import java.util.List;

import org.eclipse.jdt.ls.debug.DebugUtility;
import org.eclipse.jdt.ls.debug.adapter.IDebugAdapterContext;
import org.eclipse.jdt.ls.debug.adapter.RecyclableObjectPool;
import org.eclipse.jdt.ls.debug.adapter.Requests.Arguments;
import org.eclipse.jdt.ls.debug.adapter.Requests.ArgumentsWithThreadId;

import com.sun.jdi.ThreadReference;
import com.sun.jdi.VMDisconnectedException;

public class OnDemandThreadResourceDisposer implements IRequestHandlerResourceDisposer {
    @Override
    public void dispose(Arguments requestArguments, IDebugAdapterContext context) {
        RecyclableObjectPool<Long, Object> pool = context.getRecyclableIdPool();
        try {
            if (allThreadRunning(context)) {
                pool.removeAllObjects();
            } else {
                ThreadReference current = getThreadReference(requestArguments, context);
                if (current != null && !current.isSuspended()) {
                    pool.removeObjectsByOwner(current.uniqueID());
                }
            }
        } catch (VMDisconnectedException ex) {
            pool.removeAllObjects();
        }
    }

    private ThreadReference getThreadReference(Arguments requestArgs, IDebugAdapterContext context) {
        if (requestArgs instanceof ArgumentsWithThreadId) {
            long threadId = ((ArgumentsWithThreadId) requestArgs).threadId;
            ThreadReference current = DebugUtility.getThread(context.getDebugSession(), threadId);
            return current;
        }
        return null;
    }

    private static boolean allThreadRunning(IDebugAdapterContext context) {
        List<ThreadReference> threads = DebugUtility.getAllThreadsSafely(context.getDebugSession());
        return !threads.stream().anyMatch(ThreadReference::isSuspended);
    }
}
