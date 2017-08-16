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

package org.eclipse.jdt.ls.debug.internal;

import org.eclipse.jdt.ls.debug.internal.JavaDebuggerServerPlugin;
import org.apache.logging.log4j.LogManager;

public class Logger {
    private static final org.apache.logging.log4j.Logger inner = LogManager.getLogger(JavaDebuggerServerPlugin.class.getName());

    /**
     * Log the info message with the plugin's logger.
     * @param message
     *               message to log
     */
    public static void logInfo(String message) {
        JavaDebuggerServerPlugin.logInfo(message);
        inner.info(message);
    }

    public static void logException(String message, Exception e) {
        JavaDebuggerServerPlugin.logException(message, e);
        inner.error(message, e);
    }

    public static void logError(String error) {
        JavaDebuggerServerPlugin.logError(error);
        inner.error(error);
    }
    
    public static void logWarn(String warn) {
        inner.warn(warn);
    }
}
