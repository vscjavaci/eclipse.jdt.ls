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

package org.eclipse.jdt.ls.debug.adapter;

import com.sun.jdi.ThreadReference;

public interface IDebugAdapterContext extends IProviderContext {
    /**
     * Get a JDI thread from a thread id.
     * @param threadId the thread id
     * @return the JDI thread
     */
    ThreadReference getThread(int threadId);
    
    /**
     * Send debug event synchronously. 
     * @param event the debug event
     */
    void sendEvent(Events.DebugEvent event);

    /**
     * Send debug event asynchronously. 
     * @param event the debug event
     */
    void sendEventLater(Events.DebugEvent event);

    /**
     * Set a key/value pair for the handler during the handle processing session.
     *
     * @param key the object key
     * @param value the object 
     */
    void setSession(String key, Object value);

    /**
     * Get a session object during the handle processing.
     * 
     * @param key the object key
     * @return the object in the handle processing session
     */
    Object getSession(String key);
}
