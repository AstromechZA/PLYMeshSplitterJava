package org.uct.cs.simplify.ply.datatypes;

import java.nio.ByteBuffer;

public interface IDataTypeReader
{
    public double read(ByteBuffer b);

    public int bytesAtATime();

    public static IDataTypeReader getReaderForType(DataTypes.DataType dt)
    {
        if (dt == DataTypes.DataType.CHAR) return new charDataTypeReader();
        if (dt == DataTypes.DataType.UCHAR) return new ucharDataTypeReader();
        if (dt == DataTypes.DataType.SHORT) return new shortDataTypeReader();
        if (dt == DataTypes.DataType.USHORT) return new ushortDataTypeReader();
        if (dt == DataTypes.DataType.INT) return new intDataTypeReader();
        if (dt == DataTypes.DataType.UINT) return new uintDataTypeReader();
        if (dt == DataTypes.DataType.FLOAT) return new floatDataTypeReader();
        if (dt == DataTypes.DataType.DOUBLE) return new doubleDataTypeReader();
        else throw new IllegalArgumentException("Unknown DataType " + dt);
    }

}
