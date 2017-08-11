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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import io.reactivex.functions.Consumer;
import io.reactivex.subjects.PublishSubject;

public class ProcessConsole {
    private Process process;
    private String name;
    private boolean isClosed = false;
    private PublishSubject<String> stdoutSubject = PublishSubject.<String>create();
    private PublishSubject<String> stderrSubject = PublishSubject.<String>create();
    
    public ProcessConsole(Process process) {
        this(process, "Process");
    }

    public ProcessConsole(Process process, String name) {
        this.process = process;
        this.name = name;
    }

    /**
     * Start two separate threads to monitor the messages from stdout and stderr streams of the target process.
     */
    public void start() {
        final int BUFFERSIZE = 4096;

        BufferedReader stdout = new BufferedReader(new InputStreamReader(process.getInputStream()));
        Thread stdoutThread = new Thread(this.name + " Stdout Handler") {
            public void run() {
                char[] buffer = new char[BUFFERSIZE];
                while (!isClosed) {
                    try {
                        int read = stdout.read(buffer, 0, BUFFERSIZE);
                        if (read == -1) {
                            stdoutSubject.onComplete();
                            break;
                        }
                        stdoutSubject.onNext(new String(buffer, 0, read));
                    } catch (IOException e) {
                        stdoutSubject.onComplete();
                        break;
                    }
                }
            }
        };
        stdoutThread.setDaemon(true);
        stdoutThread.start();

        BufferedReader stderr = new BufferedReader(new InputStreamReader(process.getErrorStream()));
        Thread stderrThread = new Thread(this.name + " Stderr Handler") {
            public void run() {
                char[] buffer = new char[BUFFERSIZE];
                while (!isClosed) {
                    try {
                        int read = stderr.read(buffer, 0, BUFFERSIZE);
                        if (read == -1) {
                            stderrSubject.onComplete();
                            break;
                        }
                        stderrSubject.onNext(new String(buffer, 0, read));
                    } catch (IOException e) {
                        stderrSubject.onComplete();
                        break;
                    }
                }
            }
        };
        stderrThread.setDaemon(true);
        stderrThread.start();
    }

    public void stop() {
        this.isClosed = true;
    }

    public void onStdout(Consumer<String> callback) {
        stdoutSubject.subscribe(callback);
    }

    public void onStderr(Consumer<String> callback) {
        stderrSubject.subscribe(callback);
    }
}
