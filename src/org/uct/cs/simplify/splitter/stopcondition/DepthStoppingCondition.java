package org.uct.cs.simplify.splitter.stopcondition;

public class DepthStoppingCondition implements IStoppingCondition
{
    private final int maxdepth;

    public DepthStoppingCondition(int maxdepth)
    {
        this.maxdepth = maxdepth;
    }

    @Override
    public boolean met(int currentDepth, long currentFaces)
    {
        return currentDepth >= maxdepth;
    }
}
