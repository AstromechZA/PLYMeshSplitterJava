package org.uct.cs.simplify.ply.header;

public class PLYProperty extends PLYPropertyBase
{
    private DataType type;

    public PLYProperty(String name, DataType type)
    {
        super(name);
        this.type = type;
    }

    public DataType getType()
    {
        return this.type;
    }

    public String toString()
    {
        return String.format("PLYProperty(%s %s)", this.getType(), this.getName());
    }


}
