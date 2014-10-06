package org.uct.cs.simplify.splitter.memberships;

import org.uct.cs.simplify.model.MemoryMappedVertexReader;
import org.uct.cs.simplify.ply.reader.PLYReader;
import org.uct.cs.simplify.util.axis.IAxisReader;

import java.io.IOException;

public class PercentileFinder
{
    public static final int MAX_ITERATIONS = 10;
    private static final double APPROXIMATION_THRESHOLD = 0.01;
    private final PLYReader modelReader;
    private final IAxisReader axisReader;

    public PercentileFinder(PLYReader reader, IAxisReader axisReader)
    {
        this.modelReader = reader;
        this.axisReader = axisReader;
    }

    public double findPercentile(float percentile, double lowerBound, double upperBound) throws IOException
    {
        try (MemoryMappedVertexReader vr = new MemoryMappedVertexReader(this.modelReader))
        {
            long numVertices = vr.getCount();
            long percentileTarget = (long) (numVertices * percentile);
            long maxError = (long) (APPROXIMATION_THRESHOLD * numVertices);
            long maxThreshold = percentileTarget + maxError;
            long minThreshold = percentileTarget - maxError;

            double min = lowerBound;
            double max = upperBound;
            double approximate = (min + max) / 2;

            for (int i = 0; i < MAX_ITERATIONS; i++)
            {
                int swing = testPercentile(vr, percentile, approximate, minThreshold, maxThreshold);
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
            return approximate;
        }
    }

    private int testPercentile(MemoryMappedVertexReader vr, float percentile, double approximate, long minThreshold, long maxThreshold)
    {
        long numVertices = vr.getCount();

        long count = 0;
        for (long i = 0; i < numVertices; i++)
        {
            if (this.axisReader.read(vr.get(i)) < approximate)
            {
                count++;
                if (count > maxThreshold) return -1;
            }
        }
        return (count < minThreshold) ? 1 : 0;
    }
}
