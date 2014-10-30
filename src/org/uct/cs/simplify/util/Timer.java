package org.uct.cs.simplify.util;

public class Timer implements AutoCloseable
{
    private final long startTime = System.nanoTime();
    private final String task;

    public Timer(String task)
    {
        this.task = task;
    }

    @Override
    public void close()
    {
        long elapsed = System.nanoTime() - this.startTime;
        System.out.printf("%s : Elapsed Time: %s%n", this.task, Useful.formatTime(elapsed));
    }
}
