package org.uct.cs.simplify.octant;

import javafx.geometry.Point3D;
import org.uct.cs.simplify.util.XBoundingBox;

public class OctantFinder
{
    private final Point3D center;
    private final Octant[] by_i;

    public OctantFinder(XBoundingBox bb)
    {
        this.center = new Point3D(
            (bb.getMaxX() + bb.getMinX()) / 2,
            (bb.getMaxY() + bb.getMinY()) / 2,
            (bb.getMaxZ() + bb.getMinZ()) / 2
        );
        this.by_i = Octant.values();
    }

    public OctantFinder(Point3D center)
    {
        this.center = center;
        this.by_i = Octant.values();
    }

    public Octant getOctant(double x, double y, double z)
    {
        int i = 0;
        if (x < this.center.getX()) i += 4;
        if (y < this.center.getY()) i += 2;
        if (z < this.center.getZ()) i += 1;
        return this.by_i[ i ];
    }
}
