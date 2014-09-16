package org.uct.cs.simplify.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 Class for managing temporary files generated during processing
 */
public class TempFileManager
{
    private static Path workingDirectory = null;
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
        if(deleteOnExit) f.toFile().deleteOnExit();
        return f.toFile();
    }

}
