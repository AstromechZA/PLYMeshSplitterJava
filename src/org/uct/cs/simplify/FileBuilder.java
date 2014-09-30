package org.uct.cs.simplify;

import org.apache.commons.cli.*;
import org.uct.cs.simplify.filebuilder.PHFBuilder;
import org.uct.cs.simplify.filebuilder.PHFNode;
import org.uct.cs.simplify.filebuilder.RecursiveFilePreparer;
import org.uct.cs.simplify.util.StatRecorder;
import org.uct.cs.simplify.util.TempFileManager;
import org.uct.cs.simplify.util.Timer;
import org.uct.cs.simplify.util.Useful;

import java.io.File;
import java.io.FileWriter;
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
            if (cmd.hasOption("keeptemp"))
            {
                TempFileManager.setDeleteOnExit(false);
            }

            File scaledFile = TempFileManager.provide("rescaled", ".ply");

            Timer scaleTimer = new Timer("Rescaling");
            ScaleAndRecenter.run(inputFile, scaledFile, RESCALE_SIZE, cmd.hasOption("swapyz"));
            scaleTimer.close();

            PHFNode seed = new PHFNode(scaledFile);

            int treedepthv = Integer.parseInt(cmd.getOptionValue("treedepth"));
            PHFNode tree = RecursiveFilePreparer.prepare(seed, treedepthv);

            TempFileManager.release(scaledFile);

            String jsonHeader = PHFBuilder.compile(tree, outputFile, PHFBuilder.CompilationMode.COMPRESSED_ARRAY);

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

        Option keepTempFiles = new Option("k", "keeptemp", false, "keep any temporary files generated during phf compilation");
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
            if (treedepthv < 2 || treedepthv > 20) throw new ParseException("treedepth must be > 1 and < 21");
            return cmd;
        }
        catch (ParseException e)
        {
            System.out.printf("%s : %s%n%n", e.getClass().getName(), e.getMessage());
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("filebuilder --input <path> --output <path>", options);
            System.exit(1);
            return null;
        }
    }
}
