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

import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.eclipse.jdt.ls.debug.internal.core.IJDIEventHub;
import org.eclipse.jdt.ls.debug.internal.core.IJDIEventListener;
import org.eclipse.jdt.ls.debug.internal.core.log.Logger;

import com.sun.jdi.VMDisconnectedException;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.event.Event;
import com.sun.jdi.event.EventIterator;
import com.sun.jdi.event.EventQueue;
import com.sun.jdi.event.EventSet;
import com.sun.jdi.event.VMDeathEvent;
import com.sun.jdi.event.VMDisconnectEvent;
import com.sun.jdi.event.VMStartEvent;
import com.sun.jdi.request.EventRequest;

public class JDIEventHub implements IJDIEventHub {
	private JDIVMTarget _target;
	private HashMap<EventRequest, IJDIEventListener> _eventHandlers;
	private boolean _shutdown = false;

	public JDIEventHub(JDIVMTarget target) {
		_target = target;
		_eventHandlers = new HashMap<>(10);
	}

	@Override
	public void addJDIEventListener(EventRequest request, IJDIEventListener listener) {
		_eventHandlers.put(request, listener);
	}

	@Override
	public void removeJDIEventListener(EventRequest request) {
		_eventHandlers.remove(request);
	}

	public boolean isShutdown() {
		return _shutdown;
	}

	public void shutdown() {
		_shutdown = true;
	}

	@Override
	public void run() {
		VirtualMachine jvm = _target.getVM();
		if (jvm != null) {
			EventQueue eventQueue = jvm.eventQueue();
			EventSet eventSet = null;
			ExecutorService executor = Executors.newFixedThreadPool(10);
			while (!isShutdown()) {
				try {
					eventSet = eventQueue.remove(1000);
				} catch (InterruptedException | VMDisconnectedException e) {
					break;
				}

				if (!isShutdown() && eventSet != null) {
					final EventSet temp = eventSet;
					executor.submit(new Runnable() {

						@Override
						public void run() {
							dispatch(temp);
						}

					});
				}
			}
		}
	}

	private void dispatch(EventSet eventSet) {
		EventIterator eventIter = eventSet.eventIterator();
		// print JDI Events
		StringBuffer buf = new StringBuffer("JDI Event Set: {\n"); //$NON-NLS-1$
		while (eventIter.hasNext()) {
			buf.append(eventIter.next());
			if (eventIter.hasNext()) {
				buf.append(", ");
			}
		}
		buf.append("}\n");
		Logger.log(buf.toString());

		eventIter = eventSet.eventIterator();
		boolean vote = false;
		boolean resume = true;
		while (eventIter.hasNext()) {
			if (isShutdown()) {
				return;
			}
			Event event = eventIter.nextEvent();
			IJDIEventListener listener = _eventHandlers.get(event.request());
			if (listener != null) {
				vote = true;
				resume = listener.handleEvent(event, _target, !resume, eventSet) && resume;
				continue;
			}

			// Dispatch VM start/end events
			if (event instanceof VMDeathEvent) {
				_target.handleVMDeath((VMDeathEvent) event);
				shutdown(); // stop listening for events
			} else if (event instanceof VMDisconnectEvent) {
				_target.handleVMDisconnect((VMDisconnectEvent) event);
				shutdown(); // stop listening for events
			} else if (event instanceof VMStartEvent) {
				_target.handleVMStart((VMStartEvent) event);
			} else {
				// not handled
			}
		}

		if (vote && resume) {
			try {
				eventSet.resume();				
			} catch (VMDisconnectedException e) {
			} catch (RuntimeException e) {
			}
		}
	}

}
