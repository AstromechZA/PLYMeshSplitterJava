package org.uct.cs.simplify.model;

import org.uct.cs.simplify.ply.reader.PLYReader;
import org.uct.cs.simplify.util.Useful;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class MemoryMappedFaceReader extends StreamingFaceReader implements AutoCloseable
{
    private long index;
    private long count;
    private RandomAccessFile raf;
    private FileChannel fc;
    private MappedByteBuffer buffer;

    public MemoryMappedFaceReader(PLYReader reader) throws IOException
    {
        long c = reader.getHeader().getElement("face").getCount();
        long p = reader.getElementDimension("face").getOffset();
        long l = reader.getElementDimension("face").getLength();

        this.construct(reader.getFile(), p, c, l);
    }

    private void construct(File file, long position, long count, long length) throws IOException
    {
        this.count = count;
        this.raf = new RandomAccessFile(file, "r");
        this.fc = this.raf.getChannel();
        this.buffer = this.fc.map(FileChannel.MapMode.READ_ONLY, position, length);
        this.buffer.order(ByteOrder.LITTLE_ENDIAN);
    }

    public boolean hasNext()
    {
        return this.index <= (this.count - 1);
    }

    public Face next() throws IOException
    {
        int vertexCount = this.buffer.get() & Useful.BYTE_MASK;

        int i = this.buffer.getInt();
        int j = this.buffer.getInt();
        int k = this.buffer.getInt();

        while (vertexCount > 3)
        {
            this.buffer.getInt();
            vertexCount--;
        }

        this.index += 1;
        return new Face(i, j, k);
    }

    @Override
    public void close() throws IOException
    {
        this.buffer.clear();
        this.fc.close();
        this.raf.close();
    }

    public long getCount()
    {
        return this.count;
    }
}
