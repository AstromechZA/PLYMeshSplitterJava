package org.uct.cs.simplify.gui;

import org.uct.cs.simplify.util.IProgressReporter;
import org.uct.cs.simplify.util.Useful;

import javax.swing.*;

public class ProgressBarProgressReporter implements IProgressReporter
{
    private final JProgressBar progressBar;
    private final long startTime;
    private long lastUpdated = 0;

    public ProgressBarProgressReporter(JProgressBar progressBar)
    {
        this.progressBar = progressBar;
        this.progressBar.setStringPainted(true);
        this.progressBar.setString("");
        this.startTime = System.nanoTime();
    }

    @Override
    public void report(float percent)
    {
        this.progressBar.setValue((int) (percent * 100));

        long now = System.nanoTime();
        long elapsed = now - startTime;
        if (percent > 0 && percent < 1)
        {
            if ((now - lastUpdated) > Useful.NANOSECONDS_PER_SECOND)
            {
                long remaining = (long) ((elapsed * (1 - percent)) / percent);
                this.progressBar.setString("Remaining Time: " + Useful.formatTimeNoDecimal(remaining));
                lastUpdated = now;
            }
        }
        else
        {
            this.progressBar.setString("");
        }
    }
}
