package org.uct.cs.simplify.splitter.splitrules;

import org.uct.cs.simplify.file_builder.PackagedHierarchicalNode;

public class VertexCountRule implements ISplitRule
{
    private final int maxVertices;

    public VertexCountRule(int maxVertices)
    {
        this.maxVertices = maxVertices;
    }

    @Override
    public boolean canSplit(PackagedHierarchicalNode node)
    {
        return node.getNumVertices() > maxVertices;
    }
}
