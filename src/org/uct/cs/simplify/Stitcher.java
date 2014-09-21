package org.uct.cs.simplify;

import org.apache.commons.cli.*;
import org.uct.cs.simplify.stitcher.NaiveMeshStitcher;
import org.uct.cs.simplify.util.StatRecorder;

import java.io.File;
import java.io.IOException;

public class Stitcher
{
    public static void main(String[] args) throws IOException
    {
        CommandLine cmd = getCommandLine(args);

        try (StatRecorder ignored = new StatRecorder())
        {
            File file1 = new File(cmd.getOptionValue("filename1"));
            File file2 = new File(cmd.getOptionValue("filename2"));
            File outputFile = new File(cmd.getOptionValue("output"));

            NaiveMeshStitcher.stitch(file1, file2, outputFile);
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

        Option opFile1 = new Option("f1", "filename1", true, "path to first PLY file");
        opFile1.setRequired(true);
        options.addOption(opFile1);

        Option opFile2 = new Option("f2", "filename2", true, "path to first PLY file");
        opFile2.setRequired(true);
        options.addOption(opFile2);

        Option opFile3 = new Option("o", "output", true, "path to output PLY file");
        opFile3.setRequired(true);
        options.addOption(opFile3);

        CommandLine cmd;
        try
        {
            cmd = clp.parse(options, args);
            return cmd;
        }
        catch (ParseException e)
        {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("stitcher --filename <path>", options);
            System.exit(1);
            return null;
        }
    }
}
