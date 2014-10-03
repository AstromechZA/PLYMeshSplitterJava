package org.uct.cs.simplify.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

public class Useful
{

    public static final long NANOSECONDS_PER_MINUTE = 60_000_000_000L;
    public static final long NANOSECONDS_PER_SECOND = 1_000_000_000L;
    public static final long NANOSECONDS_PER_MILLISECONDS = 1_000_000L;
    public static final long NANOSECONDS_PER_MICROSECOND = 1_000L;
    public static final long KILOBYTE = 1024;
    public static final long MEGABYTE = 1048576;
    public static final int BYTE_MASK = 255;

    public static String getFilenameWithoutExt(String fn)
    {
        int p = fn.lastIndexOf('.');
        return (p >= 0) ? fn.substring(0, fn.lastIndexOf('.')) : fn;
    }

    public static String formatTime(long ns)
    {
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
        int o = input[pos] & BYTE_MASK;
        o |= (input[pos + 1] & BYTE_MASK) << 8;
        o |= (input[pos + 2] & BYTE_MASK) << (8 * 2);
        o |= (input[pos + 3] & BYTE_MASK) << (8 * 3);
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
        istream.read(buff);
        return new String(buff);
    }

    public static float readFloatLE(InputStream istream) throws IOException
    {
        return Float.intBitsToFloat(readIntLE(istream));
    }
}

