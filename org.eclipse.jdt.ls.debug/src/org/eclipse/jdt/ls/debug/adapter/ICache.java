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

package org.eclipse.jdt.ls.debug.adapter;

import java.util.function.Function;

public interface ICache<K, V> {

    V put(K key, V value);

    V get(K key);

    V remove(K key);

    int size();

    void clear();

    V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction);
}
