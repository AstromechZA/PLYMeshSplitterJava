package org.uct.cs.simplify.util;

public class Useful
{

    private static final long NANOSECONDS_PER_MINUTE = 60_000_000_000L;
    private static final long NANOSECONDS_PER_SECOND = 1_000_000_000L;
    private static final long NANOSECONDS_PER_MILLISECONDS = 1_000_000L;
    private static final long NANOSECONDS_PER_MICROSECOND = 1_000L;
    private static final long KILOBYTE = 1024;
    private static final long MEGABYTE = 1024 * KILOBYTE;

    public static String getFilenameWithoutExt(String fn)
    {
        return fn.substring(0, fn.lastIndexOf('.'));
    }

    public static String formatTime(long ns)
    {
        if (ns > NANOSECONDS_PER_MINUTE) return String.format("%.2f minute", ns / (double) NANOSECONDS_PER_MINUTE);
        if (ns > NANOSECONDS_PER_SECOND) return String.format("%.2f seconds", ns / (double) NANOSECONDS_PER_SECOND);
        if (ns > NANOSECONDS_PER_MILLISECONDS)
            return String.format("%.2f milliseconds", ns / (double) NANOSECONDS_PER_MILLISECONDS);
        if (ns > NANOSECONDS_PER_MICROSECOND)
            return String.format("%.2f microseconds", ns / (double) NANOSECONDS_PER_MICROSECOND);
        return String.format("%d nanoseconds", ns);
    }

    public static String convertToByteUnit(double bytes)
    {
        if (bytes > MEGABYTE) return String.format("%.2f MB", bytes / MEGABYTE);
        if (bytes > KILOBYTE) return String.format("%.2f KB", bytes / KILOBYTE);
        return String.format("%.2f B", bytes);
    }
}