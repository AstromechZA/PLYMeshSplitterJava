package org.uct.cs.simplify.util;

public class Timer implements AutoCloseable
{
    private static final int NANOSECONDS_PER_SECOND = 1_000_000_000;

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
        float seconds = elapsed / (float)NANOSECONDS_PER_SECOND;
        System.out.printf("%s : %f seconds\n", this.text, seconds);
    }


}
