package org.uct.cs.simplify.ply.datatypes;

import java.util.HashMap;

public enum DataType
{
    CHAR("char", 1),
    UCHAR("uchar", 1),
    SHORT("short", 2),
    USHORT("ushort", 2),
    INT("int", 4),
    UINT("uint", 4),
    FLOAT("float", 4),
    DOUBLE("double", 8);
    private static final HashMap<String, DataType> translator;

    static
    {
        translator = new HashMap<>();
        translator.put("CHAR", CHAR);
        translator.put("UCHAR", UCHAR);
        translator.put("SHORT", SHORT);
        translator.put("USHORT", USHORT);
        translator.put("INT", INT);
        translator.put("UINT", UINT);
        translator.put("FLOAT", FLOAT);
        translator.put("DOUBLE", DOUBLE);
        translator.put("CHAR", CHAR);

        translator.put("INT8", CHAR);
        translator.put("UINT8", UCHAR);
        translator.put("INT16", SHORT);
        translator.put("UINT16", USHORT);
        translator.put("INT32", INT);
        translator.put("UINT32", UINT);
        translator.put("FLOAT32", FLOAT);
        translator.put("FLOAT64", DOUBLE);
    }
    private final String name;
    private final int bytes;

    DataType(String name, int bytes)
    {
        this.name = name;
        this.bytes = bytes;
    }

    public static DataType from(String s)
    {
        return translator.get(s.trim().toUpperCase());
    }

    public int getByteSize()
    {
        return this.bytes;
    }

    public String toString()
    {
        return this.name;
    }
}
