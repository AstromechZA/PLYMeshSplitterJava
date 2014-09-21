package org.uct.cs.simplify.ply.datatypes;

import java.nio.ByteBuffer;

public class ucharDataTypeReader implements IDataTypeReader
{
    private static final int BYTE_MASK = 0xFF;

    @Override
    public double read(ByteBuffer b)
    {
        return (short) b.get() & BYTE_MASK;
    }

    @Override
    public int bytesAtATime()
    {
        return 1;
    }
}
