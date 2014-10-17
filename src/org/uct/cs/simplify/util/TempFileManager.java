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
            Files.deleteIfExists(f.toPath());
            filesToDelete.remove(f.toPath());
        }
        catch (IOException e)
        {
            // nothing
        }
    }

    public static void clear() throws InterruptedException
    {
        Outputter.info3f("Removing %d Temporary Files%n", filesToDelete.size());
        System.gc();

        int errorlimit = 10;
        while (!filesToDelete.isEmpty() && errorlimit > 0)
        {
            Path p = filesToDelete.removeFirst();
            try
            {
                Files.deleteIfExists(p);
            }
            catch (IOException e)
            {
                filesToDelete.addLast(p);
                errorlimit--;
                Thread.sleep(DELETE_WAIT_DELAY);
            }
        }

        if (!filesToDelete.isEmpty())
        {
            Outputter.errorf("Failed to clean up %d files. Please delete these manually:%n", filesToDelete.size());
            filesToDelete.forEach(Outputter::errorln);
        }
    }
}
