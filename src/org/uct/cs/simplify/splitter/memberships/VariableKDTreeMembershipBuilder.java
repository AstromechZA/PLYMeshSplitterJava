package org.uct.cs.simplify.splitter.memberships;

import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import javafx.geometry.Point3D;
import org.uct.cs.simplify.model.MemoryMappedVertexReader;
import org.uct.cs.simplify.model.Vertex;
import org.uct.cs.simplify.ply.reader.PLYReader;
import org.uct.cs.simplify.util.CompactBitArray;
import org.uct.cs.simplify.util.XBoundingBox;
import org.uct.cs.simplify.util.axis.Axis;
import org.uct.cs.simplify.util.axis.IAxisReader;

import java.io.IOException;

public class VariableKDTreeMembershipBuilder implements IMembershipBuilder
{
    private static final float MEDIAN_TARGET = 0.5f;

    @Override
    public MembershipBuilderResult build(PLYReader reader, XBoundingBox boundingBox) throws IOException
    {
        Axis longest = Axis.getLongestAxis(boundingBox);
        IAxisReader axisReader = longest.getReader();
        double min = longest.getBBMin(boundingBox);
        double max = longest.getBBax(boundingBox);

        double p50 = new PercentileFinder(reader, axisReader).findPercentile(MEDIAN_TARGET, min, max);

        TIntObjectMap<XBoundingBox> subNodes = splitBBIntoSubnodes(boundingBox, longest, p50);

        try (MemoryMappedVertexReader vr = new MemoryMappedVertexReader(reader))
        {
            long c = vr.getCount();
            Vertex v = new Vertex(0, 0, 0);
            CompactBitArray memberships = new CompactBitArray(1, c);
            for (int i = 0; i < c; i++)
            {
                vr.get(i, v);
                if (axisReader.read(v) > p50) memberships.set(i, 1);
            }
            return new MembershipBuilderResult(subNodes, memberships);
        }
    }

    @Override
    public int getSplitRatio()
    {
        return 2;
    }

    @Override
    public boolean isBalanced()
    {
        return true;
    }

    public static TIntObjectMap<XBoundingBox> splitBBIntoSubnodes(XBoundingBox boundingBox, Axis longest, double onPoint)
    {
        TIntObjectMap<XBoundingBox> subNodes = new TIntObjectHashMap<>(2);

        Point3D minPoint = boundingBox.getMin();
        Point3D maxPoint = boundingBox.getMax();

        Point3D mid1 = longest.modifyPointValue(maxPoint, onPoint);
        Point3D mid2 = longest.modifyPointValue(minPoint, onPoint);

        subNodes.put(0, new XBoundingBox(minPoint, mid1));
        subNodes.put(1, new XBoundingBox(mid2, maxPoint));

        return subNodes;
    }
}
