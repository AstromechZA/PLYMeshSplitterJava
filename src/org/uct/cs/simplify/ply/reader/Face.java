package org.uct.cs.simplify.ply.reader;

import java.util.List;

public class Face
{
    private final List<Integer> vertices;

    public Face(List<Integer> vertexList)
    {
        this.vertices = vertexList;
    }

    public List<Integer> getVertices()
    {
        return this.vertices;
    }

    public int getNumVertices()
    {
        return this.vertices.size();
    }

}
