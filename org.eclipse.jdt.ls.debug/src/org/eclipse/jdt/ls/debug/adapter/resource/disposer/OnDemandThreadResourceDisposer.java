package org.eclipse.jdt.ls.debug.adapter.resource.disposer;

import java.util.List;

import org.eclipse.jdt.ls.debug.DebugUtility;
import org.eclipse.jdt.ls.debug.adapter.IDebugAdapterContext;
import org.eclipse.jdt.ls.debug.adapter.RecyclableObjectPool;

import com.sun.jdi.ThreadReference;
import com.sun.jdi.VMDisconnectedException;

public class OnDemandThreadResourceDisposer implements IThreadResourceDisposer {
    @Override
    public void dispose(ThreadReference current, IDebugAdapterContext context) {
        RecyclableObjectPool<Long, Object> pool = context.getRecyclableIdPool();
        try {
            if (allThreadRunning(context)) {
                pool.removeAllObjects();
            } else if (current != null) {
                pool.removeObjectsByOwner(current.uniqueID());
            }
        } catch (VMDisconnectedException ex) {
            pool.removeAllObjects();
        }
    }
    
    private static boolean allThreadRunning(IDebugAdapterContext context) {
        List<ThreadReference> threads = DebugUtility.getAllThreadsSafely(context.getDebugSession());
        return !threads.stream().anyMatch(ThreadReference::isSuspended);
    }
}
