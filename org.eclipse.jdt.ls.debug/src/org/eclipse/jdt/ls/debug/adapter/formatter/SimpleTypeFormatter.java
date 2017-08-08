package org.eclipse.jdt.ls.debug.adapter.formatter;

import java.util.Map;

import com.sun.jdi.Type;
import com.sun.jdi.VoidType;

public class SimpleTypeFormatter implements ITypeFormatter {
    
    @Override
    public String toString(Object type, Map<String, Object> props) {
        if (type == null) {
            return Constants.NULL_STRING;
        }
        if (type instanceof VoidType) {
            return Constants.VOID_STRING;
        }
        return trimTypeName(((Type)type).name());
    }
    
    @Override
    public boolean acceptType(Type type, Map<String, Object> props) {
        return !SimpleQualifiedTypeFormatter.containsQualifedFormat(props);
    }
    
    private String trimTypeName(String type) {
        if (type.indexOf('.') >= 0) {
            type = type.substring(type.lastIndexOf('.') + 1);
        }
        return type;
    }

}
