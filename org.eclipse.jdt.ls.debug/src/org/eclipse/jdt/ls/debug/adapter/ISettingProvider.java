package org.eclipse.jdt.ls.debug.adapter;

public interface ISettingProvider extends IProvider {

    boolean isDebuggerLinesStartAt1();

    void setDebuggerLinesStartAt1(boolean debuggerLinesStartAt1);

    boolean isDebuggerPathsAreUri();

    void setDebuggerPathsAreUri(boolean debuggerPathsAreUri);

    boolean isClientLinesStartAt1();

    void setClientLinesStartAt1(boolean clientLinesStartAt1);

    boolean isClientPathsAreUri();

    void setClientPathsAreUri(boolean clientPathsAreUri);

}