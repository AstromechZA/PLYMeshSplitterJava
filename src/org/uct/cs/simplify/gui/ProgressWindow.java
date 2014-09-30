package org.uct.cs.simplify.gui;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;

public class ProgressWindow extends JFrame
{
    public static final int PROGRESSBAR_HEIGHT = 50;
    public static final int WINDOW_WIDTH = 600;
    public static final int WINDOW_HEIGHT = 400;
    private JTextArea consoleArea;
    private JProgressBar progressBar;

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
        this.add(this.consoleArea, BorderLayout.CENTER);

        this.setVisible(true);

        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("PLY Models", ".ply"));
        chooser.setDialogTitle("Pick a PLY model to process");
        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION)
        {
            //
        } else
        {
            System.exit(0);
        }
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
}
