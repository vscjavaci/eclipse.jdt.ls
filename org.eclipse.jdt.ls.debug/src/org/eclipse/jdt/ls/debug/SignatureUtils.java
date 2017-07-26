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

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.sun.jdi.ClassLoaderReference;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.Type;
import com.sun.jdi.VirtualMachine;

public abstract class SignatureUtils {
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

    /**
     * Returns the short name and signature of the JDI type, eg:
     * <code>HashMap&lt;K,V&gt;</code> for <code>java.util.HashMap</code>.
     * 
     * @param type
     *            the JDI type for evaluate
     * @return the display name
     */
    public static String getDisplayName(Type type) {
        return getDisplayName(type, false);
    }

    /**
     * Returns the name and signature of the JDI type, eg:
     * <code>java.util.HashMap&lt;K,V&gt;</code> for
     * <code>java.util.HashMap</code>.
     * 
     * @param type
     *            the JDI type for evaluate
     * @param showQuanlified
     *            whether or not to show the fully qualified class name
     * @return the display name
     */
    public static String getDisplayName(Type type, boolean showQuanlified) {
        String signature = type.signature();
        int arrayDimension = 0;
        while (signature.charAt(arrayDimension) == ARRAY) {
            arrayDimension++;
        }
        String nonArrayTypeName = getTypeNameWithoutGeneric(signature.substring(arrayDimension));
        StringBuilder sb = new StringBuilder();
        if (arrayDimension > 0) {
            if (isObjectTag(signature.charAt(arrayDimension))) {
                // if element of this array is reference type, we must resolve
                // the ReferenceType
                // from is name to get generic information
                VirtualMachine vm = type.virtualMachine();
                ReferenceType elementType = resolveReferenceType(vm, ((ReferenceType) type).classLoader(),
                        nonArrayTypeName);
                sb.append(getDisplayNameForObject(elementType, showQuanlified));
            } else {
                sb.append(showQuanlified ? nonArrayTypeName : trimTypeName(nonArrayTypeName));
            }

        } else {
            if (isObjectTag(signature.charAt(arrayDimension))) {
                sb.append(getDisplayNameForObject((ReferenceType) type, showQuanlified));
            } else {
                sb.append(showQuanlified ? nonArrayTypeName : trimTypeName(nonArrayTypeName));
            }
        }
        for (int i = 0; i < arrayDimension; i++) {
            sb.append("[]");
        }

        return sb.toString();
    }

    private static ReferenceType resolveReferenceType(VirtualMachine vm, ClassLoaderReference expectedClassLoader,
            String typeName) {
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

    private static String[] getTypeParameters(String typeSignature) throws IllegalArgumentException {
        if (typeSignature.charAt(0) == GENERIC_START && typeSignature.indexOf(GENERIC_END) > 0) {
            String templateText = typeSignature.substring(1, typeSignature.indexOf(GENERIC_END));
            return Arrays.stream(StringUtils.split(templateText, SIGNATURE_ENDCLASS)).map(template -> {
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

    private static String getDisplayNameForObject(ReferenceType type, boolean showQuanlified) {
        String typeName = getTypeNameWithoutGeneric(type.signature());
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

    private static boolean isObjectTag(char tag) {
        return (tag == OBJECT) || (tag == ARRAY) || (tag == STRING) || (tag == THREAD) || (tag == THREAD_GROUP)
                || (tag == CLASS_LOADER) || (tag == CLASS_OBJECT);
    }

    private static String getTypeNameWithoutGeneric(String signature) {
        int currentIndex = 0;
        StringBuilder sb = new StringBuilder();
        int arrayDimension = 0;
        while (signature.charAt(arrayDimension) == ARRAY) {
            arrayDimension++;
        }

        while (currentIndex < signature.length()) {
            char key = signature.charAt(currentIndex++);

            switch (key) {
            case BYTE:
                sb.append("byte");
                break;
            case CHAR:
                sb.append("char");
                break;

            case FLOAT:
                sb.append("float");
                break;

            case DOUBLE:
                sb.append("double");
                break;

            case INT:
                sb.append("int");
                break;

            case LONG:
                sb.append("long");
                break;

            case SHORT:
                sb.append("short");
                break;

            case BOOLEAN:
                sb.append("boolean");
                break;

            case VOID:
                sb.append("void");
                break;

            case OBJECT:
                int endClassSig = signature.indexOf(SIGNATURE_ENDCLASS, currentIndex);
                String fqn = signature.substring(currentIndex, endClassSig);
                fqn = fqn.replace('/', '.');
                currentIndex = endClassSig + 1;
                sb.append(fqn);
                break;
            default:
                throw new IllegalArgumentException("Invalid JNI signature character '" + key + "'");

            }
        }
        for (int i = 0; i < arrayDimension; i++) {
            sb.append("[]");
        }
        return sb.toString();
    }
}
