package org.uct.cs.simplify.ply.header;

import org.uct.cs.simplify.ply.datatypes.DataType;
import org.uct.cs.simplify.ply.datatypes.IDataTypeReader;

public class PLYListProperty extends PLYProperty
{
    private final DataType lengthType;
    private final IDataTypeReader lengthTypeReader;

    public PLYListProperty(String name, DataType innerType, DataType lengthType)
    {
        super(name, innerType);
        this.lengthType = lengthType;
        this.lengthTypeReader = IDataTypeReader.getReaderForType(this.lengthType);
    }

    public IDataTypeReader getLengthTypeReader()
    {
        return this.lengthTypeReader;
    }

    public String toString()
    {
        return String.format("property list %s %s %s", this.lengthType, this.getType(), this.getName());
    }

}
