package org.uct.cs.simplify.cropping;

import java.awt.geom.Point2D;

public abstract class GradientLineBase extends LineBase
{
    public double gradient;
    public double offset;

    public GradientLineBase(Point2D first, Point2D second)
    {
        super(first, second);

        double deltaY = first.getY() - second.getY();
        double deltaX = first.getX() - second.getX();

        this.gradient = deltaY / deltaX;

        this.offset = first.getY() - this.gradient * first.getX();
    }

    public boolean aboveTheLine(double x, double y)
    {
        return (y > (this.gradient * x + this.offset));
    }
}
