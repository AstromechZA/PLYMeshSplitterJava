package org.uct.cs.simplify.util.axis;

import javafx.geometry.Point3D;
import org.uct.cs.simplify.util.XBoundingBox;

public enum Axis
{
    X,
    Y,
    Z;

    public IAxisReader getReader()
    {
        if (this == X) return new XAxisReader();
        if (this == Y) return new YAxisReader();
        return new ZAxisReader();
    }

    public static IAxisReader getReader(Axis a)
    {
        return a.getReader();
    }

    public static Axis getLongestAxis(XBoundingBox bb)
    {
        if (bb.getWidth() > bb.getHeight())
        {
            return (bb.getWidth() > bb.getDepth()) ? X : Z;
        }
        return (bb.getHeight() > bb.getDepth()) ? Y : Z;
    }

    public double getBBMin(XBoundingBox bb)
    {
        if (this == X) return bb.getMinX();
        if (this == Y) return bb.getMinY();
        return bb.getMinZ();
    }

    public double getBBax(XBoundingBox bb)
    {
        if (this == X) return bb.getMaxX();
        if (this == Y) return bb.getMaxY();
        return bb.getMaxZ();
    }

    public Point3D modifyPointValue(Point3D point, double v)
    {
        if (this == X) return new Point3D(v, point.getY(), point.getZ());
        if (this == Y) return new Point3D(point.getX(), v, point.getZ());
        return new Point3D(point.getX(), point.getY(), v);
    }
}
