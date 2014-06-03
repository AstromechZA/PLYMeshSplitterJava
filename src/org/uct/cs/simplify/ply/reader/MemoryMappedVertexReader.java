package org.uct.cs.simplify.ply.reader;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class MemoryMappedVertexReader implements AutoCloseable
{
    private int blockSize;
    private MappedByteBuffer buffer;
    private RandomAccessFile raf;
    private FileChannel fc;
    private int count;

    public MemoryMappedVertexReader(File f, long position, int count, int blockSize) throws IOException
    {
        this.count = count;
        this.blockSize = blockSize;

        this.raf = new RandomAccessFile(f, "r");
        this.fc = raf.getChannel();

        this.buffer = fc.map(FileChannel.MapMode.READ_ONLY, position, count * blockSize);
    }

    public int getCount()
    {
        return count;
    }

    public Vertex get(int i)
    {
        int index = i * blockSize;
        buffer.position(index);
        byte[] b = new byte[ blockSize ];
        buffer.get(b, 0, blockSize);
        return new Vertex(b);
    }

    @Override
    public void close() throws IOException
    {
        this.buffer.clear();
        this.fc.close();
        this.raf.close();
    }
}
