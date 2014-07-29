package org.uct.cs.simplify.util;

public class Triple<T1, T2, T3>
{
    private final T1 first;
    private final T2 second;
    private final T3 third;

    public Triple(T1 f, T2 s, T3 t)
    {
        this.first = f;
        this.second = s;
        this.third = t;
    }

    public T1 getFirst()
    {
        return this.first;
    }

    public T2 getSecond()
    {
        return this.second;
    }

    public T3 getThird()
    {
        return this.third;
    }
}
