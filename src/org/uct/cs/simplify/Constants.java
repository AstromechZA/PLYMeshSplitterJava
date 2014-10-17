package org.uct.cs.simplify;

import org.uct.cs.simplify.splitter.memberships.IMembershipBuilder;
import org.uct.cs.simplify.splitter.memberships.MultiwayVariableKDTreeMembershipBuilder;

/**
 * A more central store for important constants as well as some comments on what the reasoning behind them is
 */
public class Constants
{
    // The method the FileBuilder uses to split up the mesh
    public static final IMembershipBuilder MEMBERSHIP_BUILDER = new MultiwayVariableKDTreeMembershipBuilder(4);

    // desired number of faces in root node of output
    public static final long FACES_IN_ROOT = 400_000;

    // max number of faces per leaf node
    public static final long MAX_FACES_PER_LEAF = 100_000;

    // make sure the mesh fits inside a box of this size (Viewer depends on this)
    public static final int PHF_RESCALE_SIZE = 1024;

    // allow simplification ratio to be amplified and reduces in different levels of the tree, by this amount.
    public static final float RATIO_VARIANCE_RANGE = 0.5f;

    // others
    public static final int BYTE_MASK = 0xFF;
    public static final int SHORT_MASK = 0xFFFF;
    public static final int INT_MASK = 0xFFFFFFFF;
    public static final long LONG_MASK = 0xFFFFFFFFL;
}
