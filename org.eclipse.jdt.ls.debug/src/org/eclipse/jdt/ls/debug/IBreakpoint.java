package org.eclipse.jdt.ls.debug;

import com.sun.jdi.request.EventRequest;

public interface IBreakpoint {
    // This is the receipt for tracking the breakpoint at JDI side
    EventRequest breakpointRequest();
}
