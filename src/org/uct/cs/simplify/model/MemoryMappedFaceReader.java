package org.uct.cs.simplify.model;

import org.uct.cs.simplify.ply.reader.PLYReader;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Iterator;

public class MemoryMappedFaceReader implements AutoCloseable, Iterator<Face>
{
    private static final int BYTE = 0xFF;

    private int index;
    private int count;
    private RandomAccessFile raf;
    private FileChannel fc;
    private MappedByteBuffer buffer;

    public MemoryMappedFaceReader(PLYReader reader) throws IOException
    {
        int c = reader.getHeader().getElement("face").getCount();
        long p = reader.getElementDimension("face").getOffset();
        long l = reader.getElementDimension("face").getLength();

        this.construct(reader.getFile(), p, c, l);
    }

    private void construct(File file, long position, int count, long length) throws IOException
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

    public Face next()
    {
        int vertexCount = this.buffer.get() & BYTE;

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

    public int getCount()
    {
        return this.count;
    }
}
