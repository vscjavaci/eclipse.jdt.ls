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

import java.util.HashMap;

/**
 *
 */
public class IdCollection<T> {
	private int _startId;
	private int _nextId;
	private HashMap<Integer, T> _idMap;
	
	public IdCollection() {
		this(1);
	}
	
	public IdCollection(int startId) {
		_startId = startId;
		_nextId = startId;
		_idMap = new HashMap<>();
	}
	
	public void reset() {
		_nextId = _startId;
		_idMap.clear();
	}
	
	public int create(T value) {
		int id = _nextId++;
		_idMap.put(id, value);
		return id;
		
	}
	
	public T get(int id) {
		return _idMap.get(id);
	}
	
	public T remove(int id) {
		return _idMap.remove(id);
	}
}
