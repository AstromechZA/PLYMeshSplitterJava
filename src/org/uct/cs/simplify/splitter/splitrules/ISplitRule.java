package org.uct.cs.simplify.splitter.splitrules;

import org.uct.cs.simplify.filebuilder.PackagedHierarchicalNode;

public interface ISplitRule
{
    boolean canSplit(PackagedHierarchicalNode node);
}
