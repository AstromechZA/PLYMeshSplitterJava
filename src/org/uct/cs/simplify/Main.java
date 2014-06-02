package org.uct.cs.simplify;

import org.uct.cs.simplify.ply.reader.PLYReader;
import org.uct.cs.simplify.ply.reader.Vertex;
import org.uct.cs.simplify.ply.reader.VertexReader;
import org.uct.cs.simplify.util.Timer;

import java.io.File;
import java.io.IOException;

public class Main
{

    public static void main(String[] args)
    {
        String filename = "C:\\Users\\Ben\\Desktop\\Gede Palace _3_Mio.ply";

        try (Timer ignored = new Timer("Entire read"))
        {

            PLYReader r = new PLYReader(new File(filename));

            try (Timer t = new Timer("v reader");
                 VertexReader vr = new VertexReader(r.getFile(), r.getPositionOfElement("vertex"), r.getHeader().getElement("vertex").getCount(), 20))
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
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }
}
