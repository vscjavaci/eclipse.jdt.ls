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

package org.eclipse.jdt.ls.debug.internal.core.impl;

import java.util.EventObject;

import org.eclipse.jdt.ls.debug.internal.core.EventType;
import org.eclipse.jdt.ls.debug.internal.core.IDebugEvent;

public class DebugEvent extends EventObject implements IDebugEvent {
	private static final long serialVersionUID = 1L;
	private EventType _kind;

	public DebugEvent(Object source, EventType kind) {
		super(source);
		_kind = kind;
	}

	@Override
	public EventType getKind() {
		return _kind;
	}
}
