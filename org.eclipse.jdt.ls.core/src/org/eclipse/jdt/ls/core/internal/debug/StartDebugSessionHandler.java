/*******************************************************************************
 * Copyright (c) 2016-2017 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ls.core.internal.debug;

import java.util.concurrent.CompletableFuture;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.lsp4j.jsonrpc.CompletableFutures;

public class StartDebugSessionHandler {
	private static final String EXTENSIONPOINT_ID = "org.eclipse.jdt.ls.core.debugserver";

	public CompletableFuture<String> startDebugServer(String type) {
		return CompletableFutures.computeAsync(cm -> {
			if (type.equals("vscode.java.debugsession")) {
				// Find some Java DebugServer implementation and start it.
				IConfigurationElement[] elements = Platform.getExtensionRegistry().getConfigurationElementsFor(EXTENSIONPOINT_ID);
				try {
					for (IConfigurationElement e : elements) {
						if ("java".equals(e.getAttribute("type"))) {
							final IDebugServer debugServer = (IDebugServer) e.createExecutableExtension("class");
							debugServer.execute();
							return String.valueOf(debugServer.getPort());
						}
					}
				} catch (CoreException e1) {
					e1.printStackTrace();
				}
				return "";
			} else {
				return "";
			}
		});
	}

}