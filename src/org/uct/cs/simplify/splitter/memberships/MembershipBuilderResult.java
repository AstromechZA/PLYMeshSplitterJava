package org.uct.cs.simplify.splitter.memberships;

import org.uct.cs.simplify.util.CompactBitArray;
import org.uct.cs.simplify.util.XBoundingBox;

import java.util.HashMap;

public class MembershipBuilderResult
{
    public final HashMap<Integer, XBoundingBox> subNodes;
    public final CompactBitArray memberships;

    public MembershipBuilderResult(HashMap<Integer, XBoundingBox> subNodes, CompactBitArray memberships)
    {
        this.subNodes = subNodes;
        this.memberships = memberships;
    }
}
