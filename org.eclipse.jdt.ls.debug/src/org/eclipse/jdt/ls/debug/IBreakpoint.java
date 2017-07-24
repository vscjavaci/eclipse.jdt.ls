package org.eclipse.jdt.ls.debug;

public interface IBreakpoint extends IDebugResource {
    String className();
    int lineNumber();

    int hitCount();
    void setHitCount(int hitCount);

    void install();
}
