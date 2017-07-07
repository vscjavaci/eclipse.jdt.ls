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

package org.eclipse.jdt.ls.debug.internal.adapter;

import java.util.ArrayList;
import java.util.List;

import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.Location;
import com.sun.jdi.Method;
import com.sun.jdi.StackFrame;

public class JDIFactory {
	private List<StackFrame> stackframes = new ArrayList<>();
//	private static ArrayList<IJavaVariable> localVars = new ArrayList<>();
//
	public synchronized Types.StackFrame createStackFrame(StackFrame stackFrame, String cwd) {
		int id = stackframes.size();
		Types.StackFrame newFrame = null;
		try {
			Location location = stackFrame.location();
			Method method = location.method();
			newFrame = new Types.StackFrame(id, method.name(),
					new Types.Source(cwd + "\\" + location.sourceName(), 0), location.lineNumber(), 0);
			stackframes.add(stackFrame);
		} catch (AbsentInformationException e) {
			e.printStackTrace();
		}

		return newFrame;
	}
//
//	public static synchronized JDIStackFrame getStackFrame(int id) {
//		if (id < 0 || id >= stackframes.size()) {
//			return null;
//		}
//		return stackframes.get(id);
//	}
//
//	public static synchronized int createVarReference(IJavaVariable var) {
//		localVars.add(var);
//		return localVars.size() - 1;
//	}
//
//	public static synchronized IJavaVariable getLocalVariable(int varReference) {
//		if (varReference < 0 || varReference >= localVars.size()) {
//			return null;
//		}
//		return localVars.get(varReference);
//	}

}
