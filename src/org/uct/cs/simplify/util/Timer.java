package org.uct.cs.simplify.util;

/**
 * Created by Ben on 2014-06-02.
 */
public class Timer implements AutoCloseable
{
    private String text;
    private long starttime;

    public Timer(String text)
    {
        this.text = text;
        this.starttime = System.nanoTime();
    }

    @Override
    public void close()
    {
        long elapsed = System.nanoTime() - this.starttime;
        float micro = elapsed / 1000f;
        float milli = micro / 1000f;
        float sec = milli / 1000f;
        System.out.printf("%s : %f seconds\n", this.text, sec);
    }

}
