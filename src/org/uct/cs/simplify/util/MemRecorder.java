package org.uct.cs.simplify.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MemRecorder implements AutoCloseable
{
    private File file;
    private int intervalMs;
    private Thread backgroundThread;
    private List<Pair<Long, Long>> recordings;

    public MemRecorder(File f, int intervalMs)
    {
        this.file = f;
        this.intervalMs = intervalMs;
        this.recordings = new ArrayList<>();
        this.backgroundThread = (new Thread(new MemRecorderThread(this)));
        this.backgroundThread.start();
    }

    @Override
    public void close() throws InterruptedException, IOException
    {
        this.backgroundThread.interrupt();
        this.backgroundThread.join();

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(this.file)))
        {
            for (Pair<Long, Long> rec : this.recordings)
            {
                bw.write(String.format("%10d %10d\n", rec.getFirst(), rec.getSecond()));
            }
        }
    }

    public int getInterval()
    {
        return intervalMs;
    }

    private void add(long ms, long used)
    {
        recordings.add(new Pair<>(ms, used));
    }

    private static class MemRecorderThread implements Runnable
    {
        private MemRecorder parent;

        public MemRecorderThread(MemRecorder mr)
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

                    Thread.sleep(parent.getInterval());
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
