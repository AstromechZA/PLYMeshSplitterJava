package org.uct.cs.simplify.ply.datatypes;

import java.nio.ByteBuffer;

public class intDataTypeReader implements IDataTypeReader
{
    @Override
    public double read(ByteBuffer b)
    {
        return b.getInt();
    }

    @Override
    public int bytesAtATime()
    {
        return 4;
    }
}
