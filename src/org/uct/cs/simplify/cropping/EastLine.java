package org.uct.cs.simplify.cropping;

import javafx.geometry.Point2D;

public class EastLine extends GradientLineBase
{
    public EastLine(Point2D first, Point2D second)
    {
        super(first, second);
    }

    @Override
    public boolean doesExclude(float x, float y)
    {
        return !aboveTheLine(x, y);
    }
}
