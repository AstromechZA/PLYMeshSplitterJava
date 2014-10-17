package org.uct.cs.simplify.simplifier;

import org.uct.cs.simplify.Constants;

public class SimplificationFactory
{
    private final float overallRatio;
    private final int splitRatio;

    public SimplificationFactory(long numFacesInEntireMesh, long numFacesInRoot, int splitRatio)
    {
        this.overallRatio = numFacesInRoot / (float) numFacesInEntireMesh;
        this.splitRatio = splitRatio;
    }

    public float getSimplificationRatioForDepth(int depth, int maxDepth)
    {
        float oneOverD = 1.0f / maxDepth;
        float range = oneOverD * Constants.RATIO_VARIANCE_RANGE;

        float start = oneOverD - range;
        float diff = range * 2.0f / (maxDepth - 1);

        float fraction = start + diff * depth;

        return (float) Math.pow(overallRatio, fraction);
    }

    public float getSimplificationRatioForDepth(int depth, int maxDepth, long numFaces)
    {
        // is root node?
        if (depth == 0)
        {
            return Constants.FACES_IN_ROOT / (float) numFaces;
        }

        // interior?
        return getSimplificationRatioForDepth(depth, maxDepth);
    }

}
