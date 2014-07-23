package org.uct.cs.simplify.util;

public class Timer implements AutoCloseable
{
    private static final long NANOSECONDS_PER_MINUTE = 60_000_000_000L;
    private static final long NANOSECONDS_PER_SECOND = 1_000_000_000L;
    private static final long NANOSECONDS_PER_MILLISECONDS = 1_000_000L;
    private static final long NANOSECONDS_PER_MICROSECOND = 1_000L;

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

    private static String formatTime(long ns)
    {
        if (ns > NANOSECONDS_PER_MINUTE) return String.format("%.2f minute", ns / (double) NANOSECONDS_PER_MINUTE);
        if (ns > NANOSECONDS_PER_SECOND) return String.format("%.2f seconds", ns / (double) NANOSECONDS_PER_SECOND);
        if (ns > NANOSECONDS_PER_MILLISECONDS) return String.format("%.2f milliseconds", ns / (double) NANOSECONDS_PER_MILLISECONDS);
        if (ns > NANOSECONDS_PER_MICROSECOND) return String.format("%.2f microseconds", ns / (double) NANOSECONDS_PER_MICROSECOND);
        return String.format("%d nanoseconds", ns);
    }

    @Override
    public void close()
    {
        long elapsed = System.nanoTime() - this.starttime;
        System.out.printf("Timer : %s : %s%n", this.text, formatTime(elapsed));
    }


}
