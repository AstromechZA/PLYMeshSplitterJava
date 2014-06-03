package org.uct.cs.simplify;

import org.uct.cs.simplify.ply.reader.*;
import org.uct.cs.simplify.util.MemRecorder;
import org.uct.cs.simplify.util.Timer;

import java.io.File;
import java.io.IOException;

public class Main
{

    public static void main(String[] args)
    {
        String filename = "C:\\Users\\Ben\\Desktop\\Chapel_of_Nossa_Senhora_de_Baluarte_ao.ply";
        //String filename = "C:\\Users\\Ben\\Desktop\\Gede Palace _3_Mio.ply";

        try (MemRecorder m = new MemRecorder(new File("C:\\Users\\Ben\\o.dat"), 50); Timer ignored = new Timer("Entire read"))
        {

            PLYReader r = new PLYReader(new File(filename));
            int c = r.getHeader().getElement("vertex").getCount();
            long p = r.getPositionOfElement("vertex");

            try (MemoryMappedVertexReader mmvr = new MemoryMappedVertexReader(r.getFile(), p, c, 20))
            {
                int fc = r.getHeader().getElement("face").getCount();
                long fp = r.getPositionOfElement("face");
                long fl = r.getLengthOfElement("face");

                try (MemoryMappedFaceReader fr = new MemoryMappedFaceReader(r.getFile(), fp, fc, fl))
                {
                    Face face;
                    while (fr.hasNext())
                    {
                        face = fr.next();
                        for (Integer i : face.getVertices())
                        {
                            Vertex v = mmvr.get(i);
                        }
                    }
                }
            }

            System.out.printf("%d vertices\n", r.getHeader().getElement("vertex").getCount());

        }
        catch (IOException | InterruptedException e)
        {
            e.printStackTrace();
        }
    }
}
