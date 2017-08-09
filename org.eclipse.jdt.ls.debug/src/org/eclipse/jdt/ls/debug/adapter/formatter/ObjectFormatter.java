package org.eclipse.jdt.ls.debug.adapter.formatter;

import java.util.Map;
import java.util.function.BiFunction;

import com.sun.jdi.ObjectReference;
import com.sun.jdi.Type;
import com.sun.jdi.Value;

public class ObjectFormatter extends AbstractFormatter implements IValueFormatter {
    private static final String OBJECT_TEMPLATE = "%s (id=%s)";
    
    /**
     * The format type function for this object.
     */
    protected BiFunction<Type, Map<String, Object>, String> typeToStringFunction;

    public ObjectFormatter(BiFunction<Type, Map<String, Object>, String> typeToStringFunction) {
        this.typeToStringFunction = typeToStringFunction;
    }

    @Override
    public String toString(Object obj, Map<String, Object> props) {
        return String.format(OBJECT_TEMPLATE, getPrefix((ObjectReference) obj, props), 
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
        return (tag == OBJECT) || (tag == ARRAY) || (tag == STRING)
                || (tag == THREAD) || (tag == THREAD_GROUP)
                || (tag == CLASS_LOADER)
                || (tag == CLASS_OBJECT);
    }

    @Override
    public Value valueOf(String value, Type type, Map<String, Object> props) {
        if (value == null || NULL_STRING.equals(value)) {
            return null;
        }
        throw new UnsupportedOperationException(String.format("Set value is not supported yet for type %s.", type.name()));
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
