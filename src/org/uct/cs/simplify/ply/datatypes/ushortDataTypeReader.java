package org.uct.cs.simplify.ply.datatypes;

import org.uct.cs.simplify.Constants;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class ushortDataTypeReader implements IDataTypeReader
{
    @Override
    public double read(ByteBuffer b)
    {
        return (int) b.getChar() & Constants.SHORT_MASK;
    }

    @Override
    public double read(InputStream stream) throws IOException
    {
        return (stream.read() | (stream.read() << 8)) & Constants.SHORT_MASK;
    }

    @Override
    public int bytesAtATime()
    {
        return 2;
    }
}
