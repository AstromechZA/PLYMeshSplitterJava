package org.uct.cs.simplify.model;

import java.io.IOException;

public abstract class StreamingVertexReader implements AutoCloseable
{
    public abstract boolean hasNext();

    public abstract Vertex next() throws IOException;

    public abstract void next(Vertex f) throws IOException;

    public abstract long getCount();

    public abstract void close() throws IOException;
}
