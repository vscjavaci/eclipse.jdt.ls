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
import java.util.Collections;
import java.util.List;

import org.eclipse.jdt.ls.debug.internal.core.IBreakpoint;
import org.eclipse.jdt.ls.debug.internal.core.IBreakpointManager;
import org.eclipse.jdt.ls.debug.internal.core.IDebugContext;

public class BreakpointManager implements IBreakpointManager {
	/**
	 * A collection of breakpoints registered with this manager.
	 */
	private List<IBreakpoint> _breakpoints;
	private IDebugContext _debugContext;

	public BreakpointManager(IDebugContext debugContext) {
		_breakpoints = Collections.synchronizedList(new ArrayList<>(5));
		_debugContext = debugContext;
	}

	public void addBreakpoint(IBreakpoint breakpoint) {
		addBreakpoints(new IBreakpoint[] { breakpoint });
	}

	public void addBreakpoints(IBreakpoint[] breakpoints) {
		for (IBreakpoint breakpoint : breakpoints) {
			synchronized (_breakpoints) {
				_breakpoints.add(breakpoint);
			}
		}
		if (breakpoints != null && breakpoints.length > 0) {
				for (IBreakpoint breakpoint : _breakpoints) {
					try {
						breakpoint.addToVMTarget(_debugContext.getVMTarget());
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			
		}
	}

	public void removeBreakpoint(IBreakpoint breakpoint) {
		removeBreakpoints(new IBreakpoint[] { breakpoint });
	}

	public void removeBreakpoints(IBreakpoint[] breakpoints) {
		for (IBreakpoint breakpoint : breakpoints) {
			if (_breakpoints.contains(breakpoint)) {
				try {
					breakpoint.removeFromVMTarget(_debugContext.getVMTarget());;
					synchronized (_breakpoints) {
						_breakpoints.remove(breakpoint);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	public IBreakpoint[] getBreakpoints() {
		return _breakpoints.toArray(new IBreakpoint[0]);
	}
}
