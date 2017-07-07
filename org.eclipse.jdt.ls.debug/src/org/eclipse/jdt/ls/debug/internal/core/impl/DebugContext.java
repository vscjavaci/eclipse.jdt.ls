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

import org.eclipse.jdt.ls.debug.internal.core.IBreakpointManager;
import org.eclipse.jdt.ls.debug.internal.core.IDebugContext;
import org.eclipse.jdt.ls.debug.internal.core.IDebugEventHub;
import org.eclipse.jdt.ls.debug.internal.core.IVMTarget;
import org.eclipse.jdt.ls.debug.internal.core.breakpoints.BreakpointManager;

/**
 *
 */
public class DebugContext implements IDebugContext {
	private BreakpointManager _breakpointManager = null;
	private DebugEventHub _debugEventHub = null;
	private IVMTarget _vmTarget;
	
	public DebugContext() {
		_breakpointManager = new BreakpointManager(this);
		_debugEventHub = new DebugEventHub();
	}

	@Override
	public IBreakpointManager getBreakpointManager() {
		return _breakpointManager;
	}

	@Override
	public IDebugEventHub getDebugEventHub() {
		return _debugEventHub;
	}
	
	public void setVMTarget(IVMTarget vmTarget) {
		_vmTarget = vmTarget;
	}
	
	public IVMTarget getVMTarget() {
		return _vmTarget;
	}
}
