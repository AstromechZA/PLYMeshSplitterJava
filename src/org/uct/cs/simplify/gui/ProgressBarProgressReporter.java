package org.uct.cs.simplify.gui;

import org.uct.cs.simplify.util.IProgressReporter;

import javax.swing.*;

public class ProgressBarProgressReporter implements IProgressReporter
{
    private final JProgressBar progressBar;

    public ProgressBarProgressReporter(JProgressBar progressBar)
    {
        this.progressBar = progressBar;
    }

    @Override
    public void report(float percent)
    {
        this.progressBar.setValue((int) percent);
    }
}
