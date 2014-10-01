package org.uct.cs.simplify.gui;

import org.uct.cs.simplify.util.ClickButtonListener;
import org.uct.cs.simplify.util.Useful;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.PrintStream;

public class ProgressWindow extends JFrame implements ICompletionListener
{
    public static final int PROGRESSBAR_HEIGHT = 30;
    public static final int WINDOW_WIDTH = 600;
    public static final int WINDOW_HEIGHT = 400;
    private JTextArea consoleArea;
    private JProgressBar progressBar;
    private JCheckBox swapYZCheckBox;
    private JSpinner treeDepthSpinner;
    private JButton goButton;
    private JButton pickOutputFileBtn;
    private JTextField pickedOutputFileDisplay;
    private JButton pickInputFileBtn;
    private JTextField pickedInputFileDisplay;
    private File selectedInputFile;
    private File selectedOutputFile;

    private Thread processingThread;

    public ProgressWindow()
    {
        this.constructForm();

        this.linkActions();

        this.setVisible(true);

        //File input = this.getInputFile();
        //String baseFileName = Useful.getFilenameWithoutExt(input.getAbsolutePath()) + ".phf";
        //File outputFile = this.getOutputFile(new File(baseFileName));

        PrintStream ps = new PrintStream(new TextAreaOutputStream(this.consoleArea));
        System.setOut(ps);
        System.setErr(ps);
    }

    private void constructForm()
    {
        // main window setup
        this.setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        this.setLayout(new BorderLayout(1, 1));
        this.setLocationRelativeTo(null);
        this.setTitle("PHF Builder");
        setFancyLookAndFeel();

        // create top frame
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;
        c.insets = new Insets(5, 5, 0, 5);
        topPanel.add(new JLabel("Convert PLY file into PHF file:"), c);

        c.gridy = 1;
        c.gridx = 0;
        pickedInputFileDisplay = new JTextField("No input file selected!");
        pickedInputFileDisplay.setEditable(false);
        topPanel.add(pickedInputFileDisplay, c);

        c.gridy = 1;
        c.gridx = 1;
        pickInputFileBtn = new JButton("Pick input file");
        topPanel.add(pickInputFileBtn, c);

        c.gridy = 2;
        c.gridx = 0;
        pickedOutputFileDisplay = new JTextField("No output file set!");
        pickedOutputFileDisplay.setEditable(false);
        pickedOutputFileDisplay.setEnabled(false);
        topPanel.add(pickedOutputFileDisplay, c);

        c.gridy = 2;
        c.gridx = 1;
        pickOutputFileBtn = new JButton("Pick output file");
        pickOutputFileBtn.setEnabled(false);
        topPanel.add(pickOutputFileBtn, c);

        c.gridy = 3;
        c.gridx = 0;
        c.weightx = 0.5;
        swapYZCheckBox = new JCheckBox("Swap YZ axis");
        swapYZCheckBox.setHorizontalTextPosition(SwingConstants.LEFT);
        swapYZCheckBox.setToolTipText("The PHF Viewer uses a Y-up coordinate system. Use this option to convert a model from Z-up to Y-up.");
        topPanel.add(swapYZCheckBox, c);

        c.gridy = 4;
        c.gridx = 0;
        c.gridwidth = 3;
        c.weightx = 0;
        c.ipady = 20;
        goButton = new JButton("Go");
        goButton.setEnabled(false);
        topPanel.add(goButton, c);

        this.add(topPanel, BorderLayout.NORTH);

        this.consoleArea = new JTextArea();
        this.consoleArea.setEditable(false);
        this.consoleArea.setBackground(Color.black);
        this.consoleArea.setForeground(Color.lightGray);
        this.consoleArea.setFont(new Font("monospaced", Font.PLAIN, 12));

        JScrollPane consoleScroll = new JScrollPane(
            this.consoleArea,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED
        );
        this.add(consoleScroll, BorderLayout.CENTER);

        this.progressBar = new JProgressBar();
        this.progressBar.setValue(0);
        Dimension d = this.progressBar.getPreferredSize();
        d.height = PROGRESSBAR_HEIGHT;
        this.progressBar.setPreferredSize(d);
        this.add(this.progressBar, BorderLayout.SOUTH);
    }

    private void linkActions()
    {
        // link Go button
        goButton.addMouseListener(new ClickButtonListener()
        {
            public void mouseClicked(MouseEvent e)
            {
                goButton.setEnabled(false);
                pickInputFileBtn.setEnabled(false);
                pickOutputFileBtn.setEnabled(false);
                swapYZCheckBox.setEnabled(false);

                processingThread = new Thread(new ProcessingRunnable(
                    selectedInputFile,
                    selectedOutputFile,
                    swapYZCheckBox.isSelected(),
                    progressBar,
                    ProgressWindow.this
                ));

                processingThread.start();
            }
        });

        // link pickInputFile button
        pickInputFileBtn.addMouseListener(new ClickButtonListener()
        {
            public void mouseClicked(MouseEvent e)
            {
                selectedInputFile = getInputFile();
                if (selectedInputFile == null)
                {
                    ProgressWindow.this.pickedInputFileDisplay.setText("No input file selected!");
                }
                else
                {
                    pickOutputFileBtn.setEnabled(true);
                    pickedOutputFileDisplay.setEnabled(true);
                    ProgressWindow.this.pickedInputFileDisplay.setText(selectedInputFile.getPath());
                }
                goButton.setEnabled((selectedInputFile != null) && (selectedOutputFile != null));
            }
        });

        // link pickOutputFileBtn button
        pickOutputFileBtn.addMouseListener(new ClickButtonListener()
        {
            public void mouseClicked(MouseEvent e)
            {
                String baseFile = (selectedInputFile == null)
                    ? "output.phf"
                    : Useful.getFilenameWithoutExt(selectedInputFile.getAbsolutePath()) + ".phf";

                selectedOutputFile = getOutputFile(new File(baseFile));
                if (selectedOutputFile == null)
                {
                    ProgressWindow.this.pickedOutputFileDisplay.setText("No output file set!");
                }
                else
                {
                    ProgressWindow.this.pickedOutputFileDisplay.setText(selectedOutputFile.getPath());
                }
                goButton.setEnabled((selectedInputFile != null) && (selectedOutputFile != null));
            }
        });
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

    @Override
    public void callback(boolean success)
    {
        if (success)
            JOptionPane.showMessageDialog(this, "File saved to " + selectedOutputFile.toPath());
        else
            JOptionPane.showMessageDialog(this, "Something went wrong! Check the log for more details.", "Error", JOptionPane.ERROR_MESSAGE);
    }
}
