package org.uct.cs.simplify.splitter.memberships;

import gnu.trove.map.TIntObjectMap;
import org.uct.cs.simplify.model.MemoryMappedVertexReader;
import org.uct.cs.simplify.model.Vertex;
import org.uct.cs.simplify.ply.reader.PLYReader;
import org.uct.cs.simplify.splitter.SplittingAxis;
import org.uct.cs.simplify.util.CompactBitArray;
import org.uct.cs.simplify.util.XBoundingBox;

import java.io.IOException;

public class VariableKDTreeMembershipBuilder implements IMembershipBuilder
{
    private static final double APPROXIMATION_THRESHOLD = 0.01;

    @Override
    public MembershipBuilderResult build(PLYReader reader, XBoundingBox boundingBox) throws IOException
    {
        try (MemoryMappedVertexReader vr = new MemoryMappedVertexReader(reader))
        {
            SplittingAxis longest = SplittingAxis.getLongestAxis(boundingBox);
            double splitPoint = calculateMedianInRegion(vr, boundingBox, longest, APPROXIMATION_THRESHOLD);
            TIntObjectMap<XBoundingBox> subNodes = SplittingAxis.splitBBIntoSubnodes(boundingBox, longest, splitPoint);

            int c = vr.getCount();
            CompactBitArray memberships = new CompactBitArray(1, c);
            switch (longest)
            {
                case X:
                    for (int i = 0; i < c; i++)
                    {
                        if (vr.get(i).x > splitPoint) memberships.set(i, 1);
                    }
                    break;
                case Y:
                    for (int i = 0; i < c; i++)
                    {
                        if (vr.get(i).y > splitPoint) memberships.set(i, 1);
                    }
                    break;
                case Z:
                    for (int i = 0; i < c; i++)
                    {
                        if (vr.get(i).z > splitPoint) memberships.set(i, 1);
                    }
                    break;
            }
            return new MembershipBuilderResult(subNodes, memberships);
        }
    }

    private static double calculateMedianInRegion(MemoryMappedVertexReader vr, XBoundingBox bb, SplittingAxis axis, double approximationThreshold)
    {
        double min, max, approximate, ratio;
        switch (axis)
        {
            case X:
                min = bb.getMinX();
                max = bb.getMaxX();
                break;
            case Y:
                min = bb.getMinY();
                max = bb.getMaxY();
                break;
            default:
                min = bb.getMinZ();
                max = bb.getMaxZ();
        }

        float nv = vr.getCount();

        approximate = (min + max) / 2;
        ratio = countValuesLessThan(vr, axis, approximate) / nv;

        double minR = 0.5 - approximationThreshold;
        double maxR = 0.5 + approximationThreshold;

        while (ratio < minR || ratio > maxR)
        {
            if (ratio > 0.5)
            {
                max = approximate;
            }
            else
            {
                min = approximate;
            }

            approximate = (min + max) / 2;
            ratio = countValuesLessThan(vr, axis, approximate) / nv;
        }
        return approximate;
    }

    private static int countValuesLessThan(MemoryMappedVertexReader vr, SplittingAxis axis, double value)
    {
        int c = vr.getCount();
        int count = 0;
        for (int i = 0; i < c; i++)
        {
            if (getValueFromVertex(vr.get(i), axis) < value) count++;
        }
        return count;
    }


    private static double getValueFromVertex(Vertex v, SplittingAxis a)
    {
        if (a == SplittingAxis.X) return v.x;
        if (a == SplittingAxis.Y) return v.y;
        return v.z;
    }

    @Override
    public int getSplitRatio()
    {
        return 2;
    }
}
