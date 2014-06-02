package org.uct.cs.simplify.ply.reader;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class VertexReader
{
    int cursor;

    public VertexReader(File f, int position, int count, int blockSize) throws IOException
    {
        long numbytes = count * blockSize;

        int chunksize = (1024 / blockSize) * blockSize;

        try (RandomAccessFile raf = new RandomAccessFile(f, "r"))
        {
            try (FileChannel fc = raf.getChannel())
            {
                fc.position(position);
                ByteBuffer buffer = ByteBuffer.allocate(chunksize);
                while (fc.read(buffer) > 0)
                {
                    buffer.flip();
                    int n = chunksize / blockSize;
                    for (int i = 0; i < n; i++)
                    {
                        byte[] b = new byte[ blockSize ];
                        buffer.get(b, 0, blockSize);
                        Vertex v = new Vertex(b);
                        System.out.println(v.x);
                    }
                    buffer.flip();
                }
            }
        }
    }
}
