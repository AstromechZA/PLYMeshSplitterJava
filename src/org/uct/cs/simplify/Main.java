package org.uct.cs.simplify;

import org.uct.cs.simplify.img.BluePrintGenerator;
import org.uct.cs.simplify.ply.reader.PLYReader;
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
