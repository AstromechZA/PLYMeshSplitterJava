package org.uct.cs.simplify.splitter.memberships;

import org.uct.cs.simplify.ply.reader.PLYReader;
import org.uct.cs.simplify.util.XBoundingBox;

public interface IMembershipBuilder
{
    public MembershipBuilderResult build(PLYReader reader, XBoundingBox boundingBox);
}
