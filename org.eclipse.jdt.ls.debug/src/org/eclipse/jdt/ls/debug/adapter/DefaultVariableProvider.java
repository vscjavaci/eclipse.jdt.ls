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

/**
 * The default provider for retrieving display name of the type.
 */
public final class DefaultVariableProvider implements IVariableProvider {
    private static final String NULL = "null";
    private static final String SQUARE_BRACKET = "[]";
    private static final char LEFT_SQUARE_BRACKET = '[';
    private static final char RIGHT_SQUARE_BRACKET = ']';
    private static final char LEFT_BRACE = '(';
    private static final char RIGHT_BRACE = ')';
    private static final char QUOTE = '\"';
    private static final char SPACE = ' ';
    private static final int DEFAULT_MAX_STRING_LENGTH = 100;
    private boolean includeStatic;
    private boolean showQualified;
    private int maxStringLength = DEFAULT_MAX_STRING_LENGTH;

    /**
     * Creates a default variable provider.
     *
     * @param includeStatic whether to show static variables
     * @param showQualified the flag to show qualified name
     */
    public DefaultVariableProvider(boolean includeStatic, boolean showQualified) {
        this.includeStatic = includeStatic;
        this.showQualified = showQualified;
    }

    /**
     * Set the max displayed string length for <code>String</code> objects.
     *
     * @param maxStringLength the max displayed string length
     */
    public void setMaxStringLength(int maxStringLength) {
        this.maxStringLength = maxStringLength;
    }

    /**
     * Test whether the value has referenced objects.
     *
     * @param value the value.
     * @return true if this value is reference objects.
     */
    @Override
    public boolean hasReferences(Value value) {
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
     *
     * @param value the value.
     * @return the display text of the value
     */
    @Override
    public String valueToString(Value value) {
        if (value == null || value instanceof VoidValue) {
            return NULL;
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
                        TypeUtils.getTypeName(arrayElementSignature, true));
                if (elementType == null) {
                    // type has not been loaded occurred while retrieving component type of array.
                    sb.append(TypeUtils.getTypeName(arrayElementSignature, showQualified));
                } else {
                sb.append(TypeUtils.getDisplayName(elementType, showQualified));
                }
            } else {
                sb.append(TypeUtils.getTypeName(arrayElementSignature, showQualified));
            }
            int length = getArrayLength(value);

            sb.append(LEFT_SQUARE_BRACKET);
            sb.append(length);
            sb.append(RIGHT_SQUARE_BRACKET);
            for (int i = 1; i < arrayDimension; i++) {
                sb.append(SQUARE_BRACKET);
            }

        } else {
            if (value instanceof StringReference) {
                sb.append(QUOTE);
                sb.append(StringUtils.abbreviate(((StringReference) value).value(),
                        this.maxStringLength));
                sb.append(QUOTE);
            } else {
                sb.append(TypeUtils.getDisplayName(type, showQualified));
            }

            if (value instanceof ClassObjectReference) {
                sb.append(LEFT_BRACE);
                sb.append(TypeUtils.getDisplayName(((ClassObjectReference) value).reflectedType(),
                        showQualified));
                sb.append(RIGHT_BRACE);
                sb.append(SPACE);
            }
        }
        sb.append(" (id=");
        sb.append(((ObjectReference) value).uniqueID());
        sb.append(RIGHT_BRACE);

        return sb.toString();
    }

    /**
     * Get display name of type of the value.
     * @param value the value
     * @return display name of type of the value.
     */
    @Override
    public String valueTypeString(Value value) {
        if (value == null || value instanceof VoidValue) {
            return NULL;
        }
        return value.type().toString();
    }

    /**
     * Get the variables of the object.
     *
     * @param obj the object
     * @return the variable list
     * @throws AbsentInformationException when there is any error in retrieving information
     */
    @Override
    public List<JavaVariable> listFieldVariables(ObjectReference obj)
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
     * Get the variables of the object with pagination.
     *
     * @param obj the object
     * @param start the start of the pagination
     * @param count the number of variables needed
     * @return the variable list
     * @throws AbsentInformationException when there is any error in retrieving information
     */
    @Override
    public List<JavaVariable> listFieldVariables(ObjectReference obj, int start, int count)
            throws AbsentInformationException {
        List<JavaVariable> res = new ArrayList<JavaVariable>();
        Type type = obj.type();
        if (type instanceof ArrayType) {
            int arrayIndex = start;
            for (Value elementValue : ((ArrayReference) obj).getValues(start, count)) {
                JavaVariable ele = new JavaVariable(String.valueOf(arrayIndex), elementValue);
                ele.arrayIndex = arrayIndex;
                arrayIndex++;
                res.add(ele);
            }
            return res;
        }
        throw new UnsupportedOperationException("Only Array type is supported.");
    }

    /**
     * Get the local variables of an stack frame.
     *
     * @param stackFrame the stack frame
     * @return local variable list
     * @throws AbsentInformationException when there is any error in retrieving information
     */
    @Override
    public List<JavaVariable> listLocalVariables(StackFrame stackFrame)
            throws AbsentInformationException {
        List<JavaVariable> res = new ArrayList<JavaVariable>();
        try {
            stackFrame.visibleVariables().stream().forEach(localVariable -> {
                JavaVariable var = new JavaVariable(localVariable.name(),
                        stackFrame.getValue(localVariable));
                var.local = localVariable;
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
    @Override
    public JavaVariable getThisVariable(StackFrame stackFrame) {
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
    @Override
    public List<JavaVariable> listStaticVariables(StackFrame stackFrame) {
        List<JavaVariable> res = new ArrayList<JavaVariable>();
        ReferenceType type = stackFrame.location().declaringType();
        type.allFields().stream().filter(t -> t.isStatic()).forEach(field -> {
            JavaVariable staticVar = new JavaVariable(field.name(), type.getValue(field));
            staticVar.field = field;
            res.add(staticVar);
        });
        return res;
    }

    private static int getArrayLength(Value value) {
        return ((ArrayReference) value).length();
    }

    private static int getArrayDimension(Type type) {
        return StringUtils.countMatches(type.signature(), "[");
    }
}