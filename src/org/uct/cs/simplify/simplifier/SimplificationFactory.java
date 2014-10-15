package org.uct.cs.simplify.simplifier;

public class SimplificationFactory
{
    private static final long DEFAULT_NUM_FACES_IN_ROOT = 400_000;
    private static final float VARIANCE_RANGE = 0.5f;

    private final float overallRatio;

    public SimplificationFactory(long numFacesInEntireMesh)
    {
        this(numFacesInEntireMesh, DEFAULT_NUM_FACES_IN_ROOT);
    }

    public SimplificationFactory(long numFacesInEntireMesh, long numFacesInRoot)
    {
        this.overallRatio = numFacesInRoot / (float) numFacesInEntireMesh;
    }

    public float getSimplificationRioForDepth(int depth, int maxDepth)
    {
        float oneOverD = 1.0f / maxDepth;
        float range = oneOverD * VARIANCE_RANGE;

        float start = oneOverD - range;
        float diff = range * 2.0f / (maxDepth - 1);

        float fraction = start + diff * depth;

        return (float) Math.pow(overallRatio, fraction);
    }

}
