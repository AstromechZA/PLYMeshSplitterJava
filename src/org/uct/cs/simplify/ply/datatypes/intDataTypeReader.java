package org.uct.cs.simplify.ply.datatypes;

import org.uct.cs.simplify.util.Useful;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class intDataTypeReader implements IDataTypeReader
{
    @Override
    public double read(ByteBuffer b)
    {
        return b.getInt();
    }

    @Override
    public double read(BufferedInputStream stream) throws IOException
    {
        return Useful.readIntLE(stream);
    }

    @Override
    public int bytesAtATime()
    {
        return 4;
    }
}
