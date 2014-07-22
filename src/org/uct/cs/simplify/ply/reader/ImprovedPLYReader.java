package org.uct.cs.simplify.ply.reader;

import org.uct.cs.simplify.ply.header.*;
import org.uct.cs.simplify.util.Pair;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.LinkedHashMap;

public class ImprovedPLYReader
{

    private final File file;
    private final PLYHeader header;
    private final LinkedHashMap<String, Pair<Long, Long>> elementDimensions;

    public ImprovedPLYReader(PLYHeader h) throws IOException
    {
        this.header = h;
        this.file = h.getFile();
        this.elementDimensions = new LinkedHashMap<>();

        this.positionScan();
    }

    public File getFile()
    {
        return this.file;
    }

    public PLYHeader getHeader()
    {
        return this.header;
    }

    public Pair<Long, Long> getElementDimension(String n)
    {
        return this.elementDimensions.get(n);
    }

    /**
     * We need some way of simply identifying the location and size of PLY elements. This method
     * fills the elementDimensions object.
     */
    private void positionScan() throws IOException
    {
        this.elementDimensions.clear();

        long dataOffset = this.header.getDataOffset();
        long payloadSize = this.file.length() - dataOffset;

        try (RandomAccessFile raf = new RandomAccessFile(this.file, "r"); FileChannel fc = raf.getChannel())
        {
            MappedByteBuffer buffer = fc.map(FileChannel.MapMode.READ_ONLY, dataOffset, payloadSize);

            int cursor = 0;

            PLYElement[] elements = new PLYElement[this.header.getElements().size() ];
            this.header.getElements().values().toArray(elements);
            int numElements = elements.length;

            for (int i = 0; i < numElements; i++)
            {
                PLYElement e = elements[ i ];

                // position is the cursor
                long elementPosition = cursor;

                // is this the last element then the size continues till EOF
                if (i == numElements - 1)
                {
                    long elementSize = payloadSize - cursor;
                    this.elementDimensions.put(e.getName(), new Pair<>(dataOffset + elementPosition, elementSize));
                    break;
                }
                else if (e.getItemSize() != null)
                {
                    long elementSize = e.getCount() * e.getItemSize();
                    this.elementDimensions.put(e.getName(), new Pair<>(dataOffset + elementPosition, elementSize));

                    cursor += elementSize;
                    buffer.position(cursor);
                }
                else
                {
                    long elementSize = calculateSizeOfListElement(e, buffer);
                    this.elementDimensions.put(e.getName(), new Pair<>(dataOffset + elementPosition, elementSize));

                    cursor += elementSize;
                    buffer.position(cursor);
                }
            }
        }

    }

    private static long calculateSizeOfListElement(PLYElement e, MappedByteBuffer buffer)
    {
        long total = 0;

        int numItems = e.getCount();
        for (int i = 0; i < numItems; i++)
        {
            for (PLYPropertyBase p : e.getProperties())
            {
                if (p instanceof PLYListProperty)
                {
                    PLYListProperty pp = (PLYListProperty) p;
                    int listSize = (int) pp.getLengthTypeReader().read(buffer);
                    int s = listSize * pp.getLengthTypeReader().bytesAtATime();
                    buffer.position(buffer.position() + s);
                }
                else if (p instanceof PLYProperty)
                {
                    PLYProperty pp = (PLYProperty) p;
                    buffer.position(buffer.position() + pp.getTypeReader().bytesAtATime());
                }
            }
        }

        return total;
    }


}
