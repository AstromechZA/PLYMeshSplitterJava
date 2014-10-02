package org.uct.cs.simplify.gui;

import org.uct.cs.simplify.FileBuilder;
import org.uct.cs.simplify.ply.header.PLYHeader;
import org.uct.cs.simplify.util.Outputter;
import org.uct.cs.simplify.util.TempFileManager;

import javax.swing.*;
import java.io.File;
import java.io.IOException;

public class ProcessingRunnable implements Runnable
{
    public static final int FACES_PER_NODE = 50000;
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
            PLYHeader header = new PLYHeader(this.inputFile);
            long numFaces = header.getElement("face").getCount();

            int treedepth = (int) Math.round((Math.log(numFaces / FACES_PER_NODE) / Math.log(2)) + 1);

            Outputter.info3f("Treedepth: %d%n", treedepth);

            FileBuilder.run(
                this.inputFile, this.outputFile, false,
                this.swapYZ, treedepth, new ProgressBarProgressReporter(this.progressBar)
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
