package org.eclipse.jdt.ls.debug.adapter;

public interface ISourceLookUpProvider extends IProvider {

    String[] getFullyQualifiedName(String uri, int[] lines, int[] columns);

    String getSourceFileURI(String fullyQualifiedName);

    String getSourceContents(String uri);
}
