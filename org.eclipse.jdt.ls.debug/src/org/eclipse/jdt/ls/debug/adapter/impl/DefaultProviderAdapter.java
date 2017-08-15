package org.eclipse.jdt.ls.debug.adapter.impl;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.ls.debug.adapter.IProvider;
import org.eclipse.jdt.ls.debug.adapter.IProviderAdapter;

public class DefaultProviderAdapter implements IProviderAdapter {

    private Map<Class<? extends IProvider>, IProvider> providerMap;
    
    public DefaultProviderAdapter() {
        providerMap = new HashMap<>();  
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public <T extends IProvider> T getProvider(Class<T> clazz) {
        if (!providerMap.containsKey(clazz)) {
            throw new IllegalArgumentException(String.format("%s has not been registered.", clazz.getName()));
        }
        return (T) providerMap.get(clazz);
    }
    
    @Override
    public void registerProvider(Class<? extends IProvider> clazz, IProvider provider) {
        if (providerMap.containsKey(clazz)) {
            throw new IllegalArgumentException(String.format("%s has already been registered.", clazz.getName()));
        }
        providerMap.put(clazz, provider);
    }
}
