package org.uct.cs.simplify.util;

import javafx.geometry.BoundingBox;
import javafx.geometry.Point3D;

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

    public XBoundingBox(Point3D minPoint, Point3D maxPoint)
    {
        super(
            minPoint.getX(),
            minPoint.getY(),
            minPoint.getZ(),

            maxPoint.getX() - minPoint.getX(),
            maxPoint.getY() - minPoint.getY(),
            maxPoint.getZ() - minPoint.getZ()
        );
    }

    public static XBoundingBox fromTo(
        double minX,
        double minY,
        double minZ,

        double maxX,
        double maxY,
        double maxZ
    )
    {
        return new XBoundingBox(minX, minY, minZ, maxX - minX, maxY - minY, maxZ - minZ);
    }


    public Point3D getCenter()
    {
        return new Point3D(
            (this.getMinX() + this.getMaxX()) / 2,
            (this.getMinY() + this.getMaxY()) / 2,
            (this.getMinZ() + this.getMaxZ()) / 2
        );
    }

    public Point3D getMin()
    {
        return new Point3D(this.getMinX(), this.getMinY(), this.getMinZ());
    }

    public Point3D getMax()
    {
        return new Point3D(this.getMaxX(), this.getMaxY(), this.getMaxZ());
    }
}
