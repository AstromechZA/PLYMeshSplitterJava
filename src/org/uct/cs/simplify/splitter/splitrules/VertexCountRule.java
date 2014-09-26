package org.uct.cs.simplify.splitter.splitrules;

import org.uct.cs.simplify.filebuilder.PHFNode;

public class VertexCountRule implements ISplitRule
{
    private final int maxVertices;

    public VertexCountRule(int maxVertices)
    {
        this.maxVertices = maxVertices;
    }

    @Override
    public boolean canSplit(PHFNode node)
    {
        return node.getNumVertices() > this.maxVertices;
    }
}
