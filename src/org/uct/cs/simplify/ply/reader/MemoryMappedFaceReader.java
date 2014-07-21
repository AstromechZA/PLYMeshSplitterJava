package org.uct.cs.simplify.ply.reader;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class MemoryMappedFaceReader implements AutoCloseable, Iterator<Face>
{
    private static final int BYTE = 0xFF;

    private int index;
    private final int count;
    private RandomAccessFile raf;
    private FileChannel fc;
    private MappedByteBuffer buffer;

    public MemoryMappedFaceReader(File file, long position, int count, long length) throws IOException
    {
        this.count = count;
        this.raf = new RandomAccessFile(file, "r");
        this.fc = this.raf.getChannel();
        this.buffer = this.fc.map(FileChannel.MapMode.READ_ONLY, position, length);
        this.buffer.order(ByteOrder.LITTLE_ENDIAN);
    }

    public boolean hasNext()
    {
        return this.index < (this.count - 1);
    }

    public Face next()
    {
        int vertexCount = this.buffer.get() & BYTE;
        List<Integer> vertexList = new ArrayList<>();
        for (int i = 0; i < vertexCount; i++)
        {
            vertexList.add(this.buffer.getInt());
        }
        this.index += 1;
        return new Face(vertexList);
    }

    @Override
    public void close() throws IOException
    {
        this.buffer.clear();
        this.fc.close();
        this.raf.close();
    }
}
