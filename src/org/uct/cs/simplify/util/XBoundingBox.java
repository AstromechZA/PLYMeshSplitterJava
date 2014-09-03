package org.uct.cs.simplify.util;

import javafx.geometry.BoundingBox;
import javafx.geometry.Point3D;
import org.uct.cs.simplify.ply.utilities.OctetFinder;

public class XBoundingBox extends BoundingBox
{

    public XBoundingBox(
        double minX,
        double minY,
        double minZ,

        double width,
        double height,
        double depth)
    {
        super(minX, minY, minZ, width, height, depth);
    }

    public XBoundingBox(
        double minX,
        double minY,
        double width,
        double height)
    {
        super(minX, minY, width, height);
    }

    public Point3D getCenter()
    {
        return new Point3D(
            (this.getMinX() + this.getMaxX()) / 2,
            (this.getMinY() + this.getMaxY()) / 2,
            (this.getMinZ() + this.getMaxZ()) / 2
        );
    }

    public XBoundingBox getSubBB(OctetFinder.Octet octet)
    {
        double halfX = getWidth() / 2;
        double halfY = getHeight() / 2;
        double halfZ = getDepth() / 2;

        double quarterX = halfX / 2;
        double quarterY = halfY / 2;
        double quarterZ = halfZ / 2;

        Point3D center = getCenter();

        int mx = octet.xm - 1;
        int my = octet.ym - 1;
        int mz = octet.zm - 1;

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
