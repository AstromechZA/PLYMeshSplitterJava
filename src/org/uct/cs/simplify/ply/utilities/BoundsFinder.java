package org.uct.cs.simplify.ply.utilities;

import javafx.geometry.BoundingBox;
import org.uct.cs.simplify.ply.reader.ImprovedPLYReader;
import org.uct.cs.simplify.ply.reader.MemoryMappedVertexReader;
import org.uct.cs.simplify.ply.reader.Vertex;

import java.io.IOException;

/**
 * Class for calculating the bounding box of a vertex set from a PLYReader
 */
public class BoundsFinder
{

    public static BoundingBox getBoundingBox(ImprovedPLYReader reader) throws IOException
    {
        return getBoundingBoxInner(reader, 1);
    }

    public static BoundingBox getBoundingBox(ImprovedPLYReader reader, int nth) throws IOException
    {
        if (nth <= 0 || nth >= 100) throw new IllegalArgumentException("Nth skipper must be 1-99");
        return getBoundingBoxInner(reader, nth);
    }

    private static BoundingBox getBoundingBoxInner(ImprovedPLYReader reader, int nth) throws IOException
    {
        int c = reader.getHeader().getElement("vertex").getCount();
        long p = reader.getElementDimension("vertex").getFirst();

        try (MemoryMappedVertexReader vr = new MemoryMappedVertexReader(reader.getFile(), p, c, 20))
        {
            int n = 0;
            float minx = Float.MAX_VALUE,
                    maxx = -Float.MAX_VALUE,
                    miny = Float.MAX_VALUE,
                    maxy = -Float.MAX_VALUE,
                    minz = Float.MAX_VALUE,
                    maxz = -Float.MAX_VALUE;
            float pr, se;

            Vertex v;
            for (int i = 0; i < c; i += nth)
            {
                v = vr.get(i);
                n += 1;

                minx = Math.min(minx, v.x);
                maxx = Math.max(maxx, v.x);

                miny = Math.min(miny, v.y);
                maxy = Math.max(maxy, v.y);

                minz = Math.min(minz, v.z);
                maxz = Math.max(maxz, v.z);
            }
            return new BoundingBox(minx, miny, minz, maxx - minx, maxy - miny, maxz - minz);
        }
    }


}
