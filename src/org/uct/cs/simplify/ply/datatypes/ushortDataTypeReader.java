package org.uct.cs.simplify.ply.datatypes;

import java.nio.ByteBuffer;

public class ushortDataTypeReader implements IDataTypeReader
{
    private static final int SHORT_MASK = 0xFFFF;

    @Override
    public double read(ByteBuffer b)
    {
        return (int) b.getChar() & SHORT_MASK;
    }

    @Override
    public int bytesAtATime()
    {
        return 2;
    }
}
