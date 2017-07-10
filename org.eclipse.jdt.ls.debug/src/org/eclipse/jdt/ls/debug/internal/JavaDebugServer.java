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

package org.eclipse.jdt.ls.debug.internal;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

import org.eclipse.jdt.ls.core.internal.debug.IDebugServer;
import org.eclipse.jdt.ls.debug.internal.adapter.DebugSession;
import org.eclipse.jdt.ls.debug.internal.adapter.DispatcherProtocol;
import org.eclipse.jdt.ls.debug.internal.adapter.DispatcherProtocol.IResponder;
import org.eclipse.jdt.ls.debug.internal.adapter.Events.DebugEvent;
import org.eclipse.jdt.ls.debug.internal.adapter.JsonUtils;
import org.eclipse.jdt.ls.debug.internal.adapter.Results.DebugResult;
import org.eclipse.jdt.ls.debug.internal.adapter.Results.ErrorResponseBody;
import org.eclipse.jdt.ls.debug.internal.adapter.Types.Message;
import org.eclipse.jdt.ls.debug.internal.core.log.Logger;

import com.google.gson.JsonObject;

public class JavaDebugServer implements IDebugServer {
	private ServerSocket serverSocket = null;
	private DebugSession debugSession = null;

	public JavaDebugServer() {
		try {
			this.serverSocket = new ServerSocket(0);
		} catch (IOException e) {
			Logger.logError(e);
		}
	}

	@Override
	public int getPort() {
		if (this.serverSocket != null) {
			return this.serverSocket.getLocalPort();
		}
		return -1;
	}

	@Override
	public void execute() {
		if (this.serverSocket != null) {
			// Execute eventLoop in a new thread.
			new Thread(new Runnable() {

				@Override
				public void run() {
					int serverPort = -1;
					try {
						// It's blocking here to waiting for incoming socket connection. 
						Socket clientSocket = serverSocket.accept();
						serverPort =  serverSocket.getLocalPort();
						Logger.log("Start debugserver on socket port " + serverPort);
						BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
						PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);

						// eventLoop
						DispatcherProtocol dispatcher = new DispatcherProtocol(in, out);
						dispatcher.eventLoop(new DispatcherProtocol.IHandler() {

							@Override
							public void run(String command, JsonObject arguments, IResponder responder) {
								if (arguments == null) {
									arguments = new JsonObject();
								}

								try {
									if (command.equals("initialize")) {
										String adapterID = JsonUtils.getString(arguments, "adapterID", "");
										debugSession = new DebugSession(true, false, responder);
										if (debugSession == null) {
											responder.setBody(new ErrorResponseBody(new Message(1103,
													"initialize: can't create debug session for adapter '{_id}'",
													JsonUtils.fromJson("{ _id: " + adapterID + "}", JsonObject.class))));
										}
									}

									if (debugSession != null) {
										DebugResult dr = debugSession.Dispatch(command, arguments);
										if (dr != null) {
											responder.setBody(dr.body);

											if (dr.events != null) {
												for (DebugEvent e : dr.events) {
													responder.addEvent(e.type, e);
												}
											}
										}
									}

									if (command.equals("disconnect")) {
										dispatcher.stop();
									}
								} catch (Exception e) {
									Logger.logError(e);
								}
							}
						});
					} catch (IOException e1) {
						Logger.logError(e1);
					} finally {
						if (serverSocket != null) {
							try {
								serverSocket.close();
							} catch (IOException e) {
								Logger.logError(e);
							}
						}
					}
					Logger.log("Close debugserver socket port " + serverPort);
				}

			}).start();
		}
	}

}

