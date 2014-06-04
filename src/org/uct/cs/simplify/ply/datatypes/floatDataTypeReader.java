package org.uct.cs.simplify.ply.datatypes;

import java.nio.ByteBuffer;

public class floatDataTypeReader implements IDataTypeReader
{
    @Override
    public double read(ByteBuffer b)
    {
        return b.getFloat();
    }

    @Override
    public int bytesAtATime()
    {
        return 4;
    }
}
