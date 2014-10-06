package org.uct.cs.simplify.util.axis;

import javafx.geometry.Point3D;
import org.uct.cs.simplify.model.Vertex;

public class YAxisReader implements IAxisReader
{
    public float read(Vertex v)
    {
        return v.y;
    }

    public double read(Point3D p)
    {
        return p.getY();
    }
}
