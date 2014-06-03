package org.uct.cs.simplify.img;

import org.uct.cs.simplify.ply.reader.PLYReader;
import org.uct.cs.simplify.ply.reader.Vertex;
import org.uct.cs.simplify.ply.reader.VertexReader;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

public class ModelImager
{

    public static BufferedImage CreateImage(PLYReader reader, int resolution, Color background, Color foreground, float alphaAdjustment) throws Exception
    {
        BufferedImage bi = new BufferedImage(resolution, resolution, BufferedImage.TYPE_INT_ARGB);

        Rectangle2D r = calculateBounds(reader);

        Graphics g = bi.getGraphics();
        Color bg = new Color(background.getRed(), background.getGreen(), background.getBlue(), 255);
        Color fg = new Color(foreground.getRed(), foreground.getGreen(), foreground.getBlue(), (int) (alphaAdjustment * 255));
        g.setColor(bg);
        g.fillRect(0, 0, resolution, resolution);
        g.setColor(fg);

        int center = resolution / 2;
        float bigdim = (float) Math.max(r.getWidth(), r.getHeight());
        float ratio = resolution / bigdim;

        try (VertexReader vr = new VertexReader(reader.getFile(), reader.getPositionOfElement("vertex"), reader.getHeader().getElement("vertex").getCount(), 20))
        {
            Vertex v;
            while (vr.hasNext())
            {
                v = vr.next();

                float ix = v.x;
                float iy = v.y;

                int tx = center + (int) ((ix - r.getCenterX()) * ratio);
                int ty = center + (int) ((iy - r.getCenterY()) * ratio);

                g.fillRect(tx, ty, 1, 1);
            }
        }

        return bi;
    }

    private static Rectangle2D calculateBounds(PLYReader reader) throws Exception
    {
        int c = reader.getHeader().getElement("vertex").getCount();
        long p = reader.getPositionOfElement("vertex");

        try (VertexReader vr = new VertexReader(reader.getFile(), p, c, 20))
        {
            Vertex v;
            int n = 0;
            float minx = Float.MAX_VALUE,
                    maxx = -Float.MAX_VALUE,
                    miny = Float.MAX_VALUE,
                    maxy = -Float.MAX_VALUE;

            while (vr.hasNext())
            {
                n += 1;
                v = vr.next();

                minx = Math.min(minx, v.x);
                maxx = Math.max(maxx, v.x);

                miny = Math.min(miny, v.y);
                maxy = Math.max(maxy, v.y);
            }
            return new Rectangle2D.Float(minx, miny, maxx - minx, maxy - miny);
        }
    }


}
