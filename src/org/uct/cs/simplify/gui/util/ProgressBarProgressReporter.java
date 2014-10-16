package org.uct.cs.simplify.gui.util;

import org.uct.cs.simplify.util.ProgressReporter;

import javax.swing.*;

public class ProgressBarProgressReporter extends ProgressReporter
{
    private final JProgressBar progressBar;

    public ProgressBarProgressReporter(JProgressBar progressBar, String taskName)
    {
        this.progressBar = progressBar;
        this.progressBar.setStringPainted(true);
        this.progressBar.setString("");
    }


    @Override
    public void output()
    {
        this.progressBar.setString(this.lastStatus);
        this.progressBar.setValue((int) (this.lastPercent * 100));
    }
}
