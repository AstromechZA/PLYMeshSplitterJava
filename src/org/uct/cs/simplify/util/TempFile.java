package org.uct.cs.simplify.util;

import java.io.File;
import java.io.IOException;

public class TempFile extends File implements AutoCloseable
{
    public TempFile(String parent, String child)
    {
        super(parent, child);
        if (this.exists()) this.delete();
    }

    public TempFile(String pathname)
    {
        super(pathname);
        if (this.exists()) this.delete();
    }

    public TempFile(File parent, String child)
    {
        super(parent, child);
        if (this.exists()) this.delete();
    }

    @Override
    public void close() throws IOException
    {
        if (!this.delete()) throw new IOException("File could not be deleted " + this.getAbsolutePath());
    }
}
