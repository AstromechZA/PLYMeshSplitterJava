package org.uct.cs.simplify.splitter.splitrules;

import org.uct.cs.simplify.file_builder.PackagedHierarchicalNode;

public class TreeDepthRule implements ISplitRule
{
    private final int numSplits;

    public TreeDepthRule(int numSplits)
    {
        this.numSplits = numSplits;
    }

    @Override
    public boolean canSplit(PackagedHierarchicalNode node)
    {
        return node.getDepth() < numSplits;
    }
}
