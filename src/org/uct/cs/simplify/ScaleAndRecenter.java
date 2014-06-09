package org.uct.cs.simplify;

import javafx.geometry.BoundingBox;
import javafx.geometry.Point3D;
import org.apache.commons.cli.*;
import org.uct.cs.simplify.ply.header.PLYHeader;
import org.uct.cs.simplify.ply.reader.ImprovedPLYReader;
import org.uct.cs.simplify.ply.utilities.BoundsFinder;
import org.uct.cs.simplify.util.Timer;

import java.io.File;
import java.io.IOException;

public class ScaleAndRecenter
{
    public static void main(String[] args) throws ParseException
    {
        CommandLine cmd = parseArgs(args);
        int rescaleToSize = (cmd.hasOption("size") ? Integer.parseInt(cmd.getOptionValue("size")) : 1024);
        String filename = cmd.getOptionValue("filename");
        String outputDirectory = cmd.getOptionValue("output");

        // checking
        if (rescaleToSize < 2) throw new IllegalArgumentException("Rescale size must not be smaller than 2 units");

        try (Timer ignored = new Timer("Processing"))
        {
            File inputFile = new File(filename);

            // output file stuff
            File outputDir = new File(new File(outputDirectory).getCanonicalPath());
            if (!outputDir.exists() && !outputDir.mkdirs())
                throw new IOException("Could not create output directory " + outputDir);
            File outputFile = new File(outputDir, inputFile.getName() + "_rescaled_" + rescaleToSize + ".ply");


            // == construct & setup PLYReader
            // this scans the target file and works out start and end ranges
            PLYHeader header = new PLYHeader(inputFile);
            ImprovedPLYReader reader = new ImprovedPLYReader(header, inputFile);

            // first have to identify bounds in order to work out ranges and center
            BoundingBox bb = BoundsFinder.getBoundingBox(reader);
            Point3D center = new Point3D(
                    (bb.getMinX() + bb.getMaxX()) / 2.0f,
                    (bb.getMinY() + bb.getMaxY()) / 2.0f,
                    (bb.getMinZ() + bb.getMaxZ()) / 2.0f);

            // mesh size is the maximum axis
            double meshHalfSize = Math.abs(Math.max(Math.max(bb.getMaxX() - center.getX(), bb.getMaxY() - center.getY()), bb.getMaxZ() - center.getZ()));
            double scale = (double) (rescaleToSize) / meshHalfSize;
            Point3D translate = new Point3D(-center.getX(), -center.getY(), -center.getZ());

            // debug
            System.out.println("InputFile: " + inputFile);
            System.out.println("OutputFile: " + outputFile);
            System.out.printf("%f → %f\n", bb.getMinX(), bb.getMaxX());
            System.out.printf("%f → %f\n", bb.getMinY(), bb.getMaxY());
            System.out.printf("%f → %f\n", bb.getMinZ(), bb.getMaxZ());
            System.out.printf("center: %f, %f, %f\n", center.getX(), center.getY(), center.getZ());
            System.out.printf("rescale ratio: %f\n", scale);

            // now we need to
            // 1) navigate to vertex element
            // 2) for each vertex
            //      3) read x,y,z floats
            //      4) transform x,y,z
            //      5) rewrite
            //      6) skip ahead

            // copy first (dataoffset) bytes to destination
            // for n in vertex_count
            //     read block of bytes
            //     transform xyz
            //     write back into block
            //     write block to destination
            // write remaining bytes to destination

        }
        catch (IOException e)
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

        Option o2 = new Option("o", "output", true, "Destination directory of model");
        o2.setRequired(true);
        options.addOption(o2);

        Option o3 = new Option("s", "size", true, "Scale the model to fit in a cube of this size");
        o3.setType(Short.class);
        options.addOption(o3);

        try
        {
            return clp.parse(options, args);
        }
        catch (ParseException e)
        {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("ScaleAndRecenter --filename <path> --output <path> --size <size>", options);
            System.exit(1);
            return null;
        }
    }
}
