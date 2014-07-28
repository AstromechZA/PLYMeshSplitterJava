package org.uct.cs.simplify.ply.datatypes;

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

    private final String name;
    private final int bytes;

    DataType(String name, int bytes)
    {
        this.name = name;
        this.bytes = bytes;
    }

    public static DataType from(String s)
    {
        return valueOf(s.trim().toUpperCase());
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
