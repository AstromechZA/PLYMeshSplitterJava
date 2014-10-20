package org.uct.cs.simplify.model;

import org.uct.cs.simplify.ply.header.PLYElement;
import org.uct.cs.simplify.ply.reader.PLYReader;
import org.uct.cs.simplify.util.ReliableBufferedInputStream;

import java.io.FileInputStream;
import java.io.IOException;

public class FastBufferedVertexReader extends StreamingVertexReader implements AutoCloseable
{
    protected final PLYReader reader;
    protected final PLYElement vertexElement;
    protected final VertexAttrMap vam;
    protected final long start;
    protected final int blockSize;
    protected final byte[] blockBuffer;
    protected long index;
    protected long count;
    protected ReliableBufferedInputStream istream;

    public FastBufferedVertexReader(PLYReader reader) throws IOException
    {
        this.reader = reader;
        this.vertexElement = reader.getHeader().getElement("vertex");
        this.count = this.vertexElement.getCount();
        this.vam = new VertexAttrMap(this.vertexElement);

        this.start = reader.getElementDimension("vertex").getOffset();

        this.istream = new ReliableBufferedInputStream(new FileInputStream(reader.getFile()));
        this.istream.skip(this.start);
        this.blockSize = this.vertexElement.getItemSize();
        this.blockBuffer = new byte[ this.blockSize ];
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

    public void skipNext(long n) throws IOException
    {
        this.index += n;
        this.istream.skip(n * this.blockSize);
    }

    @Override
    public void next(Vertex v) throws IOException
    {
        this.istream.read(this.blockBuffer, 0, this.blockSize);
        v.read(blockBuffer, vam);
        this.index++;
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
