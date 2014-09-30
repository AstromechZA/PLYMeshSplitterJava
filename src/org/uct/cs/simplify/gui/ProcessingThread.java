package org.uct.cs.simplify.gui;

import javax.swing.*;

public class ProcessingThread implements Runnable
{
    private final ProgressWindow parent;
    private final JProgressBar progressBar;

    public ProcessingThread(ProgressWindow parent, JProgressBar progressBar)
    {
        this.parent = parent;
        this.progressBar = progressBar;
    }

    @Override
    public void run()
    {
        try
        {
            int o = 0;
            while (o <= 100)
            {
                this.progressBar.setValue(o);
                this.parent.println(o);
                o++;
                Thread.sleep(100);
            }
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
    }
}
