package org.uct.cs.simplify.splitter.stopcondition;

public class LowerFaceBoundStoppingCondition implements IStoppingCondition
{
    private final long minFaces;

    public LowerFaceBoundStoppingCondition(long minFaces)
    {
        this.minFaces = minFaces;
    }

    @Override
    public boolean met(int currentDepth, long currentFaces)
    {
        return currentFaces < minFaces;
    }
}

