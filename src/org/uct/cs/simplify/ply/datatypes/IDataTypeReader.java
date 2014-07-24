package org.uct.cs.simplify.ply.datatypes;

import java.nio.ByteBuffer;

public interface IDataTypeReader
{
    public double read(ByteBuffer b);

    public int bytesAtATime();

    public static IDataTypeReader getReaderForType(DataType dt)
    {
        if (dt == DataType.CHAR) return new charDataTypeReader();
        if (dt == DataType.UCHAR) return new ucharDataTypeReader();
        if (dt == DataType.SHORT) return new shortDataTypeReader();
        if (dt == DataType.USHORT) return new ushortDataTypeReader();
        if (dt == DataType.INT) return new intDataTypeReader();
        if (dt == DataType.UINT) return new uintDataTypeReader();
        if (dt == DataType.FLOAT) return new floatDataTypeReader();
        if (dt == DataType.DOUBLE) return new doubleDataTypeReader();
        else throw new IllegalArgumentException("Unknown DataType " + dt);
    }

}
