package org.uct.cs.simplify;

import org.uct.cs.simplify.ply.header.PLYElement;
import org.uct.cs.simplify.ply.header.PLYHeader;
import org.uct.cs.simplify.ply.header.PLYPropertyBase;
import org.uct.cs.simplify.util.Timer;

import java.io.File;
import java.io.IOException;

public class Main
{

    public static void main(String[] args)
    {
        String filename = "C:\\Users\\Ben\\Desktop\\Chapel_of_Nossa_Senhora_de_Baluarte_ao.ply";

        try (Timer ignored = new Timer("Entire read"))
        {

            PLYHeader ph = new PLYHeader(new File(filename));

            System.out.println(ph.getFormat());
            for (PLYElement e : ph.getElements().values())
            {
                System.out.println(e);
                e.getProperties().forEach(System.out::println);
            }

            System.out.println(ph.getDataOffset());
            System.out.println(PLYPropertyBase.bytesInType(PLYPropertyBase.DataType.DOUBLE));
            System.out.println(ph.getElement("face"));
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }
}
