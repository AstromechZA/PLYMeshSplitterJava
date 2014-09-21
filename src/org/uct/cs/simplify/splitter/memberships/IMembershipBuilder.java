package org.uct.cs.simplify.splitter.memberships;

import org.uct.cs.simplify.ply.reader.PLYReader;
import org.uct.cs.simplify.util.XBoundingBox;

import java.io.IOException;

public interface IMembershipBuilder
{
    MembershipBuilderResult build(PLYReader reader, XBoundingBox boundingBox) throws IOException;

    int getSplitRatio();
}
