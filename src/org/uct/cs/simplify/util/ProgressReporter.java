package org.uct.cs.simplify.util;

public abstract class ProgressReporter
{
    protected String lastStatus = "";
    protected double lastPercent = 0;
    protected String taskName = "";
    private long startTime = System.nanoTime();
    private long lastUpdated = 0;

    public void report(double percent)
    {
        this.lastPercent = percent;

        long now = System.nanoTime();
        long elapsed = now - startTime;
        if (percent > 0 && percent < 1)
        {
            long remaining = (long) ((elapsed * (1 - percent)) / percent);
            this.lastStatus = String.format("%s : %.2f%% : ( Remaining Time: %s )", this.taskName, percent * 100, Useful.formatTimeNoDecimal(remaining));
        }
        else if (percent <= 0)
        {
            this.lastStatus = String.format("%s : 0%% : ( Remaining Time: Unknown )", this.taskName);
        }
        else if (percent >= 1)
        {
            this.lastStatus = String.format("%s : 100%% : ( Remaining Time: 0 )", this.taskName);
        }

        if ((now - lastUpdated) > Useful.NANOSECONDS_PER_MILLISECONDS || (percent <= 0 || percent >= 1))
        {
            this.output();
            lastUpdated = now;
        }
    }

    public abstract void output();

    public void changeTask(String task, boolean reset)
    {
        this.taskName = task;
        if (reset)
        {
            report(0);
            startTime = System.nanoTime();
        }
        else
        {
            report(lastPercent);
        }
    }

}
