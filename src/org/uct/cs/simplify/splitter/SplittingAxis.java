package org.uct.cs.simplify.splitter;

import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import javafx.geometry.Point3D;
import org.uct.cs.simplify.util.XBoundingBox;

public enum SplittingAxis
{
    X, Y, Z;

    public static SplittingAxis getLongestAxis(XBoundingBox bb)
    {
        if (bb.getWidth() > bb.getHeight())
        {
            if (bb.getWidth() > bb.getDepth()) return SplittingAxis.X;
            return SplittingAxis.Z;
        }
        else if (bb.getHeight() > bb.getDepth()) return SplittingAxis.Y;
        return SplittingAxis.Z;
    }

    public static TIntObjectMap<XBoundingBox> splitBBIntoSubnodes(XBoundingBox boundingBox, SplittingAxis axis, double onPoint)
    {
        return splitBBIntoSubnodes(boundingBox, axis, new Point3D(onPoint, onPoint, onPoint));
    }

    public static TIntObjectMap<XBoundingBox> splitBBIntoSubnodes(XBoundingBox boundingBox, SplittingAxis axis, Point3D center)
    {
        TIntObjectMap<XBoundingBox> subNodes = new TIntObjectHashMap<>(2);
        switch (axis)
        {
            case X:
                subNodes.put(
                    0, XBoundingBox.fromTo(
                        boundingBox.getMinX(), boundingBox.getMinY(), boundingBox.getMinZ(),
                        center.getX(), boundingBox.getMaxY(), boundingBox.getMaxZ()
                    )
                );
                subNodes.put(
                    1, XBoundingBox.fromTo(
                        center.getX(), boundingBox.getMinY(), boundingBox.getMinZ(),
                        boundingBox.getMaxX(), boundingBox.getMaxY(), boundingBox.getMaxZ()
                    )
                );
                break;
            case Y:
                subNodes.put(
                    0, XBoundingBox.fromTo(
                        boundingBox.getMinX(), boundingBox.getMinY(), boundingBox.getMinZ(),
                        boundingBox.getMaxX(), center.getY(), boundingBox.getMaxZ()
                    )
                );
                subNodes.put(
                    1, XBoundingBox.fromTo(
                        boundingBox.getMinX(), center.getY(), boundingBox.getMinZ(),
                        boundingBox.getMaxX(), boundingBox.getMaxY(), boundingBox.getMaxZ()
                    )
                );
                break;
            case Z:
                subNodes.put(
                    0, XBoundingBox.fromTo(
                        boundingBox.getMinX(), boundingBox.getMinY(), boundingBox.getMinZ(),
                        boundingBox.getMaxX(), boundingBox.getMaxY(), center.getZ()
                    )
                );
                subNodes.put(
                    1, XBoundingBox.fromTo(
                        boundingBox.getMinX(), boundingBox.getMinY(), center.getZ(),
                        boundingBox.getMaxX(), boundingBox.getMaxY(), boundingBox.getMaxZ()
                    )
                );
                break;
        }

        return subNodes;
    }
}
