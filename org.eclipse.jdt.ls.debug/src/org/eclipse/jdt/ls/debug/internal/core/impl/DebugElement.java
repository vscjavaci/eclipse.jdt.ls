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

import org.eclipse.jdt.ls.debug.internal.core.IDebugElement;
import org.eclipse.jdt.ls.debug.internal.core.IDebugEvent;
import org.eclipse.jdt.ls.debug.internal.core.IVMTarget;
import org.eclipse.jdt.ls.debug.internal.core.IJDIEventHub;
import org.eclipse.jdt.ls.debug.internal.core.IJDIEventListener;

import com.sun.jdi.VirtualMachine;
import com.sun.jdi.request.EventRequest;
import com.sun.jdi.request.EventRequestManager;

public abstract class DebugElement implements IDebugElement {

	private IVMTarget _target;

	public DebugElement(IVMTarget target) {
		_target = target;
	}

	@Override
	public IVMTarget getVMTarget() {
		return _target;
	}

	@Override
	public VirtualMachine getVM() {
		return ((JDIVMTarget) getVMTarget()).getVM();
	}

	public EventRequestManager getEventRequestManager() {
		VirtualMachine vm = getVM();
		if (vm == null) {
			return null;
		}
		return vm.eventRequestManager();
	}

	public void addEventListener(EventRequest request, IJDIEventListener listener) {
		IJDIEventHub eventHub = ((JDIVMTarget) getVMTarget()).getEventHub();
		if (eventHub != null) {
			eventHub.addJDIEventListener(request, listener);
		}
	}

	public void removeEventListener(EventRequest request) {
		IJDIEventHub eventHub = ((JDIVMTarget) getVMTarget()).getEventHub();
		if (eventHub != null) {
			eventHub.removeJDIEventListener(request);
		}
	}

	public void fireEvent(IDebugEvent event) {
		DebugEventHub eventHub = (DebugEventHub) getVMTarget().getDebugContext().getDebugEventHub();
		eventHub.fireDebugEventSet(new IDebugEvent[] { event });
	}
}
