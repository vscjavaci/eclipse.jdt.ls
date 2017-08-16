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

package org.eclipse.jdt.ls.debug.adapter.logger;

public class LoggerWithFilter implements ILogger {

    private static boolean isVerbose = Boolean.getBoolean("jdt.ls.debug");
    private ILogger inner;
    
    public LoggerWithFilter(ILogger inner) {
        this.inner = inner;
    }

    @Override
    public void logInfo(String message) {
        if (isVerbose) {
            inner.logInfo(message);
        }
    }

    @Override
    public void logWarn(String message) {
        inner.logWarn(message);
    }

    @Override
    public void logError(String message) {
        inner.logError(message);
    }

    @Override
    public void logException(String message, Exception e) {
        inner.logException(message, e);
    }

}
