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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public class LRUCache<K, V> implements ICache<K, V> {
    private static final float DEFAULT_LOAD_FACTOR = 0.75f;
    private LinkedHashMap<K, V> map;

    /**
     * Create a LUR cache with the max capacity.
     * @param cacheSize the max size of elements in this cache.
     */
    public LRUCache(int cacheSize) {
        if (cacheSize < 0) {
            throw new IllegalArgumentException("cacheSize is negative.");
        }
        int capacity = (int) Math.ceil(cacheSize / DEFAULT_LOAD_FACTOR) + 1;
        map = new LinkedHashMap<K, V>(capacity, DEFAULT_LOAD_FACTOR, true) {
            private static final long serialVersionUID = -7068164191168103891L;
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                return size() > cacheSize;
            }
        };
    }

    @Override
    public synchronized V put(K key, V value) {
         return map.put(key, value);
    }

    @Override
    public synchronized V get(K key) {
        return map.get(key);
    }

    @Override
    public synchronized V remove(K key) {
        return map.remove(key);
    }

    @Override
    public synchronized int size() {
        return map.size();
    }

    @Override
    public synchronized void clear() {
        map.clear();
    }


    @Override
    public synchronized V computeIfAbsent(K key,
                             Function<? super K, ? extends V> mappingFunction) {
        return map.computeIfAbsent(key, mappingFunction);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<K, V> entry : map.entrySet()) {
            sb.append(String.format("%s:%s ", entry.getKey(), entry.getValue()));
        }
        return sb.toString();
    }
}
