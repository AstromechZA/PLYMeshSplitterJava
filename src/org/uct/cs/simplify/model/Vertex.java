package org.uct.cs.simplify.model;

import javafx.geometry.Point3D;
import org.uct.cs.simplify.util.Useful;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

public class Vertex
{
    private static final byte DEFAULT_RED = (byte) 128;
    private static final byte DEFAULT_GREEN = (byte) 128;
    private static final byte DEFAULT_BLUE = (byte) 128;
    private static final byte DEFAULT_ALPHA = (byte) 255;

    public float x;
    public float y;
    public float z;
    private boolean staleHash = true;
    private double hash;
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
    }

    public void transform(Point3D translate, float scale)
    {
        this.x = (float) ((this.x + translate.getX()) * scale);
        this.y = (float) ((this.y + translate.getY()) * scale);
        this.z = (float) ((this.z + translate.getZ()) * scale);
        this.staleHash = false;
    }

    public void swapYZ()
    {
        float t = this.z;
        this.z = -this.y;
        this.y = t;
        this.staleHash = false;
    }

    @SuppressWarnings("MagicNumber")
    private double hash()
    {
        long bits = 7L;
        bits = 31L * bits + Double.doubleToLongBits(this.x);
        bits = 31L * bits + Double.doubleToLongBits(this.y);
        bits = 31L * bits + Double.doubleToLongBits(this.z);
        return (double) (bits ^ (bits >> 32));
    }

    @SuppressWarnings("MagicNumber")
    public double getHash()
    {
        if (this.staleHash)
        {
            this.hash = this.hash();
            this.staleHash = false;
        }
        return this.hash;
    }

    public void writeToStream(OutputStream stream, VertexAttrMap vam) throws IOException
    {
        Useful.writeFloatLE(stream, this.x);
        Useful.writeFloatLE(stream, this.y);
        Useful.writeFloatLE(stream, this.z);

        if (vam.hasColour)
        {
            stream.write(this.r);
            stream.write(this.g);
            stream.write(this.b);
        }

        if (vam.hasAlpha)
        {
            stream.write(this.a);
        }
    }
}
