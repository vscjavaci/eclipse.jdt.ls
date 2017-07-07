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

import java.util.List;

import org.eclipse.jdt.ls.debug.internal.core.EventType;
import org.eclipse.jdt.ls.debug.internal.core.IBreakpoint;
import org.eclipse.jdt.ls.debug.internal.core.IVMTarget;
import org.eclipse.jdt.ls.debug.internal.core.IJDIEventListener;
import org.eclipse.jdt.ls.debug.internal.core.impl.DebugEvent;
import org.eclipse.jdt.ls.debug.internal.core.impl.JDIVMTarget;

import com.sun.jdi.ReferenceType;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.event.ClassPrepareEvent;
import com.sun.jdi.event.Event;
import com.sun.jdi.event.EventSet;
import com.sun.jdi.request.ClassPrepareRequest;
import com.sun.jdi.request.EventRequest;
import com.sun.jdi.request.EventRequestManager;

public abstract class JavaBreakpoint implements IBreakpoint, IJDIEventListener {
	private String _typeName;
	private int _hitCount;

	public JavaBreakpoint(final String fullQualifiedName, final int hitCount) {
		_typeName = fullQualifiedName;
		_hitCount = hitCount;
	}

	@Override
	public int getHitCount() {
		return _hitCount;
	}

	@Override
	public void setHitCount(int hitCount) {
		_hitCount = hitCount;
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

	public void addToVMTarget(IVMTarget target) {
		String referenceTypeName = _typeName;
		String enclosingTypeName = null;
		// Parse the top enclosing type in which this breakpoint is located.
		if (_typeName != null) {
			int index = _typeName.indexOf("$");
			if (index == -1) {
				enclosingTypeName = _typeName;
			} else {
				enclosingTypeName = _typeName.substring(0, index);
			}
		}
		if (referenceTypeName == null || enclosingTypeName == null) {
			return;
		}

		if (referenceTypeName.indexOf("$") == -1) {
			target.getEventHub().addJDIEventListener(createClassPrepareRequest(referenceTypeName, null, target), this);
			// intercept local and anonymous inner classes
			target.getEventHub().addJDIEventListener(createClassPrepareRequest(enclosingTypeName + "$*", null, target), this);
		} else {
			target.getEventHub().addJDIEventListener(createClassPrepareRequest(referenceTypeName, null, target), this);
			// intercept local and anonymous inner classes
			target.getEventHub().addJDIEventListener(createClassPrepareRequest(enclosingTypeName + "$*", referenceTypeName, target), this);
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
	 * Local types (types defined in methods) are handled specially due to the
	 * different types that the local type is associated with as well as the
	 * performance problems of using ReferenceType#nestedTypes. From the Java
	 * model perspective a local type is defined within a method of a type.
	 * Therefore the type of a breakpoint placed in a local type is the type
	 * that encloses the method where the local type was defined. The local type
	 * is enclosed within the top level type according to the VM. So if "normal"
	 * attempts to create a request when a breakpoint is being added to a target
	 * fail, we must be dealing with a local type and therefore resort to
	 * looking up all of the nested types of the top level enclosing type.
	 *
	 * @param target
	 *            the target
	 * @param enclosingTypeName
	 *            the type name of the enclosing type
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
	
	public void removeFromVMTarget(IVMTarget target) {

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
		if (_hitCount > 0) {
			request.addCountFilter(_hitCount);
		}
		request.setEnabled(true);
	}
}
