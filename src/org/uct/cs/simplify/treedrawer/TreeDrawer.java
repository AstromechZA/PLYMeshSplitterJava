package org.uct.cs.simplify.treedrawer;

import org.uct.cs.simplify.filebuilder.PHFNode;
import org.uct.cs.simplify.util.Useful;

import java.awt.*;
import java.awt.image.BufferedImage;

public class TreeDrawer
{
    public static BufferedImage Draw(PHFNode root, int width, int height)
    {
        int d = MaxDepth(root);

        BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
        Graphics2D g = bi.createGraphics();
        g.setColor(Color.white);
        g.fillRect(0, 0, width, height);

        g.setColor(Color.BLACK);

        SubDraw(g, root, 0, width, 0, (height - 10) / d);

        return bi;
    }

    private static int MaxDepth(PHFNode r)
    {
        return r.getDepthOfDeepestChild();
    }

    private static void SubDraw(Graphics2D g, PHFNode n, int x1, int x2, int y, int dd)
    {
        int numc = n.getChildren().size();

        int mid = (x1 + x2) / 2;

        if (numc == 0)
        {
            g.setColor(Color.red);
            g.fillRect(mid - 3, y - 3, 6, 6);
            g.setColor(Color.black);
            g.drawRect(mid - 3, y - 3, 6, 6);
            return;
        }

        long total = 0;
        for (PHFNode node : n.getChildren())
        {
            total += node.getNumFaces();
        }

        double ratio = n.getNumFaces() / (double) total;

        g.setColor(Useful.colourFor(ratio));

        int subwidths = (x2 - x1) / numc;
        int wpad = (x2 - x1) / (2 * numc);

        int[] ax = new int[]{ mid, x2 - wpad, x1 + wpad };
        int[] ay = new int[]{ y, y + dd, y + dd };

        g.fillPolygon(ax, ay, 3);

        for (int i = 0; i < numc; i++)
        {
            int xx1 = x1 + i * subwidths;
            int xx2 = xx1 + subwidths;
            int yy = y + dd;
            int mm = (xx1 + xx2) / 2;

            g.setColor(Color.black);
            g.drawLine(mid, y, mm, yy);
            SubDraw(g, n.getChildren().get(i), xx1, xx2, yy, dd);
        }
    }
}
