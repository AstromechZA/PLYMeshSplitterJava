package org.uct.cs.simplify.splitter.memberships;

import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import javafx.geometry.Point3D;
import org.uct.cs.simplify.model.SimpleVertexReader;
import org.uct.cs.simplify.model.Vertex;
import org.uct.cs.simplify.ply.reader.PLYReader;
import org.uct.cs.simplify.util.CompactBitArray;
import org.uct.cs.simplify.util.XBoundingBox;

import java.io.IOException;

public class OcttreeMembershipBuilder extends MembershipBuilder
{
    @Override
    public MembershipBuilderResult build(PLYReader reader, XBoundingBox boundingBox) throws IOException
    {
        Point3D center = boundingBox.getCenter();
        TIntObjectMap<XBoundingBox> subNodes = new TIntObjectHashMap<>(8);
        subNodes.put(
            0, XBoundingBox.fromTo(
                boundingBox.getMinX(),
                boundingBox.getMinY(),
                boundingBox.getMinZ(),
                center.getX(),
                center.getY(),
                center.getZ()
            )
        );

        subNodes.put(
            1, XBoundingBox.fromTo(
                boundingBox.getMinX(),
                boundingBox.getMinY(),
                center.getZ(),
                center.getX(),
                center.getY(),
                boundingBox.getMaxZ()
            )
        );

        subNodes.put(
            2, XBoundingBox.fromTo(
                boundingBox.getMinX(),
                center.getY(),
                boundingBox.getMinZ(),
                center.getX(),
                boundingBox.getMaxY(),
                center.getZ()
            )
        );

        subNodes.put(
            3, XBoundingBox.fromTo(
                boundingBox.getMinX(),
                center.getY(),
                center.getZ(),
                center.getX(),
                boundingBox.getMaxY(),
                boundingBox.getMaxZ()
            )
        );

        subNodes.put(
            4, XBoundingBox.fromTo(
                center.getX(),
                boundingBox.getMinY(),
                boundingBox.getMinZ(),
                boundingBox.getMaxX(),
                center.getY(),
                center.getZ()
            )
        );

        subNodes.put(
            5, XBoundingBox.fromTo(
                center.getX(),
                boundingBox.getMinY(),
                center.getZ(),
                boundingBox.getMaxX(),
                center.getY(),
                boundingBox.getMaxZ()
            )
        );

        subNodes.put(
            6, XBoundingBox.fromTo(
                center.getX(),
                center.getY(),
                boundingBox.getMinZ(),
                boundingBox.getMaxX(),
                boundingBox.getMaxY(),
                center.getZ()
            )
        );

        subNodes.put(
            7, XBoundingBox.fromTo(
                center.getX(),
                center.getY(),
                center.getZ(),
                boundingBox.getMaxX(),
                boundingBox.getMaxY(),
                boundingBox.getMaxZ()
            )
        );

        try (SimpleVertexReader vr = new SimpleVertexReader(reader))
        {
            Vertex v = new Vertex(0, 0, 0);
            CompactBitArray memberships = new CompactBitArray(3, vr.getCount());
            long vertexIndex = 0;
            OctantFinder ofinder = new OctantFinder(center);
            while (vr.hasNext())
            {
                vr.next(v);
                memberships.set(vertexIndex, ofinder.getNode(v.x, v.y, v.z));
                vertexIndex++;
            }
            return new MembershipBuilderResult(subNodes, memberships);
        }
    }

    @Override
    public int getSplitRatio()
    {
        return 8;
    }

    @Override
    public boolean isBalanced()
    {
        return false;
    }

    private static class OctantFinder
    {
        private final double x, y, z;

        public OctantFinder(Point3D center)
        {
            this.x = center.getX();
            this.y = center.getY();
            this.z = center.getZ();
        }

        public int getNode(double x, double y, double z)
        {
            int v = 0;

            if (x > this.x) v += 4;
            if (y > this.y) v += 2;
            if (z > this.z) v += 1;
            return v;
        }
    }
}
