package org.uct.cs.simplify.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class OrderedArrayList<T> extends ArrayList<T>
{
    public OrderedArrayList(int initialCapacity)
    {
        super(initialCapacity);
    }

    @SuppressWarnings("unchecked")
    public void put(T t)
    {
        super.add(t);
        Comparable<T> cmp = (Comparable<T>) t;
        for (int i = size() - 1; i > 0 && cmp.compareTo(get(i - 1)) < 0; i--)
            Collections.swap(this, i, i - 1);
    }

    @Override
    public boolean add(T t)
    {
        throw new UnsupportedOperationException("OrderedArrayList does not support arbitrary insertion!");
    }

    @Override
    public void add(int index, T element)
    {
        throw new UnsupportedOperationException("OrderedArrayList does not support arbitrary insertion!");
    }

    @Override
    public boolean addAll(Collection<? extends T> c)
    {
        throw new UnsupportedOperationException("OrderedArrayList does not support arbitrary insertion!");
    }

    @Override
    public boolean addAll(int index, Collection<? extends T> c)
    {
        throw new UnsupportedOperationException("OrderedArrayList does not support arbitrary insertion!");
    }
}
