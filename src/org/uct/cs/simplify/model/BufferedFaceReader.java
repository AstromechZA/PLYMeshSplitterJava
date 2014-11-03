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

public class BufferedFaceReader extends StreamingFaceReader
{
    private static final int FACES_PER_BUCKET = 512;

    protected final PLYReader reader;
    protected final PLYElement faceElement;
    protected final long start;
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

    public BufferedFaceReader(PLYReader reader) throws IOException
    {
        this.reader = reader;
        this.faceElement = reader.getHeader().getElement("face");
        this.count = faceElement.getCount();
        this.start = reader.getElementDimension("face").getOffset();
        this.istream = new FileInputStream(reader.getFile());
        this.inputChannel = this.istream.getChannel();
        this.inputChannel.position(this.start);
        this.blockBuffer = ByteBuffer.allocateDirect(13 * FACES_PER_BUCKET);
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
        if (cursor > maxcursor)
        {
            bucketIndex++;
            if (bucketIndex < maxBucketIndex)
            {
                blockBuffer.clear();
                maxcursor = FACES_PER_BUCKET - 1;
            }
            else if (bucketIndex == maxBucketIndex)
            {
                blockBuffer = ByteBuffer.allocateDirect(facesInLastBucket * 13);
                maxcursor = facesInLastBucket - 1;
            }
            else
            {
                throw new BufferUnderflowException();
            }
            inputChannel.read(blockBuffer);
            blockBuffer.flip();
            blockBuffer.order(ByteOrder.LITTLE_ENDIAN);
            cursor = 0;
        }
        int pos = cursor * 13 + 1;
        f.i = Useful.readIntLE(blockBuffer, pos);
        f.j = Useful.readIntLE(blockBuffer, pos + 4);
        f.k = Useful.readIntLE(blockBuffer, pos + 8);
        index++;
        cursor++;

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
