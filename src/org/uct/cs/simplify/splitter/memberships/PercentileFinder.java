package org.uct.cs.simplify.splitter.memberships;

import org.uct.cs.simplify.model.FastBufferedVertexReader;
import org.uct.cs.simplify.model.Vertex;
import org.uct.cs.simplify.ply.reader.PLYReader;
import org.uct.cs.simplify.util.Outputter;
import org.uct.cs.simplify.util.axis.IAxisReader;

import java.io.IOException;

public class PercentileFinder
{
    public static final int MAX_ITERATIONS = 10;
    private static final double APPROXIMATION_THRESHOLD = 0.001;
    private final PLYReader modelReader;
    private final IAxisReader axisReader;

    public PercentileFinder(PLYReader reader, IAxisReader axisReader)
    {
        this.modelReader = reader;
        this.axisReader = axisReader;
    }

    public double findPercentile(float percentile, double lowerBound, double upperBound) throws IOException
    {
        long numVertices = modelReader.getHeader().getElement("vertex").getCount();
        long skips = (numVertices / 100_000_000);
        double divNumVertices = numVertices / (double) (skips + 1);
        long percentileTarget = (long) (divNumVertices * percentile);
        long maxError = (long) (APPROXIMATION_THRESHOLD * divNumVertices);
        long maxThreshold = percentileTarget + maxError;
        long minThreshold = percentileTarget - maxError;

        double min = lowerBound;
        double max = upperBound;
        double approximate = (min + max) / 2;

        Outputter.debugf("attempting to find p%d from %d vertices (skip %d)%n", (int) (percentile * 100), (long) divNumVertices, skips);

        for (int i = 0; i < MAX_ITERATIONS; i++)
        {
            try (FastBufferedVertexReader vr = new FastBufferedVertexReader(this.modelReader))
            {
                int swing = (skips == 0) ?
                    testPercentile(vr, approximate, minThreshold, maxThreshold)
                    :
                    testPercentile(vr, approximate, minThreshold, maxThreshold, skips);
                if (swing < 0)
                {
                    max = approximate;
                }
                else if (swing > 0)
                {
                    min = approximate;
                }
                else
                {
                    return approximate;
                }
                approximate = (min + max) / 2;
            }
        }
        return approximate;
    }

    private int testPercentile(FastBufferedVertexReader vr, double approximate, long minThreshold, long maxThreshold) throws IOException
    {
        long count = 0;
        Vertex v = new Vertex(0, 0, 0);
        while (vr.hasNext())
        {
            vr.next(v);
            if (this.axisReader.read(v) < approximate)
            {
                count++;
                if (count > maxThreshold) return -1;
            }
        }
        return (count < minThreshold) ? 1 : 0;
    }

    private int testPercentile(FastBufferedVertexReader vr, double approximate, long minThreshold, long maxThreshold, long skips) throws IOException
    {
        long count = 0;
        Vertex v = new Vertex(0, 0, 0);
        while (vr.hasNext())
        {
            vr.next(v);
            if (this.axisReader.read(v) < approximate)
            {
                count++;
                if (count > maxThreshold) return -1;
            }
            vr.skipNext(skips);
        }
        return (count < minThreshold) ? 1 : 0;
    }
}
