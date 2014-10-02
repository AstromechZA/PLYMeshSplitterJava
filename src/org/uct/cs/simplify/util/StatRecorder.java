package org.uct.cs.simplify.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class StatRecorder implements AutoCloseable
{
    private static final int DEFAULT_INTERVAL_MS = 100;

    private final int intervalMs;
    private final List<Recording> recordings;
    private final Thread backgroundThread;
    private final long startTime;

    public StatRecorder()
    {
        this(DEFAULT_INTERVAL_MS);
    }

    public StatRecorder(int intervalMs)
    {
        this.startTime = System.nanoTime();
        if (intervalMs < 10) throw new IllegalArgumentException("intervalMS is too small!");
        this.intervalMs = intervalMs;
        this.recordings = new ArrayList<>(1000);
        this.backgroundThread = new Thread(new MemRecorderThread(this));
        this.backgroundThread.start();
    }

    @Override
    public void close() throws InterruptedException
    {
        this.backgroundThread.interrupt();
        this.backgroundThread.join();

        long min = Integer.MAX_VALUE;
        long max = Integer.MIN_VALUE;
        long start = this.recordings.get(0).usage;
        long end = this.recordings.get(this.recordings.size() - 1).usage;
        long total = 0;

        for (Recording p : this.recordings)
        {
            long v = p.usage;
            if (v < min) min = v;
            if (v > max) max = v;
            total += v;
        }

        double average = total / (double) this.recordings.size();
        long elapsed = System.nanoTime() - this.startTime;

        System.out.printf("%n=== Memory Stats =========%n");
        System.out.printf("Mem : Start :   %s%n", Useful.formatBytes(start));
        System.out.printf("Mem : Min :     %s%n", Useful.formatBytes(min));
        System.out.printf("Mem : Average : %s%n", Useful.formatBytes(average));
        System.out.printf("Mem : Max :     %s%n", Useful.formatBytes(max));
        System.out.printf("Mem : End :     %s%n", Useful.formatBytes(end));
        System.out.printf("Time:           %s%n", Useful.formatTime(elapsed));
        System.out.printf("==========================%n");
    }

    public int getInterval()
    {
        return this.intervalMs;
    }

    private void add(long ms, long used)
    {
        this.recordings.add(new Recording(ms, used));
    }

    private static class MemRecorderThread implements Runnable
    {
        private final StatRecorder parent;

        public MemRecorderThread(StatRecorder mr)
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
                    if (used > 0) this.parent.add(ms, used);
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

    public void dump(File out) throws IOException
    {
        try (PrintWriter writer = new PrintWriter(new FileOutputStream(out)))
        {
            for (Recording recording : this.recordings)
            {
                writer.printf("%d,%d%n", recording.time, recording.usage);
            }
        }
    }

    private static class Recording
    {
        final long time, usage;

        private Recording(long time, long usage)
        {
            this.time = time;
            this.usage = usage;
        }
    }
}
