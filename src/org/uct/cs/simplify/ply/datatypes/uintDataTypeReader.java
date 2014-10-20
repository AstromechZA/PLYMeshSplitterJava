package org.uct.cs.simplify.ply.datatypes;

import org.uct.cs.simplify.Constants;
import org.uct.cs.simplify.util.Useful;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class uintDataTypeReader implements IDataTypeReader
{
    @Override
    public double read(ByteBuffer b)
    {
        return (long) b.getInt() & Constants.LONG_MASK;
    }

    @Override
    public double read(InputStream stream) throws IOException
    {
        return (long) Useful.readIntLE(stream) & Constants.LONG_MASK;
    }

    @Override
    public int bytesAtATime()
    {
        return 4;
    }


}
