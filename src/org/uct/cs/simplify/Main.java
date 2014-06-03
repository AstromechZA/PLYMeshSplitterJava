package org.uct.cs.simplify;

import org.uct.cs.simplify.ply.reader.MemoryMappedVertexReader;
import org.uct.cs.simplify.ply.reader.PLYReader;
import org.uct.cs.simplify.ply.reader.Vertex;
import org.uct.cs.simplify.util.Timer;

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
            int c = r.getHeader().getElement("vertex").getCount();
            long p = r.getPositionOfElement("vertex");

            try (MemoryMappedVertexReader mmvr = new MemoryMappedVertexReader(r.getFile(), p, c, 20))
            {
                for (int i = 0; i < c; i++)
                {
                    Vertex n = mmvr.get(0);
                }
            }

            System.out.printf("%d vertices\n", r.getHeader().getElement("vertex").getCount());

        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }
}
