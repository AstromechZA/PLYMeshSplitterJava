package org.uct.cs.simplify.util;

public class Pair<T1, T2>
{
    private final T1 first;
    private final T2 second;

    public Pair(T1 f, T2 s)
    {
        this.first = f;
        this.second = s;
    }

    public T1 getFirst()
    {
        return this.first;
    }

    public T2 getSecond()
    {
        return this.second;
    }

}
