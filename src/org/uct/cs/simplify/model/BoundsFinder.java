package org.uct.cs.simplify.model;

import org.uct.cs.simplify.ply.reader.PLYReader;
import org.uct.cs.simplify.util.XBoundingBox;

import java.io.IOException;

/**
 * Class for calculating the bounding box of a vertex set from a PLYReader
 */
public class BoundsFinder
{

    public static XBoundingBox getBoundingBox(PLYReader reader) throws IOException
    {
        return getBoundingBoxInner(reader, 1);
    }

    private static XBoundingBox getBoundingBoxInner(PLYReader reader, int nth) throws IOException
    {
        try (StreamingVertexReader vr = new FastBufferedVertexReader(reader))
        {
            float minx = Float.MAX_VALUE,
                maxx = -Float.MAX_VALUE,
                miny = Float.MAX_VALUE,
                maxy = -Float.MAX_VALUE,
                minz = Float.MAX_VALUE,
                maxz = -Float.MAX_VALUE;

            Vertex v = new Vertex(0, 0, 0);
            while (vr.hasNext())
            {
                vr.next(v);
                minx = Math.min(minx, v.x);
                maxx = Math.max(maxx, v.x);

                miny = Math.min(miny, v.y);
                maxy = Math.max(maxy, v.y);

                minz = Math.min(minz, v.z);
                maxz = Math.max(maxz, v.z);
            }
            return new XBoundingBox(minx, miny, minz, maxx - minx, maxy - miny, maxz - minz);
        }
    }


}
