package org.uct.cs.simplify.img;

import org.uct.cs.simplify.ply.reader.Vertex;

public class XZAxisValueGetter implements IAxisValueGetter
{
    @Override
    public float getPrimaryAxisValue(Vertex v)
    {
        return v.x;
    }

    @Override
    public float getSecondaryAxisValue(Vertex v)
    {
        return v.z;
    }
}
