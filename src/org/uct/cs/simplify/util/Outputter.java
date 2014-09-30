package org.uct.cs.simplify.util;

public class Outputter
{
    public static final int DEBUG = 0;
    public static final int INFO_1 = 1;
    public static final int INFO_2 = 2;
    public static final int INFO_3 = 3;

    private static int currentLevel = INFO_2;

    public static void println(int logLevel, Object o)
    {
        if (logLevel >= currentLevel) System.out.println(o);
    }

    public static void printf(int logLevel, String format, Object... args)
    {
        if (logLevel >= currentLevel) System.out.printf(format, args);
    }

    public static void debugln(Object o)
    {
        println(DEBUG, o);
    }

    public static void debugf(String format, Object... args)
    {
        printf(DEBUG, format, args);
    }

    public static void info1ln(Object o)
    {
        println(INFO_1, o);
    }

    public static void info1f(String format, Object... args)
    {
        printf(INFO_1, format, args);
    }

    public static void info2ln(Object o)
    {
        println(INFO_2, o);
    }

    public static void info2f(String format, Object... args)
    {
        printf(INFO_2, format, args);
    }

    public static void info3ln(Object o)
    {
        println(INFO_3, o);
    }

    public static void info3f(String format, Object... args)
    {
        printf(INFO_3, format, args);
    }

    public static void errorln(Object o)
    {
        System.err.println(o);
    }

    public static void errorf(String format, Object... args)
    {
        System.err.printf(format, args);
    }
}
