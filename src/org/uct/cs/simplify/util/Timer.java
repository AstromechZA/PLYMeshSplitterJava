package org.uct.cs.simplify.util;

public class Timer implements AutoCloseable
{
    private String text;
    private long starttime;

    public Timer()
    {
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
        if (this.text == null) return;

        long elapsed = System.nanoTime() - this.starttime;
        float micro = elapsed / 1000f;
        float milli = micro / 1000f;
        float sec = milli / 1000f;
        System.out.printf("%s : %f seconds\n", this.text, sec);
    }


}
