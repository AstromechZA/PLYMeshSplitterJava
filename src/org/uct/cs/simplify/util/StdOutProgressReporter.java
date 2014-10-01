package org.uct.cs.simplify.util;

public class StdOutProgressReporter implements IProgressReporter
{
    @Override
    public void report(float percent)
    {
        System.out.printf("Progress: %.2f%%%n", percent);
    }
}
