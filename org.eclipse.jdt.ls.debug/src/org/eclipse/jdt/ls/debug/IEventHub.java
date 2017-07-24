package org.eclipse.jdt.ls.debug;

import com.sun.jdi.VirtualMachine;

import io.reactivex.Observable;

public interface IEventHub extends AutoCloseable {
    void start(VirtualMachine vm);
    Observable<DebugEvent> events();
    Observable<DebugEvent> breakpointEvents();
    Observable<DebugEvent> threadEvents();
    Observable<DebugEvent> exceptionEvents();
    Observable<DebugEvent> stepEvents();
    Observable<DebugEvent> vmEvents();
}
