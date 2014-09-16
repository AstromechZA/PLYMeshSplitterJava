package org.uct.cs.simplify.ply.datatypes;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Vertex
{
    public final float x;
    public final float y;
    public final float z;
    //    public int r;
//    public int g;
//    public int b;
//    public int a;
    public final int hash;

    public Vertex(byte[] input)
    {
        ByteBuffer bf = ByteBuffer.wrap(input);
        bf.order(ByteOrder.LITTLE_ENDIAN);
        this.x = bf.getFloat();
        this.y = bf.getFloat();
        this.z = bf.getFloat();
//
//        this.r = 0xFF & ((int)bf.get());
//        this.g = 0xFF & ((int)bf.get());
//        this.b = 0xFF & ((int)bf.get());
//        this.a = 0xFF & ((int)bf.get());

        long bits = 7L;
        bits = 31L * bits + Double.doubleToLongBits(this.x);
        bits = 31L * bits + Double.doubleToLongBits(this.y);
        bits = 31L * bits + Double.doubleToLongBits(this.z);
        hash = (int) (bits ^ (bits >> 32));

    }

    @Override
    public int hashCode()
    {
        return this.hash;
    }

}
