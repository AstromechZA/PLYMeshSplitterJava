package org.uct.cs.simplify.splitter.memberships;

import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import javafx.geometry.Point3D;
import org.uct.cs.simplify.model.FastBufferedVertexReader;
import org.uct.cs.simplify.model.Vertex;
import org.uct.cs.simplify.ply.reader.PLYReader;
import org.uct.cs.simplify.util.CompactBitArray;
import org.uct.cs.simplify.util.ProgressBar;
import org.uct.cs.simplify.util.XBoundingBox;
import org.uct.cs.simplify.util.axis.Axis;
import org.uct.cs.simplify.util.axis.IAxisReader;

import java.io.IOException;

public class MultiwayVariableKDTreeMembershipBuilder extends MembershipBuilder
{
    private final int order;

    public MultiwayVariableKDTreeMembershipBuilder(int order)
    {
        if (order <= 2)
            throw new RuntimeException("MultiwayVariableKDTreeMembershipBuilder must have an order of greater than 2 subnodes.");

        this.order = order;
    }

    @Override
    public MembershipBuilderResult build(PLYReader reader, XBoundingBox boundingBox) throws IOException
    {
        // first calculate longest axis
        Axis longest = Axis.getLongestAxis(boundingBox);
        IAxisReader axisReader = longest.getReader();

        // get min and max of the longest axis from the bounding box
        double min = longest.getBBMin(boundingBox);
        double max = longest.getBBax(boundingBox);

        // now initialise list of split points
        double[] splitPoints = new double[ this.order - 1 ];
        float div = 1.0f / this.order;
        double lastSplitPoint = min;
        PercentileFinder percentileFinder = new PercentileFinder(reader, axisReader);
        for (int i = 1; i < order; i++)
        {
            lastSplitPoint = percentileFinder.findPercentile(i * div, lastSplitPoint, max);
            splitPoints[ i - 1 ] = lastSplitPoint;
        }

        // build subnode bounding boxes
        TIntObjectMap<XBoundingBox> subNodes = new TIntObjectHashMap<>(this.order);

        Point3D minPoint = boundingBox.getMin();
        Point3D maxPoint = boundingBox.getMax();
        double lastPoint = axisReader.read(minPoint);

        int nodeId = 0;
        for (double splitPoint : splitPoints)
        {
            minPoint = longest.modifyPointValue(minPoint, lastPoint);
            maxPoint = longest.modifyPointValue(maxPoint, splitPoint);
            subNodes.put(nodeId, new XBoundingBox(minPoint, maxPoint));
            nodeId++;
            lastPoint = splitPoint;
        }
        minPoint = longest.modifyPointValue(minPoint, lastPoint);
        maxPoint = boundingBox.getMax();
        subNodes.put(nodeId, new XBoundingBox(minPoint, maxPoint));

        try (FastBufferedVertexReader vr = new FastBufferedVertexReader(reader))
        {
            Vertex v = new Vertex(0, 0, 0);
            int numBits = (int) Math.ceil(Math.log(this.order) / Math.log(2));
            CompactBitArray memberships = new CompactBitArray(numBits, vr.getCount());
            try (ProgressBar pb = new ProgressBar("Scanning", vr.getCount()))
            {
                long vertexIndex = 0;
                while (vr.hasNext())
                {
                    vr.next(v);
                    int membership = 0;
                    for (double point : splitPoints)
                    {
                        if (axisReader.read(v) < point) break;
                        membership++;
                    }
                    memberships.set(vertexIndex, membership);
                    vertexIndex++;
                    pb.tick();
                }
            }
            return new MembershipBuilderResult(subNodes, memberships);
        }
    }

    @Override
    public int getSplitRatio()
    {
        return this.order;
    }

    @Override
    public boolean isBalanced()
    {
        return true;
    }
}
