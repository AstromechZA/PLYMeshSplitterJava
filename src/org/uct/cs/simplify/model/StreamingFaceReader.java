package org.uct.cs.simplify.model;


import java.io.IOException;

public abstract class StreamingFaceReader implements AutoCloseable
{
    public abstract boolean hasNext();

    public abstract Face next() throws IOException;

    public abstract void next(Face f) throws IOException;

    public abstract long getCount();

    public abstract void close() throws IOException;
}
