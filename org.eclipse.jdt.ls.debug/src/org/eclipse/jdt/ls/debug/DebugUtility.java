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

package org.eclipse.jdt.ls.debug;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.ls.debug.internal.DebugSession;

import com.sun.jdi.Bootstrap;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.connect.Connector.Argument;
import com.sun.jdi.connect.IllegalConnectorArgumentsException;
import com.sun.jdi.connect.LaunchingConnector;
import com.sun.jdi.connect.VMStartException;

public class DebugUtility {
    static IDebugSession launch(String mainClass, List<String> classPaths)
            throws IOException, IllegalConnectorArgumentsException, VMStartException {
        List<LaunchingConnector> connectors = Bootstrap.virtualMachineManager().launchingConnectors();
        LaunchingConnector connector = connectors.get(0);

        Map<String, Argument> arguments = connector.defaultArguments();
        arguments.get("options").setValue("-cp " + String.join(",", classPaths));
        arguments.get("suspend").setValue("true");
        arguments.get("main").setValue(mainClass);

        return new DebugSession(connector.launch(arguments));
    }

    static IDebugSession attach(/* TODO: arguments? */) {
        throw new UnsupportedOperationException();
    }

    static void stepOver(ThreadReference thread) {
        throw new UnsupportedOperationException();
    }

    static void stepInto(ThreadReference thread) {
        throw new UnsupportedOperationException();
    }

    static void stepOut(ThreadReference thread) {
        throw new UnsupportedOperationException();
    }
}

