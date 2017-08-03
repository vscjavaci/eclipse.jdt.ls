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

import com.sun.jdi.ClassLoaderReference;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.Type;
import com.sun.jdi.VirtualMachine;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;


/**
 * The type utility for retrieving display name of the type.
 */
public final class TypeUtils {
    static final char ARRAY = '[';
    static final char BYTE = 'B';
    static final char CHAR = 'C';
    static final char OBJECT = 'L';
    static final char FLOAT = 'F';
    static final char DOUBLE = 'D';
    static final char INT = 'I';
    static final char LONG = 'J';
    static final char SHORT = 'S';
    static final char VOID = 'V';
    static final char BOOLEAN = 'Z';
    static final char STRING = 's';
    static final char THREAD = 't';
    static final char THREAD_GROUP = 'g';
    static final char CLASS_LOADER = 'l';
    static final char CLASS_OBJECT = 'c';
    static final char SIGNATURE_ENDCLASS = ';';
    static final char GENERIC_START = '<';
    static final char GENERIC_END = '>';
    static final char COLON = ':';

    static Map<Character, String> primitiveTypeMap = new HashMap<Character, String>();

    static {
        primitiveTypeMap.put(BYTE, "byte");
        primitiveTypeMap.put(CHAR, "char");
        primitiveTypeMap.put(FLOAT, "float");
        primitiveTypeMap.put(DOUBLE, "double");
        primitiveTypeMap.put(INT, "int");
        primitiveTypeMap.put(LONG, "long");
        primitiveTypeMap.put(SHORT, "short");
        primitiveTypeMap.put(VOID, "void");
        primitiveTypeMap.put(BOOLEAN, "boolean");
    }

    /**
     * Returns the short name and signature of the JDI type, eg: <code>HashMap&lt;K,V&gt;</code> for
     * <code>java.util.HashMap</code>.
     *
     * @param type
     *            the JDI type for evaluate
     * @return the display name
     */
    public static String getDisplayName(Type type) {
        return getDisplayName(type, false);
    }

    /**
     * Returns the name and signature of the JDI type, eg: <code>java.util.HashMap&lt;K,V&gt;</code>
     * for <code>java.util.HashMap</code>.
     *
     * @param type
     *            the JDI type for evaluate
     * @param showQualified
     *            whether or not to show the fully qualified class name
     * @return the display name
     */
    public static String getDisplayName(Type type, boolean showQualified) {
        if (type == null) {
            throw new UnsupportedOperationException("Array type is not supported.");
        }
        String signature = type.signature();
        if (signature.charAt(0) == ARRAY) {
            throw new UnsupportedOperationException("Array type is not supported.");
        }
        if (isObjectTag(signature.charAt(0))) {
            return getDisplayNameForReferenceType((ReferenceType) type, showQualified);
        } else {
            String typeName = getTypeName(signature);
            return showQualified ? typeName : trimTypeName(typeName);
        }
    }


    /**
     * Find the reference type from JVM.
     *
     * @param vm
     *            the JVM.
     * @param expectedClassLoader
     *            the class loader of the class(because JVM many have two or more classes
     *            with same full qualified name)
     * @param typeName
     *            the full qualified class name
     * @return the reference type
     */
    public static ReferenceType resolveReferenceType(VirtualMachine vm,
            ClassLoaderReference expectedClassLoader, String typeName) {
        List<?> list = vm.classesByName(typeName);
        Iterator<?> iter = list.iterator();
        while (iter.hasNext()) {
            ReferenceType componentType = (ReferenceType) iter.next();
            ClassLoaderReference cl = componentType.classLoader();
            if ((cl == null) ? (expectedClassLoader == null) : (cl.equals(expectedClassLoader))) {
                return componentType;
            }
        }
        return null;
    }


    /**
     * Returns whether the type is an object type.
     *
     * @param tag
     *            the first character of the signature
     * @return whether the type is an object type.
     */
    public static boolean isObjectTag(char tag) {
        return (tag == OBJECT) || (tag == ARRAY) || (tag == STRING) || (tag == THREAD)
                || (tag == THREAD_GROUP) || (tag == CLASS_LOADER) || (tag == CLASS_OBJECT);
    }

    /**
     * Returns the fully qualified name or primitive string of the type.
     *
     * @param signature
     *            the signature of the type
     * @return whether the type is an object type.
     */
    public static String getTypeName(String signature) {
        StringBuilder sb = new StringBuilder();
        int arrayDimension = 0;
        while (signature.charAt(arrayDimension) == ARRAY) {
            arrayDimension++;
        }
        if (arrayDimension > 0) {
            sb.append(getTypeName(signature.substring(arrayDimension)));
        } else {
            if (OBJECT == signature.charAt(arrayDimension)) {
                int endClassSig = signature.indexOf(SIGNATURE_ENDCLASS, arrayDimension);
                String fqn = signature.substring(arrayDimension + 1, endClassSig);
                fqn = fqn.replace('/', '.');
                sb.append(fqn);
            } else {
                sb.append(primitiveTypeMap.get(signature.charAt(arrayDimension)));
            }
        }
        for (int i = 0; i < arrayDimension; i++) {
            sb.append("[]");
        }
        return sb.toString();
    }

    private TypeUtils() {

    }

    private static String[] getTypeParameters(String typeSignature)
            throws IllegalArgumentException {
        if (typeSignature.charAt(0) == GENERIC_START && typeSignature.indexOf(GENERIC_END) > 0) {
            String templateText = typeSignature.substring(1, typeSignature.indexOf(GENERIC_END));
            return Arrays.stream(StringUtils.split(templateText, SIGNATURE_ENDCLASS))
                    .map(template -> {
                        return StringUtils.split(template, COLON)[0];
                    }).toArray(String[]::new);
        }
        return new String[0];
    }

    private static String trimTypeName(String type) {
        if (type.indexOf('.') >= 0) {
            type = type.substring(type.lastIndexOf('.') + 1);
        }
        if (type.indexOf('/') >= 0) {
            type = type.substring(type.lastIndexOf('/') + 1);
        }
        return type;
    }

    private static String getDisplayNameForReferenceType(ReferenceType type,
            boolean showQuanlified) {
        String typeName = getTypeName(type.signature());
        // we need to get generic info
        StringBuilder sb = new StringBuilder();
        String genericSignature = type.genericSignature();
        if (StringUtils.isBlank(genericSignature)) {
            sb.append(showQuanlified ? typeName : trimTypeName(typeName));
        } else {
            sb.append(showQuanlified ? typeName : trimTypeName(typeName));
            String[] typeParameters = getTypeParameters(genericSignature);
            if (typeParameters.length > 0) {
                sb.append('<').append(typeParameters[0]);
                for (int i = 1; i < typeParameters.length; i++) {
                    sb.append(',').append(typeParameters[i]);
                }
                sb.append('>');
            }
        }
        return sb.toString();
    }
}
