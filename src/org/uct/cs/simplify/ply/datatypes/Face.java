package org.uct.cs.simplify.ply.datatypes;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;

public class Face
{
    private final TIntArrayList vertices;

    public Face(TIntArrayList vertexList)
    {
        this.vertices = vertexList;
    }

    public TIntList getVertices()
    {
        return this.vertices;
    }

    public int getNumVertices()
    {
        return this.vertices.size();
    }

}
