package org.uct.cs.simplify.ply.datatypes;

import java.io.BufferedInputStream;
import java.io.IOException;
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
    public double read(BufferedInputStream stream) throws IOException
    {
        return (short) stream.read() & BYTE_MASK;
    }

    @Override
    public int bytesAtATime()
    {
        return 1;
    }
}
