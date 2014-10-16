package org.uct.cs.simplify.ply.datatypes;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class charDataTypeReader implements IDataTypeReader
{
    @Override
    public double read(ByteBuffer b)
    {
        return b.get();
    }

    @Override
    public double read(BufferedInputStream stream) throws IOException
    {
        return stream.read();
    }

    @Override
    public int bytesAtATime()
    {
        return 1;
    }
}
