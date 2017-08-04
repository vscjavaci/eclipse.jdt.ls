package org.eclipse.jdt.ls.debug.adapter.jdt;

import org.eclipse.jdt.ls.debug.adapter.IProviderContext;
import org.eclipse.jdt.ls.debug.adapter.ISourceLookUpProvider;
import org.eclipse.jdt.ls.debug.adapter.IVirtualMachineManagerProvider;

public class JDTProviderContext implements IProviderContext {

    private ISourceLookUpProvider sourceLookUpProvider;
    private IVirtualMachineManagerProvider virtualMachineManagerProvider;
    
    @Override
    public ISourceLookUpProvider getSourceLookUpProvider() {
        return this.sourceLookUpProvider;
    }

    @Override
    public IVirtualMachineManagerProvider getVirtualMachineManagerProvider() {
        return this.virtualMachineManagerProvider;
    }

    public JDTProviderContext() {
        this.sourceLookUpProvider = new JDTSourceLookUpProvider();
        this.virtualMachineManagerProvider = new JDTVirtualMachineManagerProvider();
    }
}
