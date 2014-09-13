package org.uct.cs.simplify.splitter.memberships;

import gnu.trove.map.TIntObjectMap;
import javafx.geometry.Point3D;
import org.uct.cs.simplify.ply.reader.MemoryMappedVertexReader;
import org.uct.cs.simplify.ply.reader.PLYReader;
import org.uct.cs.simplify.splitter.SplittingAxis;
import org.uct.cs.simplify.util.CompactBitArray;
import org.uct.cs.simplify.util.ProgressBar;
import org.uct.cs.simplify.util.XBoundingBox;

import java.io.IOException;

public class KDTreeMembershipBuilder implements IMembershipBuilder
{
    @Override
    public MembershipBuilderResult build(PLYReader reader, XBoundingBox boundingBox) throws IOException
    {
        SplittingAxis longest = SplittingAxis.getLongestAxis(boundingBox);
        Point3D center = boundingBox.getCenter();
        TIntObjectMap<XBoundingBox> subNodes = SplittingAxis.splitBBIntoSubnodes(boundingBox, longest, center);

        try (
            MemoryMappedVertexReader vr = new MemoryMappedVertexReader(reader);
            ProgressBar pb = new ProgressBar("Calculating Memberships", vr.getCount())
        )
        {
            int c = vr.getCount();
            CompactBitArray memberships = new CompactBitArray(1, c);
            switch (longest)
            {
                case X:
                    for (int i = 0; i < c; i++)
                    {
                        pb.tick();
                        if (vr.get(i).x > center.getX()) memberships.set(i, 1);
                    }
                    break;
                case Y:
                    for (int i = 0; i < c; i++)
                    {
                        pb.tick();
                        if (vr.get(i).y > center.getY()) memberships.set(i, 1);
                    }
                    break;
                case Z:
                    for (int i = 0; i < c; i++)
                    {
                        pb.tick();
                        if (vr.get(i).z > center.getZ()) memberships.set(i, 1);
                    }
                    break;
            }
            return new MembershipBuilderResult(subNodes, memberships);
        }
    }
}
