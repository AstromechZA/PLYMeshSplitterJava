package org.uct.cs.simplify.ply.datatypes;

import java.nio.ByteBuffer;

public class ushortDataTypeReader implements IDataTypeReader
{
    @Override
    public double read(ByteBuffer b)
    {
        return ((int) b.getChar() & 0xFFFF);
    }

    @Override
    public int bytesAtATime()
    {
        return 2;
    }
}
