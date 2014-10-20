package org.uct.cs.simplify.ply.datatypes;

import org.uct.cs.simplify.util.Useful;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class floatDataTypeReader implements IDataTypeReader
{
    @Override
    public double read(ByteBuffer b)
    {
        return b.getFloat();
    }

    @Override
    public double read(InputStream stream) throws IOException
    {
        return Useful.readFloatLE(stream);
    }

    @Override
    public int bytesAtATime()
    {
        return 4;
    }
}
