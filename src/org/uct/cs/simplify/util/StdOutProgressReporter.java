package org.uct.cs.simplify.util;

public class StdOutProgressReporter extends ProgressReporter
{
    @Override
    public void output()
    {
        System.out.println(this.lastStatus);
    }
}
