package org.uct.cs.simplify.model;

import org.uct.cs.simplify.ply.header.PLYElement;
import org.uct.cs.simplify.ply.reader.PLYReader;
import org.uct.cs.simplify.util.ReliableBufferedInputStream;

import java.io.FileInputStream;
import java.io.IOException;

public class SimpleVertexReader extends StreamingVertexReader implements AutoCloseable
{
    protected final PLYReader reader;
    protected final PLYElement vertexElement;
    protected final VertexAttrMap vam;
    protected final long start;
    protected final int blockSize;
    protected final byte[] blockBuffer;
    protected final long count;
    protected final long itercount;
    protected final ReliableBufferedInputStream istream;
    protected final long nth;
    protected final long skip;
    protected final long skipBytes;
    protected long index;

    public SimpleVertexReader(PLYReader reader) throws IOException
    {
        this(reader, 1);
    }

    public SimpleVertexReader(PLYReader reader, long nth) throws IOException
    {
        if (nth < 1) throw new IllegalArgumentException("nth must be at least 1");

        this.nth = nth;
        this.skip = nth - 1;
        this.reader = reader;
        this.vertexElement = reader.getHeader().getElement("vertex");
        this.count = this.vertexElement.getCount();
        this.itercount = (nth == 1) ? this.count : (long) Math.ceil(this.count / (double) this.nth);

        this.vam = new VertexAttrMap(this.vertexElement);

        this.start = reader.getElementDimension("vertex").getOffset();

        this.istream = new ReliableBufferedInputStream(new FileInputStream(reader.getFile()));
        this.istream.skip(this.start);
        this.blockSize = this.vertexElement.getItemSize();
        this.blockBuffer = new byte[ this.blockSize ];
        this.skipBytes = this.skip * this.blockSize;
    }

    @Override
    public boolean hasNext()
    {
        return this.index < this.count;
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
        this.istream.read(this.blockBuffer, 0, this.blockSize);
        v.read(blockBuffer, vam);
        this.index += nth;
        this.istream.skip(this.skipBytes);
    }

    public long getSampling()
    {
        return this.itercount;
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
        return this.vam;
    }

}
