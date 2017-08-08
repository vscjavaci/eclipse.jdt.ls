package org.eclipse.jdt.ls.debug.adapter.formatter;

import java.util.Map;
import java.util.function.BiFunction;

import com.sun.jdi.ArrayReference;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.Type;
import com.sun.jdi.Value;

public class ArrayObjectFormatter extends ObjectFormatter {

    public ArrayObjectFormatter(BiFunction<Type, Map<String, Object>, String> typeStringFunction) {
        super(typeStringFunction);
    }

    @Override
    protected String getPrefix(ObjectReference value, Map<String, Object> props) {
        String arrayTypeString = super.getPrefix(value, props);
        return arrayTypeString.replaceFirst("\\[\\]", String.format(Constants.ARRAY_PREFIX
                + HexicalNumericFormatter.numbericToString(arrayLength(value),
                        HexicalNumericFormatter.containsHexFormat(props))));
    }

    @Override
    public boolean acceptType(Type type, Map<String, Object> props) {
        return type != null && type.signature().charAt(0) == Constants.ARRAY;
    }

    private static int arrayLength(Value value) {
        return ((ArrayReference) value).length();
    }
}
