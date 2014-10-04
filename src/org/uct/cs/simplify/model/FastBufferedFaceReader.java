package org.uct.cs.simplify.model;

import org.uct.cs.simplify.ply.reader.PLYReader;
import org.uct.cs.simplify.util.Useful;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class FastBufferedFaceReader extends StreamingFaceReader implements AutoCloseable
{
    private long index;
    private long count;
    private BufferedInputStream istream;

    public FastBufferedFaceReader(PLYReader reader) throws IOException
    {
        long c = reader.getHeader().getElement("face").getCount();
        long p = reader.getElementDimension("face").getOffset();
        long l = reader.getElementDimension("face").getLength();

        this.construct(reader.getFile(), p, c, l);
    }

    private void construct(File file, long position, long count, long length) throws IOException
    {
        this.count = count;
        this.istream = new BufferedInputStream(new FileInputStream(file));
        this.istream.skip(position);

    }

    public boolean hasNext()
    {
        return this.index <= (this.count - 1);
    }

    public Face next() throws IOException
    {
        istream.read();
        this.index += 1;
        return new Face(Useful.readIntLE(istream), Useful.readIntLE(istream), Useful.readIntLE(istream));
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
