/*******************************************************************************
* Copyright (c) 2017 Microsoft Corporation and others.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*     Microsoft Corporation - initial API and implementation
*******************************************************************************/

package org.eclipse.jdt.ls.debug.internal.core.breakpoints;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.eclipse.jdt.ls.debug.internal.core.EventType;
import org.eclipse.jdt.ls.debug.internal.core.IBreakpoint;
import org.eclipse.jdt.ls.debug.internal.core.IJDIEventListener;
import org.eclipse.jdt.ls.debug.internal.core.IVMTarget;
import org.eclipse.jdt.ls.debug.internal.core.impl.DebugEvent;

import com.sun.jdi.ReferenceType;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.event.ClassPrepareEvent;
import com.sun.jdi.event.Event;
import com.sun.jdi.event.EventSet;
import com.sun.jdi.request.ClassPrepareRequest;
import com.sun.jdi.request.EventRequest;
import com.sun.jdi.request.EventRequestManager;

public abstract class JavaBreakpoint implements IBreakpoint, IJDIEventListener {
    private String typeName;
    private int hitCount;
    protected HashMap<IVMTarget, List<EventRequest>> requestsByTarget;

    /**
     * Constructor.
     * @param fullQualifiedName
     *                the full qualified name
     * @param hitCount
     *                the hit count number  
     */
    public JavaBreakpoint(final String fullQualifiedName, final int hitCount) {
        this.typeName = fullQualifiedName;
        this.hitCount = hitCount;
        this.requestsByTarget = new HashMap<>(1);
    }

    @Override
    public int getHitCount() {
        return this.hitCount;
    }

    @Override
    public void setHitCount(int hitCount) {
        this.hitCount = hitCount;
    }

    @Override
    public boolean handleEvent(Event event, IVMTarget target, boolean suspendVote, EventSet eventSet) {
        if (event instanceof ClassPrepareEvent) {
            return handleClassPrepareEvent((ClassPrepareEvent) event, target, suspendVote);
        }
        return handleBreakpointEvent(event, target, suspendVote);
    }

    public boolean handleClassPrepareEvent(ClassPrepareEvent event, IVMTarget target, boolean suspendVote) {
        createRequest(target, event.referenceType());
        return true;
    }

    public boolean handleBreakpointEvent(Event event, IVMTarget target, boolean suspendVote) {
        target.fireEvent(new DebugEvent(event, EventType.BREAKPOINT_EVENT));
        return false;
    }

    /**
     * Registers the breakpoint to the specified VM target.
     */
    public void addToVMTarget(IVMTarget target) {
        String referenceTypeName = this.typeName;
        String enclosingTypeName = null;
        // Parse the top enclosing type in which this breakpoint is located.
        if (this.typeName != null) {
            int index = this.typeName.indexOf("$");
            if (index == -1) {
                enclosingTypeName = this.typeName;
            } else {
                enclosingTypeName = this.typeName.substring(0, index);
            }
        }
        if (referenceTypeName == null || enclosingTypeName == null) {
            return;
        }

        if (referenceTypeName.indexOf("$") == -1) {
            this.registerRequest(createClassPrepareRequest(referenceTypeName, null, target), target);
            // intercept local and anonymous inner classes
            this.registerRequest(createClassPrepareRequest(enclosingTypeName + "$*", null, target), target);
        } else {
            this.registerRequest(createClassPrepareRequest(referenceTypeName, null, target), target);
            // intercept local and anonymous inner classes
            this.registerRequest(
                    createClassPrepareRequest(enclosingTypeName + "$*", referenceTypeName, target), target);
        }

        VirtualMachine vm = target.getVM();
        List<ReferenceType> classes = vm.classesByName(referenceTypeName);
        boolean success = false;
        for (ReferenceType type : classes) {
            if (createRequest(target, type)) {
                success = true;
            }
        }

        if (!success) {
            addToTargetForLocalType(target, enclosingTypeName);
        }
    }

    /**
     * Handles those local types defined in method.
     * 
     * @param target
     *              VM target
     * @param enclosingTypeName
     *              the full qualified name
     */
    protected void addToTargetForLocalType(IVMTarget target, String enclosingTypeName) {
        List<ReferenceType> classes = target.getVM().classesByName(enclosingTypeName);
        for (ReferenceType type : classes) {
            for (ReferenceType nestedType : type.nestedTypes()) {
                if (createRequest(target, nestedType)) {
                    break;
                }
            }
        }
    }

    /**
     * Remove the breakpoint from debuggee VM.
     */
    public void removeFromVMTarget(IVMTarget target) {
        List<EventRequest> list = this.requestsByTarget.get(target);
        if (list == null) {
            return ;
        }
        for (EventRequest request : list) {
            try {
                EventRequestManager manager = target.getEventRequestManager();
                if (manager != null) {
                    manager.deleteEventRequest(request);
                }
            } finally {
                target.getEventHub().removeJDIEventListener(request);
            }
        }
        this.requestsByTarget.remove(target);
    }

    protected ClassPrepareRequest createClassPrepareRequest(String classPattern, String classExclusionPattern,
            IVMTarget target) {
        EventRequestManager manager = target.getVM().eventRequestManager();
        ClassPrepareRequest req = manager.createClassPrepareRequest();
        if (classPattern != null) {
            req.addClassFilter(classPattern);
        }
        if (classExclusionPattern != null) {
            req.addClassExclusionFilter(classExclusionPattern);
        }
        req.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD);
        req.enable();
        return req;
    }

    protected abstract boolean createRequest(IVMTarget target, ReferenceType type);

    protected void configureRequest(EventRequest request) {
        request.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD);
        if (this.hitCount > 0) {
            request.addCountFilter(this.hitCount);
        }
        request.setEnabled(true);
    }
    
    protected void registerRequest(EventRequest request, IVMTarget target) {
        if (request == null) {
            return;
        }
        
        List<EventRequest> list = this.requestsByTarget.get(target);
        if (list == null) {
            list = new ArrayList<>();
            this.requestsByTarget.put(target, list);
        }
        // cache EventRequest.
        list.add(request);
        // register event listener to EventHub.
        target.getEventHub().addJDIEventListener(request, this);
    }
}
