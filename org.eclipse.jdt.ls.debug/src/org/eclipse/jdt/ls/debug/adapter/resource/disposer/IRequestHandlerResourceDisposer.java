package org.eclipse.jdt.ls.debug.adapter.resource.disposer;

import org.eclipse.jdt.ls.debug.adapter.IDebugAdapterContext;
import org.eclipse.jdt.ls.debug.adapter.Requests.Arguments;

public interface IRequestHandlerResourceDisposer {
    /**
     * dispose request handler resources.
     *
     * @param requestArguments
     *            request handler arguments.
     * @param context
     *            debug adapter context
     */
    void dispose(Arguments requestArguments, IDebugAdapterContext context);
}
