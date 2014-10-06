package org.uct.cs.simplify.util.axis;

import javafx.geometry.Point3D;
import org.uct.cs.simplify.model.Vertex;

public class ZAxisReader implements IAxisReader
{
    public float read(Vertex v)
    {
        return v.z;
    }

    public double read(Point3D p)
    {
        return p.getZ();
    }
}
