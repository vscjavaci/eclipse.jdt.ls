package org.eclipse.jdt.ls.debug.internal;

import io.reactivex.*;
import io.reactivex.subjects.*;
import com.sun.jdi.*;
import com.sun.jdi.event.*;

public class EventHub implements AutoCloseable {
	private PublishSubject<DebugEvent> subject = PublishSubject.<DebugEvent>create();
	public Observable<DebugEvent> observable() {
		return subject;
	}
	
	public void start(VirtualMachine vm) {
		EventQueue queue = vm.eventQueue();
		while (true) {
			try {
				if (Thread.interrupted()) {
					break;
				}
				
				EventSet set = queue.remove();
				boolean shouldResume = true;
				for (Event event : set) {
					DebugEvent dbgEvent = new DebugEvent();
					dbgEvent.event = event;
					subject.onNext(dbgEvent);
					shouldResume &= dbgEvent.shouldResume;
				}
				
				if (shouldResume) {
					set.resume();
				}
			} catch (InterruptedException e) {
				subject.onComplete();
				return;
			} catch (VMDisconnectedException e) {
				subject.onError(e);
				return;
			}
		}
	}
	
	@Override
	public void close() {
		Thread.currentThread().interrupt();
	}
}
