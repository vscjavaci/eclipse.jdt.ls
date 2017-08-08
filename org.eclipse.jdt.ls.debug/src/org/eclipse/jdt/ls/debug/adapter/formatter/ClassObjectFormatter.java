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
        String basePrefix = super.getPrefix(value, props);
        Type classType = ((ClassObjectReference) value).reflectedType();
        StringBuilder sb = new StringBuilder();
        sb.append(basePrefix);
        sb.append(Constants.LEFT_BRACE);
        sb.append(typeToStringFunction.apply(classType, props));
        sb.append(Constants.RIGHT_BRACE);
        return sb.toString();
    }

    @Override
    public boolean acceptType(Type type, Map<String, Object> props) {
        return type != null && (type.signature().charAt(0) == Constants.CLASS_OBJECT
                || type.signature().equals(Constants.CLASS_SIGNATURE));
    }
}
