package org.uct.cs.simplify.octant;

import javafx.geometry.Point3D;
import org.uct.cs.simplify.util.XBoundingBox;

public enum Octant
{
    PXPYPZ(+1, +1, +1),
    PXPYnz(+1, +1, -1),
    PXnyPZ(+1, -1, +1),
    PXnynz(+1, -1, -1),
    nxPYPZ(-1, +1, +1),
    nxPYnz(-1, +1, -1),
    nxnyPZ(-1, -1, +1),
    nxnynz(-1, -1, -1);

    public final int xm, ym, zm;

    Octant(int xm, int ym, int zm)
    {
        this.xm = xm;
        this.ym = ym;
        this.zm = zm;
    }

    public Point3D calculateCenterBasedOn(Point3D splitPoint, int processDepth, XBoundingBox bb)
    {
        double div = Math.pow(2, processDepth + 1);

        double bbx = bb.getWidth() / div;
        double bby = bb.getHeight() / div;
        double bbz = bb.getDepth() / div;

        return new Point3D(
            splitPoint.getX() + bbx * this.xm,
            splitPoint.getY() + bby * this.ym,
            splitPoint.getZ() + bbz * this.zm
        );
    }

    public XBoundingBox getSubBB(XBoundingBox bb)
    {
        double halfX = bb.getWidth() / 2;
        double halfY = bb.getHeight() / 2;
        double halfZ = bb.getDepth() / 2;

        double quarterX = halfX / 2;
        double quarterY = halfY / 2;
        double quarterZ = halfZ / 2;

        Point3D center = bb.getCenter();

        int mx = this.xm - 1;
        int my = this.ym - 1;
        int mz = this.zm - 1;

        return new XBoundingBox(
            center.getX() + mx * quarterX,
            center.getY() + my * quarterY,
            center.getZ() + mz * quarterZ,
            halfX,
            halfY,
            halfZ
        );
    }
}
