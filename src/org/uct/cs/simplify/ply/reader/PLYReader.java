package org.uct.cs.simplify.ply.reader;

import org.uct.cs.simplify.ply.header.*;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;

public class PLYReader
{
    private File file;
    private PLYHeader header;
    private LinkedHashMap<String, Long> elementPositions;

    public PLYReader(File f) throws IOException
    {
        this.file = f;
        this.header = new PLYHeader(f);

        this.elementPositions = calculateElementPositionsNaive();


    }

    private LinkedHashMap<String, Long> calculateElementPositionsNaive()
    {
        LinkedHashMap<String, Long> out = new LinkedHashMap<>();

        long fileSize = this.file.length();
        // current cursor in file
        long cursor = this.header.getDataOffset();

        for (PLYElement e : this.header.getElements().values())
        {
            if (cursor == -1) throw new RuntimeException("Element positions cannot be calculated naively!");

            out.put(e.getName(), cursor);

            long itemSize = 0;

            for (PLYPropertyBase p : e.getProperties())
            {
                if (p instanceof PLYListProperty)
                {
                    itemSize = -1;
                    break;
                }
                else
                {
                    itemSize += PLYPropertyBase.bytesInType(((PLYProperty) p).getType());
                }
            }

            if (itemSize == -1)
            {
                cursor = -1;
            }
            else
            {
                cursor += itemSize * e.getCount();
            }


        }

        return out;
    }

    public long getPositionOfElement(String n)
    {
        return this.elementPositions.get(n);
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
