package org.uct.cs.simplify.ply.reader;

import org.uct.cs.simplify.ply.header.*;
import org.uct.cs.simplify.util.Timer;

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

        try (Timer t = new Timer("v reader"); VertexReader vr = new VertexReader(f, elementPositions.get("vertex"), this.header.getElement("vertex").getCount(), 20))
        {
            Vertex v;
            int n = 0;
            float minx = Float.MAX_VALUE,
                    maxx = -Float.MAX_VALUE,
                    miny = Float.MAX_VALUE,
                    maxy = -Float.MAX_VALUE,
                    minz = Float.MAX_VALUE,
                    maxz = -Float.MAX_VALUE;

            while (vr.hasNext())
            {
                n += 1;
                v = vr.next();

                minx = Math.min(minx, v.x);
                maxx = Math.max(maxx, v.x);

                miny = Math.min(miny, v.y);
                maxy = Math.max(maxy, v.y);

                minz = Math.min(minz, v.z);
                maxz = Math.max(maxz, v.z);
            }
            System.out.printf("x: %f  %f  %f\n", minx, maxx, (minx + maxx) / 2);
            System.out.printf("y: %f  %f  %f\n", miny, maxy, (miny + maxy) / 2);
            System.out.printf("z: %f  %f  %f\n", minz, maxz, (minz + maxz) / 2);


        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

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


}
