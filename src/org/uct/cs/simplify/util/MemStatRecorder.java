package org.uct.cs.simplify.util;

import java.util.ArrayList;
import java.util.List;

public class MemStatRecorder implements AutoCloseable
{
    private static final int DEFAULT_INTERVAL_MS = 50;

    private static final long KILOBYTE = 2^10;
    private static final long MEGABYTE = 2^10 * KILOBYTE;

    private int intervalMs;
    private List<Pair<Long, Long>> recordings;
    private Thread backgroundThread;

    public MemStatRecorder()
    {
        this.construct(DEFAULT_INTERVAL_MS);
    }

    public MemStatRecorder(int intervalMs)
    {
        this.construct(intervalMs);
    }

    private void construct(int intervalMs)
    {
        if (intervalMs < 10) throw new IllegalArgumentException("intervalMS is too small!");
        this.intervalMs = intervalMs;
        this.recordings = new ArrayList<>();
        this.backgroundThread = (new Thread(new MemRecorderThread(this)));
        this.backgroundThread.start();
    }

    private static String convertToByteUnit(double bytes)
    {
        if (bytes > MEGABYTE) return String.format("%.2f MB", bytes / MEGABYTE);
        if (bytes > KILOBYTE) return String.format("%.2f KB", bytes / KILOBYTE);
        return String.format("%.2f B", bytes);
    }


    @Override
    public void close() throws InterruptedException
    {
        this.backgroundThread.interrupt();
        this.backgroundThread.join();

        long min = Integer.MAX_VALUE;
        long max = Integer.MIN_VALUE;
        long start = this.recordings.get(0).getSecond();
        long end = this.recordings.get(this.recordings.size()-1).getSecond();
        long total = 0;

        for (Pair<Long, Long> p : this.recordings)
        {
            long v = p.getSecond();
            if (v < min) min = v;
            if (v > max) max = v;
            total += v;
        }

        double average = total / (double)this.recordings.size();

        System.out.printf("%nMem Stats%n============%n");
        System.out.printf("Start: %s%n", convertToByteUnit(start));
        System.out.printf("Min: %s%n", convertToByteUnit(min));
        System.out.printf("Average: %s%n", convertToByteUnit(average));
        System.out.printf("Max: %s%n", convertToByteUnit(max));
        System.out.printf("End: %s%n", convertToByteUnit(end));
    }

    public int getInterval()
    {
        return this.intervalMs;
    }

    private void add(long ms, long used)
    {
        this.recordings.add(new Pair<>(ms, used));
    }

    private static class MemRecorderThread implements Runnable
    {
        private final MemStatRecorder parent;

        public MemRecorderThread(MemStatRecorder mr)
        {
            this.parent = mr;
        }

        @Override
        public void run()
        {
            try
            {
                do
                {
                    long ms = System.currentTimeMillis();
                    Runtime r = Runtime.getRuntime();
                    long used = r.totalMemory() - r.freeMemory();

                    this.parent.add(ms, used);

                    Thread.sleep(this.parent.getInterval());
                }
                while (!Thread.currentThread().isInterrupted());
            }
            catch (InterruptedException e)
            {
                //
            }
        }
    }
}
