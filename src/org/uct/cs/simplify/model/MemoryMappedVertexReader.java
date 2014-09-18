package org.uct.cs.simplify.model;

import org.uct.cs.simplify.ply.reader.PLYReader;

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
    private final VertexAttrMap vam;

    public MemoryMappedVertexReader(PLYReader reader, String vertexElementName) throws IOException
    {
        this.vam = new VertexAttrMap(reader.getHeader().getElement(vertexElementName));
        int c = reader.getHeader().getElement(vertexElementName).getCount();
        long p = reader.getElementDimension(vertexElementName).getOffset();
        int blockSize = reader.getHeader().getElement(vertexElementName).getItemSize();

        this.construct(reader.getFile(), p, c, blockSize);
    }

    public MemoryMappedVertexReader(PLYReader reader) throws IOException
    {
        this(reader, "vertex");
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
        byte[] b = new byte[ this.blockSize ];
        this.buffer.get(b, 0, this.blockSize);
        return new Vertex(b, this.vam);
    }

    @Override
    public void close() throws IOException
    {
        this.buffer.clear();
        this.fc.close();
        this.raf.close();
    }
}
