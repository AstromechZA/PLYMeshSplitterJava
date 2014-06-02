package org.uct.cs.simplify.ply.reader;

import org.uct.cs.simplify.ply.header.PLYHeader;

import java.io.File;
import java.io.IOException;

public class PLYReader
{
    private PLYHeader header;

    public PLYReader(File f) throws IOException
    {
        this.header = new PLYHeader(f);


    }

    public PLYHeader getHeader()
    {
        return this.header;
    }


}
