package org.uct.cs.simplify.img;

import org.uct.cs.simplify.ply.reader.PLYReader;
import org.uct.cs.simplify.ply.reader.Vertex;
import org.uct.cs.simplify.ply.reader.VertexReader;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.Arrays;

public class ModelImager
{

    public static BufferedImage CreateImage(PLYReader reader, int resolution, Color background, Color foreground, float alphaAdjustment) throws Exception
    {
        BufferedImage bi = new BufferedImage(resolution, resolution, BufferedImage.TYPE_INT_RGB);

        Rectangle2D r = calculateBounds(reader);

        int[] pixels = ((DataBufferInt) bi.getRaster().getDataBuffer()).getData();

        int bgi = background.getRGB();
        int fgi = foreground.getRGB();

        Arrays.fill(pixels, bgi);

        int w = bi.getWidth();

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

                int tx = (int) (center + (ix - r.getCenterX()) * ratio);
                int ty = (int) (center + (iy - r.getCenterY()) * ratio);

                int index = ty * w + tx;
                pixels[ index ] = blend(pixels[ index ], fgi, alphaAdjustment);
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

    private static int blend(int bgi, int fgi, float amount)
    {
        float namount = 1 - amount;
        int dr = (bgi >> 16) & 0xFF;
        int dg = (bgi >> 8) & 0xFF;
        int db = (bgi) & 0xFF;

        int sr = (fgi >> 16) & 0xFF;
        int sg = (fgi >> 8) & 0xFF;
        int sb = (fgi) & 0xFF;

        int rr = (int) (sr * amount + dr * namount) & 0xFF;
        int rg = (int) (sg * amount + dg * namount) & 0xFF;
        int rb = (int) (sb * amount + db * namount) & 0xFF;

        return (rr << 16) + (rg << 8) + (rb);
    }

}
