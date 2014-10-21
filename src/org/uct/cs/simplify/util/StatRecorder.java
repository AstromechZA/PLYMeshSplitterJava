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

        NumberSummary ns = new NumberSummary(this.recordings.size());

        for (Recording p : this.recordings)
        {
            ns.add(p.usage);
        }

        long elapsed = System.nanoTime() - this.startTime;

        System.out.printf("%n=== Memory Stats ===========================%n");
        System.out.printf("Mem : Min :      (%12d) %s%n", (long) ns.min, Useful.formatBytes((long) ns.min));
        System.out.printf("Mem : P50 :      (%12d) %s%n", (long) ns.p50, Useful.formatBytes((long) ns.p50));
        System.out.printf("Mem : Max :      (%12d) %s%n", (long) ns.max, Useful.formatBytes((long) ns.max));
        System.out.printf("Mem : Mean :     (%12d) %s%n", (long) ns.mean, Useful.formatBytes((long) ns.mean));
        System.out.printf("Time:            (%12s) %s%n", elapsed / Useful.NANOSECONDS_PER_SECOND, Useful.formatTime(elapsed));
        System.out.printf("============================================%n");
    }

    public int getInterval()
    {
        return this.intervalMs;
    }

    private void add(long ms, long used)
    {
        this.recordings.add(new Recording(ms, used));
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
