package org.uct.cs.simplify.gui.cropper;

import javafx.geometry.Point2D;
import org.uct.cs.simplify.util.ICompletionListener;

import javax.swing.*;
import java.io.File;
import java.util.ArrayList;

public class CroppingRunnable implements Runnable
{
    private final JProgressBar progressBar;
    private final File inputFile, outputFile;
    private final ICompletionListener listener;
    private final ArrayList<Point2D> hullPoints;

    public CroppingRunnable(JProgressBar progressBar, File inputFile, File outputFile, ICompletionListener listener, ArrayList<Point2D> hullPoints)
    {
        this.progressBar = progressBar;
        this.inputFile = inputFile;
        this.outputFile = outputFile;
        this.listener = listener;
        this.hullPoints = hullPoints;
    }

    @Override
    public void run()
    {

    }
}
