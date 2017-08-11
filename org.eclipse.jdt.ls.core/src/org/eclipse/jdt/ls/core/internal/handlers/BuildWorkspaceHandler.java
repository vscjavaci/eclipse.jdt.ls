/*******************************************************************************
 * Copyright (c) 2017 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Microsoft Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ls.core.internal.handlers;

import static org.eclipse.jdt.ls.core.internal.JavaLanguageServerPlugin.logException;
import static org.eclipse.lsp4j.jsonrpc.CompletableFutures.computeAsync;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.ls.core.internal.BuildWorkspaceResult;
import org.eclipse.jdt.ls.core.internal.BuildWorkspaceStatus;
import org.eclipse.jdt.ls.core.internal.SharedASTProvider;

/**
 * @author xuzho
 *
 */
public class BuildWorkspaceHandler {
	private SharedASTProvider sharedASTProvider;

	public BuildWorkspaceHandler() {
		this.sharedASTProvider = SharedASTProvider.getInstance();
	}

	public CompletableFuture<BuildWorkspaceResult> buildWorkspace() {
		return computeAsync((cc) -> {
			try {
				ResourcesPlugin.getWorkspace().build(IncrementalProjectBuilder.INCREMENTAL_BUILD, new NullProgressMonitor());
				List<IProblem> errors = getBuildErrors();
				if (errors.isEmpty()) {
					return new BuildWorkspaceResult(BuildWorkspaceStatus.SUCCESS);
				} else {
					return new BuildWorkspaceResult(BuildWorkspaceStatus.FAILURE);
				}
			} catch (CoreException e) {
				logException("Failed to build workspace.", e);
				return new BuildWorkspaceResult(BuildWorkspaceStatus.FAILURE);
			}
		});
	}

	private List<IProblem> getBuildErrors() {
		this.sharedASTProvider.invalidateAll();
		ArrayList<IProblem> errors = new ArrayList<>();
		List<ICompilationUnit> toValidate = Arrays.asList(JavaCore.getWorkingCopies(null));
		List<CompilationUnit> astRoots = this.sharedASTProvider.getASTs(toValidate, new NullProgressMonitor());
		for (CompilationUnit astRoot : astRoots) {
			for (IProblem problem : astRoot.getProblems()) {
				if (problem.isError()) {
					errors.add(problem);
				}
			}
		}
		return errors;
	}
}
