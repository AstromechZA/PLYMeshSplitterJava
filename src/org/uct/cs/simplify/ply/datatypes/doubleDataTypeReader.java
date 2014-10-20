package org.uct.cs.simplify.ply.datatypes;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class doubleDataTypeReader implements IDataTypeReader
{
    @Override
    public double read(ByteBuffer b)
    {
        return b.getDouble();
    }

    @Override
    public double read(InputStream stream) throws IOException
    {
        long o = stream.read();
        o |= ((long) stream.read() << 8);
        o |= ((long) stream.read() << (8 * 2));
        o |= ((long) stream.read() << (8 * 3));
        o |= ((long) stream.read() << (8 * 4));
        o |= ((long) stream.read() << (8 * 5));
        o |= ((long) stream.read() << (8 * 6));
        o |= ((long) stream.read() << (8 * 7));

        return Double.longBitsToDouble(o);
    }

    @Override
    public int bytesAtATime()
    {
        return 8;
    }
}
