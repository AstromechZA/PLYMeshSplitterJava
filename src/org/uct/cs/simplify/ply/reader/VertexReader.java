package org.uct.cs.simplify.ply.reader;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Iterator;

public class VertexReader implements AutoCloseable, Iterator<Vertex>
{
    RandomAccessFile raf;
    FileChannel fc;

    int largeChunkSize;
    int numChunks;
    int remChunks;
    int chunkIndex;

    ByteBuffer chunk;

    int numBlocks;
    int blockIndex;
    int blockSize;


    public VertexReader(File f, long position, int count, int blockSize) throws IOException
    {
        raf = new RandomAccessFile(f, "r");
        fc = raf.getChannel();

        this.blockSize = blockSize;

        largeChunkSize = (1024 / blockSize) * blockSize;
        numChunks = (count * blockSize) / largeChunkSize;
        remChunks = (count * blockSize) % largeChunkSize;
        chunkIndex = numChunks;

        //skip to position
        fc.position(position);

        loadChunk();
    }

    private void loadChunk() throws IOException
    {
        if (chunkIndex < 0) throw new IOException("wdawdaw");

        int n = (chunkIndex > 0) ? largeChunkSize : remChunks;

        chunk = ByteBuffer.allocate(n);
        n = fc.read(chunk);
        chunk.flip();
        chunkIndex--;

        numBlocks = n / blockSize;
        blockIndex = numBlocks;

    }

    public boolean hasNext()
    {
        return chunkIndex > 0 || blockIndex > 0;
    }

    public Vertex next()
    {
        try
        {
            if (blockIndex == 0) loadChunk();
        }
        catch (IOException e)
        {
            return null;
        }

        byte[] b = new byte[ blockSize ];
        chunk.get(b, 0, blockSize);

        blockIndex--;

        return new Vertex(b);
    }

    @Override
    public void close() throws IOException
    {
        fc.close();
        raf.close();
    }
}
