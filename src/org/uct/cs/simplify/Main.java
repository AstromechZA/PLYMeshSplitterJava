package org.uct.cs.simplify;

import org.uct.cs.simplify.img.BluePrintGenerator;
import org.uct.cs.simplify.ply.reader.PLYReader;
import org.uct.cs.simplify.util.Timer;

import javax.imageio.ImageIO;
import java.awt.*;
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
                int size = 2048;
                int halfsize = size / 2;

                BufferedImage xy = BluePrintGenerator.CreateImage(r, size, 0.1f, BluePrintGenerator.Axis.X_Y);
                BufferedImage xz = BluePrintGenerator.CreateImage(r, halfsize, 0.02f, BluePrintGenerator.Axis.X_Z);
                BufferedImage yz = BluePrintGenerator.CreateImage(r, halfsize, 0.02f, BluePrintGenerator.Axis.Y_Z);

                BufferedImage bi = new BufferedImage(size + halfsize, size, BufferedImage.TYPE_INT_RGB);
                Graphics2D g = (Graphics2D) bi.getGraphics();
                g.drawImage(xy, 0, 0, null);
                g.drawImage(xz, size, 0, null);
                g.drawImage(yz, size, halfsize, null);
                g.setColor(Color.white);
                g.drawRect(0, 0, size - 1, size - 1);
                g.drawRect(size, 0, halfsize - 1, halfsize - 1);
                g.drawRect(size, halfsize, halfsize - 1, halfsize - 1);

                ImageIO.write(bi, "png", new File("C:\\Users\\Ben\\o.png"));
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
