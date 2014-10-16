package org.uct.cs.simplify.ply.datatypes;

import java.io.BufferedInputStream;
import java.io.IOException;
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
    public double read(BufferedInputStream stream) throws IOException
    {
        return (stream.read() | (stream.read() << 8)) & SHORT_MASK;
    }

    @Override
    public int bytesAtATime()
    {
        return 2;
    }
}
