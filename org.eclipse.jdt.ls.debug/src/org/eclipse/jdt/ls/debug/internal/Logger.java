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

import org.eclipse.jdt.ls.debug.adapter.logger.BundleLogger;
import org.eclipse.jdt.ls.debug.adapter.logger.CompositeLogger;
import org.eclipse.jdt.ls.debug.adapter.logger.DefaultLogger;
import org.eclipse.jdt.ls.debug.adapter.logger.ILogger;
import org.eclipse.jdt.ls.debug.adapter.logger.LoggerWithFilter;

public class Logger {
    private static ILogger provider;

    static {
        ArrayList<ILogger> list = new ArrayList<ILogger>();
        list.add(new DefaultLogger());
        list.add(new LoggerWithFilter(new BundleLogger()));
        provider = new CompositeLogger(list);
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
        provider.logInfo(warn);
    }
}
