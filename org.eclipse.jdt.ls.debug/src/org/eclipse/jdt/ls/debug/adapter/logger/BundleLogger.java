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

import org.eclipse.jdt.ls.debug.internal.JavaDebuggerServerPlugin;

public class BundleLogger implements ILogger {

    @Override
    public void logInfo(String message) {
        JavaDebuggerServerPlugin.logInfo(message);
    }

    @Override
    public void logWarn(String message) {
        JavaDebuggerServerPlugin.logInfo(message);
    }

    @Override
    public void logError(String message) {
        JavaDebuggerServerPlugin.logError(message);
    }

    @Override
    public void logException(String message, Exception e) {
        JavaDebuggerServerPlugin.logException(message, e);
    }

}
