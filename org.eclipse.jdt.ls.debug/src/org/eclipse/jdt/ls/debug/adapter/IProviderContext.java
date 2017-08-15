package org.eclipse.jdt.ls.debug.adapter;

public interface IProviderContext {
    
    <T extends IProvider> T getProvider(Class<T> clazz);

    void registerProvider(Class<? extends IProvider> clazz, IProvider provider);

    ISourceLookUpProvider getSourceLookUpProvider();

    IVirtualMachineManagerProvider getVirtualMachineManagerProvider();
}
