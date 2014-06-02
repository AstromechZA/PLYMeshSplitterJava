package org.uct.cs.simplify.ply.header;

public class PLYListProperty extends PLYProperty
{
    private DataType lengthType;

    public PLYListProperty(String name, DataType innerType, DataType lengthType)
    {
        super(name, innerType);
        this.lengthType = lengthType;
    }

    public DataType getLengthType()
    {
        return this.lengthType;
    }

    public String toString()
    {
        return String.format("PLYListProperty(%s %s)[%s]", this.getType(), this.getName(), this.getLengthType());
    }

}
