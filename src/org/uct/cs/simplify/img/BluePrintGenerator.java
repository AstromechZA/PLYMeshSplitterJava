package org.uct.cs.simplify.img;

import org.uct.cs.simplify.ply.reader.ImprovedPLYReader;
import org.uct.cs.simplify.ply.reader.MemoryMappedVertexReader;
import org.uct.cs.simplify.ply.reader.Vertex;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.IOException;
import java.util.Arrays;

public class BluePrintGenerator
{

    public static Color defaultBackground = new Color(102, 102, 204);
    public static Color defaultForeground = Color.white;

    public static BufferedImage CreateImage(ImprovedPLYReader reader, int resolution, float alphaAdjustment) throws IOException
    {
        return makeBufferedImage(reader, resolution, defaultBackground, defaultForeground, alphaAdjustment, Axis.X_Y);
    }

    public static BufferedImage CreateImage(ImprovedPLYReader reader, int resolution, float alphaAdjustment, Axis type) throws IOException
    {
        return makeBufferedImage(reader, resolution, defaultBackground, defaultForeground, alphaAdjustment, type);
    }

    public static BufferedImage CreateImage(ImprovedPLYReader reader, int resolution, Color background, Color foreground, float alphaAdjustment) throws IOException
    {
        return makeBufferedImage(reader, resolution, background, foreground, alphaAdjustment, Axis.X_Y);
    }

    public static BufferedImage CreateImage(ImprovedPLYReader reader, int resolution, Color background, Color foreground, float alphaAdjustment, Axis type) throws IOException
    {
        return makeBufferedImage(reader, resolution, background, foreground, alphaAdjustment, type);
    }

    private static BufferedImage makeBufferedImage(ImprovedPLYReader reader, int resolution, Color background, Color foreground, float alphaAdjustment, Axis type) throws IOException
    {
        IAxisValueGetter avg = parseAVG(type);

        BufferedImage bi = new BufferedImage(resolution, resolution, BufferedImage.TYPE_INT_RGB);

        Rectangle2D r = calculateBounds(reader, avg);

        int[] pixels = ((DataBufferInt) bi.getRaster().getDataBuffer()).getData();

        int bgi = background.getRGB();
        int fgi = foreground.getRGB();

        Arrays.fill(pixels, bgi);

        int w = bi.getWidth();

        int center = resolution / 2;
        float bigdim = (float) Math.max(r.getWidth(), r.getHeight());
        int border = 10;
        float ratio = (resolution - border) / bigdim;

        long position = reader.getElementDimension("vertex").getFirst();

        try (MemoryMappedVertexReader vr = new MemoryMappedVertexReader(reader.getFile(), position, reader.getHeader().getElement("vertex").getCount(), 20))
        {
            int c = vr.getCount();
            Vertex v;
            for (int i = 0; i < c; i++)
            {
                v = vr.get(i);

                int tx = (int) (center + (avg.getPrimaryAxisValue(v) - r.getCenterX()) * ratio);
                int ty = (int) (center - (avg.getSecondaryAxisValue(v) - r.getCenterY()) * ratio);

                int index = ty * w + tx;
                pixels[ index ] = blend(pixels[ index ], fgi, alphaAdjustment);
            }
        }
        return bi;
    }

    private static IAxisValueGetter parseAVG(Axis type)
    {
        if (type == Axis.X_Y) return new XYAxisValueGetter();
        if (type == Axis.X_Z) return new XZAxisValueGetter();
        return new YZAxisValueGetter();
    }

    private static Rectangle2D calculateBounds(ImprovedPLYReader reader, IAxisValueGetter avg) throws IOException
    {
        int c = reader.getHeader().getElement("vertex").getCount();
        long p = reader.getElementDimension("vertex").getFirst();

        try (MemoryMappedVertexReader vr = new MemoryMappedVertexReader(reader.getFile(), p, c, 20))
        {
            int n = 0;
            float minx = Float.MAX_VALUE,
                    maxx = -Float.MAX_VALUE,
                    miny = Float.MAX_VALUE,
                    maxy = -Float.MAX_VALUE;
            float pr, se;

            Vertex v;
            for (int i = 0; i < c; i++)
            {
                v = vr.get(i);
                n += 1;
                pr = avg.getPrimaryAxisValue(v);
                se = avg.getSecondaryAxisValue(v);

                minx = Math.min(minx, pr);
                maxx = Math.max(maxx, pr);

                miny = Math.min(miny, se);
                maxy = Math.max(maxy, se);
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

    public enum Axis
    {
        X_Y, X_Z, Y_Z
    }

}
