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

import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.ArrayReference;
import com.sun.jdi.ArrayType;
import com.sun.jdi.ClassObjectReference;
import com.sun.jdi.Field;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.PrimitiveValue;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.StackFrame;
import com.sun.jdi.StringReference;
import com.sun.jdi.Type;
import com.sun.jdi.Value;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.VoidValue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.ls.debug.JavaVariable;

/**
 * The variable utility for retrieving display name of the type.
 */
public final class VariableUtils {
    /**
     * Test whether the value has referenced objects.
     * @param value the value.
     * @param includeStatic the static flag.
     * @return true if this value is reference objects.
     */
    public static boolean hasReferences(Value value, boolean includeStatic) {
        if (value == null) {
            return false;
        }
        Type type = value.type();
        if (type instanceof ArrayType) {
            return ((ArrayReference) value).length() > 0;
        }
        if (value.type() instanceof ReferenceType) {
            return ((ReferenceType) type).allFields().stream()
                    .filter(t -> includeStatic ? true : !t.isStatic()).toArray().length > 0;
        }
        return false;
    }

    /**
     * Get display text of the value.
     * @param value the value.
     * @param showQualified the flag to show qualified name
     * @return the display text of the value
     */
    public static String valueToString(Value value, boolean showQualified) {
        if (value == null || value instanceof VoidValue) {
            return "null";
        }

        if (value instanceof PrimitiveValue) {
            return value.toString();
        }
        Type type = value.type();
        StringBuilder sb = new StringBuilder();
        if (value instanceof ArrayReference) {
            int arrayDimension = getArrayDimension(type);
            String arrayElementSignature = type.signature().substring(arrayDimension);
            if (TypeUtils.isObjectTag(arrayElementSignature.charAt(0))) {
                VirtualMachine vm = type.virtualMachine();
                ReferenceType elementType = TypeUtils.resolveReferenceType(vm,
                        ((ReferenceType) type).classLoader(),
                        TypeUtils.getTypeName(arrayElementSignature));
                sb.append(TypeUtils.getDisplayName(elementType, showQualified));
            } else {
                sb.append(TypeUtils.getTypeName(arrayElementSignature));
            }
            int length = getArrayLength(value);

            sb.append('[');
            sb.append(length);
            sb.append(']');
            for (int i = 1; i < arrayDimension; i++) {
                sb.append("[]");
            }

        } else {
            if (value instanceof StringReference) {
                sb.append("\"");
                sb.append(((StringReference) value).value());
                sb.append("\"");
            } else {
                sb.append(TypeUtils.getDisplayName(type, showQualified));
            }

            if (value instanceof ClassObjectReference) {
                sb.append('(');
                sb.append(TypeUtils.getDisplayName(((ClassObjectReference) value).reflectedType(),
                        showQualified));
                sb.append(')');
                sb.append(' ');
            }
        }
        sb.append(" (id=");
        sb.append(((ObjectReference) value).uniqueID());
        sb.append(")");

        return sb.toString();
    }

    /**
     * Get display name of type of the value.
     * @param value the value
     * @param showQualifiedName the flag to show qualified name
     * @return display name of type of the value.
     */
    public static String valueTypeString(Value value, boolean showQualifiedName) {
        if (value == null || value instanceof VoidValue) {
            return "null";
        }
        return value.type().toString();
    }

    /**
     * Get the variables of the object.
     * @param obj the object
     * @param includeStatic whether or not to include static fields.
     * @return the variable list
     * @throws AbsentInformationException when there is any error in retrieving information
     */
    public static List<JavaVariable> listFieldVariables(ObjectReference obj, boolean includeStatic)
            throws AbsentInformationException {
        List<JavaVariable> res = new ArrayList<JavaVariable>();
        Type type = obj.type();
        if (type instanceof ArrayType) {
            int arrayIndex = 0;
            for (Value elementValue : ((ArrayReference) obj).getValues()) {
                JavaVariable ele = new JavaVariable(String.valueOf(arrayIndex), elementValue);
                ele.arrayIndex = arrayIndex;
                arrayIndex++;
                res.add(ele);
            }
            return res;
        }
        List<Field> fields = obj.referenceType().allFields().stream()
                .filter(t -> includeStatic || !t.isStatic()).collect(Collectors.toList());

        Collections.sort(fields, new Comparator<Field>() {
            @Override
            public int compare(Field a, Field b) {
                return sortChildren(a, b);
            }

            private int sortChildren(Field v1, Field v2) {
                try {
                    boolean v1isStatic = v1.isStatic();
                    boolean v2isStatic = v2.isStatic();
                    if (v1isStatic && !v2isStatic) {
                        return -1;
                    }
                    if (!v1isStatic && v2isStatic) {
                        return 1;
                    }
                    return v1.name().compareToIgnoreCase(v2.name());
                } catch (Exception de) {
                    de.printStackTrace();
                    return -1;
                }
            }
        });

        fields.forEach(f -> {
            JavaVariable var = new JavaVariable(f.name(), obj.getValue(f));
            var.field = f;
            res.add(var);
        });
        return res;
    }

    /**
     * Get the local variables of an stack frame.
     *
     * @param stackFrame the stack frame
     * @return local variable list
     * @throws AbsentInformationException when there is any error in retrieving information
     */
    public static List<JavaVariable> listLocalVariables(StackFrame stackFrame)
            throws AbsentInformationException {
        List<JavaVariable> res = new ArrayList<JavaVariable>();
        try {
            stackFrame.visibleVariables().stream().forEach(localVariable -> {
                JavaVariable var = new JavaVariable(localVariable.name(),
                        stackFrame.getValue(localVariable));
                var.local = true;
                res.add(var);
            });
        } catch (AbsentInformationException ex) {
            int argId = 0;
            for (Value argValue : stackFrame.getArgumentValues()) {
                JavaVariable var = new JavaVariable("arg" + argId, argValue);
                var.argumentIndex = argId++;
                res.add(var);
            }
        }
        return res;
    }

    /**
     * Get the this variable of an stack frame.
     *
     * @param stackFrame the stack frame
     * @return this variable
     */
    public static JavaVariable getThisVariable(StackFrame stackFrame) {
        ObjectReference thisObject = stackFrame.thisObject();
        if (thisObject == null) {
            return null;
        }
        JavaVariable thisVar = new JavaVariable("this", thisObject);
        return thisVar;
    }

    /**
     * Get the static variable of an stack frame.
     *
     * @param stackFrame the stack frame
     * @return the static variable of an stack frame.
     */
    public static List<JavaVariable> listStaticVariables(StackFrame stackFrame) {
        List<JavaVariable> res = new ArrayList<JavaVariable>();
        ReferenceType type = stackFrame.location().declaringType();
        type.allFields().stream().filter(t -> t.isStatic()).forEach(field -> {
            JavaVariable staticVar = new JavaVariable(field.name(), type.getValue(field));
            staticVar.field = field;
            res.add(staticVar);
        });
        return res;
    }

    private VariableUtils() {

    }

    private static int getArrayLength(Value value) {
        return ((ArrayReference) value).length();
    }

    private static int getArrayDimension(Type type) {
        return StringUtils.countMatches(type.signature(), '[');
    }


}
