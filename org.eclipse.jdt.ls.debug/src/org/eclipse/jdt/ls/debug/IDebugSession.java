package org.eclipse.jdt.ls.debug;

import com.sun.jdi.ThreadReference;

public interface IDebugSession {
    static IDebugSession launch(/* TODO: arguments? */) {
        throw new UnsupportedOperationException();
    }

    static IDebugSession attach(/* TODO: arguments? */) {
        throw new UnsupportedOperationException();
    }

    void suspend();

    void resume();

    void detach();

    void terminate();

    // breakpoints
    IBreakpoint createBreakpoint(String className, int lineNumber);
    IBreakpoint createBreakpoint(String className, int lineNumber, int hitCount);

    // createExeptionBreakpoint
    // createFunctionBreakpoint

    ThreadReference[] allThreads();

    IEventHub eventHub();
}

