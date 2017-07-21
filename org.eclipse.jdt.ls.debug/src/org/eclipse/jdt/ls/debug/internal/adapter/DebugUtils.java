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

import java.io.File;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.TypeNameMatch;
import org.eclipse.jdt.core.search.TypeNameMatchRequestor;
import org.eclipse.jdt.internal.core.BinaryMember;
import org.eclipse.jdt.internal.core.SourceField;
import org.eclipse.jdt.internal.core.SourceMethod;
import org.eclipse.jdt.internal.core.SourceType;
import org.eclipse.jdt.ls.core.internal.JDTUtils;
import org.eclipse.jdt.ls.core.internal.handlers.JsonRpcHelpers;
import org.eclipse.jdt.ls.debug.internal.core.IBreakpoint;
import org.eclipse.jdt.ls.debug.internal.core.IBreakpointManager;
import org.eclipse.jdt.ls.debug.internal.core.breakpoints.FakeJavaLineBreakpoint;
import org.eclipse.jdt.ls.debug.internal.core.breakpoints.JavaLineBreakpoint;
import org.eclipse.jdt.ls.debug.internal.core.log.Logger;

import com.sun.jdi.Location;

public class DebugUtils {

    /**
     * According to source breakpoint, set breakpoint info to debugee VM.
     */
    public static List<Types.Breakpoint> addBreakpoint(Types.Source source, Types.SourceBreakpoint[] lines,
            boolean sourceModified, IBreakpointManager manager) {
        List<Types.Breakpoint> resBreakpoints = new ArrayList<>();
        List<IBreakpoint> javaBreakpoints = new ArrayList<>();
        ITypeRoot element = null;
        String sourcefile = source.path.replace("\\", "/");
        URI sourceUri = JDTUtils.toURI(sourcefile);
        if (sourceUri != null && sourceUri.getScheme().equals("jdt")) {
            element = JDTUtils.resolveTypeRoot(sourcefile);
        } else {
            Path path = Paths.get(source.path);
            sourcefile = path.normalize().toString();
            element = JDTUtils.resolveCompilationUnit(path.toUri());
        }
        
        for (Types.SourceBreakpoint bp : lines) {
            boolean valid = false;
            try {
                if (element != null) {
                    String fqn = null;
                    int offset = JsonRpcHelpers.toOffset(element.getBuffer(), bp.line, 0);
                    IJavaElement javaElement = element.getElementAt(offset);
                    if (javaElement instanceof SourceField || javaElement instanceof SourceMethod
                            || javaElement instanceof BinaryMember) {
                        IType type = ((IMember) javaElement).getDeclaringType();
                        fqn = type.getFullyQualifiedName();
                    } else if (javaElement instanceof SourceType) {
                        fqn = ((SourceType) javaElement).getFullyQualifiedName();
                    }
                    
                    if (fqn != null) {
                        javaBreakpoints.add(new JavaLineBreakpoint(fqn, bp.line, -1));
                        valid = true;
                    }
                }
            } catch (Exception e) {
                Logger.logException("Add breakpoint exception", e);
            }
            if (!valid) {
                javaBreakpoints.add(new FakeJavaLineBreakpoint(null, bp.line, -1));
            }
        }

        IBreakpoint[] added = manager.addBreakpoints(sourcefile, javaBreakpoints.toArray(new IBreakpoint[0]), sourceModified);
        for (IBreakpoint add : added) {
            resBreakpoints.add(new Types.Breakpoint(add.getId(), add.isVerified(), ((JavaLineBreakpoint) add).getLineNumber(), ""));
        }
        
        return resBreakpoints;
    }
    
    /**
     * Search the absolute path of the java file under the specified source path directory.
     * @param sourcePath
     *                  the project source path directories
     * @param sourceName
     *                  the java file path
     * @return the absolute file path
     */
    public static String sourceLookup(String[] sourcePath, String sourceName) {
        for (String path : sourcePath) {
            String fullpath = Paths.get(path, sourceName).toString();
            if (new File(fullpath).isFile()) {
                return fullpath;
            }
        }
        return null;
    }
    
    /**
     * Searches the source file from the full qualified name and returns the uri string.
     * @param project
     *               project instance
     * @param fqcn
     *               full qualified name
     * @return the uri string of source file
     */
    public static String getURI(IProject project, String fqcn) throws JavaModelException {
        IJavaSearchScope searchScope = project != null ? JDTUtils.createSearchScope(JavaCore.create(project)) : SearchEngine.createWorkspaceScope();
        
        int lastDot = fqcn.lastIndexOf(".");
        String packageName = lastDot > 0 ? fqcn.substring(0, lastDot) : "";
        String className = lastDot > 0 ? fqcn.substring(lastDot + 1) : fqcn;
        ClassUriExtractor extractor = new ClassUriExtractor();
        
        new SearchEngine().searchAllTypeNames(packageName.toCharArray(),SearchPattern.R_EXACT_MATCH,
                className.toCharArray(), SearchPattern.R_EXACT_MATCH,
                IJavaSearchConstants.TYPE,
                searchScope,
                extractor,
                IJavaSearchConstants.WAIT_UNTIL_READY_TO_SEARCH,
                new NullProgressMonitor());
        return extractor.uri;
    }
    
    /**
     * Searches the source file from the full qualified name and returns the uri string.
     * @param location
     *                jdi location
     * @return the uri string of source file
     */
    public static String getURI(Location location) throws JavaModelException {
        return getURI(null, location.declaringType().name());
    }
    
    /**
     * Gets the source contents of the uri path.
     * @param uri
     *           the uri string
     * @return the source contents
     */
    public static String getSource(String uri) {
        String source = null;
        IClassFile cf = JDTUtils.resolveClassFile(uri);
        if (cf != null) {
            try {
                IBuffer buffer = cf.getBuffer();
                if (buffer != null) {
                    source = buffer.getContents();
                }
                if (source == null) {
                    source = JDTUtils.disassemble(cf);
                }
            } catch (JavaModelException e) {
                e.printStackTrace();
            }
            if (source == null) {
                source = "";
            }
        }
        return source;
    }

    private static class ClassUriExtractor extends TypeNameMatchRequestor {

        String uri;

        @Override
        public void acceptTypeNameMatch(TypeNameMatch match) {
            if (match.getType().isBinary()) {
                uri = JDTUtils.getFileURI(match.getType().getClassFile());
            }  else {
                uri = JDTUtils.getFileURI(match.getType().getResource());
            }
        }
    }
}
