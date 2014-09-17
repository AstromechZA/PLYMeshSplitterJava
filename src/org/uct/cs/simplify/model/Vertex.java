package org.uct.cs.simplify.model;

import org.uct.cs.simplify.util.Useful;

public class Vertex
{
    public final float x;
    public final float y;
    public final float z;
    public final int hash;
    public byte r = (byte) 128;
    public byte g = (byte) 128;
    public byte b = (byte) 128;
    public byte a = (byte) 255;

    public Vertex(byte[] input, VertexAttrMap vam)
    {
        this.x = Useful.readFloatLE(input, vam.xOffset);
        this.y = Useful.readFloatLE(input, vam.yOffset);
        this.z = Useful.readFloatLE(input, vam.zOffset);

        if (vam.hasColour)
        {
            this.r = input[ vam.redOffset ];
            this.g = input[ vam.greenOffset ];
            this.b = input[ vam.blueOffset ];
        }

        if (vam.hasAlpha)
        {
            this.a = input[ vam.alphaOffset ];
        }

        this.hash = hash();
    }

    private int hash()
    {
        long bits = 7L;
        bits = 31L * bits + Double.doubleToLongBits(this.x);
        bits = 31L * bits + Double.doubleToLongBits(this.y);
        bits = 31L * bits + Double.doubleToLongBits(this.z);
        return (int) (bits ^ (bits >> 32));
    }

    @Override
    public int hashCode()
    {
        return this.hash;
    }

}
