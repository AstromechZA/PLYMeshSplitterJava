package org.uct.cs.simplify.splitter.stopcondition;

public interface IStoppingCondition
{
    public boolean met(int currentDepth, long currentFaces);
}
