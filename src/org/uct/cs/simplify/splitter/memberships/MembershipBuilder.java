package org.uct.cs.simplify.splitter.memberships;

import org.uct.cs.simplify.ply.reader.PLYReader;
import org.uct.cs.simplify.util.XBoundingBox;

import java.io.IOException;

public abstract class MembershipBuilder
{
    public abstract MembershipBuilderResult build(PLYReader reader, XBoundingBox boundingBox) throws IOException;

    public abstract int getSplitRatio();

    public abstract boolean isBalanced();

    public static MembershipBuilder get(String s)
    {
        s = s.toLowerCase();
        if (s.equals("octree"))
        {
            return new OcttreeMembershipBuilder();
        }
        else if (s.equals("kdtree"))
        {
            return new KDTreeMembershipBuilder();
        }
        else if (s.equals("vkdtree"))
        {
            return new VariableKDTreeMembershipBuilder();
        }
        else if (s.startsWith("mkdtree"))
        {
            int n = Integer.parseInt(s.substring(7, 8));
            return new MultiwayVariableKDTreeMembershipBuilder(n);
        }
        else
        {
            throw new IllegalArgumentException("'" + s + "' is not a valid name for a hiearchy");
        }
    }
}
