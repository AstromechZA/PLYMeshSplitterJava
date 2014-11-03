package org.uct.cs.simplify.model;

import org.uct.cs.simplify.ply.header.PLYElement;
import org.uct.cs.simplify.ply.reader.PLYReader;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;

public class SkippableVertexReader extends StreamingVertexReader implements AutoCloseable
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


    public SkippableVertexReader(PLYReader r) throws IOException
    {
        reader = r;
        vertexElement = reader.getHeader().getElement("vertex");
        count = vertexElement.getCount();
        vam = new VertexAttrMap(this.vertexElement);

        start = reader.getElementDimension("vertex").getOffset();

        istream = new FileInputStream(reader.getFile());
        inputChannel = istream.getChannel();
        inputChannel.position(start);
        blockSize = vertexElement.getItemSize();
        blockBuffer = ByteBuffer.allocateDirect(blockSize);
        blockBuffer.order(ByteOrder.LITTLE_ENDIAN);
    }

    @Override
    public boolean hasNext()
    {
        return index < count;
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
        index++;
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

    @Override
    public VertexAttrMap getVam()
    {
        return vam;
    }

    public void skipTo(long i) throws IOException
    {
        if (index == i) return;
        this.index = i;
        this.inputChannel.position(start + i * blockSize);
    }

    public long getIndex()
    {
        return index;
    }
}
