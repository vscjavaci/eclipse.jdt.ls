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
package org.eclipse.jdt.ls.core.internal;

import org.eclipse.lsp4j.jsonrpc.validation.NonNull;
import org.eclipse.xtext.xbase.lib.Pure;
import org.eclipse.xtext.xbase.lib.util.ToStringBuilder;

/*
 * Representation of a request that client send to server to resolve class path
 */
public class ClasspathResolveRequestParams {
	/*
	 *  the fully qualified name of mainClass
	 */
	@NonNull
	private String mainClass;

	/*
	 * the project name
	 */
	private String projectName;

	public ClasspathResolveRequestParams(final String mainClass) {
		this.mainClass = mainClass;
	}

	public ClasspathResolveRequestParams(final String mainClass, final String projectName) {
		this.mainClass = mainClass;
		this.projectName = projectName;
	}

	/**
	 * @return the mainClass
	 */
	@NonNull
	@Pure
	public String getMainClass() {
		return mainClass;
	}

	/**
	 * @param mainClass
	 *            the mainClass to set
	 */
	public void setMainClass(String mainClass) {
		this.mainClass = mainClass;
	}

	/**
	 * @return the projectName
	 */
	@Pure
	public String getProjectName() {
		return projectName;
	}

	/**
	 * @param projectName
	 *            the projectName to set
	 */
	public void setProjectName(String projectName) {
		this.projectName = projectName;
	}

	@Override
	@Pure
	public String toString() {
		ToStringBuilder b = new ToStringBuilder(this);
		b.add("mainClass", this.mainClass);
		b.add("projectName", this.projectName);
		return b.toString();
	}

	@Override
	@Pure
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		ClasspathResolveRequestParams other = (ClasspathResolveRequestParams) obj;
		if (this.mainClass == null) {
			if (other.mainClass != null) {
				return false;
			}
		} else if (!this.mainClass.equals(other.mainClass)) {
			return false;
		}
		if (this.projectName == null) {
			if (other.projectName != null) {
				return false;
			}
		} else if (!this.projectName.equals(other.projectName)) {
			return false;
		}
		return true;
	}

	@Override
	@Pure
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((this.mainClass == null) ? 0 : this.mainClass.hashCode());
		result = prime * result + ((this.projectName == null) ? 0 : this.projectName.hashCode());
		return result;
	}
}
