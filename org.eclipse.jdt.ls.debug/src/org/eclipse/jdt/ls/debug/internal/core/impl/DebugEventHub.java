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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.jdt.ls.debug.internal.core.IDebugEvent;
import org.eclipse.jdt.ls.debug.internal.core.IDebugEventHub;
import org.eclipse.jdt.ls.debug.internal.core.IDebugEventSetListener;

public class DebugEventHub implements IDebugEventHub {
	// private List<Object> _eventQueue;
	private List<IDebugEventSetListener> _eventListeners;

	public DebugEventHub() {
		// _eventQueue = Collections.synchronizedList(new ArrayList<>(5));
		_eventListeners = Collections.synchronizedList(new ArrayList<>(5));
	}

	public void fireDebugEventSet(IDebugEvent[] events) {
		if (events == null || _eventListeners.isEmpty()) {
			return;
		}
		// synchronized (_eventQueue) {
		// _eventQueue.add(events);
		// }
		for (IDebugEventSetListener listener : _eventListeners) {
			listener.handleDebugEvents(events);
		}
	}

	public void addDebugEventSetListener(IDebugEventSetListener listener) {
		synchronized (_eventListeners) {
			_eventListeners.add(listener);
		}
	}

	public void removeDebugEventSetListener(IDebugEventSetListener listener) {
		synchronized (_eventListeners) {
			_eventListeners.remove(listener);
		}
	}
}
