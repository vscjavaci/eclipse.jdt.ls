package org.eclipse.jdt.ls.debug.adapter;

public interface IProviderAdapter {
    
    <T extends IProvider> T getProvider(Class<T> clazz);

    void registerProvider(Class<? extends IProvider> clazz, IProvider provider);
}
