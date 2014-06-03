package org.uct.cs.simplify.ply.reader;

import java.util.List;

public class Face
{
    private List<Integer> vertices;

    public Face(List<Integer> vertexList)
    {
        this.vertices = vertexList;
    }

    public int getNumVertices()
    {
        return this.vertices.size();
    }

}
