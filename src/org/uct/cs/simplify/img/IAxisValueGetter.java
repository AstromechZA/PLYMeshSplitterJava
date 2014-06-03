package org.uct.cs.simplify.img;

import org.uct.cs.simplify.ply.reader.Vertex;

public interface IAxisValueGetter
{
    public float getPrimaryAxisValue(Vertex v);

    public float getSecondaryAxisValue(Vertex v);
}
