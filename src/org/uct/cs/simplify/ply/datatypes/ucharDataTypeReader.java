package org.uct.cs.simplify.ply.datatypes;

import java.nio.ByteBuffer;

public class ucharDataTypeReader implements IDataTypeReader
{
    @Override
    public double read(ByteBuffer b)
    {
        return ((short) b.get() & 0xFF);
    }

    @Override
    public int bytesAtATime()
    {
        return 1;
    }
}
