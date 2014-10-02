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
    public static final int WINDOW_WIDTH = 800;
    public static final int WINDOW_HEIGHT = 800;
    public static final String NO_INPUT_FILE_SELECTED = "No input file selected!";
    public static final String NO_OUTPUT_FILE_SET = "No output file set!";
    public static final Color PROGRESSBAR_COLOR = new Color(0, 200, 0);

    private JTextArea consoleArea;
    private JProgressBar progressBar;
    private JCheckBox swapYZCheckBox;
    private JButton goButton;
    private JButton pickOutputFileBtn;
    private JTextField pickedOutputFileDisplay;
    private JButton pickInputFileBtn;
    private JTextField pickedInputFileDisplay;
    private File selectedInputFile;
    private File selectedOutputFile;
    private JButton resetInputsBtn;

    private Thread processingThread;

    public ProgressWindow()
    {
        this.constructForm();

        this.linkActions();

        this.setVisible(true);

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
        this.pickedInputFileDisplay = new JTextField(NO_INPUT_FILE_SELECTED);
        this.pickedInputFileDisplay.setEditable(false);
        topPanel.add(this.pickedInputFileDisplay, c);

        c.gridy = 1;
        c.gridx = 1;
        this.pickInputFileBtn = new JButton("Pick input file");
        topPanel.add(this.pickInputFileBtn, c);

        c.gridy = 2;
        c.gridx = 0;
        this.pickedOutputFileDisplay = new JTextField(NO_OUTPUT_FILE_SET);
        this.pickedOutputFileDisplay.setEditable(false);
        this.pickedOutputFileDisplay.setEnabled(false);
        topPanel.add(this.pickedOutputFileDisplay, c);

        c.gridy = 2;
        c.gridx = 1;
        this.pickOutputFileBtn = new JButton("Pick output file");
        this.pickOutputFileBtn.setEnabled(false);
        topPanel.add(this.pickOutputFileBtn, c);

        c.gridy = 3;
        c.gridx = 0;
        c.weightx = 0.5;
        this.swapYZCheckBox = new JCheckBox("Swap YZ axis");
        this.swapYZCheckBox.setHorizontalTextPosition(SwingConstants.LEFT);
        this.swapYZCheckBox.setToolTipText(
            "The PHF Viewer uses a Y-up coordinate system. Use this option to convert a model from Z-up to Y-up."
        );
        topPanel.add(this.swapYZCheckBox, c);

        c.gridy = 3;
        c.gridx = 1;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.EAST;
        this.resetInputsBtn = new JButton("Reset");
        this.resetInputsBtn.setHorizontalAlignment(SwingConstants.RIGHT);
        topPanel.add(this.resetInputsBtn, c);

        c.gridy = 4;
        c.gridx = 0;
        c.gridwidth = 3;
        c.weightx = 0;
        c.ipady = 20;
        c.fill = GridBagConstraints.HORIZONTAL;
        this.goButton = new JButton("Go");
        this.goButton.setEnabled(false);
        topPanel.add(this.goButton, c);

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
        this.goButton.addMouseListener(
            new ClickButtonListener()
            {
                public void mouseClicked(MouseEvent e)
                {
                    ProgressWindow.this.goButton.setEnabled(false);
                    ProgressWindow.this.pickInputFileBtn.setEnabled(false);
                    ProgressWindow.this.pickOutputFileBtn.setEnabled(false);
                    ProgressWindow.this.swapYZCheckBox.setEnabled(false);
                    ProgressWindow.this.resetInputsBtn.setEnabled(false);

                    ProgressWindow.this.processingThread = new Thread(
                        new ProcessingRunnable(
                            ProgressWindow.this.selectedInputFile,
                            ProgressWindow.this.selectedOutputFile,
                            ProgressWindow.this.swapYZCheckBox.isSelected(),
                            ProgressWindow.this.progressBar,
                            ProgressWindow.this
                        )
                    );

                    ProgressWindow.this.processingThread.start();
                }
            }
        );

        // link pickInputFile button
        this.pickInputFileBtn.addMouseListener(
            new ClickButtonListener()
            {
                public void mouseClicked(MouseEvent e)
                {
                    ProgressWindow.this.selectedInputFile = ProgressWindow.this.getInputFile();
                    if (ProgressWindow.this.selectedInputFile == null)
                    {
                        ProgressWindow.this.pickedInputFileDisplay.setText(NO_INPUT_FILE_SELECTED);
                    } else
                    {
                        ProgressWindow.this.pickOutputFileBtn.setEnabled(true);
                        ProgressWindow.this.pickedOutputFileDisplay.setEnabled(true);
                        ProgressWindow.this.pickedInputFileDisplay
                            .setText(ProgressWindow.this.selectedInputFile.getPath());
                    }
                    ProgressWindow.this.goButton.setEnabled(
                        (ProgressWindow.this.selectedInputFile != null) && (ProgressWindow.this.selectedOutputFile !=
                            null)
                    );
                }
            }
        );

        // link pickOutputFileBtn button
        this.pickOutputFileBtn.addMouseListener(
            new ClickButtonListener()
            {
                public void mouseClicked(MouseEvent e)
                {
                    String baseFile = (ProgressWindow.this.selectedInputFile == null)
                        ? "output.phf"
                        : Useful
                        .getFilenameWithoutExt(ProgressWindow.this.selectedInputFile.getAbsolutePath()) + ".phf";

                    ProgressWindow.this.selectedOutputFile = ProgressWindow.this.getOutputFile(new File(baseFile));
                    if (ProgressWindow.this.selectedOutputFile == null)
                    {
                        ProgressWindow.this.pickedOutputFileDisplay.setText(NO_OUTPUT_FILE_SET);
                    } else
                    {
                        ProgressWindow.this.pickedOutputFileDisplay.setText(
                            ProgressWindow.this.selectedOutputFile.getPath()
                        );
                    }
                    ProgressWindow.this.goButton.setEnabled(
                        (ProgressWindow.this.selectedInputFile != null) && (ProgressWindow.this.selectedOutputFile !=
                            null)
                    );
                }
            }
        );

        this.resetInputsBtn.addMouseListener(
            new ClickButtonListener()
            {
                @Override
                public void mouseClicked(MouseEvent e)
                {
                    ProgressWindow.this.selectedInputFile = null;
                    ProgressWindow.this.selectedOutputFile = null;
                    ProgressWindow.this.pickInputFileBtn.setEnabled(true);
                    ProgressWindow.this.pickOutputFileBtn.setEnabled(false);
                    ProgressWindow.this.swapYZCheckBox.setEnabled(true);
                    ProgressWindow.this.swapYZCheckBox.setSelected(true);
                    ProgressWindow.this.pickedInputFileDisplay.setEnabled(true);
                    ProgressWindow.this.pickedInputFileDisplay.setText(NO_INPUT_FILE_SELECTED);
                    ProgressWindow.this.pickedOutputFileDisplay.setEnabled(false);
                    ProgressWindow.this.pickedOutputFileDisplay.setText(NO_OUTPUT_FILE_SET);
                    ProgressWindow.this.progressBar.setValue(0);

                    ProgressWindow.this.consoleArea.setText("");
                }
            }
        );
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
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels())
            {
                if ("Nimbus".equals(info.getName()))
                {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
            UIManager.getLookAndFeelDefaults().put("nimbusOrange", PROGRESSBAR_COLOR);
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
            JOptionPane.showMessageDialog(this, "File saved to " + this.selectedOutputFile.toPath());
        else
            JOptionPane.showMessageDialog(this, "Something went wrong! Check the log for more details.", "Error", JOptionPane.ERROR_MESSAGE);
        this.resetInputsBtn.setEnabled(true);
    }
}
