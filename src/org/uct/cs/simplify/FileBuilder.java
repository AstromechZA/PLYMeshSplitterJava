package org.uct.cs.simplify;

import org.apache.commons.cli.*;
import org.uct.cs.simplify.filebuilder.PackagedHierarchicalFileBuilder;
import org.uct.cs.simplify.filebuilder.PackagedHierarchicalNode;
import org.uct.cs.simplify.filebuilder.RecursiveFilePreparer;
import org.uct.cs.simplify.util.StatRecorder;
import org.uct.cs.simplify.util.TempFileManager;
import org.uct.cs.simplify.util.Timer;
import org.uct.cs.simplify.util.Useful;

import java.io.File;
import java.io.IOException;

public class FileBuilder
{
    private static final int RESCALE_SIZE = 1024;

    public static void main(String[] args) throws IOException
    {
        CommandLine cmd = getCommandLine(args);

        try (StatRecorder ignored = new StatRecorder())
        {
            File inputFile = new File(cmd.getOptionValue("input"));
            File outputDir = new File(cmd.getOptionValue("output"));
            File outputFile = new File(outputDir, Useful.getFilenameWithoutExt(inputFile.getName()) + ".phf");

            TempFileManager.setWorkingDirectory(outputDir.toPath());

            File scaledFile = TempFileManager.provide("rescaled", ".ply");

            Timer scaleTimer = new Timer("Rescaling");
            ScaleAndRecenter.run(inputFile, scaledFile, RESCALE_SIZE, true);
            scaleTimer.close();

            PackagedHierarchicalNode seed = new PackagedHierarchicalNode(scaledFile);

            PackagedHierarchicalNode tree = RecursiveFilePreparer.buildNodes(seed, 2);

            PackagedHierarchicalFileBuilder.compile(tree, outputFile);

            TempFileManager.clear();
        }
        catch (InterruptedException | IllegalArgumentException e)
        {
            e.printStackTrace();
        }

    }

    private static CommandLine getCommandLine(String[] args)
    {
        CommandLineParser clp = new BasicParser();

        Options options = new Options();

        Option inputFile = new Option("i", "input", true, "path to first PLY file");
        inputFile.setRequired(true);
        options.addOption(inputFile);

        Option outputFile = new Option("o", "output", true, "path to output directory");
        outputFile.setRequired(true);
        options.addOption(outputFile);

        CommandLine cmd;
        try
        {
            cmd = clp.parse(options, args);
            return cmd;
        }
        catch (ParseException e)
        {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("filebuilder --input <path> --output <path>", options);
            System.exit(1);
            return null;
        }
    }
}
