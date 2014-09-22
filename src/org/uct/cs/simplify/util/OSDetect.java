package org.uct.cs.simplify.util;

public class OSDetect
{
    public static final String osName;
    public static final boolean isWindows;
    public static final boolean isMac;
    public static final boolean isUnix;

    static
    {
        osName = System.getProperty("os.name").toLowerCase();
        isWindows = osName.contains("win");
        isMac = osName.contains("mac");
        isUnix = osName.contains("nix") || osName.contains("nux") || osName.contains("aix");
    }
}
