package org.uct.cs.simplify.util;

public class Useful
{

    public static String getFilenameWithoutExt(String fn)
    {
        return fn.substring(0, fn.lastIndexOf('.'));
    }

}
