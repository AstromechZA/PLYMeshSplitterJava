package org.uct.cs.simplify.util;

import java.io.*;
import java.lang.reflect.Array;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 Class for managing temporary files generated during processing
 */
public class TempFileManager
{
    public static final int DELETE_WAIT_DELAY = 500;
    private static final Map<File, TempFileRegistration> tempFileRegistry = new HashMap<>();
    private static Path workingDirectory;

    public static Path getWorkingDirectory() throws IOException
    {
        if (workingDirectory == null)
        {
            workingDirectory = Files.createTempDirectory("PLYMeshSplitterJ");
        }
        return workingDirectory;
    }

    public static void setWorkingDirectory(Path workingDirectory) throws IOException
    {
        workingDirectory = workingDirectory.resolve("temporary files");

        if (!Files.exists(workingDirectory))
        {
            Files.createDirectories(workingDirectory);
        }

        if (!Files.isDirectory(workingDirectory))
        {
            throw new IOException("working directory is not valid");
        }

        TempFileManager.workingDirectory = workingDirectory;
    }

    public static File provide() throws IOException
    {
        return provideInner("temp", ".temp");
    }

    public static File provide(String pref) throws IOException
    {
        return provideInner(pref, ".temp");
    }

    public static File provide(String pref, String suff) throws IOException
    {
        return provideInner(pref, suff);
    }

    private static File provideInner(String pref, String suff) throws IOException
    {
        File f = Files.createTempFile(getWorkingDirectory(), pref, suff).toFile();
        TempFileRegistration r = new TempFileRegistration(f);
        tempFileRegistry.put(f, r);
        return f;
    }

    public static void release(File f)
    {
        try
        {
            if (Files.exists(f.toPath()))
            {
                if(tempFileRegistry.containsKey(f))
                {
                    TempFileRegistration r = tempFileRegistry.get(f);
                    if(!r.isReleased()) r.markReleased();
                }
                Files.delete(f.toPath());
            }
        }
        catch (IOException e)
        { /**/ }
    }

    public static void release(TempFileRegistration r)
    {
        try
        {
            if (Files.exists(r.getPath()))
            {
                if(!r.isReleased()) r.markReleased();
                Files.delete(r.getPath());
            }
        }
        catch (IOException e)
        { /**/ }
    }

    public static void clear() throws InterruptedException
    {
        clear(null);
    }

    public static void clear(File dumpFile) throws InterruptedException
    {
        System.gc();

        int tempFilesRegistered = 0;

        OrderedArrayList<TFAction> createActions = new OrderedArrayList<>(tempFileRegistry.size());
        OrderedArrayList<TFAction> releaseActions = new OrderedArrayList<>(tempFileRegistry.size());

        // first attempt to delete all files
        for (TempFileRegistration r : tempFileRegistry.values())
        {
            tempFilesRegistered++;
            if (!r.isReleased())
            {
                release(r);
            }
            if(r.isReleased())
            {
                createActions.put(new TFAction(r.timeProvided, r.finalByteSize));
                releaseActions.put(new TFAction(r.timeReleased, r.finalByteSize));
            }
        }

        ArrayList<Pair<Long, Long>> datapoints = new ArrayList<>();

        long currentBytes = 0;
        long totalBytes = 0;
        long maximumBytes = 0;

        PeekableListIterator<TFAction> creates = new PeekableListIterator<>(createActions);
        PeekableListIterator<TFAction> releases = new PeekableListIterator<>(releaseActions);

        while(creates.hasNext() && releases.hasNext())
        {
            if (creates.peekNext().compareTo(releases.peekNext()) <= 0)
            {
                TFAction a = creates.next();
                currentBytes += a.bytes;
                totalBytes += a.bytes;
                maximumBytes = Math.max(maximumBytes, currentBytes);
                datapoints.add(new Pair<>(a.time, currentBytes));
            }
            else
            {
                TFAction a = releases.next();
                currentBytes -= a.bytes;
                datapoints.add(new Pair<>(a.time, currentBytes));
            }
        }
        while(creates.hasNext())
        {
            TFAction a = creates.next();
            currentBytes += a.bytes;
            totalBytes += a.bytes;
            maximumBytes = Math.max(maximumBytes, currentBytes);
            datapoints.add(new Pair<>(a.time, currentBytes));
        }
        while(releases.hasNext())
        {
            TFAction a = releases.next();
            currentBytes -= a.bytes;
            datapoints.add(new Pair<>(a.time, currentBytes));
        }

        Outputter.info1f("Tempfiles used: %d.%n", tempFilesRegistered);
        Outputter.info1f("Ending byte total: %d (%s)%n", currentBytes, Useful.formatBytes(currentBytes));
        Outputter.info1f("Maximum byte total: %d (%s)%n", maximumBytes, Useful.formatBytes(maximumBytes));
        Outputter.info1f("Total byte total: %d (%s)%n", totalBytes, Useful.formatBytes(totalBytes));

        if(dumpFile != null)
        {
            Outputter.info1f("Writing file manager datapoints to %s.%n", dumpFile);
            try (PrintWriter writer = new PrintWriter(new FileOutputStream(dumpFile)))
            {
                for (Pair<Long, Long> d : datapoints)
                {
                    writer.printf("%d,%d%n", d.getFirst(), d.getSecond());
                }
            }
            catch (FileNotFoundException e)
            {
                e.printStackTrace();
            }
        }
    }

    public static void resetStatsAndLists()
    {
        tempFileRegistry.clear();
    }

    private static class TempFileRegistration
    {
        private boolean isReleased = false;

        // attributes set on create
        private final File file;
        private final long timeProvided;

        // attributes set on delete
        private long finalByteSize;
        private long timeReleased;

        public TempFileRegistration(File f)
        {
            this.file = f;
            this.timeProvided = System.nanoTime();
        }

        public void markReleased()
        {
            this.finalByteSize = this.file.length();
            this.timeReleased = System.nanoTime();
            this.isReleased = true;
        }

        public boolean isReleased()
        {
            return this.isReleased;
        }

        public Path getPath()
        {
            return this.file.toPath();
        }
    }

    private static class TFAction implements Comparable<TFAction>, Comparator<TFAction>
    {
        public final long time;
        public final long bytes;

        public TFAction(long time, long bytes)
        {
            this.time = time;
            this.bytes = bytes;
        }

        @Override
        public int compareTo(TFAction o)
        {
            return Long.compare(this.time, o.time);
        }

        @Override
        public int compare(TFAction o1, TFAction o2)
        {
            return Long.compare(o1.time, o2.time);
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            TFAction tfAction = (TFAction) o;
            return bytes == tfAction.bytes && time == tfAction.time;
        }
    }

}
