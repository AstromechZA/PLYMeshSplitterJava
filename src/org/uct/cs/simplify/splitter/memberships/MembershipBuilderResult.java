package org.uct.cs.simplify.splitter.memberships;

import gnu.trove.map.TIntObjectMap;
import org.uct.cs.simplify.util.CompactBitArray;
import org.uct.cs.simplify.util.XBoundingBox;

public class MembershipBuilderResult
{
    public final TIntObjectMap<XBoundingBox> subNodes;
    public final CompactBitArray memberships;

    public MembershipBuilderResult(TIntObjectMap<XBoundingBox> subNodes, CompactBitArray memberships)
    {
        this.subNodes = subNodes;
        this.memberships = memberships;
    }
}
