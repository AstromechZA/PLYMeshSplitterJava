package org.uct.cs.simplify.ply.reader;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

public class MemoryMappedFaceReader implements AutoCloseable
{
    int index = 0;
    int count;

    private RandomAccessFile raf;
    private FileChannel fc;
    private MappedByteBuffer buffer;

    public MemoryMappedFaceReader(File file, long position, int count, long length) throws IOException
    {
        this.count = count;
        this.raf = new RandomAccessFile(file, "r");
        this.fc = raf.getChannel();
        this.buffer = fc.map(FileChannel.MapMode.READ_ONLY, position, length);
        this.buffer.order(ByteOrder.LITTLE_ENDIAN);
    }

    public boolean hasNext()
    {
        return index < (count - 1);
    }

    public Face next()
    {
        int vertexCount = buffer.get() & 0xFF;
        List<Integer> vertexList = new ArrayList<>();
        for (int i = 0; i < vertexCount; i++)
        {
            vertexList.add(buffer.getInt());
        }
        index += 1;
        return new Face(vertexList);
    }

    @Override
    public void close() throws IOException
    {
        buffer.clear();
        fc.close();
        raf.close();
    }
}
