package org.uct.cs.simplify;

import org.apache.commons.cli.*;
import org.uct.cs.simplify.img.BluePrintGenerator;
import org.uct.cs.simplify.ply.header.PLYHeader;
import org.uct.cs.simplify.ply.reader.ImprovedPLYReader;
import org.uct.cs.simplify.util.Timer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class BlueprintifyMain
{
    private static final int DEFAULT_RESOLUTION = 1024;
    private static final float DEFAULT_ALPHA_MOD = 0.1f;
    private static final String OUTPUT_FORMAT = "png";

    public static void main(String[] args)
    {
        CommandLine cmd = parseArgs(args);
        try (Timer ignored = new Timer("Elapsed:"))
        {
            // process arguments
            int resolution = (cmd.hasOption("resolution") ? (int) cmd.getParsedOptionValue("resolution") : DEFAULT_RESOLUTION);
            float alphamod = (cmd.hasOption("alphamod") ? (float) cmd.getParsedOptionValue("alphamod") : DEFAULT_ALPHA_MOD);
            String filename = cmd.getOptionValue("filename");
            String outputDirectory = cmd.getOptionValue("output");

            // process output dir
            File outputDir = new File(new File(outputDirectory).getCanonicalPath());
            if (!outputDir.exists() && !outputDir.mkdirs())
                throw new IOException("Could not create output directory " + outputDir);

            File inputFile = new File(filename);
            PLYHeader header = new PLYHeader(inputFile);
            ImprovedPLYReader r = new ImprovedPLYReader(header, inputFile);

            // process input model to find output name
            File outputFile = new File(outputDir, inputFile.getName() + "." + OUTPUT_FORMAT);

            BufferedImage bi = BluePrintGenerator.CreateImage(r, resolution, alphamod);
            ImageIO.write(bi, OUTPUT_FORMAT, outputFile);

            System.out.println("Saved blueprint to " + outputFile);
        }
        catch (ParseException | IOException e)
        {
            e.printStackTrace();
        }
    }

    private static CommandLine parseArgs(String[] args)
    {
        CommandLineParser clp = new BasicParser();

        Options options = new Options();

        Option o1 = new Option("f", "filename", true, "Path to PLY model to process");
        o1.setRequired(true);
        options.addOption(o1);

        Option o2 = new Option("o", "output", true, "Destination directory of blueprint");
        o2.setRequired(true);
        options.addOption(o2);

        Option o3 = new Option("r", "resolution", true, "Resolution of image to output (default 1024)");
        o3.setType(Short.class);
        options.addOption(o3);

        Option o4 = new Option("a", "alphamod", true, "Translucency of applied pixels (default 0.1f)");
        o4.setType(Float.class);
        options.addOption(o4);

        try
        {
            return clp.parse(options, args);
        }
        catch (ParseException e)
        {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("BlueprintifyPLY --filename <path> --output <path>", options);
            System.exit(1);
            return null;
        }
    }


}
