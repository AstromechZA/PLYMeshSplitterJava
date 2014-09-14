package org.uct.cs.simplify;

import org.apache.commons.cli.*;
import org.uct.cs.simplify.file_builder.PackagedHierarchicalNode;
import org.uct.cs.simplify.splitter.HierarchicalSplitter;
import org.uct.cs.simplify.util.MemStatRecorder;
import org.uct.cs.simplify.util.Timer;
import org.uct.cs.simplify.util.Useful;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

public class Splitter
{
    private static final int RESCALE_TO_FIT = 1024;

    public static void main(String[] args)
    {
        CommandLine cmd = getCommandLine(args);

        try (Timer ignored = new Timer("Total"); MemStatRecorder ignored2 = new MemStatRecorder())
        {
            File inputFile = new File(cmd.getOptionValue("filename"));
            File outputDir = new File(new File(cmd.getOptionValue("output")).getCanonicalPath());
            if (!outputDir.exists() && !outputDir.mkdirs())
                throw new IOException("Could not create output directory " + outputDir);

            File scaledFile = new File(
                outputDir,
                String.format(
                    "%s_rescaled_%d.ply",
                    Useful.getFilenameWithoutExt(inputFile.getName()),
                    RESCALE_TO_FIT
                )
            );
            ScaleAndRecenter.run(inputFile, scaledFile, RESCALE_TO_FIT, cmd.hasOption("swapyz"));

            PackagedHierarchicalNode tree = HierarchicalSplitter.split(
                scaledFile,
                outputDir,
                HierarchicalSplitter.DepthControl.TREE_DEPTH_LIMIT
            );

            File outJson = new File(
                outputDir,
                String.format("%s_split_tree.json", Useful.getFilenameWithoutExt(inputFile.getName()))
            );

            System.out.printf("Writing json to %s%n", outJson.getAbsolutePath());
            try (PrintWriter pw = new PrintWriter(outJson))
            {
                pw.print(PackagedHierarchicalNode.buildJSONHierarchy(tree));
            }
        }
        catch (IOException | InterruptedException | IllegalArgumentException e)
        {
            e.printStackTrace();
        }
    }

    /**
     * Parses the given string array as an options array and returns a CommandLine instance
     * containing the results. If required options were missing or an error occured, it will print
     * usage to standard out and System.exit(1).
     *
     * @param args String[] containing program arguments
     * @return CommandLine instance containing results
     */
    private static CommandLine getCommandLine(String[] args)
    {
        CommandLineParser clp = new BasicParser();

        Options options = new Options();

        Option opFilename = new Option("f", "filename", true, "path to PLY model to process");
        opFilename.setRequired(true);
        options.addOption(opFilename);

        Option opOutput = new Option("o", "output", true, "Destination directory of models");
        opOutput.setRequired(true);
        options.addOption(opOutput);

        Option opSwapYZ = new Option("yz", "swapyz", false, "Swap the Y and Z axis during preprocessing");
        options.addOption(opSwapYZ);

        CommandLine cmd;
        try
        {
            cmd = clp.parse(options, args);
            return cmd;
        }
        catch (ParseException e)
        {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("PLYMeshSplitterJ --filename <path>", options);
            System.exit(1);
            return null;
        }
    }


}
