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
import org.eclipse.jdt.ls.debug.internal.core.IDebugContext;
import org.eclipse.jdt.ls.debug.internal.core.IJDIEventHub;
import org.eclipse.jdt.ls.debug.internal.core.IThread;
import org.eclipse.jdt.ls.debug.internal.core.IThreadManager;
import org.eclipse.jdt.ls.debug.internal.core.IVMTarget;

import com.sun.jdi.VirtualMachine;
import com.sun.jdi.event.VMDeathEvent;
import com.sun.jdi.event.VMDisconnectEvent;
import com.sun.jdi.event.VMStartEvent;

public class JDIVMTarget extends DebugElement implements IVMTarget {

	private VirtualMachine _jvm;
	private boolean _resumeOnStartup;
	private IDebugContext _debugContext;
	private JDIEventHub _vmEventHub;
	private JDIThreadManager _vmThreadManager;
	
	public JDIVMTarget(IDebugContext context, VirtualMachine jvm, boolean resumeOnStartup) {
		super(null);
		_jvm = jvm;
		_resumeOnStartup = resumeOnStartup;
		_debugContext = context;
		_debugContext.setVMTarget(this);
		initialize();
	}

	public void initialize() {
		_vmEventHub = new JDIEventHub(this);
		_vmThreadManager = new JDIThreadManager(this);
		
		Thread t = new Thread(_vmEventHub, "VirtualMachineEventHub");
		t.setDaemon(true);
		t.start();
	}

	@Override
	public IVMTarget getVMTarget() {
		return this;
	}

	@Override
	public VirtualMachine getVM() {
		return _jvm;
	}

	@Override
	public IJDIEventHub getEventHub() {
		return _vmEventHub;
	}
	
	public IThreadManager getThreadManager() {
		return _vmThreadManager;
	}
	
	@Override
	public IThread[] getThreads() {
		return _vmThreadManager.getThreads();
	}
	
	public IDebugContext getDebugContext() {
		return _debugContext;
	}

	@Override
	public void fireCreationEvent() {
		fireEvent(new DebugEvent(this, EventType.VMSTART_EVENT));
	}

	@Override
	public void fireTerminateEvent() {
		fireEvent(new DebugEvent(this, EventType.VMDEATH_EVENT));
	}

	public void handleVMDeath(VMDeathEvent event) {
		fireTerminateEvent();
	}

	public void handleVMDisconnect(VMDisconnectEvent event) {
		fireTerminateEvent();
	}

	public void handleVMStart(VMStartEvent event) {
		fireCreationEvent();
	}

	public void resume() {
		_resumeOnStartup = true;
		VirtualMachine vm = getVM();
		if (vm != null) {
			vm.resume();
		}
	}
}
