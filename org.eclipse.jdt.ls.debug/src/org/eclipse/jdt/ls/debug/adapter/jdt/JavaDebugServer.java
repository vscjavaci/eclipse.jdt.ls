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

package org.eclipse.jdt.ls.debug.adapter.jdt;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

import org.eclipse.jdt.ls.core.debug.IDebugServer;
import org.eclipse.jdt.ls.debug.adapter.ProtocolServer;
import org.eclipse.jdt.ls.debug.internal.Logger;

public class JavaDebugServer implements IDebugServer {
    private static SingletonDebugServer singletonServer;

    private static synchronized void createIfNotExist() {
        if (singletonServer == null) {
            singletonServer = new SingletonDebugServer();
        }
    }

    public JavaDebugServer() {
        createIfNotExist();
    }

    @Override
    public int getPort() {
        if (singletonServer != null) {
            return singletonServer.getPort();
        }
        return -1;
    }

    @Override
    public void start() {
        if (singletonServer != null) {
            singletonServer.start();
        }
    }

    @Override
    public void stop() {
        if (singletonServer != null) {
            singletonServer.stop();
        }
    }

    private static class SingletonDebugServer {
        private ServerSocket serverSocket = null;
        private boolean isStarted = false;

        /**
         * Constructs a SingletonDebugServer instance which will launch a ServerSocket to
         * listen for incoming socket connection.
         */
        SingletonDebugServer() {
            try {
                this.serverSocket = new ServerSocket(0, 1);
            } catch (IOException e) {
                Logger.logException("Create ServerSocket exception", e);
            }
        }

        synchronized int getPort() {
            if (this.serverSocket != null) {
                return this.serverSocket.getLocalPort();
            }
            return -1;
        }

        synchronized void start() {
            if (this.serverSocket != null && !this.isStarted) {
                this.isStarted = true;
                // Execute eventLoop in a new thread.
                new Thread(new Runnable() {

                    @Override
                    public void run() {
                        while (true) {
                            try {
                                // Allow server socket to service multiple clients at the same time.
                                // When a request comes in, create a connection thread to process it.
                                // Then the server goes back to listen for new connection request.
                                Socket connection = serverSocket.accept();
                                createConnectionThread(connection);
                            } catch (IOException e1) {
                                Logger.logException("Setup socket connection exception", e1);
                                closeServerSocket();
                                return;
                            }
                        }
                    }

                }, "Singleton Java Debug Server").start();
            }
        }

        synchronized void stop() {
            closeServerSocket();
        }

        private void closeServerSocket() {
            if (serverSocket != null) {
                try {
                    Logger.logInfo("Close debugserver socket port " + serverSocket.getLocalPort());
                    serverSocket.close();
                } catch (IOException e) {
                    Logger.logException("Close ServerSocket exception", e);
                }
            }
            serverSocket = null;
        }

        private void createConnectionThread(Socket connection) {
            new Thread(() -> {
                try {
                    BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    PrintWriter out = new PrintWriter(connection.getOutputStream(), true);
                    ProtocolServer protocolServer = new ProtocolServer(in, out, JdtProviderContextFactory.createProviderContext());
                    // protocol server will dispatch request and send response in a while-loop.
                    protocolServer.start();
                } catch (IOException e) {
                    Logger.logException("Socket connection exception", e);
                } finally {
                    Logger.logInfo("Debug connection closed");
                }
            }, "Debug Protocol Server").start();
        }
    }
}
