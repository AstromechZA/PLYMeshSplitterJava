package org.uct.cs.simplify.ply.datatypes;

import java.nio.ByteBuffer;

public class uintDataTypeReader implements IDataTypeReader
{
    private static final long LONG_MASK = 0xFFFFFFFFL;

    @Override
    public double read(ByteBuffer b)
    {
        return (long) b.getInt() & LONG_MASK;
    }

    @Override
    public int bytesAtATime()
    {
        return 4;
    }
}
