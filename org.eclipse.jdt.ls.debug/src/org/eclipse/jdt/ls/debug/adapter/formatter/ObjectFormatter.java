package org.eclipse.jdt.ls.debug.adapter.formatter;

import java.util.Map;
import java.util.function.BiFunction;

import com.sun.jdi.ObjectReference;
import com.sun.jdi.Type;
import com.sun.jdi.Value;

public class ObjectFormatter implements IValueFormatter {
    /**
     * The format type function for this object.
     */
    protected BiFunction<Type, Map<String, Object>, String> typeToStringFunction;

    public ObjectFormatter(BiFunction<Type, Map<String, Object>, String> typeToStringFunction) {
        this.typeToStringFunction = typeToStringFunction;
    }

    @Override
    public String toString(Object obj, Map<String, Object> props) {
        return String.format(Constants.OBJECT_TEMPLATE, getPrefix((ObjectReference) obj, props), 
                HexicalNumericFormatter.numbericToString(
                        ((ObjectReference) obj).uniqueID(),
                        HexicalNumericFormatter.containsHexFormat(props)));
    }

    @Override
    public boolean acceptType(Type type, Map<String, Object> props) {
        if (type == null) {
            return false;
        }
        char tag = type.signature().charAt(0);
        return (tag == Constants.OBJECT) || (tag == Constants.ARRAY) || (tag == Constants.STRING)
                || (tag == Constants.THREAD) || (tag == Constants.THREAD_GROUP)
                || (tag == Constants.CLASS_LOADER)
                || (tag == Constants.CLASS_OBJECT);
    }

    @Override
    public Value valueOf(String value, Type type, Map<String, Object> props) {
        if (value == null || Constants.NULL_STRING.equals(value)) {
            return null;
        }
        throw new UnsupportedOperationException("set value is not supported yet for type " + type.name());
    }

    /**
     * The type with additional prefix before id=${id} of this object.(eg: class, array length)
     * @param value The object value.
     * @param type The type of the object
     * @param props additional information about expected format
     * @return the type name with additional text
     */
    protected String getPrefix(ObjectReference value, Map<String, Object> props) {
        return typeToStringFunction.apply(value.type(), props);
    }
}
