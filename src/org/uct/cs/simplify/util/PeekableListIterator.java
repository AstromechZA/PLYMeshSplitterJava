package org.uct.cs.simplify.util;

import java.util.List;
import java.util.ListIterator;

public class PeekableListIterator<T>
{
    private final ListIterator<T> inner;
    private T peeked = null;

    public PeekableListIterator(ListIterator<T> inner)
    {
        this.inner = inner;
    }

    public PeekableListIterator(List<T> wrap)
    {
        this(wrap.listIterator());
    }

    public boolean hasNext()
    {
        return peeked != null || inner.hasNext();
    }

    public T next()
    {
        if(peeked != null)
        {
            T t = peeked;
            peeked = null;
            return t;
        }
        else
        {
            return inner.next();
        }
    }

    public T peekNext()
    {
        if(peeked == null)
        {
            peeked = inner.next();
        }
        return peeked;
    }
}
