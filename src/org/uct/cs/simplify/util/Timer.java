package org.uct.cs.simplify.util;

public class Timer implements AutoCloseable
{

    private final String text;
    private final long starttime;

    public Timer(String text)
    {
        this.text = text;
        this.starttime = System.nanoTime();
    }

    public long getElapsed()
    {
        return System.nanoTime() - this.starttime;
    }

    public long stop()
    {
        long elapsed = this.getElapsed();
        System.out.printf("Timer : %s : %s%n", this.text, Useful.formatTime(elapsed));
        return elapsed;
    }


    @Override
    public void close()
    {
        this.stop();
    }


}
