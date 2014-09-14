package org.uct.cs.simplify.splitter.splitrules;

import org.uct.cs.simplify.file_builder.PackagedHierarchicalNode;

public interface ISplitRule
{
    public boolean canSplit(PackagedHierarchicalNode node);
}
