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

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jdt.ls.debug.internal.adapter.Results.ErrorResponseBody;
import org.eclipse.jdt.ls.debug.internal.adapter.Results.InitializeResponseBody;
import org.eclipse.jdt.ls.debug.internal.core.log.Logger;

import com.google.gson.JsonObject;

public class DispatcherProtocol {
	private static int BUFFER_SIZE = 4096;
	private static String TWO_CRLF = "\r\n\r\n";
	private static Pattern CONTENT_LENGTH_MATCHER = Pattern.compile("Content-Length: (\\d+)");

	private Reader _reader;
	private Writer _writer;

	private CharBuffer _rawData;
	private boolean _terminateSession = false;
	private int _bodyLength = -1;
	private int _sequenceNumber = 1;

	private Object _lock = new Object();
	private boolean _isDispatchingData;
	private IHandler _handler;

	private ConcurrentLinkedQueue<Messages.DispatcherEvent> _eventQueue;

	public DispatcherProtocol(Reader reader, Writer writer) {
		this._reader = reader;
		this._writer = writer;
		this._bodyLength = -1;
		this._sequenceNumber = 1;
		this._rawData = new CharBuffer();
		this._eventQueue = new ConcurrentLinkedQueue<>();
	}

	public void eventLoop(IHandler handler) {
		this._handler = handler;

		char[] buffer = new char[BUFFER_SIZE];
		try {
			while (!this._terminateSession) {
				int read = this._reader.read(buffer, 0, BUFFER_SIZE);
				if (read == 0) {
					break;
				}

				if (read > 0) {
					this._rawData.append(buffer, read);
					this.processData();
				}
			}
		} catch (IOException e) {
			Logger.logError(e);
		}
	}

	public void stop() {
		this._terminateSession = true;
	}

	public void sendEvent(String eventType, Object body) {
		sendMessage(new Messages.DispatcherEvent(eventType, body));
	}

	public void sendEventLater(String eventType, Object body) {
		synchronized(this._lock) {
			if (this._isDispatchingData) {
				this._eventQueue.offer(new Messages.DispatcherEvent(eventType, body));
			} else {
				sendMessage(new Messages.DispatcherEvent(eventType, body));
			}
		}
	}

	private void processData() {
		while (true) {
			if (this._bodyLength >= 0) {
				if (this._rawData.length() >= this._bodyLength) {
					char[] buf = this._rawData.removeFirst(this._bodyLength);
					this._bodyLength = -1;
					dispatch(new String(buf));
				}
			} else {
				String body = this._rawData.getString();
				int idx = body.indexOf(TWO_CRLF);
				if (idx != -1) {
					Matcher matcher = CONTENT_LENGTH_MATCHER.matcher(body);
					if (matcher.find()) {
						this._bodyLength = Integer.parseInt(matcher.group(1));
						this._rawData.removeFirst(idx + TWO_CRLF.length());
						continue;
					}
				}
			}
			break;
		}
	}

	private void dispatch(String request) {
		try {
			Logger.log("\n[REQUEST]");
			Logger.log(request);
			Messages.DispatcherRequest dispatchRequest = JsonUtils.fromJson(request, Messages.DispatcherRequest.class);
			if (dispatchRequest.type.equals("request")) {
				if (this._handler != null) {
					synchronized(this._lock) {
						this._isDispatchingData = true;
					}
					int seq = dispatchRequest.seq;
					String command = dispatchRequest.command;
					JsonObject arguments = dispatchRequest.arguments != null ? dispatchRequest.arguments : new JsonObject();
					Messages.DispatcherResponse response = new Messages.DispatcherResponse(seq, command);
					DispatchResponder responder = new DispatchResponder(this, response);

					this._handler.run(command, arguments, responder);

					sendMessage(response);
				}
			}
		} finally {
			synchronized(this._lock) {
				this._isDispatchingData = false;
			}

			while (this._eventQueue.peek() != null) {
				sendMessage(this._eventQueue.poll());
			}
		}
	}

	private void sendMessage(Messages.DispatcherMessage message) {
		message.seq = this._sequenceNumber++;

		String jsonMessage = JsonUtils.toJson(message);
		char[] jsonBytes = jsonMessage.toCharArray();

		String header = String.format("Content-Length: %d%s", jsonBytes.length, TWO_CRLF);
		char[] headerBytes = header.toCharArray();

		char[] data = new char[headerBytes.length + jsonBytes.length];
		System.arraycopy(headerBytes, 0, data, 0, headerBytes.length);
		System.arraycopy(jsonBytes, 0, data, headerBytes.length, jsonBytes.length);

		try {
			Logger.log("\n[[RESPONSE]]");
			Logger.log(new String(data));
			this._writer.write(data, 0, data.length);
			this._writer.flush();
		} catch (IOException e) {
			Logger.logError(e);
		}
	}

	class CharBuffer {
		private char[] _buffer;

		public CharBuffer() {
			this._buffer = new char[0];
		}

		public int length() {
			return this._buffer.length;
		}

		public String getString() {
			return new String(this._buffer);
		}

		public void append(char[] b, int length) {
			char[] newBuffer = new char[this._buffer.length + length];
			System.arraycopy(_buffer, 0, newBuffer, 0, this._buffer.length);
			System.arraycopy(b, 0, newBuffer, this._buffer.length, length);
			this._buffer = newBuffer;
		}

		public char[] removeFirst(int n) {
			char[] b= new char[n];
			System.arraycopy(this._buffer, 0, b, 0, n);
			char[] newBuffer = new char[this._buffer.length - n];
			System.arraycopy(this._buffer, n, newBuffer, 0, this._buffer.length - n);
			this._buffer = newBuffer;
			return b;
		}
	}

	public static interface IResponder {
		void setBody(Object body);
		void addEvent(String type, Object body);
	}

	public static interface IHandler {
		public void run(String command, JsonObject arguments, IResponder responder);
	}

	static class DispatchResponder implements IResponder {
		private DispatcherProtocol _protocol;
		private Messages.DispatcherResponse _response;

		public DispatchResponder(DispatcherProtocol protocol, Messages.DispatcherResponse response) {
			this._protocol = protocol;
			this._response = response;
		}

		@Override
		public void setBody(Object body) {
			this._response.body = body;
			if (body instanceof ErrorResponseBody) {
				this._response.success = false;
				this._response.message = "Error response body";
			} else {
				this._response.success = true;
				if (body instanceof InitializeResponseBody) {
					this._response.body = ((InitializeResponseBody) body).body;
				}
			}
		}

		@Override
		public void addEvent(String type, Object body) {
			this._protocol.sendEventLater(type, body);
		}

	}
}
