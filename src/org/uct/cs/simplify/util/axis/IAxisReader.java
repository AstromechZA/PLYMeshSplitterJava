package org.uct.cs.simplify.util.axis;

import javafx.geometry.Point3D;
import org.uct.cs.simplify.model.Vertex;

public interface IAxisReader
{
    public float read(Vertex v);

    public double read(Point3D p);
}
