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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.eclipse.jdt.ls.debug.IBreakpoint;
import org.eclipse.jdt.ls.debug.internal.Logger;

public class BreakpointManager {
    /**
     * A collection of breakpoints registered with this manager.
     */
    private List<IBreakpoint> breakpoints;
    private HashMap<String, HashMap<String, IBreakpoint>> sourceToBreakpoints;

    /**
     * Constructor.
     */
    public BreakpointManager() {
        this.breakpoints = Collections.synchronizedList(new ArrayList<>(5));
        this.sourceToBreakpoints = new HashMap<>();
    }

    /**
     * Adds breakpoints to breakpoint manager.
     * Deletes all breakpoints that are no longer listed.
     * @param source
     *              source path of breakpoints
     * @param breakpoints
     *              full list of breakpoints that locates in this source file
     * @return the full breakpoint list that locates in the source file
     */
    public IBreakpoint[] addBreakpoints(String source, IBreakpoint[] breakpoints) {
        return addBreakpoints(source, breakpoints, false);
    }

    /**
     * Adds breakpoints to breakpoint manager.
     * Deletes all breakpoints that are no longer listed.
     * In the case of modified source, delete everything.
     * @param source
     *              source path of breakpoints
     * @param breakpoints
     *              full list of breakpoints that locates in this source file
     * @param sourceModified
     *              the source file are modified or not.
     * @return the full breakpoint list that locates in the source file
     */
    public IBreakpoint[] addBreakpoints(String source, IBreakpoint[] breakpoints, boolean sourceModified) {
        List<IBreakpoint> result = new ArrayList<>();
        HashMap<String, IBreakpoint> breakpointMap = this.sourceToBreakpoints.get(source);
        // When source file is modified, delete all previous added breakpoints.
        if (sourceModified && breakpointMap != null) {
            for (IBreakpoint bp : breakpointMap.values()) {
                try {
                    // Destroy the breakpoint on the debugee VM.
                    bp.close();
                } catch (Exception e) {
                    Logger.logException("Remove breakpoint exception", e);
                }
                this.breakpoints.remove(bp);
            }
            this.sourceToBreakpoints.put(source, null);
            breakpointMap = null;
        }
        if (breakpointMap == null) {
            breakpointMap = new HashMap<>();
            this.sourceToBreakpoints.put(source, breakpointMap);
        }

        // Compute the breakpoints that are newly added.
        List<IBreakpoint> toAdd = new ArrayList<>();
        HashMap<String, Boolean> visited = new HashMap<>();
        for (IBreakpoint breakpoint : breakpoints) {
            IBreakpoint existed = breakpointMap.get(String.valueOf(breakpoint.lineNumber()));
            if (existed != null) {
                result.add(existed);
                visited.put(String.valueOf(existed.lineNumber()), true);
                continue;
            } else {
                result.add(breakpoint);
            }
            toAdd.add(breakpoint);
        }

        // Compute the breakpoints that are no longer listed.
        List<IBreakpoint> toRemove = new ArrayList<>();
        for (IBreakpoint breakpoint : breakpointMap.values()) {
            if (!visited.containsKey(String.valueOf(breakpoint.lineNumber()))) {
                toRemove.add(breakpoint);
            }
        }

        removeBreakpoints(source, toRemove.toArray(new IBreakpoint[0]));
        internalAddBreakpoints(source, toAdd.toArray(new IBreakpoint[0]));
        
        return result.toArray(new IBreakpoint[0]);
    }

    private void internalAddBreakpoints(String source, IBreakpoint[] breakpoints) {
        HashMap<String, IBreakpoint> breakpointMap = this.sourceToBreakpoints.get(source);
        if (breakpointMap == null) {
            breakpointMap = new HashMap<>();
            this.sourceToBreakpoints.put(source, breakpointMap);
        }

        if (breakpoints != null && breakpoints.length > 0) {
            for (IBreakpoint breakpoint : breakpoints) {
                this.breakpoints.add(breakpoint);
                breakpointMap.put(String.valueOf(breakpoint.lineNumber()), breakpoint);
            }
        }
    }

    /**
     * Removes the specified breakpoints from breakpoint manager.
     */
    public void removeBreakpoints(String source, IBreakpoint[] breakpoints) {
        HashMap<String, IBreakpoint> breakpointMap = this.sourceToBreakpoints.get(source);
        if (breakpointMap == null || breakpointMap.isEmpty() || breakpoints.length == 0) {
            return ;
        }

        for (IBreakpoint breakpoint : breakpoints) {
            if (this.breakpoints.contains(breakpoint)) {
                try {
                    // Destroy the breakpoint on the debugee VM.
                    breakpoint.close();
                    this.breakpoints.remove(breakpoint);
                    breakpointMap.remove(String.valueOf(breakpoint.lineNumber()));
                } catch (Exception e) {
                    Logger.logException("Remove breakpoint exception", e);
                }
            }
        }
    }

    public IBreakpoint[] getBreakpoints() {
        return this.breakpoints.toArray(new IBreakpoint[0]);
    }

    /**
     * Gets the registered breakpoints at the source file.
     */
    public IBreakpoint[] getBreakpoints(String source) {
        HashMap<String, IBreakpoint> breakpointMap = this.sourceToBreakpoints.get(source);
        if (breakpointMap == null) {
            return new IBreakpoint[0];
        }
        return breakpointMap.values().toArray(new IBreakpoint[0]);
    }

}
