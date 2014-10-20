package org.uct.cs.simplify.model;

import org.uct.cs.simplify.ply.header.PLYElement;
import org.uct.cs.simplify.ply.reader.PLYReader;
import org.uct.cs.simplify.util.ReliableBufferedInputStream;
import org.uct.cs.simplify.util.Useful;

import java.io.FileInputStream;
import java.io.IOException;

public class FastBufferedFaceReader extends StreamingFaceReader implements AutoCloseable
{
    protected final PLYReader reader;
    protected final PLYElement faceElement;
    protected long index;
    protected long count;
    protected ReliableBufferedInputStream istream;

    public FastBufferedFaceReader(PLYReader reader) throws IOException
    {
        this.reader = reader;
        this.faceElement = reader.getHeader().getElement("face");
        this.count = this.faceElement.getCount();

        this.istream = new ReliableBufferedInputStream(new FileInputStream(reader.getFile()));

        this.istream.skip(reader.getElementDimension("face").getOffset());
    }

    public boolean hasNext()
    {
        return this.index <= (this.count - 1);
    }

    public void next(Face f) throws IOException
    {
        istream.read();
        f.i = Useful.readIntLE(istream);
        f.j = Useful.readIntLE(istream);
        f.k = Useful.readIntLE(istream);


        this.index += 1;
    }

    public Face next() throws IOException
    {
        Face f = new Face(0, 0, 0);
        next(f);
        return f;
    }

    public long getCount()
    {
        return this.count;
    }

    public void close() throws IOException
    {
        this.istream.close();
    }
}
