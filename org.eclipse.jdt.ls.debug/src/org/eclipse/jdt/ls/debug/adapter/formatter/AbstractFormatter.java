package org.eclipse.jdt.ls.debug.adapter.formatter;

public abstract class AbstractFormatter {
    protected static final char ARRAY = '[';
    protected static final char BYTE = 'B';
    protected static final char CHAR = 'C';
    protected static final char OBJECT = 'L';
    protected static final char FLOAT = 'F';
    protected static final char DOUBLE = 'D';
    protected static final char INT = 'I';
    protected static final char LONG = 'J';
    protected static final char SHORT = 'S';
    protected static final char VOID = 'V';
    protected static final char BOOLEAN = 'Z';
    protected static final char STRING = 's';
    protected static final char THREAD = 't';
    
    protected static final char THREAD_GROUP = 'g';
    protected static final char CLASS_LOADER = 'l';
    protected static final char CLASS_OBJECT = 'c';
    
    protected static final String QUALIFIED_FORMAT = "qualifed_format";    
    protected static final String NULL_STRING = "null";
    protected static final String VOID_STRING = "void";
    
    protected static final String QUOTE_STRING = "\"";
    protected static final String STRING_SIGNATURE = "Ljava/lang/String;";
    protected static final String CLASS_SIGNATURE = "Ljava/lang/Class;";
}
