package org.eclipse.jdt.ls.debug;

import java.util.List;

import com.sun.jdi.request.EventRequest;

import io.reactivex.disposables.Disposable;

public interface IDebugResource extends AutoCloseable {
    List<EventRequest> requests();
    List<Disposable> subscriptions();
}
