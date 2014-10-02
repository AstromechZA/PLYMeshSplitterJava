package org.uct.cs.simplify;

import org.apache.commons.cli.*;
import org.uct.cs.simplify.filebuilder.PHFBuilder;
import org.uct.cs.simplify.filebuilder.PHFNode;
import org.uct.cs.simplify.filebuilder.RecursiveFilePreparer;
import org.uct.cs.simplify.util.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class FileBuilder
{
    public static final int MAX_DEPTH = 20;
    private static final int RESCALE_SIZE = 1024;

    public static String run(File inputFile, File outputFile, boolean keepNodes, boolean swapYZ, int treedepth, IProgressReporter progressReporter)
        throws IOException, InterruptedException
    {
        StatRecorder sr = new StatRecorder();
        // use the directory of the outputfile as the default output directory
        File outputDir = outputFile.getParentFile();

        // generate tempfiles in the outputdir
        TempFileManager.setWorkingDirectory(outputDir.toPath());
        // delete all tempfiles afterwards
        TempFileManager.setDeleteOnExit(!keepNodes);

        // create scaled and recentered version of input
        File scaledFile = TempFileManager.provide("rescaled", ".ply");
        ScaleAndRecenter.run(inputFile, scaledFile, RESCALE_SIZE, swapYZ);

        // build tree
        PHFNode tree = RecursiveFilePreparer.prepare(new PHFNode(scaledFile), treedepth, progressReporter);

        // compile into output file
        String jsonHeader = PHFBuilder.compile(tree, outputFile);
        Outputter.info3f("Processing complete. Final file: %s%n", outputFile);
        sr.close();
        sr.dump(new File(outputDir, "memdump"));
        return jsonHeader;
    }

    public static void main(String[] args) throws IOException
    {
        CommandLine cmd = getCommandLine(args);
        try (StatRecorder ignored = new StatRecorder())
        {
            File inputFile = new File(cmd.getOptionValue("input"));
            File outputFile = new File(cmd.getOptionValue("output"));
            File outputDir = outputFile.getParentFile();

            String jsonHeader = run(
                inputFile,
                outputFile,
                cmd.hasOption("keeptemp"),
                cmd.hasOption("swapyz"),
                Integer.parseInt(cmd.getOptionValue("treedepth")),
                new StdOutProgressReporter()
            );

            if (cmd.hasOption("dumpjson"))
            {
                File headerFile = new File(outputDir, Useful.getFilenameWithoutExt(inputFile.getName()) + ".json");
                try (FileWriter fw = new FileWriter(headerFile))
                {
                    fw.write(jsonHeader);
                }
            }

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

        Option treedepth = new Option("d", "treedepth", true, "Dump the JSON header into separate file");
        treedepth.setRequired(true);
        treedepth.setType(Number.class);
        options.addOption(treedepth);

        Option keepTempFiles = new Option(
            "k", "keeptemp", false, "keep any temporary files generated during phf compilation"
        );
        options.addOption(keepTempFiles);

        Option swapYZ = new Option("s", "swapyz", false, "Rotate model 90 around X. (convert coordinate frame)");
        options.addOption(swapYZ);

        Option dumpJSON = new Option("j", "dumpjson", false, "Dump the JSON header into separate file");
        options.addOption(dumpJSON);


        CommandLine cmd;
        try
        {
            cmd = clp.parse(options, args);
            long treedepthv = (Long) cmd.getParsedOptionValue("treedepth");
            if (treedepthv < 2 || treedepthv > MAX_DEPTH) throw new ParseException("treedepth must be > 1 and < 21");
            return cmd;
        }
        catch (ParseException e)
        {
            Outputter.errorf("%s : %s%n%n", e.getClass().getName(), e.getMessage());
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("filebuilder --input <path> --output <path>", options);
            System.exit(1);
            return null;
        }
    }
}
