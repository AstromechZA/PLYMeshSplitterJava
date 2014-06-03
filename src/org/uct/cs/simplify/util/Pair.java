package org.uct.cs.simplify.util;

public class Pair<T1, T2>
{
    private T1 first;
    private T2 second;

    public Pair(T1 f, T2 s)
    {
        first = f;
        second = s;
    }

    public T1 getFirst()
    {
        return first;
    }

    public T2 getSecond()
    {
        return second;
    }

}
