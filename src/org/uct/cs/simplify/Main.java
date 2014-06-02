package org.uct.cs.simplify;

import org.uct.cs.simplify.ply.PLYElement;
import org.uct.cs.simplify.ply.PLYHeader;
import org.uct.cs.simplify.util.Timer;

import java.io.File;
import java.io.IOException;

public class Main
{

    public static void main(String[] args)
    {
        //String filename = "C:\\Users\\Ben\\Desktop\\Gede Palace _3_Mio.ply";
        String filename = "C:\\Users\\Ben\\Desktop\\Chapel_of_Nossa_Senhora_de_Baluarte_ao.ply";

        try (Timer ignored = new Timer("Entire read"))
        {

            PLYHeader ph = new PLYHeader(new File(filename));

            System.out.println(ph.getFormat());
            for (PLYElement e : ph.getElements())
            {
                System.out.println(e.toString());
                e.getProperties().forEach(System.out::println);
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }
}
