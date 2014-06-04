package org.uct.cs.simplify.ply.datatypes;

import java.nio.ByteBuffer;

public class doubleDataTypeReader implements IDataTypeReader
{
    @Override
    public double read(ByteBuffer b)
    {
        return b.getDouble();
    }

    @Override
    public int bytesAtATime()
    {
        return 8;
    }
}
