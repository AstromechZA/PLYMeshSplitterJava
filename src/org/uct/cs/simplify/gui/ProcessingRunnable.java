package org.uct.cs.simplify.gui;

import org.uct.cs.simplify.FileBuilder;
import org.uct.cs.simplify.util.TempFileManager;

import javax.swing.*;
import java.io.File;
import java.io.IOException;

public class ProcessingRunnable implements Runnable
{
    private final JProgressBar progressBar;
    private final File inputFile, outputFile;
    private final boolean swapYZ;
    private final ICompletionListener listener;

    public ProcessingRunnable(File inputFile, File outputFile, boolean swapYZ, JProgressBar progressBar, ICompletionListener listener)
    {
        this.progressBar = progressBar;
        this.inputFile = inputFile;
        this.outputFile = outputFile;
        this.swapYZ = swapYZ;
        this.listener = listener;
    }

    @Override
    public void run()
    {
        boolean success = false;
        try
        {
            FileBuilder.run(
                this.inputFile, this.outputFile, false,
                this.swapYZ, 7, new ProgressBarProgressReporter(this.progressBar)
            );
            success = true;
        }
        catch (IOException | InterruptedException | RuntimeException e)
        {
            e.printStackTrace();
        }

        try
        {
            TempFileManager.clear();
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
        if (this.listener != null) this.listener.callback(success);
    }
}
