package org.uct.cs.simplify.splitter.memberships;

import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import javafx.geometry.Point3D;
import org.uct.cs.simplify.model.FastBufferedVertexReader;
import org.uct.cs.simplify.model.Vertex;
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

        TIntObjectMap<XBoundingBox> subNodes = splitBBIntoSubnodes(boundingBox, longest);

        try (FastBufferedVertexReader vr = new FastBufferedVertexReader(reader))
        {
            Vertex v = new Vertex(0, 0, 0);
            CompactBitArray memberships = new CompactBitArray(1, vr.getCount());
            long vertexIndex = 0;
            while (vr.hasNext())
            {
                vr.next(v);
                if (axisReader.read(v) > p50) memberships.set(vertexIndex, 1);
                vertexIndex++;
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
        return false;
    }

    public static TIntObjectMap<XBoundingBox> splitBBIntoSubnodes(XBoundingBox boundingBox, Axis longest)
    {
        TIntObjectMap<XBoundingBox> subNodes = new TIntObjectHashMap<>(2);

        // first calculate longest axis
        IAxisReader axisReader = longest.getReader();

        Point3D minPoint = boundingBox.getMin();
        Point3D maxPoint = boundingBox.getMax();
        double split = (axisReader.read(maxPoint) - axisReader.read(minPoint)) / 2;

        Point3D mid1 = longest.modifyPointValue(maxPoint, split);
        Point3D mid2 = longest.modifyPointValue(minPoint, split);

        subNodes.put(0, new XBoundingBox(minPoint, mid1));
        subNodes.put(1, new XBoundingBox(mid2, maxPoint));

        return subNodes;
    }


}
