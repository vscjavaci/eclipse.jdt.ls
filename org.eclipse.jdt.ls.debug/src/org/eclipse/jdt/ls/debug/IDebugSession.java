package org.eclipse.jdt.ls.debug;

import java.util.function.Consumer;

import org.eclipse.debug.core.DebugEvent;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.event.Event;
import com.sun.jdi.event.StepEvent;

public interface IDebugSession {
    void suspend();

    void resume();

    static IDebugSession launch(/* TODO: arguments? */) {
        throw new UnsupportedOperationException();
    }

    static IDebugSession attach(/* TODO: arguments? */) {
        throw new UnsupportedOperationException();
    }

    void detach();

    void terminate();

    // breakpoints
    void addBreakpoint(String className, int lineNumber, Consumer<IBreakpoint> validatedHandler, Consumer<DebugEvent> breakEventHandler);

    void addBreakpoint(String className, int lineNumber, int hitCount, Consumer<IBreakpoint> validatedHandler, Consumer<DebugEvent> breakEventHandler);

    void removeBreakpoint(IBreakpoint breakpoint);

    ThreadReference[] allThreads();
    
    EventHub eventHub();
}

