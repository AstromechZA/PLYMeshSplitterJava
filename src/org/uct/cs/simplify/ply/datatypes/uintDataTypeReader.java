package org.uct.cs.simplify.ply.datatypes;

import java.nio.ByteBuffer;

public class uintDataTypeReader implements IDataTypeReader
{
    @Override
    public double read(ByteBuffer b)
    {
        return ((long) b.getInt() & 0xFFFFFFFFL);
    }

    @Override
    public int bytesAtATime()
    {
        return 4;
    }
}
