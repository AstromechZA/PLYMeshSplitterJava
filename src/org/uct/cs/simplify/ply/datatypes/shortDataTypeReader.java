package org.uct.cs.simplify.ply.datatypes;

import java.nio.ByteBuffer;

public class shortDataTypeReader implements IDataTypeReader
{
    @Override
    public double read(ByteBuffer b)
    {
        return b.getChar();
    }

    @Override
    public int bytesAtATime()
    {
        return 2;
    }
}
