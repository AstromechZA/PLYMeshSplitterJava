package org.uct.cs.simplify.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;

/**
 Class for managing temporary files generated during processing
 */
public class TempFileManager
{
    public static final int DELETE_WAIT_DELAY = 500;
    private static final ArrayDeque<Path> filesToDelete = new ArrayDeque<>(10);
    private static Path workingDirectory;
    private static boolean deleteOnExit = true;
    private static long filesCreated = 0;
    private static long filesDeleted = 0;
    private static long bytesUsed = 0;

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

    public static void setDeleteOnExit(boolean b)
    {
        TempFileManager.deleteOnExit = b;
    }

    public static File provide() throws IOException
    {
        return provide("temp", ".temp");
    }

    public static File provide(String pref) throws IOException
    {
        return provide(pref, ".temp");
    }

    public static File provide(String pref, String suff) throws IOException
    {
        return provide(pref, suff, TempFileManager.deleteOnExit);
    }

    public static File provide(String pref, String suff, boolean deleteOnExit) throws IOException
    {
        Path f = Files.createTempFile(getWorkingDirectory(), pref, suff);
        filesCreated++;
        if (deleteOnExit)
        {
            filesToDelete.add(f);
        }
        return f.toFile();
    }

    public static void release(File f)
    {
        try
        {
            if (Files.exists(f.toPath()))
            {
                long l = f.length();
                Files.delete(f.toPath());
                bytesUsed += l;
                filesDeleted++;
            }
            filesToDelete.remove(f.toPath());
        }
        catch (IOException e)
        {
            // nothing
        }
    }

    public static void clear() throws InterruptedException
    {
        Outputter.info1f("Removing %d Temporary Files%n", filesToDelete.size());
        System.gc();

        int errorlimit = 10;
        while (!filesToDelete.isEmpty() && errorlimit > 0)
        {
            Path p = filesToDelete.removeFirst();
            try
            {
                if (Files.exists(p))
                {
                    long l = p.toFile().length();
                    Files.delete(p);
                    bytesUsed += l;
                    filesDeleted++;
                }
            }
            catch (IOException e)
            {
                filesToDelete.addLast(p);
                errorlimit--;
                Thread.sleep(DELETE_WAIT_DELAY);
            }
        }

        Outputter.info1f("Tempfiles used: %d. Bytes written: (%d) %s%n", filesDeleted, bytesUsed, Useful.formatBytes(bytesUsed));

        if (!filesToDelete.isEmpty())
        {
            Outputter.errorf("Failed to clean up %d files. Please delete these manually:%n", filesToDelete.size());
            filesToDelete.forEach(Outputter::errorln);
        }
    }

    public static void resetStatsAndLists()
    {
        filesCreated = 0;
        filesDeleted = 0;
        bytesUsed = 0;
        filesToDelete.clear();
    }
}
