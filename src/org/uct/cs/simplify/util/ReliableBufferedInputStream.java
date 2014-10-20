package org.uct.cs.simplify.util;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

public class ReliableBufferedInputStream extends BufferedInputStream
{
    public ReliableBufferedInputStream(InputStream in)
    {
        super(in);
    }

    @Override
    public synchronized long skip(long n) throws IOException
    {
        long i = n;
        do
        {
            i = i - super.skip(i);
        }
        while (i != 0L);
        return n;
    }

    @Override
    public synchronized int read(byte[] b, int off, int len) throws IOException
    {
        int i = 0;
        do
        {
            i += super.read(b, i, len - i);
        }
        while (i != len);
        return i;
    }
}
