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

import static org.eclipse.jdt.ls.debug.adapter.formatter.TypeIdentifiers.BOOLEAN;
import static org.eclipse.jdt.ls.debug.adapter.formatter.TypeIdentifiers.BYTE;
import static org.eclipse.jdt.ls.debug.adapter.formatter.TypeIdentifiers.CHAR;
import static org.eclipse.jdt.ls.debug.adapter.formatter.TypeIdentifiers.DOUBLE;
import static org.eclipse.jdt.ls.debug.adapter.formatter.TypeIdentifiers.FLOAT;
import static org.eclipse.jdt.ls.debug.adapter.formatter.TypeIdentifiers.INT;
import static org.eclipse.jdt.ls.debug.adapter.formatter.TypeIdentifiers.LONG;
import static org.eclipse.jdt.ls.debug.adapter.formatter.TypeIdentifiers.SHORT;
import static org.eclipse.jdt.ls.debug.adapter.formatter.TypeIdentifiers.STRING_SIGNATURE;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.ls.debug.adapter.formatter.IValueFormatter;
import org.eclipse.jdt.ls.debug.adapter.formatter.NumericFormatEnum;
import org.eclipse.jdt.ls.debug.adapter.formatter.NumericFormatter;
import org.eclipse.jdt.ls.debug.adapter.formatter.SimpleTypeFormatter;
import org.eclipse.jdt.ls.debug.internal.Logger;

import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.ArrayReference;
import com.sun.jdi.ArrayType;
import com.sun.jdi.ClassNotLoadedException;
import com.sun.jdi.ClassType;
import com.sun.jdi.Field;
import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.InvalidTypeException;
import com.sun.jdi.LocalVariable;
import com.sun.jdi.Location;
import com.sun.jdi.Method;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.StackFrame;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.Type;
import com.sun.jdi.TypeComponent;
import com.sun.jdi.Value;

public class VariableDebugAdapter {
    private boolean showStaticVariables = true;
    private boolean showQualified = false;
    private boolean hexFormat = true;
    private final DebugAdapter parent;
    private RecyclableObjectPool<ThreadReference, Object> objectPool;
    public IVariableProvider provider;
    private Map<String, Object> options;

    /**
     * A variable debug adapter for stacktrace/scopes/variables/setVariable.
     * 
     * @param debugAdapter
     *            the parent debug adapter this variable adapter works for.
     */
    public VariableDebugAdapter(DebugAdapter debugAdapter) {
        this.parent = debugAdapter;
        this.objectPool = new RecyclableObjectPool<>();
        this.provider = new DefaultVariableProvider(showStaticVariables);

        this.options = new HashMap<>();
        if (hexFormat) {
            options.put(NumericFormatter.NUMERIC_FORMAT_OPTION, NumericFormatEnum.HEX);
        }
        options.put(SimpleTypeFormatter.QUALIFIED_CLASS_NAME_OPTION, showQualified);
    }

    /**
     * Clear all the variable caches.
     */
    public void recyclableAllObject() {
        this.objectPool.removeAllObjects();
    }

    /**
     * Clear all the variable caches related to the specified thread.
     */
    public void recyclableThreads(ThreadReference thread) {
        this.objectPool.removeObjectsByOwner(thread);
    }

    Responses.ResponseBody stackTrace(Requests.StackTraceArguments arguments) {
        List<Types.StackFrame> result = new ArrayList<>();
        if (arguments.startFrame < 0 || arguments.levels < 0) {
            return new Responses.StackTraceResponseBody(result, 0);
        }
        ThreadReference thread = parent.getThread(arguments.threadId);
        int totalFrames = 0;
        if (thread != null) {
            try {
                totalFrames = thread.frameCount();
                if (totalFrames <= arguments.startFrame) {
                    return new Responses.StackTraceResponseBody(result, totalFrames);
                }
                try {
                    List<StackFrame> stackFrames = arguments.levels == 0
                            ? thread.frames(arguments.startFrame, totalFrames - arguments.startFrame)
                            : thread.frames(arguments.startFrame,
                                    Math.min(totalFrames - arguments.startFrame, arguments.levels));
                    for (int i = 0; i < arguments.levels; i++) {
                        StackFrame stackFrame = stackFrames.get(arguments.startFrame + i);
                        int frameId = this.objectPool.addObject(stackFrame.thread(), stackFrame);
                        Types.StackFrame clientStackFrame = convertDebuggerStackFrameToClient(stackFrame, frameId);
                        result.add(clientStackFrame);
                    }
                } catch (IndexOutOfBoundsException ex) {
                    // ignore if stack frames overflow
                    return new Responses.StackTraceResponseBody(result, totalFrames);
                }
            } catch (IncompatibleThreadStateException | AbsentInformationException | URISyntaxException e) {
                Logger.logException("DebugSession#stackTrace exception", e);
            }
        }
        return new Responses.StackTraceResponseBody(result, totalFrames);
    }

    Responses.ResponseBody scopes(Requests.ScopesArguments arguments) {
        List<Types.Scope> scopes = new ArrayList<>();
        StackFrame stackFrame = (StackFrame) this.objectPool.getObjectById(arguments.frameId);
        if (stackFrame == null) {
            return new Responses.ScopesResponseBody(scopes);
        }
        StackFrameScope localScope = new StackFrameScope();
        localScope.stackFrame = stackFrame;
        localScope.scope = "Local";

        scopes.add(
                new Types.Scope(localScope.scope, this.objectPool.addObject(stackFrame.thread(), localScope), false));

        return new Responses.ScopesResponseBody(scopes);
    }

    Responses.ResponseBody variables(Requests.VariablesArguments arguments) {
        List<Types.Variable> list = new ArrayList<>();
        List<Variable> variables;
        Object obj = this.objectPool.getObjectById(arguments.variablesReference);
        ThreadReference thread;
        try {
            if (obj instanceof StackFrameScope) {
                StackFrame frame = ((StackFrameScope) obj).stackFrame;
                thread = frame.thread();
                variables = this.provider.listLocalVariables(frame);
                Variable thisVariable = this.provider.getThisVariable(frame);
                if (thisVariable != null) {
                    variables.add(thisVariable);
                }
                if (showStaticVariables && frame.location().method().isStatic()) {
                    variables.addAll(this.provider.listStaticVariables(frame));
                }
            } else if (obj instanceof ThreadObjectReference) {
                ObjectReference currentObj = ((ThreadObjectReference) obj).object;
                thread = ((ThreadObjectReference) obj).thread;

                if (arguments.count > 0) {
                    variables = this.provider.listFieldVariables(currentObj, arguments.start, arguments.count);
                } else {
                    variables = this.provider.listFieldVariables(currentObj);
                }
            } else {
                return new Responses.ErrorResponseBody(parent.convertDebuggerMessageToClient(String
                        .format("VariablesRequest: Invalid variablesReference %d.", arguments.variablesReference)));
            }
        } catch (AbsentInformationException e) {
            Logger.logException("VariableDebugAdapter#variables exception", e);
            return new Responses.ErrorResponseBody(
                    parent.convertDebuggerMessageToClient("VariablesRequest: Variable information is not available."));
        }
        // find variable name duplicates
        Set<String> duplicateNames = getDuplicateNames(
                variables.stream().map(var -> var.name).collect(Collectors.toList()));
        Map<Variable, String> variableNameMap = new HashMap<>();
        if (!duplicateNames.isEmpty()) {
            Map<String, List<Variable>> duplicateVars = variables.stream()
                    .filter(var -> duplicateNames.contains(var.name))
                    .collect(Collectors.groupingBy(var -> var.name, Collectors.toList()));

            duplicateVars.forEach((k, duplicateVariables) -> {
                Set<String> declarationTypeNames = new HashSet<>();
                boolean declarationTypeNameConflict = false;
                // try use type formatter to resolve name conflict
                for (Variable javaVariable : duplicateVariables) {
                    String name;
                    Type declarationType = javaVariable.getDeclaringType();
                    if (declarationType != null) {
                        String declarationTypeName = this.provider.typeToString(declarationType, options);
                        name = String.format("%s (%s)", javaVariable.name, declarationTypeName);
                        if (!declarationTypeNames.add(name)) {
                            declarationTypeNameConflict = true;
                            break;
                        }
                        variableNameMap.put(javaVariable, name);
                    }

                }
                // if there are duplicate names on declaration types, use fully qualified name
                if (declarationTypeNameConflict) {
                    for (Variable javaVariable : duplicateVariables) {
                        Type declarationType = javaVariable.getDeclaringType();
                        if (declarationType != null) {
                            variableNameMap.put(javaVariable,
                                    String.format("%s (%s)", javaVariable.name, declarationType.name()));
                        }

                    }
                }
            });
        }
        for (Variable javaVariable : variables) {
            Value value = javaVariable.value;
            String name = javaVariable.name;
            if (variableNameMap.containsKey(javaVariable)) {
                name = variableNameMap.get(javaVariable);
            }
            int referenceId = getReferenceId(thread, value);
            Types.Variable typedVariables = new Types.Variable(name, this.provider.valueToString(value, options),
                    this.provider.typeToString(value == null ? null : value.type(), options), referenceId, null);
            if (javaVariable.value instanceof ArrayReference) {
                typedVariables.indexedVariables = ((ArrayReference) javaVariable.value).length();
            }
            list.add(typedVariables);
        }
        return new Responses.VariablesResponseBody(list);
    }

    Responses.ResponseBody evaluate(Requests.EvaluateArguments arguments) {
        return new Responses.ResponseBody();
    }

    Responses.ResponseBody setVariable(Requests.SetVariableArguments arguments) {
        Object obj = this.objectPool.getObjectById(arguments.variablesReference);
        ThreadReference thread;
        String name = arguments.name;
        Value newValue = null;
        String belongToClass = null;
        if (arguments.name.contains("(")) {
            name = arguments.name.substring(0, arguments.name.indexOf('(')).trim();
            belongToClass = arguments.name.substring(arguments.name.indexOf('(') + 1, arguments.name.indexOf(')'))
                    .trim();
        }
        try {
            if (obj instanceof StackFrameScope) {
                if (arguments.name.equals("this")) {
                    throw new UnsupportedOperationException("SetVariableRequest: 'This' variable cannot be changed.");
                }
                StackFrame frame = ((StackFrameScope) obj).stackFrame;
                thread = frame.thread();
                LocalVariable variable = frame.visibleVariableByName(name);
                if (StringUtils.isBlank(belongToClass) && variable != null) {
                    newValue = this.setFrameValue(frame, variable, arguments.value);
                } else {
                    if (showStaticVariables && frame.location().method().isStatic()) {
                        ReferenceType type = frame.location().declaringType();
                        if (StringUtils.isBlank(belongToClass)) {
                            Field field = type.fieldByName(name);
                            newValue = setStaticFieldValue(type, field, arguments.name, arguments.value);
                        } else {
                            if (frame.location().method().isStatic() && showStaticVariables) {
                                newValue = setFieldValueWithConflict(null, type.allFields(), name, belongToClass,
                                        arguments.value);
                            }
                        }

                    } else {
                        throw new UnsupportedOperationException(
                                String.format("SetVariableRequest: Variable %s cannot be found.", arguments.name));
                    }
                }
            } else if (obj instanceof ThreadObjectReference) {
                ObjectReference currentObj = ((ThreadObjectReference) obj).object;
                thread = ((ThreadObjectReference) obj).thread;
                if (currentObj instanceof ArrayReference) {
                    ArrayReference array = (ArrayReference) currentObj;
                    Type eleType = ((ArrayType) array.referenceType()).componentType();
                    newValue = setArrayValue(array, eleType, Integer.parseInt(arguments.name), arguments.value);
                } else {
                    if (StringUtils.isBlank(belongToClass)) {
                        Field field = currentObj.referenceType().fieldByName(name);
                        if (field != null) {
                            if (field.isStatic()) {
                                newValue = this.setStaticFieldValue(currentObj.referenceType(), field,
                                        arguments.name, arguments.value);
                            } else {
                                newValue = this.setObjectFieldValue(currentObj, field, arguments.name,
                                        arguments.value);
                            }
                        } else {
                            throw new IllegalArgumentException(
                                    String.format("SetVariableRequest: Variable %s cannot be found.", arguments.name));
                        }
                    } else {
                        newValue = setFieldValueWithConflict(currentObj, currentObj.referenceType().allFields(),
                                name, belongToClass, arguments.value);
                    }
                }
            } else {
                throw new IllegalArgumentException(
                        String.format("SetVariableRequest: Variable %s cannot be found.", arguments.name));
            }
        } catch (IllegalArgumentException | AbsentInformationException | InvalidTypeException
                | UnsupportedOperationException | ClassNotLoadedException e) {
            return new Responses.ErrorResponseBody(parent.convertDebuggerMessageToClient(e.getMessage()));
        }
        int referenceId = getReferenceId(thread, newValue);

        int indexedVariables = 0;
        if (newValue instanceof ArrayReference) {
            indexedVariables = ((ArrayReference) newValue).length();
        }
        return new Responses.SetVariablesResponseBody(
                this.provider.typeToString(newValue == null ? null : newValue.type(), options), // type
                this.provider.valueToString(newValue, options), // value,
                referenceId, indexedVariables);

    }

    private static Set<String> getDuplicateNames(Collection<String> list) {
        Set<String> result = new HashSet<>();
        Set<String> set = new HashSet<>();

        for (String item : list) {
            if (!set.contains(item)) {
                set.add(item);
            } else {
                result.add(item);
            }
        }
        return result;
    }

    private Types.StackFrame convertDebuggerStackFrameToClient(StackFrame stackFrame, int frameId)
            throws URISyntaxException, AbsentInformationException {
        Location location = stackFrame.location();
        Method method = location.method();
        Types.Source clientSource = parent.convertDebuggerSourceToClient(location);
        return new Types.StackFrame(frameId, method.name(), clientSource,
                parent.convertDebuggerLineToClient(location.lineNumber()), 0);
    }

    private Value setValueProxy(Type type, String value, SetValueFunction setValueFunc)
            throws ClassNotLoadedException, InvalidTypeException {
        IValueFormatter formatter = getFormatterForModification(type);
        Value newValue = formatter.valueOf(value, type, options);
        setValueFunc.apply(newValue);
        return newValue;
    }

    private IValueFormatter getFormatterForModification(Type type) {
        char signature0 = type.signature().charAt(0);

        if (signature0 == LONG || signature0 == INT || signature0 == SHORT || signature0 == BYTE || signature0 == FLOAT
                || signature0 == DOUBLE || signature0 == BOOLEAN || signature0 == CHAR
                || type.signature().equals(STRING_SIGNATURE)) {
            return this.provider.getValueFormatter(type, this.options);
        }
        throw new UnsupportedOperationException(String.format("Set value for type %s is not supported.", type.name()));
    }

    private Value setStaticFieldValue(Type declaringType, Field field, String name, String value)
            throws ClassNotLoadedException, InvalidTypeException {
        if (field.isFinal()) {
            throw new UnsupportedOperationException(
                    String.format("SetVariableRequest: Final field %s cannot be changed.", name));
        }
        if (!(declaringType instanceof ClassType)) {
            throw new UnsupportedOperationException(
                    String.format("SetVariableRequest: Field %s in interface cannot be changed.", name));
        }
        return setValueProxy(field.type(), value, newValue -> ((ClassType) declaringType).setValue(field, newValue));
    }

    private Value setFrameValue(StackFrame frame, LocalVariable localVariable, String value)
            throws ClassNotLoadedException, InvalidTypeException {
        return setValueProxy(localVariable.type(), value, newValue -> frame.setValue(localVariable, newValue));
    }

    private Value setObjectFieldValue(ObjectReference obj, Field field, String name, String value)
            throws ClassNotLoadedException, InvalidTypeException {
        if (field.isFinal()) {
            throw new UnsupportedOperationException(
                    String.format("SetVariableRequest: Final field %s cannot be changed.", name));
        }
        return setValueProxy(field.type(), value, newValue -> obj.setValue(field, newValue));
    }

    private Value setArrayValue(ArrayReference array, Type eleType, int index, String value)
            throws ClassNotLoadedException, InvalidTypeException {
        return setValueProxy(eleType, value, newValue -> array.setValue(index, newValue));
    }

    private Value setFieldValueWithConflict(ObjectReference obj, List<Field> fields, String name, String belongToClass,
            String value) throws ClassNotLoadedException, InvalidTypeException {
        Field field;
        // first try to resolve filed by fully qualified name
        List<Field> narrowedFields = fields.stream().filter(TypeComponent::isStatic)
                .filter(t -> t.name().equals(name) && t.declaringType().name().equals(belongToClass))
                .collect(Collectors.toList());
        if (narrowedFields.isEmpty()) {
            // second try to resolve filed by formatted name
            narrowedFields = fields.stream().filter(TypeComponent::isStatic)
                    .filter(t -> t.name().equals(name)
                            && this.provider.typeToString(t.declaringType(), this.options).equals(belongToClass))
                    .collect(Collectors.toList());
        }
        if (narrowedFields.size() == 1) {
            field = narrowedFields.get(0);
        } else {
            throw new UnsupportedOperationException(String.format("SetVariableRequest: Name conflicted for %s.", name));
        }
        return field.isStatic() ? setStaticFieldValue(field.declaringType(), field, name, value)
                : this.setObjectFieldValue(obj, field, name, value);

    }

    private int getReferenceId(ThreadReference thread, Value value) {
        if (value instanceof ObjectReference && this.provider.hasChildren(value)) {
            ThreadObjectReference threadObjectReference = new ThreadObjectReference();
            threadObjectReference.thread = thread;
            threadObjectReference.object = (ObjectReference) value;
            return this.objectPool.addObject(thread, threadObjectReference);
        }
        return 0;
    }
}
