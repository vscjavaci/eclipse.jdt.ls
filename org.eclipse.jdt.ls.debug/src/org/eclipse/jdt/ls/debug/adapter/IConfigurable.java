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

package org.eclipse.jdt.ls.debug.adapter;

import java.util.HashMap;
import java.util.Map;

public interface IConfigurable {
    /**
     * Initialize this configurable object.
     * @param options the options
     */
    default void initialize(Map<String, Object> options) {

    }

    /**
     * Get the default options for this configurable object.
     *
     * @return The default options.
     */
    default Map<String, Object> getDefaultOptions() {
        return new HashMap<>();
    }
}
