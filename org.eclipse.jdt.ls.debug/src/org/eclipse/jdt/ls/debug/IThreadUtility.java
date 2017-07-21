package org.eclipse.jdt.ls.debug;

import com.sun.jdi.ThreadReference;
import com.sun.jdi.event.StepEvent;
import java.util.function.Consumer;

import org.eclipse.debug.core.DebugEvent;

interface IThreadUtility {
    static void stepOver(ThreadReference thread, Consumer<DebugEvent> eventHandler) {
        throw new UnsupportedOperationException();
    }

    static void stepInto(ThreadReference thread, Consumer<DebugEvent> eventHandler) {
        throw new UnsupportedOperationException();
    }
    
    static void stepOut(ThreadReference thread, Consumer<DebugEvent> eventHandler) {
        throw new UnsupportedOperationException();
    }
}

