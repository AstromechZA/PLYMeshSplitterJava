package org.uct.cs.simplify.util;

import java.util.Arrays;

public class ProgressBar implements AutoCloseable
{
    private final String title;
    private final char progressChar;
    private final int maxTicks;
    private final long startTime;
    private int progressTicks;
    private int position;


    public ProgressBar(String title, int maxTicks)
    {
        this(title, maxTicks, '|');
    }

    public ProgressBar(String title, int maxTicks, char progressChar)
    {
        this.title = title;
        this.progressChar = progressChar;
        this.maxTicks = maxTicks;
        this.position = 0;
        this.progressTicks = 0;
        this.startTime = System.nanoTime();
        this.printTitle();
    }

    private void printTitle()
    {
        int l = 100;
        l -= this.title.length()+4;
        char[] pad = new char[l/2];
        Arrays.fill(pad, '=');
        String padding = new String(pad);
        String last = ((this.title.length() % 2 == 0) ? "" : "=");
        System.out.printf("%s  %s  %s%s%n", padding, this.title, padding, last);
    }

    public void tick()
    {
        this.progressTicks += 1;

        int newPosition = (int)((100f*this.progressTicks) / this.maxTicks);
        int toPrint = newPosition - this.position;
        if (toPrint > 0) {
            char[] chars = new char[toPrint];
            Arrays.fill(chars, this.progressChar);

            System.out.print(chars);
        }
        this.position = newPosition;
    }

    @Override
    public void close()
    {
        int toPrint = 100 - this.position;
        if (toPrint > 0)
        {
            char[] chars = new char[ toPrint ];
            Arrays.fill(chars, this.progressChar);

            System.out.print(chars);
        }
        String timestr = Useful.formatTime(System.nanoTime() - this.startTime);
        timestr = String.format(" ( %s ) ", timestr);
        char[] pad = new char[ 100 - timestr.length() - 3 ];
        Arrays.fill(pad, '=');
        System.out.printf("%n%s%s===%n", new String(pad), timestr);
    }
}
