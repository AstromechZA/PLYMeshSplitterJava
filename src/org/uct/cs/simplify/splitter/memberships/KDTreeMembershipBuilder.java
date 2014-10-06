package org.uct.cs.simplify.splitter.memberships;

import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import javafx.geometry.Point3D;
import org.uct.cs.simplify.model.MemoryMappedVertexReader;
import org.uct.cs.simplify.ply.reader.PLYReader;
import org.uct.cs.simplify.util.CompactBitArray;
import org.uct.cs.simplify.util.XBoundingBox;
import org.uct.cs.simplify.util.axis.Axis;
import org.uct.cs.simplify.util.axis.IAxisReader;

import java.io.IOException;

public class KDTreeMembershipBuilder implements IMembershipBuilder
{
    @Override
    public MembershipBuilderResult build(PLYReader reader, XBoundingBox boundingBox) throws IOException
    {
        Axis longest = Axis.getLongestAxis(boundingBox);
        IAxisReader axisReader = longest.getReader();

        Point3D center = boundingBox.getCenter();
        float p50 = (float) axisReader.read(center);

        TIntObjectMap<XBoundingBox> subNodes = splitBBIntoSubnodes(boundingBox, longest, center);

        try (MemoryMappedVertexReader vr = new MemoryMappedVertexReader(reader))
        {
            long c = vr.getCount();
            CompactBitArray memberships = new CompactBitArray(1, c);
            for (int i = 0; i < c; i++)
            {
                if (axisReader.read(vr.get(i)) > p50) memberships.set(i, 1);
            }
            return new MembershipBuilderResult(subNodes, memberships);
        }
    }

    @Override
    public int getSplitRatio()
    {
        return 2;
    }

    public static TIntObjectMap<XBoundingBox> splitBBIntoSubnodes(XBoundingBox boundingBox, Axis axis, double onPoint)
    {
        return splitBBIntoSubnodes(boundingBox, axis, new Point3D(onPoint, onPoint, onPoint));
    }

    public static TIntObjectMap<XBoundingBox> splitBBIntoSubnodes(XBoundingBox boundingBox, Axis axis, Point3D center)
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
