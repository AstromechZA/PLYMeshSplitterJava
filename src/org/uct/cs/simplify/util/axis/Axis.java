package org.uct.cs.simplify.util.axis;

import org.uct.cs.simplify.util.Pair;
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

    public Pair<Double, Double> getAxisBounds(XBoundingBox bb)
    {
        if (this == X) return new Pair<>(bb.getMinX(), bb.getMaxX());
        if (this == Y) return new Pair<>(bb.getMinY(), bb.getMaxY());
        return new Pair<>(bb.getMinZ(), bb.getMaxZ());
    }


}
