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

import org.eclipse.jdt.ls.debug.internal.core.IVMTarget;

import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.Location;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.request.EventRequest;
import com.sun.jdi.request.EventRequestManager;

public class JavaLineBreakpoint extends JavaBreakpoint {
	private int _lineNumber;

	public JavaLineBreakpoint(final String fullQualifiedName, final int lineNumber, final int hitCount) {
		super(fullQualifiedName, hitCount);
		_lineNumber = lineNumber;
	}

	public int getLineNumber() {
		return _lineNumber;
	}

	@Override
	protected boolean createRequest(IVMTarget target, ReferenceType type) {
		int lineNumber = getLineNumber();
		List<Location> locations = determineLocations(lineNumber, type);
		if (locations == null || locations.isEmpty()) {
			return false;
		}
		EventRequestManager manager = target.getEventRequestManager();
		if (manager == null) {
			return false;
		}
		EventRequest[] requests = new EventRequest[locations.size()];
		int i = 0;
		for (Location location : locations) {
			requests[i] = manager.createBreakpointRequest(location);
			configureRequest(requests[i]);
			target.getEventHub().addJDIEventListener(requests[i], this); // register event listener to EventHub
			i++;
		}
		return true;
	}
	
	protected List<Location> determineLocations(int lineNumber, ReferenceType type) {
		List<Location> locations = null;
		try {
			locations = type.locationsOfLine("Java", null, lineNumber);
		} catch (AbsentInformationException e) {
			e.printStackTrace();
			return null;
		}
		return locations;
	}
}
