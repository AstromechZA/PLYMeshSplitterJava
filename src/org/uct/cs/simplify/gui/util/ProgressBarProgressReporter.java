package org.uct.cs.simplify.gui.util;

import org.uct.cs.simplify.util.ProgressReporter;

import javax.swing.*;

public class ProgressBarProgressReporter extends ProgressReporter
{
    private final JProgressBar progressBar;

    public ProgressBarProgressReporter(JProgressBar progressBar, String taskName)
    {
        this.taskName = taskName;
        this.progressBar = progressBar;
        this.progressBar.setStringPainted(true);
        this.lastStatus = String.format("%s : 0%% : ( Remaining Time: Unknown )", this.taskName);
        this.progressBar.setString(this.lastStatus);
    }


    @Override
    public void output()
    {
        this.progressBar.setString(this.lastStatus);
        this.progressBar.setValue((int) (this.lastPercent * 100));
        this.progressBar.repaint();
    }
}
