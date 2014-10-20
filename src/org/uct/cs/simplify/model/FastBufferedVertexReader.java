package org.uct.cs.simplify.model;

import org.uct.cs.simplify.ply.header.PLYElement;
import org.uct.cs.simplify.ply.reader.PLYReader;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;

public class FastBufferedVertexReader extends StreamingVertexReader implements AutoCloseable
{
    protected final PLYReader reader;
    protected final PLYElement vertexElement;
    protected final VertexAttrMap vam;
    protected final long start;
    protected final int blockSize;
    protected final ByteBuffer blockBuffer;
    protected long index;
    protected long count;
    protected FileInputStream istream;
    protected FileChannel inputChannel;


    public FastBufferedVertexReader(PLYReader reader) throws IOException
    {
        this.reader = reader;
        this.vertexElement = reader.getHeader().getElement("vertex");
        this.count = this.vertexElement.getCount();
        this.vam = new VertexAttrMap(this.vertexElement);

        this.start = reader.getElementDimension("vertex").getOffset();

        this.istream = new FileInputStream(reader.getFile());
        this.inputChannel = this.istream.getChannel();
        this.inputChannel.position(this.start);
        this.blockSize = this.vertexElement.getItemSize();
        this.blockBuffer = ByteBuffer.allocateDirect(this.blockSize);
        this.blockBuffer.order(ByteOrder.LITTLE_ENDIAN);
    }

    @Override
    public boolean hasNext()
    {
        return this.index <= (this.count - 1);
    }

    @Override
    public Vertex next() throws IOException
    {
        Vertex v = new Vertex(0, 0, 0);
        next(v);
        return v;
    }

    @Override
    public void next(Vertex v) throws IOException
    {
        inputChannel.read(blockBuffer);
        blockBuffer.flip();
        v.read(blockBuffer, vam);
        this.index++;
        blockBuffer.clear();
    }

    @Override
    public long getCount()
    {
        return this.count;
    }

    @Override
    public void close() throws IOException
    {
        this.istream.close();
    }

    public void reset() throws IOException
    {
        this.index = 0;
        this.inputChannel.position(this.start);
    }

    public VertexAttrMap getVam()
    {
        return vam;
    }

    public void skipForwardTo(int i) throws IOException
    {
        if (index == i)
        {
            return;
        }
        if (index <= i)
        {
            this.inputChannel.position(this.start + i * this.blockSize);
        }
        else
        {
            throw new RuntimeException("Cannot skip backwards! (" + index + " -> " + i);
        }
    }

    public long getIndex()
    {
        return index;
    }
}
