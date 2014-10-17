package org.uct.cs.simplify.util;

public class StdOutProgressReporter extends ProgressReporter
{
    public StdOutProgressReporter(String taskName)
    {
        this.taskName = taskName;
    }

    @Override
    public void output()
    {
        System.out.println(this.lastStatus);
    }
}
