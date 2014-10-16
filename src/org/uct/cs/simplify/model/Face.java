package org.uct.cs.simplify.model;

import org.uct.cs.simplify.util.Useful;

import java.io.IOException;
import java.io.OutputStream;

public class Face
{
    public int i, j, k;

    public Face(int i, int j, int k)
    {
        this.i = i;
        this.j = j;
        this.k = k;
    }

    public void writeToStream(OutputStream stream) throws IOException
    {
        stream.write((byte) 3);
        Useful.writeIntLE(stream, this.i);
        Useful.writeIntLE(stream, this.j);
        Useful.writeIntLE(stream, this.k);
    }
}
