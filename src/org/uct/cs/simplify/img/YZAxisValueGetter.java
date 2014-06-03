package org.uct.cs.simplify.img;

import org.uct.cs.simplify.ply.reader.Vertex;

public class YZAxisValueGetter implements IAxisValueGetter
{

    @Override
    public float getPrimaryAxisValue(Vertex v)
    {
        return v.y;
    }

    @Override
    public float getSecondaryAxisValue(Vertex v)
    {
        return v.z;
    }
}
