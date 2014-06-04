package org.uct.cs.simplify.ply.header;

import static org.uct.cs.simplify.ply.datatypes.DataTypes.parseDataType;

public class PLYPropertyBase
{
    private String name;

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
            return new PLYListProperty(parts[ 4 ], parseDataType(parts[ 3 ]), parseDataType(parts[ 2 ]));
        return new PLYProperty(parts[ 2 ], parseDataType(parts[ 1 ]));
    }



}
