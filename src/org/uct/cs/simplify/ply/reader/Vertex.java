package org.uct.cs.simplify.ply.reader;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Vertex
{
    public float x;
    public float y;
    public float z;
    public byte[] data;

    public Vertex(byte[] input)
    {
        this.data = input;
        ByteBuffer bf = ByteBuffer.wrap(input);
        bf.order(ByteOrder.LITTLE_ENDIAN);
        this.x = bf.getFloat();
        this.y = bf.getFloat();
        this.z = bf.getFloat();
    }


}
