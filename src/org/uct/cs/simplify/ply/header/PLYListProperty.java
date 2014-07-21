package org.uct.cs.simplify.ply.header;

import org.uct.cs.simplify.ply.datatypes.DataTypes;
import org.uct.cs.simplify.ply.datatypes.IDataTypeReader;

public class PLYListProperty extends PLYProperty
{
    private final DataTypes.DataType lengthType;
    private final IDataTypeReader lengthTypeReader;

    public PLYListProperty(String name, DataTypes.DataType innerType, DataTypes.DataType lengthType)
    {
        super(name, innerType);
        this.lengthType = lengthType;
        this.lengthTypeReader = IDataTypeReader.getReaderForType(this.lengthType);
    }

    public DataTypes.DataType getLengthType()
    {
        return this.lengthType;
    }

    public IDataTypeReader getLengthTypeReader()
    {
        return this.lengthTypeReader;
    }

    public String toString()
    {
        return String.format("PLYListProperty(%s %s)[%s]", this.getType(), this.getName(), this.getLengthType());
    }

}
