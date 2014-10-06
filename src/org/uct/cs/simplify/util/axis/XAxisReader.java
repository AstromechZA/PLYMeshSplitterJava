package org.uct.cs.simplify.util.axis;

import javafx.geometry.Point3D;
import org.uct.cs.simplify.model.Vertex;

public class XAxisReader implements IAxisReader
{
    public float read(Vertex v)
    {
        return v.x;
    }

    public double read(Point3D p)
    {
        return p.getX();
    }
}
