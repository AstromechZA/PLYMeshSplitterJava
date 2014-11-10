package org.uct.cs.simplify.cropping;

import java.awt.geom.Point2D;

public abstract class LineBase
{
    public final Point2D first, second;

    public LineBase(Point2D first, Point2D second)
    {
        this.first = first;
        this.second = second;
    }

    public abstract boolean doesExclude(float x, float y);

    /////////////////////////////////////////

    public static LineBase makeLine(Point2D first, Point2D second)
    {
        // first test for vertical line
        if (first.getX() == second.getX())
        {
            if (first.getY() < second.getY())
            {
                return new VerticalUpLine(first, second);
            }
            else
            {
                return new VerticalDownLine(first, second);
            }
        }
        else if (first.getX() < second.getX())
        {
            return new EastLine(first, second);
        }
        else
        {
            return new WestLine(first, second);
        }
    }
}
