package org.uct.cs.simplify.ply.datatypes;

import java.nio.ByteBuffer;

public class uintDataTypeReader implements IDataTypeReader
{
    private static final long LONG = 0xFFFFFFFFL;

    @Override
    public double read(ByteBuffer b)
    {
        return (long) b.getInt() & LONG;
    }

    @Override
    public int bytesAtATime()
    {
        return 4;
    }
}
