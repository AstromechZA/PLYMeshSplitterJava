package org.uct.cs.simplify.util;

import java.io.File;
import java.io.IOException;

public class TempFile extends File implements AutoCloseable
{

    public TempFile(String parent, String child)
    {
        super(parent, child);
    }

    public TempFile(String pathname)
    {
        super(pathname);
    }

    public TempFile(File parent, String child)
    {
        super(parent, child);
    }

    @Override
    public void close() throws IOException
    {
        if (!this.delete()) throw new IOException("File could not be deleted " + this.getAbsolutePath());
    }
}
