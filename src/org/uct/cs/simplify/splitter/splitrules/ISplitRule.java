package org.uct.cs.simplify.splitter.splitrules;

import org.uct.cs.simplify.filebuilder.PHFNode;

public interface ISplitRule
{
    boolean canSplit(PHFNode node);
}
