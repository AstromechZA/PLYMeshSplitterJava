package org.uct.cs.simplify.simplifier;

import org.uct.cs.simplify.Constants;

public class SimplificationFactory
{
    private final float overallRatio;

    public SimplificationFactory(long numFacesInEntireMesh, long numFacesInRoot)
    {
        this.overallRatio = numFacesInRoot / (float) numFacesInEntireMesh;
    }

    public float getSimplificationRioForDepth(int depth, int maxDepth)
    {
        float oneOverD = 1.0f / maxDepth;
        float range = oneOverD * Constants.RATIO_VARIANCE_RANGE;

        float start = oneOverD - range;
        float diff = range * 2.0f / (maxDepth - 1);

        float fraction = start + diff * depth;

        return (float) Math.pow(overallRatio, fraction);
    }

}
