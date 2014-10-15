package org.uct.cs.simplify.splitter.stopcondition;

public class LowerFaceBoundStoppingCondition implements IStoppingCondition
{
    private final int minFaces;

    public LowerFaceBoundStoppingCondition(int minFaces)
    {
        this.minFaces = minFaces;
    }

    @Override
    public boolean met(int currentDepth, long currentFaces)
    {
        return currentFaces < minFaces;
    }
}

