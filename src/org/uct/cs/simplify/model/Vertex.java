package org.uct.cs.simplify.model;

import org.uct.cs.simplify.util.Useful;

import java.nio.ByteBuffer;

public class Vertex
{
    private static final byte DEFAULT_RED = (byte) 128;
    private static final byte DEFAULT_GREEN = (byte) 128;
    private static final byte DEFAULT_BLUE = (byte) 128;
    private static final byte DEFAULT_ALPHA = (byte) 255;

    public final float x;
    public final float y;
    public final float z;
    public final double hash;
    public byte r = DEFAULT_RED;
    public byte g = DEFAULT_GREEN;
    public byte b = DEFAULT_BLUE;
    public byte a = DEFAULT_ALPHA;

    public Vertex(byte[] input, VertexAttrMap vam)
    {
        this.x = Useful.readFloatLE(input, vam.xOffset);
        this.y = Useful.readFloatLE(input, vam.yOffset);
        this.z = Useful.readFloatLE(input, vam.zOffset);

        if (vam.hasColour)
        {
            this.r = input[vam.redOffset];
            this.g = input[vam.greenOffset];
            this.b = input[vam.blueOffset];
        }

        if (vam.hasAlpha)
        {
            this.a = input[vam.alphaOffset];
        }

        this.hash = this.hash();
    }

    public Vertex(ByteBuffer input, VertexAttrMap vam)
    {
        this.x = Useful.readFloatLE(input, vam.xOffset);
        this.y = Useful.readFloatLE(input, vam.yOffset);
        this.z = Useful.readFloatLE(input, vam.zOffset);

        if (vam.hasColour)
        {
            this.r = input.get(vam.redOffset);
            this.g = input.get(vam.greenOffset);
            this.b = input.get(vam.blueOffset);
        }

        if (vam.hasAlpha)
        {
            this.a = input.get(vam.alphaOffset);
        }

        this.hash = this.hash();
    }

    private double hash()
    {
        long bits = 7L;
        bits = 31L * bits + Double.doubleToLongBits(this.x);
        bits = 31L * bits + Double.doubleToLongBits(this.y);
        bits = 31L * bits + Double.doubleToLongBits(this.z);
        return (double) (bits ^ (bits >> 32));
    }

    public double getHash()
    {
        return this.hash;
    }

}
