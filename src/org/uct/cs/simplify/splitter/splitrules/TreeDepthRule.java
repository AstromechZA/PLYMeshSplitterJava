package org.uct.cs.simplify.splitter.splitrules;

import org.uct.cs.simplify.filebuilder.PHFNode;

public class TreeDepthRule implements ISplitRule
{
    private final int numSplits;

    public TreeDepthRule(int numSplits)
    {
        this.numSplits = numSplits;
    }

    @Override
    public boolean canSplit(PHFNode node)
    {
        return node.getDepth() < this.numSplits;
    }
}
