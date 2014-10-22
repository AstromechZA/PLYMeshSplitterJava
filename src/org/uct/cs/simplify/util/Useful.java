package org.uct.cs.simplify.util;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

public class Useful
{

    public static final long NANOSECONDS_PER_HOUR = 3_600_000_000_000L;
    public static final long NANOSECONDS_PER_MINUTE = 60_000_000_000L;
    public static final long NANOSECONDS_PER_SECOND = 1_000_000_000L;
    public static final long NANOSECONDS_PER_MILLISECONDS = 1_000_000L;
    public static final long NANOSECONDS_PER_MICROSECOND = 1_000L;
    public static final long KILOBYTE = 1024;
    public static final long MEGABYTE = KILOBYTE * 1024;
    public static final long GIGABYTE = MEGABYTE * 1024;
    public static final int BYTE_MASK = 255;

    public static String getFilenameWithoutExt(String fn)
    {
        int p = fn.lastIndexOf('.');
        return (p >= 0) ? fn.substring(0, fn.lastIndexOf('.')) : fn;
    }

    public static String formatTime(long ns)
    {
        if (ns > NANOSECONDS_PER_HOUR) return String.format("%.2f hours", ns / (double) NANOSECONDS_PER_HOUR);
        if (ns > NANOSECONDS_PER_MINUTE) return String.format("%.2f minutes", ns / (double) NANOSECONDS_PER_MINUTE);
        if (ns > NANOSECONDS_PER_SECOND) return String.format("%.2f seconds", ns / (double) NANOSECONDS_PER_SECOND);
        if (ns > NANOSECONDS_PER_MILLISECONDS)
            return String.format("%.2f milliseconds", ns / (double) NANOSECONDS_PER_MILLISECONDS);
        if (ns > NANOSECONDS_PER_MICROSECOND)
            return String.format("%.2f microseconds", ns / (double) NANOSECONDS_PER_MICROSECOND);
        return String.format("%d nanoseconds", ns);
    }

    public static String formatTimeNoDecimal(long ns)
    {
        if (ns > NANOSECONDS_PER_HOUR) return String.format("%.0f hours", ns / (double) NANOSECONDS_PER_HOUR);
        if (ns > NANOSECONDS_PER_MINUTE) return String.format("%.0f minutes", ns / (double) NANOSECONDS_PER_MINUTE);
        if (ns > NANOSECONDS_PER_SECOND) return String.format("%.0f seconds", ns / (double) NANOSECONDS_PER_SECOND);
        if (ns > NANOSECONDS_PER_MILLISECONDS)
            return String.format("%.0f milliseconds", ns / (double) NANOSECONDS_PER_MILLISECONDS);
        if (ns > NANOSECONDS_PER_MICROSECOND)
            return String.format("%.0f microseconds", ns / (double) NANOSECONDS_PER_MICROSECOND);
        return String.format("%d nanoseconds", ns);
    }

    public static String formatBytes(double bytes)
    {
        if (bytes > GIGABYTE) return String.format("%.2f GB", bytes / GIGABYTE);
        if (bytes > MEGABYTE) return String.format("%.2f MB", bytes / MEGABYTE);
        if (bytes > KILOBYTE) return String.format("%.2f KB", bytes / KILOBYTE);
        return String.format("%.2f B", bytes);
    }

    public static void writeIntLE(OutputStream stream, int i) throws IOException
    {
        stream.write(i & BYTE_MASK);
        stream.write((i >> 8) & BYTE_MASK);
        stream.write((i >> (8 * 2)) & BYTE_MASK);
        stream.write((i >> (8 * 3)) & BYTE_MASK);
    }

    public static void writeFloatLE(OutputStream stream, float f) throws IOException
    {
        writeIntLE(stream, Float.floatToIntBits(f));
    }

    public static int readIntLE(byte[] input, int pos)
    {
        int o = input[ pos ] & BYTE_MASK;
        o |= (input[ pos + 1 ] & BYTE_MASK) << 8;
        o |= (input[ pos + 2 ] & BYTE_MASK) << (8 * 2);
        o |= (input[ pos + 3 ] & BYTE_MASK) << (8 * 3);
        return o;
    }

    public static float readFloatLE(byte[] input, int pos)
    {
        return Float.intBitsToFloat(readIntLE(input, pos));
    }

    public static int readIntLE(ByteBuffer input, int pos)
    {
        int o = input.get(pos) & BYTE_MASK;
        o |= (input.get(pos + 1) & BYTE_MASK) << 8;
        o |= (input.get(pos + 2) & BYTE_MASK) << (8 * 2);
        o |= (input.get(pos + 3) & BYTE_MASK) << (8 * 3);
        return o;
    }

    public static float readFloatLE(ByteBuffer input, int pos)
    {
        return Float.intBitsToFloat(readIntLE(input, pos));
    }

    public static int readIntLE(InputStream istream) throws IOException
    {
        int o = istream.read() & BYTE_MASK;
        o |= (istream.read() & BYTE_MASK) << 8;
        o |= (istream.read() & BYTE_MASK) << (8 * 2);
        o |= (istream.read() & BYTE_MASK) << (8 * 3);
        return o;
    }

    public static String readString(InputStream istream, int length) throws IOException
    {
        byte[] buff = new byte[ length ];
        istream.read(buff, 0, length);
        return new String(buff);
    }

    public static float readFloatLE(InputStream istream) throws IOException
    {
        return Float.intBitsToFloat(readIntLE(istream));
    }

    public static Color colourFor(double ratio)
    {
        if (ratio > 1) ratio = 1;
        if (ratio < 0) ratio = 0;

        double r = 1;
        double g = 1;
        double b = 1;

        if (ratio < (0.25))
        {
            r = 0;
            g = 4 * (ratio);
        }
        else if (ratio < (0.5))
        {
            r = 0;
            b = 1 + 4 * (0.25 - ratio);
        }
        else if (ratio < (0.75))
        {
            r = 4 * (ratio - 0.5);
            b = 0;
        }
        else
        {
            g = 1 + 4 * (0.75 - ratio);
            b = 0;
        }

        return new Color((int) (r * 255), (int) (g * 255), (int) (b * 255));
    }
}

