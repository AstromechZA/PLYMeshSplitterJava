package org.uct.cs.simplify.gui.preprocessor;

import org.uct.cs.simplify.gui.util.TextAreaOutputStream;
import org.uct.cs.simplify.util.ClickButtonActionListener;
import org.uct.cs.simplify.util.ICompletionListener;
import org.uct.cs.simplify.util.Useful;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.io.PrintStream;

public class PreprocessorWindow extends JFrame implements ICompletionListener
{
    public static final int PROGRESSBAR_HEIGHT = 30;
    public static final int WINDOW_WIDTH = 800;
    public static final int WINDOW_HEIGHT = 600;
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
    private JButton abortBtn;

    private Thread processingThread;

    public PreprocessorWindow()
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
        this.setTitle("PLY Preprocessor");
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
        c.weightx = 1;
        this.pickedInputFileDisplay = new JTextField(NO_INPUT_FILE_SELECTED);
        this.pickedInputFileDisplay.setEditable(false);
        topPanel.add(this.pickedInputFileDisplay, c);

        c.gridy = 1;
        c.gridx = 1;
        c.weightx = 0;
        this.pickInputFileBtn = new JButton("Pick input file");
        topPanel.add(this.pickInputFileBtn, c);

        c.gridy = 2;
        c.gridx = 0;
        c.weightx = 1;
        this.pickedOutputFileDisplay = new JTextField(NO_OUTPUT_FILE_SET);
        this.pickedOutputFileDisplay.setEditable(false);
        this.pickedOutputFileDisplay.setEnabled(false);
        topPanel.add(this.pickedOutputFileDisplay, c);

        c.gridy = 2;
        c.gridx = 1;
        c.weightx = 0;
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
        c.gridx = 1;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.EAST;
        this.abortBtn = new JButton("Abort");
        this.abortBtn.setHorizontalAlignment(SwingConstants.RIGHT);
        this.abortBtn.setEnabled(false);
        topPanel.add(this.abortBtn, c);

        c.gridy = 5;
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
        this.goButton.addActionListener(
            new ClickButtonActionListener()
            {
                public void mouseClicked()
                {
                    PreprocessorWindow.this.goButton.setEnabled(false);
                    PreprocessorWindow.this.pickInputFileBtn.setEnabled(false);
                    PreprocessorWindow.this.pickOutputFileBtn.setEnabled(false);
                    PreprocessorWindow.this.swapYZCheckBox.setEnabled(false);
                    PreprocessorWindow.this.resetInputsBtn.setEnabled(false);

                    PreprocessorWindow.this.processingThread = new Thread(
                        new ProcessingRunnable(
                            PreprocessorWindow.this.selectedInputFile,
                            PreprocessorWindow.this.selectedOutputFile,
                            PreprocessorWindow.this.swapYZCheckBox.isSelected(),
                            PreprocessorWindow.this.progressBar,
                            PreprocessorWindow.this
                        )
                    );

                    PreprocessorWindow.this.abortBtn.setEnabled(true);
                    PreprocessorWindow.this.processingThread.start();
                }
            }
        );

        // link pickInputFile button
        this.pickInputFileBtn.addActionListener(
            new ClickButtonActionListener()
            {
                public void mouseClicked()
                {
                    PreprocessorWindow.this.selectedInputFile = PreprocessorWindow.this.getInputFile();
                    if (PreprocessorWindow.this.selectedInputFile == null)
                    {
                        PreprocessorWindow.this.pickedInputFileDisplay.setText(NO_INPUT_FILE_SELECTED);
                    }
                    else
                    {
                        PreprocessorWindow.this.pickOutputFileBtn.setEnabled(true);
                        PreprocessorWindow.this.pickedOutputFileDisplay.setEnabled(true);
                        PreprocessorWindow.this.pickedInputFileDisplay
                            .setText(PreprocessorWindow.this.selectedInputFile.getPath());
                    }
                    PreprocessorWindow.this.goButton.setEnabled(
                        (PreprocessorWindow.this.selectedInputFile != null) && (PreprocessorWindow.this.selectedOutputFile !=
                            null)
                    );
                }
            }
        );

        // link pickOutputFileBtn button
        this.pickOutputFileBtn.addActionListener(
            new ClickButtonActionListener()
            {
                public void mouseClicked()
                {
                    String baseFile = (PreprocessorWindow.this.selectedInputFile == null)
                        ? "output.phf"
                        : Useful
                        .getFilenameWithoutExt(PreprocessorWindow.this.selectedInputFile.getAbsolutePath()) + ".phf";

                    PreprocessorWindow.this.selectedOutputFile = PreprocessorWindow.this.getOutputFile(new File(baseFile));
                    if (PreprocessorWindow.this.selectedOutputFile == null)
                    {
                        PreprocessorWindow.this.pickedOutputFileDisplay.setText(NO_OUTPUT_FILE_SET);
                    }
                    else
                    {
                        PreprocessorWindow.this.pickedOutputFileDisplay.setText(
                            PreprocessorWindow.this.selectedOutputFile.getPath()
                        );
                    }
                    PreprocessorWindow.this.goButton.setEnabled(
                        (PreprocessorWindow.this.selectedInputFile != null) && (PreprocessorWindow.this.selectedOutputFile !=
                            null)
                    );
                }
            }
        );

        this.resetInputsBtn.addActionListener(
            new ClickButtonActionListener()
            {
                @Override
                public void mouseClicked()
                {
                    PreprocessorWindow.this.selectedInputFile = null;
                    PreprocessorWindow.this.selectedOutputFile = null;
                    PreprocessorWindow.this.pickInputFileBtn.setEnabled(true);
                    PreprocessorWindow.this.pickOutputFileBtn.setEnabled(false);
                    PreprocessorWindow.this.swapYZCheckBox.setEnabled(true);
                    PreprocessorWindow.this.swapYZCheckBox.setSelected(false);
                    PreprocessorWindow.this.pickedInputFileDisplay.setEnabled(true);
                    PreprocessorWindow.this.pickedInputFileDisplay.setText(NO_INPUT_FILE_SELECTED);
                    PreprocessorWindow.this.pickedOutputFileDisplay.setEnabled(false);
                    PreprocessorWindow.this.pickedOutputFileDisplay.setText(NO_OUTPUT_FILE_SET);
                    PreprocessorWindow.this.progressBar.setValue(0);
                    PreprocessorWindow.this.progressBar.setString("");

                    PreprocessorWindow.this.consoleArea.setText("");
                }
            }
        );

        this.abortBtn.addActionListener(
            new ClickButtonActionListener()
            {
                @Override
                public void mouseClicked()
                {
                    PreprocessorWindow.this.processingThread.interrupt();
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
        this.abortBtn.setEnabled(false);
    }
}
