package org.eclipse.jdt.ls.debug.adapter.formatter;

import java.util.Map;
import java.util.function.BiFunction;

import com.sun.jdi.ClassObjectReference;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.Type;

public class ClassObjectFormatter extends ObjectFormatter {
    public ClassObjectFormatter(BiFunction<Type, Map<String, Object>, String> typeStringFunction) {
        super(typeStringFunction);
    }

    @Override
    protected String getPrefix(ObjectReference value, Map<String, Object> props) {
        Type classType = ((ClassObjectReference) value).reflectedType();
        return String.format(Constants.CLASSS_PREFIX, super.getPrefix(value, props),
                typeToStringFunction.apply(classType, props));
    }

    @Override
    public boolean acceptType(Type type, Map<String, Object> props) {
        return type != null && (type.signature().charAt(0) == Constants.CLASS_OBJECT
                || type.signature().equals(Constants.CLASS_SIGNATURE));
    }
}
