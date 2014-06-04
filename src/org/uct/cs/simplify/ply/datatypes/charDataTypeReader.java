package org.uct.cs.simplify.ply.datatypes;

import java.nio.ByteBuffer;

public class charDataTypeReader implements IDataTypeReader
{
    @Override
    public double read(ByteBuffer b)
    {
        return b.get();
    }

    @Override
    public int bytesAtATime()
    {
        return 1;
    }
}
