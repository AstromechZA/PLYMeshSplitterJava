package org.uct.cs.simplify.cropping;

import java.awt.geom.Point2D;

public class VerticalUpLine extends LineBase
{
    public VerticalUpLine(Point2D first, Point2D second)
    {
        super(first, second);
    }

    @Override
    public boolean doesExclude(float x, float y)
    {
        return x < first.getX();
    }
}
