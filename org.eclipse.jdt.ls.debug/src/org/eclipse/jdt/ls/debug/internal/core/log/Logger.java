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

package org.eclipse.jdt.ls.debug.internal.core.log;

public class Logger {
	private static final boolean isDebug = Boolean.getBoolean("jdt.ls.debug");

	public static void log(String message) {
		if (isDebug) {
			System.out.println(message);			
		}
	}

	public static void logError(Exception e) {
		e.printStackTrace();
	}

}
