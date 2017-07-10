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

import java.lang.reflect.Type;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

public class JsonUtils {
	private static final Gson gson = new Gson();
	
	public static <T> T fromJson(String json, Class<T> classOfT) throws JsonSyntaxException {
		return gson.fromJson(json, classOfT);
	}
	
	public static <T> T fromJson(String json, Type typeOfT) throws JsonSyntaxException {
		return gson.fromJson(json, typeOfT);
	}
	
	public static <T> T fromJson(JsonElement json, Class<T> classOfT) throws JsonSyntaxException {
		return gson.fromJson(json, classOfT);
	}
	
	public <T> T fromJson(JsonElement json, Type typeOfT) throws JsonSyntaxException {
		return gson.fromJson(json, typeOfT);
	}
	
	public static String toJson(Object src) {
		return gson.toJson(src);
	}
	
	public static String toJson(Object src, Type typeOfSrc) {
		return gson.toJson(src, typeOfSrc);
	}
	
	public static int getInt(JsonObject args, String property, int defaultValue) {
		try {
			return args.getAsInt();
		} catch (Exception e) {
			// ignore and return default value;
		}
		return defaultValue;
	}

	public static String getString(JsonObject args, String property, String defaultValue) {
		String value = null;
		try {
			JsonElement obj = args.get(property);
			value = obj.getAsString();
		} catch (Exception e) {
			// ignore and return default value;
		}
		if (value == null) {
			return defaultValue;
		}
		value = value.trim();
		if (value.length() == 0) {
			return defaultValue;
		}
		return value;
	}

	public static boolean getBoolean(JsonObject args, String property, boolean defaultValue) {
		try {
			JsonElement obj = args.get(property);
			return obj.getAsBoolean();
		} catch (Exception e) {
			// ignore and return default value;
		}
		return defaultValue;
	}
	
	public static String[] getStringArray(JsonElement args, String property) {
		if (args instanceof JsonArray) {
			JsonArray array = (JsonArray) args;
			int size = array.size();
			String[] result = new String[size];
			for (int i = 0; i < size; i++) {
				result[i] = array.get(i).getAsString();
			}
			return result;
		} else {
			return new String[0];
		}
	}
}
