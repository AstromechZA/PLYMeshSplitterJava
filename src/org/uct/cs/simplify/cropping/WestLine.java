package org.uct.cs.simplify.cropping;

import javafx.geometry.Point2D;

public class WestLine extends GradientLineBase
{
    public WestLine(Point2D first, Point2D second)
    {
        super(first, second);
    }

    @Override
    public boolean doesExclude(float x, float y)
    {
        return aboveTheLine(x, y);
    }
}
