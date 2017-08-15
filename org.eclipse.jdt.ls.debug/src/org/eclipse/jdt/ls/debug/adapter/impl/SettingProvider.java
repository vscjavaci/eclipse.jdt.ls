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


package org.eclipse.jdt.ls.debug.adapter.impl;

import org.eclipse.jdt.ls.debug.adapter.ISettingProvider;

public class SettingProvider implements ISettingProvider {
    private boolean debuggerLinesStartAt1 = true;
    private boolean debuggerPathsAreUri = true;
    private boolean clientLinesStartAt1 = true;
    private boolean clientPathsAreUri = false;
    
    /* (non-Javadoc)
     * @see org.eclipse.jdt.ls.debug.adapter.impl.ISettingProvider#isDebuggerLinesStartAt1()
     */
    @Override
    public boolean isDebuggerLinesStartAt1() {
        return debuggerLinesStartAt1;
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.jdt.ls.debug.adapter.impl.ISettingProvider#setDebuggerLinesStartAt1(boolean)
     */
    @Override
    public void setDebuggerLinesStartAt1(boolean debuggerLinesStartAt1) {
        this.debuggerLinesStartAt1 = debuggerLinesStartAt1;
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.jdt.ls.debug.adapter.impl.ISettingProvider#isDebuggerPathsAreUri()
     */
    @Override
    public boolean isDebuggerPathsAreUri() {
        return debuggerPathsAreUri;
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.jdt.ls.debug.adapter.impl.ISettingProvider#setDebuggerPathsAreUri(boolean)
     */
    @Override
    public void setDebuggerPathsAreUri(boolean debuggerPathsAreUri) {
        this.debuggerPathsAreUri = debuggerPathsAreUri;
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.jdt.ls.debug.adapter.impl.ISettingProvider#isClientLinesStartAt1()
     */
    @Override
    public boolean isClientLinesStartAt1() {
        return clientLinesStartAt1;
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.jdt.ls.debug.adapter.impl.ISettingProvider#setClientLinesStartAt1(boolean)
     */
    @Override
    public void setClientLinesStartAt1(boolean clientLinesStartAt1) {
        this.clientLinesStartAt1 = clientLinesStartAt1;
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.jdt.ls.debug.adapter.impl.ISettingProvider#isClientPathsAreUri()
     */
    @Override
    public boolean isClientPathsAreUri() {
        return clientPathsAreUri;
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.jdt.ls.debug.adapter.impl.ISettingProvider#setClientPathsAreUri(boolean)
     */
    @Override
    public void setClientPathsAreUri(boolean clientPathsAreUri) {
        this.clientPathsAreUri = clientPathsAreUri;
    }
}
