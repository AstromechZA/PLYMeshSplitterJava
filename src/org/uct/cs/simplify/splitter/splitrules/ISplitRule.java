package org.uct.cs.simplify.splitter.splitrules;

import org.uct.cs.simplify.filebuilder.PackagedHierarchicalNode;

public interface ISplitRule
{
    public boolean canSplit(PackagedHierarchicalNode node);
}
