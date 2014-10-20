package org.uct.cs.simplify.ply.reader;

import org.uct.cs.simplify.ply.header.*;
import org.uct.cs.simplify.util.ReliableBufferedInputStream;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.LinkedHashMap;

public class PLYReader
{

    public final LinkedHashMap<String, ElementDimension> elementDimensions;
    private final File file;
    private final PLYHeader header;

    public PLYReader(PLYHeader h) throws IOException
    {
        this.header = h;
        this.file = h.getFile();
        this.elementDimensions = new LinkedHashMap<>(2);

        this.positionScan();
    }

    public PLYReader(File f) throws IOException
    {
        this(new PLYHeader(f));
    }

    public File getFile()
    {
        return this.file;
    }

    public PLYHeader getHeader()
    {
        return this.header;
    }

    public ElementDimension getElementDimension(String n)
    {
        return this.elementDimensions.get(n);
    }

    /**
     We need some way of simply identifying the location and size of PLY elements. This method fills the
     elementDimensions object.
     */
    private void positionScan() throws IOException
    {
        this.elementDimensions.clear();

        long dataOffset = this.header.getDataOffset();
        long payloadSize = this.file.length() - dataOffset;

        try (ReliableBufferedInputStream istream = new ReliableBufferedInputStream(new FileInputStream(this.file)))
        {
            // skip to start
            istream.skip(dataOffset);

            long cursor = 0;

            PLYElement[] elements = new PLYElement[this.header.getElements().size()];
            this.header.getElements().values().toArray(elements);
            int numElements = elements.length;

            for (int i = 0; i < numElements; i++)
            {
                PLYElement e = elements[i];

                // position is the cursor
                long elementPosition = cursor;

                // is this the last element then the size continues till EOF
                if (i == numElements - 1)
                {
                    long elementSize = payloadSize - cursor;
                    this.elementDimensions
                        .put(e.getName(), new ElementDimension(dataOffset + elementPosition, elementSize));
                    break;
                }
                else if (!e.hasListProperty())
                {
                    long elementSize = e.getCount() * (long) e.getItemSize();
                    this.elementDimensions
                        .put(e.getName(), new ElementDimension(dataOffset + elementPosition, elementSize));
                    istream.skip(elementSize);
                    cursor += elementSize;
                }
                else
                {
                    long elementSize = calculateSizeOfListElement(e, istream);
                    this.elementDimensions
                        .put(e.getName(), new ElementDimension(dataOffset + elementPosition, elementSize));
                    cursor += elementSize;
                }
            }
        }

    }

    private static long calculateSizeOfListElement(PLYElement e, ReliableBufferedInputStream stream) throws IOException
    {
        long total = 0;

        long numItems = e.getCount();
        for (long i = 0; i < numItems; i++)
        {
            for (PLYPropertyBase p : e.getProperties())
            {
                if (p instanceof PLYListProperty)
                {
                    PLYListProperty pp = (PLYListProperty) p;
                    int listSize = (int) pp.getLengthTypeReader().read(stream);
                    long s = listSize * pp.getTypeReader().bytesAtATime();
                    stream.skip(s);
                    total += pp.getLengthTypeReader().bytesAtATime() + s;
                }
                else if (p instanceof PLYProperty)
                {
                    PLYProperty pp = (PLYProperty) p;
                    stream.skip(pp.getTypeReader().bytesAtATime());
                    total += pp.getTypeReader().bytesAtATime();
                }
            }
        }

        return total;
    }


}
