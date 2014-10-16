package org.uct.cs.simplify.gui.cropper;

import javafx.geometry.Point2D;
import org.uct.cs.simplify.blueprint.BluePrintGenerator;
import org.uct.cs.simplify.blueprint.BluePrintGenerator.CoordinateSpace;
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

        this.cropDisplay = new CroppingDisplay(IMAGE_RESOLUTION);
        this.add(cropDisplay, BorderLayout.CENTER);

        JPanel otherPanel = new JPanel();
        otherPanel.setLayout(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(5, 5, 0, 5);
        c.weightx = 0.25;
        c.gridy = 0;
        c.gridx = 0;
        c.gridwidth = 3;
        this.pickedInputFileDisplay = new JTextField(NO_INPUT_FILE_SELECTED);
        this.pickedInputFileDisplay.setEditable(false);
        otherPanel.add(this.pickedInputFileDisplay, c);

        c.gridx = 3;
        this.pickInputFileBtn = new JButton("Pick input file");
        otherPanel.add(this.pickInputFileBtn, c);

        c.gridx = 6;
        this.axisBox = new JComboBox<>(AXIS_STRINGS);
        otherPanel.add(this.axisBox, c);

        c.gridx = 9;
        this.loadButton = new JButton("Load input");
        otherPanel.add(this.loadButton, c);

        c.gridy = 1;
        c.gridx = 0;
        c.gridwidth = 12;
        this.modeButton = new JButton("Edit area");
        otherPanel.add(modeButton, c);

        c.gridy = 2;
        c.gridx = 0;
        c.gridwidth = 4;
        this.pickedOutputFileDisplay = new JTextField(NO_OUTPUT_FILE_SET);
        this.pickedOutputFileDisplay.setEditable(false);
        this.pickedOutputFileDisplay.setEnabled(false);
        otherPanel.add(this.pickedOutputFileDisplay, c);

        c.gridy = 2;
        c.gridx = 4;
        this.pickOutputFileBtn = new JButton("Pick output file");
        this.pickOutputFileBtn.setEnabled(false);
        otherPanel.add(this.pickOutputFileBtn, c);

        c.gridy = 2;
        c.gridx = 8;
        this.goButton = new JButton("Crop!");
        this.goButton.setEnabled(false);
        otherPanel.add(goButton, c);

        this.add(otherPanel, BorderLayout.NORTH);

        this.progressBar = new JProgressBar();
        this.progressBar.setValue(0);
        Dimension d = this.progressBar.getPreferredSize();
        d.height = 30;
        this.progressBar.setPreferredSize(d);
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
                    modeButton.setText("Edit Hull");
                    goButton.setEnabled(cropDisplay.getHull().size() > 2);
                }
                else
                {
                    cropDisplay.setMode(CroppingDisplay.WorkingMode.EDIT_MODE);
                    modeButton.setText("Stop Editting");
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
                ArrayList<Point2D> worldHullPoints = new ArrayList<>();
                for (Point2D p : cropDisplay.getHull())
                {
                    worldHullPoints.add(cropDisplay.getWorldPointFromBlueprint((int) p.getX(), (int) p.getY()));
                }

                croppingThread = new Thread(
                    new CroppingRunnable(progressBar, selectedInputFile, selectedOutputFile, CroppingWindow.this, worldHullPoints)
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
                }
                else
                {
                    pickedInputFileDisplay.setText(selectedInputFile.getPath());
                }
                goButton.setEnabled(
                    (selectedInputFile != null) && (selectedOutputFile != null)
                );
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
                    .getFilenameWithoutExt(selectedInputFile.getAbsolutePath()) + ".phf";

                selectedOutputFile = getOutputFile(new File(baseFile));
                if (selectedOutputFile == null)
                {
                    pickedOutputFileDisplay.setText(NO_OUTPUT_FILE_SET);
                }
                else
                {
                    pickedOutputFileDisplay.setText(selectedOutputFile.getPath());
                }
                goButton.setEnabled(
                    (selectedInputFile != null) && (selectedOutputFile != null)
                );
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
                    int skipsize = (int) (numVertices / 1_000_000.0);
                    if (skipsize == 0) skipsize = 1;

                    float alpha = (numVertices < 100_000) ? 1 : 0.1f;

                    BluePrintGenerator.CoordinateSpace c;
                    CoordinateSpace v = CoordinateSpace.values()[ axisBox.getSelectedIndex() ];

                    BluePrintGenerator.BlueprintGeneratorResult b = BluePrintGenerator.createImage(
                        reader,
                        CroppingWindow.IMAGE_RESOLUTION,
                        alpha,
                        v,
                        skipsize
                    );
                    cropDisplay.reset();
                    cropDisplay.setBlueprint(b);
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
            JOptionPane.showMessageDialog(this, "File saved to " + this.selectedOutputFile.toPath());
        else
            JOptionPane.showMessageDialog(this, "Something went wrong! Check the log for more details.", "Error", JOptionPane.ERROR_MESSAGE);
    }
}
