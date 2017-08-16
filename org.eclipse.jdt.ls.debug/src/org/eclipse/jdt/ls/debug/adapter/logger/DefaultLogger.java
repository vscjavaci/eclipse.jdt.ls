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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DefaultLogger implements ILogger {

    private static final Logger inner = LogManager.getLogger(DefaultLogger.class.getName());
    
    @Override
    public void logInfo(String message) {
        inner.info(message);
    }

    @Override
    public void logWarn(String message) {
        inner.warn(message);
    }

    @Override
    public void logError(String message) {
        inner.error(message);
    }

    @Override
    public void logException(String message, Exception e) {
        inner.error(message, e);
    }

}
