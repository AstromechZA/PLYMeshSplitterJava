package org.uct.cs.simplify.ply.reader;

import org.uct.cs.simplify.ply.datatypes.IDataTypeReader;
import org.uct.cs.simplify.ply.header.*;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;

public class PLYReader
{
    private File file;
    private PLYHeader header;
    private LinkedHashMap<String, Long> elementPositions;
    private LinkedHashMap<String, Long> elementLengths;

    public PLYReader(File f) throws IOException
    {
        this.file = f;
        this.header = new PLYHeader(f);

        calculateElementPositionsNaive();
    }

    private void calculateElementPositionsNaive()
    {
        this.elementPositions = new LinkedHashMap<>();
        this.elementLengths = new LinkedHashMap<>();

        long fileSize = this.file.length();
        // current cursor in file
        long cursor = this.header.getDataOffset();

        PLYElement last = null;

        for (PLYElement e : this.header.getElements().values())
        {
            if (cursor == -1) throw new RuntimeException("Element positions cannot be calculated naively!");

            this.elementPositions.put(e.getName(), cursor);

            long itemSize = 0;

            for (PLYPropertyBase p : e.getProperties())
            {
                if (p instanceof PLYListProperty)
                {
                    itemSize = -1;
                    break;
                }
                else if (p instanceof PLYProperty)
                {
                    itemSize += IDataTypeReader.getReaderForType(((PLYProperty) p).getType()).bytesAtATime();
                }
            }

            if (itemSize == -1)
            {
                cursor = -1;
            }
            else
            {
                long l = itemSize * e.getCount();
                cursor += l;
                this.elementLengths.put(e.getName(), l);
            }

            last = e;
        }

        if (last != null && this.elementPositions.containsKey(last.getName()))
        {
            long l = fileSize - this.elementPositions.get(last.getName());
            this.elementLengths.put(last.getName(), l);
        }

    }

    public long getPositionOfElement(String n)
    {
        return this.elementPositions.get(n);
    }

    public long getLengthOfElement(String n)
    {
        return this.elementLengths.get(n);
    }

    public PLYHeader getHeader()
    {
        return this.header;
    }


    public File getFile()
    {
        return file;
    }

}
