package org.uct.cs.simplify.gui.cropper;

import javafx.geometry.Point2D;
import org.uct.cs.simplify.blueprint.BluePrintGenerator;
import org.uct.cs.simplify.blueprint.BluePrintGenerator.CoordinateSpace;
import org.uct.cs.simplify.cropping.LineBase;
import org.uct.cs.simplify.ply.reader.PLYReader;
import org.uct.cs.simplify.util.ClickButtonActionListener;
import org.uct.cs.simplify.util.ICompletionListener;
import org.uct.cs.simplify.util.Useful;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class CroppingWindow extends JFrame implements ICompletionListener
{
    public static final int IMAGE_RESOLUTION = 600;
    public static final String NO_INPUT_FILE_SELECTED = "No input file selected!";
    public static final String NO_OUTPUT_FILE_SET = "No output file set!";
    public static final String WINDOW_TITLE = "Geometry Cropper";
    public static final Color PROGRESSBAR_COLOR = new Color(0, 200, 0);
    public static final String[] AXIS_STRINGS = new String[]{ "TOP", "FRONT", "SIDE" };

    private CroppingDisplay cropDisplay;
    private JProgressBar progressBar;
    private JButton modeButton;
    private JComboBox<String> axisBox;
    private JButton loadButton;
    private JButton goButton;

    private JButton pickOutputFileBtn;
    private JTextField pickedOutputFileDisplay;
    private JButton pickInputFileBtn;
    private JTextField pickedInputFileDisplay;
    private File selectedInputFile;
    private File selectedOutputFile;
    private Thread croppingThread;
    private JComboBox<Integer> samplingBox;

    public CroppingWindow()
    {
        this.createForm();
        this.linkActions();

        this.setVisible(true);
    }

    private void createForm()
    {
        this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        this.setLayout(new BorderLayout(1, 1));
        this.setTitle("PHF Builder");
        setFancyLookAndFeel();

        // build components
        this.cropDisplay = new CroppingDisplay(IMAGE_RESOLUTION);

        this.pickedInputFileDisplay = new JTextField(NO_INPUT_FILE_SELECTED);
        this.pickedInputFileDisplay.setEditable(false);

        this.pickInputFileBtn = new JButton("Pick input file");

        this.axisBox = new JComboBox<>(AXIS_STRINGS);
        this.axisBox.setEnabled(false);

        this.samplingBox = new JComboBox<>(new Integer[]{ 1, 10, 100, 1000, 10000, 100000 });
        this.samplingBox.setEnabled(false);

        this.loadButton = new JButton("Load input");
        this.loadButton.setEnabled(false);

        this.modeButton = new JButton("Edit area");
        this.modeButton.setEnabled(false);

        this.pickedOutputFileDisplay = new JTextField(NO_OUTPUT_FILE_SET);
        this.pickedOutputFileDisplay.setEditable(false);
        this.pickedOutputFileDisplay.setEnabled(false);

        this.pickOutputFileBtn = new JButton("Pick output file");
        this.pickOutputFileBtn.setEnabled(false);

        this.goButton = new JButton("Crop!");
        this.goButton.setEnabled(false);

        this.progressBar = new JProgressBar();
        this.progressBar.setValue(0);
        Dimension d = this.progressBar.getPreferredSize();
        d.height = 30;
        this.progressBar.setPreferredSize(d);

        // do layout

        this.add(cropDisplay, BorderLayout.CENTER);

        JPanel otherPanel = new JPanel();
        otherPanel.setLayout(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(5, 5, 0, 5);
        c.weightx = 0.5;
        c.gridy = 0;
        c.gridx = 0;
        c.gridwidth = 2;
        otherPanel.add(this.pickedInputFileDisplay, c);

        c.gridx = 2;
        c.gridwidth = 1;
        otherPanel.add(this.pickInputFileBtn, c);

        c.gridy = 1;
        c.gridx = 0;
        otherPanel.add(new JLabel("View:"), c);

        c.gridx = 1;
        c.gridwidth = 2;
        otherPanel.add(this.axisBox, c);

        c.gridy = 2;
        c.gridx = 0;
        otherPanel.add(new JLabel("Sampling:"), c);

        c.gridx = 1;
        c.gridwidth = 2;
        otherPanel.add(samplingBox, c);

        c.gridy = 3;
        c.gridx = 0;
        c.gridwidth = 3;
        otherPanel.add(this.loadButton, c);

        c.gridy = 4;
        otherPanel.add(new JLabel(" "), c);

        c.gridy = 5;
        c.gridx = 0;
        c.gridwidth = 3;
        otherPanel.add(modeButton, c);

        c.gridy = 6;
        c.gridx = 0;
        c.gridwidth = 2;
        otherPanel.add(this.pickedOutputFileDisplay, c);

        c.gridx = 2;
        c.gridwidth = 1;
        otherPanel.add(this.pickOutputFileBtn, c);

        c.gridy = 7;
        c.gridx = 0;
        c.gridwidth = 3;
        otherPanel.add(goButton, c);

        this.add(otherPanel, BorderLayout.WEST);


        this.add(this.progressBar, BorderLayout.SOUTH);

        this.pack();
        this.setLocationRelativeTo(null);
    }

    private void setFancyLookAndFeel()
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

    private void linkActions()
    {
        modeButton.addActionListener(new ClickButtonActionListener()
        {
            @Override
            public void mouseClicked()
            {
                if (cropDisplay.getMode() == CroppingDisplay.WorkingMode.EDIT_MODE)
                {
                    cropDisplay.setMode(CroppingDisplay.WorkingMode.NONE);

                    pickInputFileBtn.setEnabled(true);
                    pickOutputFileBtn.setEnabled(true);
                    axisBox.setEnabled(true);
                    samplingBox.setEnabled(true);
                    loadButton.setEnabled(true);
                    goButton.setEnabled(cropDisplay.getHull().size() > 2);
                }
                else
                {
                    cropDisplay.setMode(CroppingDisplay.WorkingMode.EDIT_MODE);

                    pickInputFileBtn.setEnabled(false);
                    pickOutputFileBtn.setEnabled(false);
                    axisBox.setEnabled(false);
                    samplingBox.setEnabled(false);
                    loadButton.setEnabled(false);
                    goButton.setEnabled(false);

                }
            }
        });

        goButton.addActionListener(new ClickButtonActionListener()
        {
            @Override
            public void mouseClicked()
            {
                // check hull points
                if (cropDisplay.getHull().size() < 3) throw new RuntimeException("Not enough hull points!");

                // other wise
                // all good
                modeButton.setEnabled(false);
                goButton.setEnabled(false);
                pickInputFileBtn.setEnabled(false);
                pickOutputFileBtn.setEnabled(false);
                progressBar.grabFocus();

                // start thread thing here
                // build lines
                ArrayList<LineBase> hullLines = new ArrayList<>();
                Point2D last = cropDisplay.getHull().get(cropDisplay.getHull().size() - 1);
                for (Point2D p : cropDisplay.getHull())
                {
                    Point2D tl = cropDisplay.getBluePrint().getWorldPointFromImage((int) last.getX(), (int) last.getY());
                    Point2D tp = cropDisplay.getBluePrint().getWorldPointFromImage((int) p.getX(), (int) p.getY());

                    LineBase line = LineBase.makeLine(tl, tp);
                    hullLines.add(line);
                    System.out.printf("%s : %f,%f %f,%f%n", line.getClass().getName(), line.first.getX(), line.first.getY(), line.second.getX(), line.second.getY());
                    last = p;
                }

                croppingThread = new Thread(
                    new CroppingRunnable(progressBar, selectedInputFile, selectedOutputFile, CroppingWindow.this, hullLines, cropDisplay.getBluePrint())
                );

                croppingThread.start();


            }
        });

        // link pickInputFile button
        pickInputFileBtn.addActionListener(new ClickButtonActionListener()
        {
            public void mouseClicked()
            {
                selectedInputFile = getInputFile();
                if (selectedInputFile == null)
                {
                    pickedInputFileDisplay.setText(NO_INPUT_FILE_SELECTED);
                    axisBox.setEnabled(false);
                    samplingBox.setEnabled(false);
                    loadButton.setEnabled(false);
                }
                else
                {
                    pickedInputFileDisplay.setText(selectedInputFile.getPath());
                    axisBox.setEnabled(true);
                    samplingBox.setEnabled(true);
                    loadButton.setEnabled(true);

                }
            }
        });

        // link pickOutputFileBtn button
        pickOutputFileBtn.addActionListener(new ClickButtonActionListener()
        {
            public void mouseClicked()
            {
                String baseFile = (selectedInputFile == null)
                    ? "output.phf"
                    : Useful
                    .getFilenameWithoutExt(selectedInputFile.getAbsolutePath()) + "_cropped.ply";

                selectedOutputFile = getOutputFile(new File(baseFile));
                if (selectedOutputFile == null)
                {
                    pickedOutputFileDisplay.setText(NO_OUTPUT_FILE_SET);
                    goButton.setEnabled(false);
                }
                else
                {
                    pickedOutputFileDisplay.setText(selectedOutputFile.getPath());
                    goButton.setEnabled(true);
                }
            }
        });

        loadButton.addActionListener(new ClickButtonActionListener()
        {
            @Override
            public void mouseClicked()
            {
                try
                {
                    PLYReader reader = new PLYReader(selectedInputFile);

                    long numVertices = reader.getHeader().getElement("vertex").getCount();
                    float alpha = (numVertices < 100_000) ? 1 : 0.1f;

                    BluePrintGenerator.CoordinateSpace c;
                    CoordinateSpace v = CoordinateSpace.values()[ axisBox.getSelectedIndex() ];

                    BluePrintGenerator.BlueprintGeneratorResult b = BluePrintGenerator.createImage(
                        reader,
                        CroppingWindow.IMAGE_RESOLUTION,
                        alpha,
                        v,
                        samplingBox.getItemAt(samplingBox.getSelectedIndex())
                    );
                    cropDisplay.reset();
                    cropDisplay.setBlueprint(b);

                    modeButton.setEnabled(true);
                    pickedOutputFileDisplay.setEnabled(true);
                    pickOutputFileBtn.setEnabled(true);
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
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
        chooser.setFileFilter(new FileNameExtensionFilter("PLY Models", "ply"));
        chooser.setSelectedFile(baseFile);
        int result = chooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION)
        {
            return chooser.getSelectedFile();
        }
        return null;
    }

    @Override
    public void callback(boolean success)
    {
        if (success)
            JOptionPane.showMessageDialog(this, "File saved to " + this.selectedOutputFile);
        else
            JOptionPane.showMessageDialog(this, "Something went wrong! Check the log for more details.", "Error", JOptionPane.ERROR_MESSAGE);
        this.repaint();
    }
}
