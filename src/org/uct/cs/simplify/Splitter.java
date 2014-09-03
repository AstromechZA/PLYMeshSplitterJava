package org.uct.cs.simplify;

import org.apache.commons.cli.*;
import org.uct.cs.simplify.splitter.OctreeSplitter;
import org.uct.cs.simplify.util.MemStatRecorder;
import org.uct.cs.simplify.util.Timer;

import java.io.File;
import java.io.IOException;

public class Splitter
{

    public static void run(File inputFile, File outputDir, int maxDepth, boolean swapYZ) throws IOException
    {
        System.out.printf("Intput File: %s%n", inputFile.getAbsolutePath());
        System.out.printf("Output Directory: %s%n", outputDir.getAbsolutePath());

        OctreeSplitter splitter = new OctreeSplitter(inputFile, outputDir, maxDepth, swapYZ);
        splitter.run();
    }

    public static void main(String[] args)
    {
        CommandLine cmd = getCommandLine(args);

        try (Timer ignored = new Timer("Total"); MemStatRecorder ignored2 = new MemStatRecorder())
        {
            File file = new File(cmd.getOptionValue("filename"));
            File outputDir = new File(new File(cmd.getOptionValue("output")).getCanonicalPath());
            if (!outputDir.exists() && !outputDir.mkdirs())
                throw new IOException("Could not create output directory " + outputDir);

            int depth = Integer.parseInt(cmd.getOptionValue("depth"));
            if (depth < 2 || depth > 8) throw new IllegalArgumentException("Splitting depth must be between 1 and 9!");

            run(file, outputDir, depth, cmd.hasOption("swapyz"));
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

        Option opDepth = new Option("d", "depth", true, "Number of levels to split to");
        opDepth.setType(Short.class);
        options.addOption(opDepth);

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
