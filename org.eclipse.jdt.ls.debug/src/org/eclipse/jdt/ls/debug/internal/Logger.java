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

import java.util.ArrayList;

import org.eclipse.jdt.ls.debug.adapter.logger.CompositeLogger;
import org.eclipse.jdt.ls.debug.adapter.logger.DefaultLogger;
import org.eclipse.jdt.ls.debug.adapter.logger.ILogger;

public class Logger {
    private static CompositeLogger provider;

    static {
        ArrayList<ILogger> list = new ArrayList<ILogger>();
        list.add(new DefaultLogger());
        provider = new CompositeLogger(list);
    }
    
    public static void registerLogger(ILogger logger) {
        provider.registerLogger(logger);
    }
    
    public static void unregisterLogger(ILogger logger) {
        provider.unregisterLogger(logger);
    }
    
    /**
     * Log the info message with the provider.
     * @param message
     *               message to log
     */
    public static void logInfo(String message) {
        provider.logInfo(message);
    }

    public static void logException(String message, Exception e) {
        provider.logException(message, e);
    }

    public static void logError(String error) {
        provider.logError(error);
    }
    
    public static void logWarn(String warn) {
        provider.logWarn(warn);
    }
}
