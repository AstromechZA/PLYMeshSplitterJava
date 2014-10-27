package org.uct.cs.simplify.model;

import org.uct.cs.simplify.ply.header.PLYElement;
import org.uct.cs.simplify.ply.reader.PLYReader;
import org.uct.cs.simplify.util.Useful;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;

public class ReliableBufferedFaceReader extends StreamingFaceReader
{
    private static final int FACES_PER_BUCKET = 4096;

    protected final PLYReader reader;
    protected final PLYElement faceElement;
    protected final long start;
    protected final int blockSize;
    protected ByteBuffer blockBuffer;
    protected long index;
    protected long count;
    protected FileInputStream istream;
    protected FileChannel inputChannel;

    protected int facesInLastBucket;
    protected long maxBucketIndex;
    protected long bucketIndex;
    protected int maxcursor;
    protected int cursor;

    public ReliableBufferedFaceReader(PLYReader reader) throws IOException
    {
        this.reader = reader;
        this.faceElement = reader.getHeader().getElement("face");
        this.count = faceElement.getCount();
        this.start = reader.getElementDimension("face").getOffset();
        this.istream = new FileInputStream(reader.getFile());
        this.inputChannel = this.istream.getChannel();
        this.inputChannel.position(this.start);
        this.blockSize = 13 * FACES_PER_BUCKET;
        this.blockBuffer = ByteBuffer.allocateDirect(this.blockSize);
        this.blockBuffer.order(ByteOrder.LITTLE_ENDIAN);

        this.facesInLastBucket = (int) (this.count % FACES_PER_BUCKET);
        this.maxBucketIndex = (this.count / FACES_PER_BUCKET);
        this.bucketIndex = -1;
        this.cursor = 0;
        this.maxcursor = -1;
    }


    @Override
    public boolean hasNext()
    {
        return this.index < this.count;
    }

    @Override
    public Face next() throws IOException
    {
        Face f = new Face(0, 0, 0);
        next(f);
        return f;
    }

    @Override
    public void next(Face f) throws IOException
    {
        if (cursor <= maxcursor)
        {
            f.i = Useful.readIntLE(blockBuffer, cursor * 13 + 1);
            f.j = Useful.readIntLE(blockBuffer, cursor * 13 + 5);
            f.k = Useful.readIntLE(blockBuffer, cursor * 13 + 9);
            index++;
            cursor++;
        }
        else
        {
            bucketIndex++;
            if (bucketIndex < maxBucketIndex)
            {
                blockBuffer.clear();
                blockBuffer.order(ByteOrder.LITTLE_ENDIAN);
                inputChannel.read(blockBuffer);
                cursor = 0;
                maxcursor = FACES_PER_BUCKET - 1;
            }
            else if (bucketIndex == maxBucketIndex)
            {
                blockBuffer = ByteBuffer.allocateDirect(this.facesInLastBucket * 13);
                blockBuffer.order(ByteOrder.LITTLE_ENDIAN);
                inputChannel.read(blockBuffer);
                cursor = 0;
                maxcursor = facesInLastBucket - 1;
            }
            else
            {
                throw new BufferUnderflowException();
            }
        }

    }

    @Override
    public long getCount()
    {
        return this.count;
    }

    @Override
    public void close() throws IOException
    {
        this.istream.close();
    }
}
