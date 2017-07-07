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
import java.util.Iterator;
import java.util.List;

import org.eclipse.jdt.ls.debug.internal.core.IVMTarget;
import org.eclipse.jdt.ls.debug.internal.core.IJDIEventListener;
import org.eclipse.jdt.ls.debug.internal.core.IThread;
import org.eclipse.jdt.ls.debug.internal.core.IThreadManager;
import org.eclipse.jdt.ls.debug.internal.core.log.Logger;

import com.sun.jdi.ObjectCollectedException;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.VMDisconnectedException;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.event.Event;
import com.sun.jdi.event.EventSet;
import com.sun.jdi.event.ThreadDeathEvent;
import com.sun.jdi.event.ThreadStartEvent;
import com.sun.jdi.request.EventRequest;
import com.sun.jdi.request.EventRequestManager;

/**
 *
 */
public class JDIThreadManager implements IThreadManager {
	private List<IThread> _threads;
	private JDIVMTarget _target;

	public JDIThreadManager(JDIVMTarget target) {
		_target = target;
		_threads = Collections.synchronizedList(new ArrayList<IThread>(5));
		initialize();
	}
	
	protected void initialize() {
		// register event handlers for thread creation, thread termination.
		new ThreadStartHandler();
		new ThreadDeathHandler();

		// Adds all of pre-existings threads to this debug target.
		List<ThreadReference> threads = null;
		VirtualMachine vm = _target.getVM();
		if (vm != null) {
			// try {
			// String name = vm.name();
			// fSupportsDisableGC = !name.equals("Classic VM"); //$NON-NLS-1$
			// } catch (RuntimeException e) {
			// Logger.logError(e);
			// }
			try {
				threads = vm.allThreads();
			} catch (RuntimeException e) {
				Logger.logError(e);
			}
			if (threads != null) {
				Iterator<ThreadReference> initialThreads = threads.iterator();
				while (initialThreads.hasNext()) {
					createThread(initialThreads.next());
				}
			}
		}
	}
	
	public IThread[] getThreads() {
		synchronized (_threads) {
			return _threads.toArray(new IThread[0]);
		}
	}
	
	public IThread findThread(ThreadReference threadReference) {
		for (IThread thread : _threads) {
			if (thread.getUnderlyingThread().equals(threadReference)) {
				return thread;
			}
		}
		return null;
	}

	public IThread createThread(ThreadReference threadReference) {
		IThread jdiThread = new JDIThread(_target, threadReference);
		synchronized (_threads) {
			_threads.add(jdiThread);
		}
		jdiThread.fireCreationEvent();
		return jdiThread;
	}
	
	
	class ThreadStartHandler implements IJDIEventListener {

		protected EventRequest _request;

		protected ThreadStartHandler() {
			EventRequestManager manager = _target.getEventRequestManager();
			if (manager != null) {
				try {
					EventRequest req = manager.createThreadStartRequest();
					req.setSuspendPolicy(EventRequest.SUSPEND_NONE);
					req.enable();
					_target.addEventListener(req, this);
					_request = req;
				} catch (RuntimeException e) {
					Logger.logError(e);
				}
			}
		}

		@Override
		public boolean handleEvent(Event event, IVMTarget target, boolean suspendVote, EventSet eventSet) {
			ThreadReference thread = ((ThreadStartEvent) event).thread();
			try {
				// https://bugs.eclipse.org/bugs/show_bug.cgi?id=443727
				// the backing ThreadReference could be read in as null
				if (thread == null || thread.isCollected()) {
					return false;
				}
			} catch (VMDisconnectedException exception) {
				return false;
			} catch (ObjectCollectedException e) {
				return false;
			}
			IThread jdiThread = findThread(thread);
			if (jdiThread == null) {
				jdiThread = createThread(thread);
			} else {
				// TODO
			}
			return true;
		}

		protected void deleteRequest() {
			if (_request != null) {
				_target.removeEventListener(_request);
				_request = null;
			}
		}
	}

	class ThreadDeathHandler implements IJDIEventListener {

		protected ThreadDeathHandler() {
			EventRequestManager manager = _target.getEventRequestManager();
			if (manager != null) {
				try {
					EventRequest req = manager.createThreadDeathRequest();
					req.setSuspendPolicy(EventRequest.SUSPEND_NONE);
					req.enable();
					_target.addEventListener(req, this);
				} catch (RuntimeException e) {
					Logger.logError(e);
				}
			}
		}

		@Override
		public boolean handleEvent(Event event, IVMTarget target, boolean suspendVote, EventSet eventSet) {
			ThreadReference ref = ((ThreadDeathEvent) event).thread();
			IThread thread = findThread(ref);
			if (thread != null) {
				synchronized (_threads) {
					_threads.remove(thread);
				}
				thread.fireTerminateEvent();
			}
			return true;
		}

	}
}
