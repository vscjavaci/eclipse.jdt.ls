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

package org.eclipse.jdt.ls.debug.internal.core.impl;

import org.eclipse.jdt.ls.debug.internal.core.EventType;
import org.eclipse.jdt.ls.debug.internal.core.IJDIEventListener;
import org.eclipse.jdt.ls.debug.internal.core.IThread;
import org.eclipse.jdt.ls.debug.internal.core.IVMTarget;

import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.StackFrame;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.event.Event;
import com.sun.jdi.event.EventSet;
import com.sun.jdi.request.EventRequest;
import com.sun.jdi.request.EventRequestManager;
import com.sun.jdi.request.StepRequest;

public class JDIThread extends DebugElement implements IThread, IJDIEventListener {

    private ThreadReference thread;

    public JDIThread(IVMTarget target, ThreadReference thread) {
        super(target);
        this.thread = thread;
    }

    @Override
    public ThreadReference getUnderlyingThread() {
        return this.thread;
    }

    protected StackFrame getTopStackFrame() {
        try {
            return this.thread.frame(0);
        } catch (IncompatibleThreadStateException e) {
            return null;
        }
    }
    
    @Override
    public void fireCreationEvent() {
        fireEvent(new DebugEvent(this, EventType.THREADSTART_EVENT));
    }

    @Override
    public void fireTerminateEvent() {
        fireEvent(new DebugEvent(this, EventType.THREADDEATH_EVENT));
    }

    @Override
    public void stepInto() {
        createStepRequest(StepRequest.STEP_INTO);
        resume();
    }

    @Override
    public void stepOver() {
        createStepRequest(StepRequest.STEP_OVER);
        resume();
    }

    public void stepOut() {
        createStepRequest(StepRequest.STEP_OUT);
        resume();
    }

    @Override
    public void resume() {
        thread.resume();
    }
    
    @Override
    public boolean handleEvent(Event event, IVMTarget target, boolean suspendVote, EventSet eventSet) {
        target.fireEvent(new DebugEvent(event, EventType.STEP_EVENT));
        return false;
    }
    
    private void createStepRequest(int stepKind) {
        StackFrame top = getTopStackFrame();
        if (top == null) {
            return ;
        }
        EventRequestManager manager = getEventRequestManager();
        if (manager == null) {
            return ;
        }
        StepRequest request = manager.createStepRequest(thread, StepRequest.STEP_LINE, stepKind);
        request.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD);
        request.addCountFilter(1);
        request.setEnabled(true);
        
        this.getVMTarget().getEventHub().addJDIEventListener(request, this);
    }
}
