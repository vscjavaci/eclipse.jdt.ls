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

import java.util.List;

public class CompositeLogger implements ILogger {

    private List<ILogger> loggers;
    
    /**
     *  constructor.
     *  @param loggers
     *                inner logger list.
     */
    public CompositeLogger(List<ILogger> loggers) {
        if (loggers == null) {
            throw new IllegalArgumentException("loggers is null");
        }
        this.loggers = loggers;
    }
    
    /**
     * register a logger.
     * @param logger
     *              the logger to register
     */
    public void registerLogger(ILogger logger) {
        if (logger == null) {
            throw new IllegalArgumentException("logger is null");
        }
        this.loggers.add(logger);
    }
    
    /**
     * unregister a logger.
     * @param logger
     *              the logger to unregister
     */
    public void unregisterLogger(ILogger logger) {
        if (logger == null) {
            throw new IllegalArgumentException("logger is null");
        }
        this.loggers.remove(logger);
    }
    
    @Override
    public void logInfo(String message) {
        for (ILogger p : loggers) {
            p.logInfo(message);
        }
    }

    @Override
    public void logWarn(String message) {
        for (ILogger p : loggers) {
            p.logWarn(message);
        }
    }

    @Override
    public void logError(String message) {
        for (ILogger p : loggers) {
            p.logError(message);
        }
    }

    @Override
    public void logException(String message, Exception e) {
        for (ILogger p : loggers) {
            p.logException(message, e);
        }
    }

}
