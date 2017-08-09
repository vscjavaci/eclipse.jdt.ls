package org.eclipse.jdt.ls.debug.adapter.formatter;

import java.util.Map;
import java.util.function.BiFunction;

import com.sun.jdi.ClassObjectReference;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.Type;

public class ClassObjectFormatter extends ObjectFormatter {
    private static final String CLASSS_PREFIX = "%s (%s)";
    
    public ClassObjectFormatter(BiFunction<Type, Map<String, Object>, String> typeStringFunction) {
        super(typeStringFunction);
    }

    @Override
    protected String getPrefix(ObjectReference value, Map<String, Object> props) {
        Type classType = ((ClassObjectReference) value).reflectedType();
        return String.format(CLASSS_PREFIX, super.getPrefix(value, props),
                typeToStringFunction.apply(classType, props));
    }

    @Override
    public boolean acceptType(Type type, Map<String, Object> props) {
        return type != null && (type.signature().charAt(0) == CLASS_OBJECT
                || type.signature().equals(CLASS_SIGNATURE));
    }
}
