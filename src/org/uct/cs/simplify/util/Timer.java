package org.uct.cs.simplify.util;

public class Timer implements AutoCloseable
{

    private final String text;
    private final long starttime;

    public Timer()
    {
        this.text = "Timer";
        this.starttime = System.nanoTime();
    }

    public Timer(String text)
    {
        this.text = text;
        this.starttime = System.nanoTime();
    }

    public long getElapsed()
    {
        return System.nanoTime() - this.starttime;
    }

    @Override
    public void close()
    {
        long elapsed = System.nanoTime() - this.starttime;
        System.out.printf("Timer : %s : %s%n", this.text, Useful.formatTime(elapsed));
    }


}
