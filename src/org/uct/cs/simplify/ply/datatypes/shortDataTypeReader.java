package org.uct.cs.simplify.ply.datatypes;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class shortDataTypeReader implements IDataTypeReader
{
    @Override
    public double read(ByteBuffer b)
    {
        return b.getChar();
    }

    @Override
    public double read(InputStream stream) throws IOException
    {
        return (short) (stream.read() | (stream.read() << 8));
    }

    @Override
    public int bytesAtATime()
    {
        return 2;
    }
}
