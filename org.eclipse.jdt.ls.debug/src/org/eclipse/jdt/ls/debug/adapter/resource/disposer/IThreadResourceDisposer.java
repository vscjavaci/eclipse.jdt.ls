package org.eclipse.jdt.ls.debug.adapter.resource.disposer;

import org.eclipse.jdt.ls.debug.adapter.IDebugAdapterContext;

import com.sun.jdi.ThreadReference;

public interface IThreadResourceDisposer {
    /**
     * dispose thread resources.
     * @param current
     *               current thread reference.
     * @param context
     *               debug adapter context
     */
    void dispose(ThreadReference current, IDebugAdapterContext context);
}
