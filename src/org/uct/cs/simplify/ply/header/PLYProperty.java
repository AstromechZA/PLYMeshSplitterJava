package org.uct.cs.simplify.ply.header;

import org.uct.cs.simplify.ply.datatypes.DataTypes;
import org.uct.cs.simplify.ply.datatypes.IDataTypeReader;


public class PLYProperty extends PLYPropertyBase
{
    private final DataTypes.DataType type;
    private final IDataTypeReader typeReader;

    public PLYProperty(String name, DataTypes.DataType type)
    {
        super(name);
        this.type = type;
        this.typeReader = IDataTypeReader.getReaderForType(this.type);
    }

    public DataTypes.DataType getType()
    {
        return this.type;
    }

    public IDataTypeReader getTypeReader()
    {
        return this.typeReader;
    }

    public String toString()
    {
        return String.format("property %s %s", this.getType(), this.getName());
    }


}
