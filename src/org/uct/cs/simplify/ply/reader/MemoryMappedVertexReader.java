package org.uct.cs.simplify.ply.reader;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class MemoryMappedVertexReader implements AutoCloseable
{
    private int blockSize;
    private int count;
    private MappedByteBuffer buffer;
    private RandomAccessFile raf;
    private FileChannel fc;

    public MemoryMappedVertexReader(PLYReader reader, String vertexElementName) throws IOException
    {
        int c = reader.getHeader().getElement(vertexElementName).getCount();
        long p = reader.getElementDimension(vertexElementName).getFirst();
        int blockSize = reader.getHeader().getElement(vertexElementName).getItemSize();

        this.construct(reader.getFile(), p, c, blockSize);
    }

    public MemoryMappedVertexReader(PLYReader reader) throws IOException
    {
        int c = reader.getHeader().getElement("vertex").getCount();
        long p = reader.getElementDimension("vertex").getFirst();
        int blockSize = reader.getHeader().getElement("vertex").getItemSize();

        this.construct(reader.getFile(), p, c, blockSize);
    }

    private void construct(File f, long position, int count, int blockSize) throws IOException
    {
        this.count = count;
        this.blockSize = blockSize;

        this.raf = new RandomAccessFile(f, "r");
        this.fc = this.raf.getChannel();

        this.buffer = this.fc.map(FileChannel.MapMode.READ_ONLY, position, count * blockSize);
    }

    public int getCount()
    {
        return this.count;
    }

    public Vertex get(int i)
    {
        int index = i * this.blockSize;
        this.buffer.position(index);
        byte[] b = new byte[this.blockSize];
        this.buffer.get(b, 0, this.blockSize);
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
