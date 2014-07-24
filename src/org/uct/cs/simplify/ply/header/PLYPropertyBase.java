package org.uct.cs.simplify.ply.header;

import org.uct.cs.simplify.ply.datatypes.DataType;

public class PLYPropertyBase
{
    private final String name;

    public PLYPropertyBase(String name)
    {
        this.name = name;
    }

    public String getName()
    {
        return this.name;
    }

    public String toString()
    {
        return String.format("PLYPropertyBase(%s)", this.name);
    }

    public static PLYPropertyBase FromString(String line)
    {
        line = line.trim();
        String[] parts = line.split(" ");
        if (parts[ 1 ].equals("list"))
            return new PLYListProperty(parts[ 4 ], DataType.from(parts[3]), DataType.from(parts[2]));
        return new PLYProperty(parts[ 2 ], DataType.from(parts[1]));
    }



}
