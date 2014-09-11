package org.uct.cs.simplify.util;

import java.util.ArrayList;
import java.util.List;

public class MemStatRecorder implements AutoCloseable
{
    private static final int DEFAULT_INTERVAL_MS = 50;

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

    @Override
    public void close() throws InterruptedException
    {
        this.backgroundThread.interrupt();
        this.backgroundThread.join();

        long min = Integer.MAX_VALUE;
        long max = Integer.MIN_VALUE;
        long start = this.recordings.get(0).getSecond();
        long end = this.recordings.get(this.recordings.size() - 1).getSecond();
        long total = 0;

        for (Pair<Long, Long> p : this.recordings)
        {
            long v = p.getSecond();
            if (v < min) min = v;
            if (v > max) max = v;
            total += v;
        }

        double average = total / (double) this.recordings.size();

        System.out.printf("Mem : Start :   %s%n", Useful.formatBytes(start));
        System.out.printf("Mem : Min :     %s%n", Useful.formatBytes(min));
        System.out.printf("Mem : Average : %s%n", Useful.formatBytes(average));
        System.out.printf("Mem : Max :     %s%n", Useful.formatBytes(max));
        System.out.printf("Mem : End :     %s%n", Useful.formatBytes(end));
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
