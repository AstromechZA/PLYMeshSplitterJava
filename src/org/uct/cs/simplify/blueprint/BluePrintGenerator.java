package org.uct.cs.simplify.blueprint;

import javafx.geometry.Point2D;
import org.uct.cs.simplify.model.MemoryMappedVertexReader;
import org.uct.cs.simplify.model.Vertex;
import org.uct.cs.simplify.ply.reader.PLYReader;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.IOException;
import java.util.Arrays;

/** Class providing methods to build interesting blueprint-like images from a PLY model. * */
public class BluePrintGenerator
{
    private static final Color DEFAULT_BACKGROUND = new Color(100, 100, 2 * 100);
    private static final Color DEFAULT_FOREGROUND = Color.white;
    private static final int BYTE_MASK = 0xFF;

    public static BlueprintGeneratorResult createImage(PLYReader reader, int resolution, float alphaAdjustment, CoordinateSpace type, int skipSize)
        throws IOException
    {
        return createImage(reader, resolution, DEFAULT_BACKGROUND, DEFAULT_FOREGROUND, alphaAdjustment, type, skipSize);
    }

    public static BlueprintGeneratorResult createImage(
        PLYReader reader, int resolution, Color background, Color foreground, float alphaAdjustment, CoordinateSpace type, int skipSize
    )
        throws IOException
    {
        return makeBufferedImage(reader, resolution, background, foreground, alphaAdjustment, type, skipSize);
    }

    /**
     * Return a image with the models geometry projected onto the canvas.
     *
     * @param reader          The model reader
     * @param resolution      The size of the output image (This is a square image)
     * @param background      The background Color
     * @param foreground      The forground Color
     * @param alphaAdjustment The blend adjustment (0.1 is good)
     * @param coordinateSpace Which axes to project onto the canvas
     * @return The resulting BufferedImage
     * @throws IOException
     */
    private static BlueprintGeneratorResult makeBufferedImage(
        PLYReader reader, int resolution, Color background, Color foreground, float alphaAdjustment,
        CoordinateSpace coordinateSpace,
        int skipSize
    )
        throws IOException
    {
        if (skipSize < 1) throw new RuntimeException("Skipsize must be atleast 1");

        IAxisValueGetter avg = parseAVG(coordinateSpace);

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

        try (MemoryMappedVertexReader vr = new MemoryMappedVertexReader(reader))
        {
            int c = vr.getCount();
            Vertex v = new Vertex(0, 0, 0);
            for (int i = 0; i < c; i += skipSize)
            {
                vr.get(i, v);
                int tx = (int) (center + ((avg.getPrimaryAxisValue(v) - r.getCenterX()) * ratio));
                int ty = (int) (center - ((avg.getSecondaryAxisValue(v) - r.getCenterY()) * ratio));
                int index = ty * w + tx;
                pixels[ index ] = blend(pixels[ index ], fgi, alphaAdjustment);
            }
        }
        return new BlueprintGeneratorResult(bi, center, ratio, r.getCenterX(), r.getCenterY());
    }

    /**
     * Constructs an AxisValueGetter for the specific coordinate space
     *
     * @param coordinateSpace The coordinate space required.
     * @return The AxisValueGetter object
     */
    private static IAxisValueGetter parseAVG(CoordinateSpace coordinateSpace)
    {
        if (coordinateSpace == CoordinateSpace.X_Y) return new XYAxisValueGetter();
        if (coordinateSpace == CoordinateSpace.X_Z) return new XZAxisValueGetter();
        return new YZAxisValueGetter();
    }

    private static Rectangle2D calculateBounds(PLYReader reader, IAxisValueGetter avg) throws IOException
    {
        try (MemoryMappedVertexReader vr = new MemoryMappedVertexReader(reader))
        {
            float minx = Float.MAX_VALUE,
                maxx = -Float.MAX_VALUE,
                miny = Float.MAX_VALUE,
                maxy = -Float.MAX_VALUE;
            float pr, se;

            Vertex v;
            int c = vr.getCount();
            for (int i = 0; i < c; i++)
            {
                v = vr.get(i);
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
        int dr = (bgi >> (8 * 2)) & BYTE_MASK;
        int dg = (bgi >> 8) & BYTE_MASK;
        int db = bgi & BYTE_MASK;

        int sr = (fgi >> (8 * 2)) & BYTE_MASK;
        int sg = (fgi >> 8) & BYTE_MASK;
        int sb = fgi & BYTE_MASK;

        int rr = (int) ((sr * amount) + (dr * namount)) & BYTE_MASK;
        int rg = (int) ((sg * amount) + (dg * namount)) & BYTE_MASK;
        int rb = (int) ((sb * amount) + (db * namount)) & BYTE_MASK;

        return (rr << (8 * 2)) + (rg << 8) + rb;
    }

    public enum CoordinateSpace
    {
        X_Y, X_Z, Y_Z
    }

    public interface IAxisValueGetter
    {
        float getPrimaryAxisValue(Vertex v);

        float getSecondaryAxisValue(Vertex v);
    }

    public static class XYAxisValueGetter implements IAxisValueGetter
    {
        @Override
        public float getPrimaryAxisValue(Vertex v)
        {
            return v.x;
        }

        @Override
        public float getSecondaryAxisValue(Vertex v)
        {
            return v.y;
        }
    }

    public static class XZAxisValueGetter implements IAxisValueGetter
    {
        @Override
        public float getPrimaryAxisValue(Vertex v)
        {
            return v.x;
        }

        @Override
        public float getSecondaryAxisValue(Vertex v)
        {
            return v.z;
        }
    }

    public static class YZAxisValueGetter implements IAxisValueGetter
    {
        @Override
        public float getPrimaryAxisValue(Vertex v)
        {
            return v.y;
        }

        @Override
        public float getSecondaryAxisValue(Vertex v)
        {
            return v.z;
        }
    }

    public static class BlueprintGeneratorResult
    {
        public final BufferedImage output;
        public final int center;
        public final float ratio;
        public final double centerPrimary;
        public final double centerSecondary;

        public BlueprintGeneratorResult(BufferedImage output, int center, float ratio, double centerPrimary, double centerSecondary)
        {
            this.output = output;
            this.center = center;
            this.ratio = ratio;
            this.centerPrimary = centerPrimary;
            this.centerSecondary = centerSecondary;
        }

        public Point2D getWorldPointFromImage(int x, int y)
        {
            double tx = ((x - center) / ratio) + centerPrimary;
            double ty = ((center - y) / ratio) + centerSecondary;
            return new Point2D(tx, ty);
        }
    }
}
