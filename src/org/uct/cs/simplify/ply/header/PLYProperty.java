package org.uct.cs.simplify.ply.header;

import org.uct.cs.simplify.ply.datatypes.DataType;
import org.uct.cs.simplify.ply.datatypes.IDataTypeReader;


public class PLYProperty extends PLYPropertyBase
{
    private final DataType type;
    private final IDataTypeReader typeReader;

    public PLYProperty(String name, DataType type)
    {
        super(name);
        this.type = type;
        this.typeReader = IDataTypeReader.getReaderForType(this.type);
    }

    public DataType getType()
    {
        return this.type;
    }

    public IDataTypeReader getTypeReader()
    {
        return this.typeReader;
    }

    public String toString()
    {
        return String.format("property %s %s", this.type, this.getName());
    }


}
