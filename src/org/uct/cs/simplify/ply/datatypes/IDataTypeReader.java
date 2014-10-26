package org.uct.cs.simplify.ply.datatypes;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public interface IDataTypeReader
{
    double read(ByteBuffer b);

    int bytesAtATime();


    double read(InputStream stream) throws IOException;
}
