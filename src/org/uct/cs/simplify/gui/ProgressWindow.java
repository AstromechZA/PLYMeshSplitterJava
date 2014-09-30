package org.uct.cs.simplify.gui;

import org.uct.cs.simplify.util.Useful;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;

public class ProgressWindow extends JFrame
{
    public static final int PROGRESSBAR_HEIGHT = 50;
    public static final int WINDOW_WIDTH = 600;
    public static final int WINDOW_HEIGHT = 400;
    private JTextArea consoleArea;
    private JProgressBar progressBar;
    private JScrollPane consoleScroll;

    public ProgressWindow()
    {
        this.setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        this.setLayout(new BorderLayout(1, 1));
        this.setLocationRelativeTo(null);
        setFancyLookAndFeel();

        this.progressBar = new JProgressBar();
        this.progressBar.setValue(0);
        Dimension d = this.progressBar.getPreferredSize();
        d.height = PROGRESSBAR_HEIGHT;
        this.progressBar.setPreferredSize(d);
        this.add(this.progressBar, BorderLayout.NORTH);

        this.consoleArea = new JTextArea("Console here");
        this.consoleArea.setEditable(false);
        this.consoleArea.setBackground(Color.black);
        this.consoleArea.setForeground(Color.gray);

        this.consoleScroll = new JScrollPane(
            this.consoleArea,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED
        );
        this.add(this.consoleScroll, BorderLayout.CENTER);

        this.setVisible(true);

        File input = this.getInputFile();
        String baseFileName = Useful.getFilenameWithoutExt(input.getAbsolutePath()) + ".phf";
        File outputFile = this.getOutputFile(new File(baseFileName));

        new Thread(new ProcessingThread(this, this.progressBar)).start();
    }

    private File getInputFile()
    {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("PLY Models", "ply"));
        chooser.setDialogTitle("Pick a PLY model to process");
        chooser.setMultiSelectionEnabled(false);
        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION)
        {
            return chooser.getSelectedFile();
        } else
        {
            System.exit(0);
        }
        return null;
    }

    private File getOutputFile(File baseFile)
    {
        JFileChooser chooser = new JFileChooser();
        chooser.setMultiSelectionEnabled(false);
        chooser.setDialogTitle("Pick output file");
        chooser.setFileFilter(new FileNameExtensionFilter("PHF Data file", "phf"));
        chooser.setSelectedFile(baseFile);
        int result = chooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION)
        {
            return chooser.getSelectedFile();
        } else
        {
            System.exit(0);
        }
        return null;
    }

    private static void setFancyLookAndFeel()
    {
        try
        {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels())
            {
                if ("Nimbus".equals(info.getName()))
                {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public void println(Object o)
    {
        this.consoleArea.append(o.toString() + "\n");
        JScrollBar bar = this.consoleScroll.getVerticalScrollBar();
        bar.setValue(bar.getMaximum());
    }

}
