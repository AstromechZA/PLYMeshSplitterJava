package org.uct.cs.simplify.ply.datatypes;

import org.uct.cs.simplify.util.Useful;

import java.io.BufferedInputStream;
import java.io.IOException;
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
    public double read(BufferedInputStream stream) throws IOException
    {
        return (long) Useful.readIntLE(stream) & LONG_MASK;
    }

    @Override
    public int bytesAtATime()
    {
        return 4;
    }


}
