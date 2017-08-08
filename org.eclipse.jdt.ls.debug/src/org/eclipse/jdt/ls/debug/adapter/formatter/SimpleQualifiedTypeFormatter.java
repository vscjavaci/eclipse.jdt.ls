package org.eclipse.jdt.ls.debug.adapter.formatter;

import java.util.Map;

import com.sun.jdi.Type;
import com.sun.jdi.VoidType;

public class SimpleQualifiedTypeFormatter implements ITypeFormatter {

    @Override
    public String toString(Object type, Map<String, Object> props) {
        if (type == null) {
            return Constants.NULL_STRING;
        }
        if (type instanceof VoidType) {
            return Constants.VOID_STRING;
        }
        return ((Type)type).name();
    }

    @Override
    public boolean acceptType(Type type, Map<String, Object> props) {
        return containsQualifedFormat(props);
    }

    static boolean containsQualifedFormat(Map<String, Object> props) {
        return props.containsKey(Constants.QUALIFIED_FORMAT)
                && Boolean.parseBoolean(String.valueOf(props.get(Constants.QUALIFIED_FORMAT)));
    }
}
