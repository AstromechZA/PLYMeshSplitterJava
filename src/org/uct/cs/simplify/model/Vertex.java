package org.uct.cs.simplify.model;

import javafx.geometry.Point3D;
import org.uct.cs.simplify.util.Useful;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

public class Vertex
{
    private static final byte DEFAULT_RED = (byte) 128;
    public byte r = DEFAULT_RED;
    private static final byte DEFAULT_GREEN = (byte) 128;
    public byte g = DEFAULT_GREEN;
    private static final byte DEFAULT_BLUE = (byte) 128;
    public byte b = DEFAULT_BLUE;
    private static final byte DEFAULT_ALPHA = (byte) 255;
    public byte a = DEFAULT_ALPHA;
    public float x;
    public float y;
    public float z;

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
    }

    public void swapYZ()
    {
        float t = this.z;
        this.z = -this.y;
        this.y = t;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || this.getClass() != o.getClass()) return false;

        Vertex vertex = (Vertex) o;
        return Float.compare(vertex.x, this.x) == 0 && Float.compare(vertex.y, this.y) == 0 && Float.compare(
            vertex.z,
            this.z
        ) == 0;
    }

    @Override
    public int hashCode()
    {
        int result = (this.x != +0.0f ? Float.floatToIntBits(this.x) : 0);
        result = 31 * result + (this.y != +0.0f ? Float.floatToIntBits(this.y) : 0);
        return 31 * result + (this.z != +0.0f ? Float.floatToIntBits(this.z) : 0);
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
