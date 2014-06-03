package org.uct.cs.simplify;

import org.uct.cs.simplify.img.BluePrintGenerator;
import org.uct.cs.simplify.ply.reader.PLYReader;
import org.uct.cs.simplify.ply.reader.Vertex;
import org.uct.cs.simplify.ply.reader.VertexReader;
import org.uct.cs.simplify.util.Timer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class Main
{

    public static void main(String[] args)
    {
        String filename = "C:\\Users\\Ben\\Desktop\\Chapel_of_Nossa_Senhora_de_Baluarte_ao.ply";
        //String filename = "C:\\Users\\Ben\\Desktop\\Gede Palace _3_Mio.ply";

        try (Timer ignored = new Timer("Entire read"))
        {

            PLYReader r = new PLYReader(new File(filename));

            System.out.printf("%d vertices\n", r.getHeader().getElement("vertex").getCount());

            try (VertexReader vr = new VertexReader(r.getFile(), r.getPositionOfElement("vertex"), r.getHeader().getElement("vertex").getCount(), 20))
            {
                Vertex v;
                int n = 0;
                float minx = Float.MAX_VALUE,
                        maxx = -Float.MAX_VALUE,
                        miny = Float.MAX_VALUE,
                        maxy = -Float.MAX_VALUE,
                        minz = Float.MAX_VALUE,
                        maxz = -Float.MAX_VALUE;

                long s = System.nanoTime();
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
                long e = System.nanoTime();
                System.out.printf("%f vertices per second.\n", (float) (e - s) / n);
                System.out.printf("x: %f < %f @ %f\n", minx, maxx, (minx + maxx) / 2);
                System.out.printf("y: %f < %f @ %f\n", miny, maxy, (miny + maxy) / 2);
                System.out.printf("z: %f < %f @ %f\n", minz, maxz, (minz + maxz) / 2);


            }
            catch (Exception e)
            {
                e.printStackTrace();
            }

            try
            {
                BufferedImage bi = BluePrintGenerator.CreateImage(r, 1024, 0.1f, BluePrintGenerator.Axis.X_Z);
                ImageIO.write(bi, "jpg", new File("C:\\Users\\Ben\\o.jpg"));
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
