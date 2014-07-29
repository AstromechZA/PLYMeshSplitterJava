package org.uct.cs.simplify.ply.utilities;

import javafx.geometry.BoundingBox;
import javafx.geometry.Point3D;

public class OctetFinder
{
    private final Point3D center;
    private final Octet[] by_i;

    public OctetFinder(BoundingBox bb)
    {
        this.center = new Point3D(
                (bb.getMaxX() + bb.getMinX()) / 2,
                (bb.getMaxY() + bb.getMinY()) / 2,
                (bb.getMaxZ() + bb.getMinZ()) / 2
        );
        this.by_i = Octet.values();
    }

    public OctetFinder(Point3D center)
    {
        this.center = center;
        this.by_i = Octet.values();
    }

    public Octet getOctet(double x, double y, double z)
    {
        int i = 0;
        if (x < this.center.getX()) i += 4;
        if (y < this.center.getY()) i += 2;
        if (z < this.center.getZ()) i += 1;
        return this.by_i[i];
    }

    public enum Octet
    {
        PXPYPZ(+1, +1, +1),
        PXPYnz(+1, +1, -1),
        PXnyPZ(+1, -1, +1),
        PXnynz(+1, -1, -1),
        nxPYPZ(-1, +1, +1),
        nxPYnz(-1, +1, -1),
        nxnyPZ(-1, -1, +1),
        nxnynz(-1, -1, -1);

        private final int xm, ym, zm;

        Octet(int xm, int ym, int zm)
        {
            this.xm = xm;
            this.ym = ym;
            this.zm = zm;
        }

        public Point3D calculateCenterBasedOn(Point3D splitPoint, int processDepth, BoundingBox bb)
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
    }
}
