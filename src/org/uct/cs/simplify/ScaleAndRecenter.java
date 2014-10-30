package org.uct.cs.simplify;

import javafx.geometry.Point3D;
import org.apache.commons.cli.*;
import org.uct.cs.simplify.model.*;
import org.uct.cs.simplify.ply.header.PLYElement;
import org.uct.cs.simplify.ply.header.PLYHeader;
import org.uct.cs.simplify.ply.reader.PLYReader;
import org.uct.cs.simplify.util.*;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class ScaleAndRecenter
{
    private static final int DEFAULT_RESCALE_SIZE = 1024;

    public static double rescale(File inputFile, File outputFile, int size, boolean swapYZ) throws IOException
    {
        return rescale(new PLYReader(inputFile), outputFile, size, swapYZ);
    }

    public static double rescale(PLYReader reader, File outputFile, int targetSize, boolean swapYZ) throws IOException
    {
        try (Timer ignored = new Timer("Rescaling"))
        {
            // first have to identify bounds in order to work out ranges and center
            Outputter.info1ln("Calculating bounding box");
            XBoundingBox bb = BoundsFinder.getBoundingBox(reader);
            Point3D center = new Point3D(
                (bb.getMinX() + bb.getMaxX()) / 2,
                (bb.getMinY() + bb.getMaxY()) / 2,
                (bb.getMinZ() + bb.getMaxZ()) / 2
            );

            // calculate transform
            // - mesh size is the length of the longest axis
            double scale = targetSize / (2 * Math.abs(
                Math.max(
                    Math.max(
                        bb.getMaxX() - center.getX(),
                        bb.getMaxY() - center.getY()
                    ),
                    bb.getMaxZ() - center.getZ()
                )
            ));
            // - translate to center
            Point3D translate = new Point3D(-center.getX(), -center.getY(), -center.getZ());

            // debug
            Outputter.info3ln("Rescaling and Centering...");
            Outputter.info2f("%s -> %s%n", reader.getFile(), outputFile);
            Outputter.debugf("Scale ratio: %f%n", scale);
            Outputter.info1f("Swapping YZ axis: %s%n", swapYZ);

            PLYElement vertexE = reader.getHeader().getElement("vertex");
            PLYElement faceE = reader.getHeader().getElement("face");
            long numVertices = vertexE.getCount();
            long numFaces = faceE.getCount();

            if (numVertices > Integer.MAX_VALUE)
            {
                throw new RuntimeException("WOAH. Even we can't deal with a mesh containing more than " + Integer.MAX_VALUE + " vertices.");
            }

            if (numFaces > (Integer.MAX_VALUE * 2L))
            {
                throw new RuntimeException("WOAH. Even we can't deal with a mesh containing more than " + (Integer.MAX_VALUE * 2L) + " faces.");
            }

            try (BufferedOutputStream ostream = new BufferedOutputStream(new FileOutputStream(outputFile)))
            {
                VertexAttrMap vam = new VertexAttrMap(vertexE);
                // construct new header
                PLYHeader header = PLYHeader.constructHeader(numVertices, numFaces, vam);

                ostream.write((header + "\n").getBytes());

                try (StreamingVertexReader vr = new FastBufferedVertexReader(reader))
                {
                    try (ProgressBar progress = new ProgressBar("Rescaling Vertices", numVertices))
                    {
                        Vertex v = new Vertex(0, 0, 0);
                        while (vr.hasNext())
                        {
                            vr.next(v);
                            v.transform(translate, (float) scale);
                            if (swapYZ) v.swapYZ();
                            v.writeToStream(ostream, vam);
                            progress.tick();
                        }
                    }
                }

                try (StreamingFaceReader fr = new CleverFaceReader(reader))
                {
                    try (ProgressBar progress = new ProgressBar("Filtering Face information", numFaces))
                    {
                        Face f = new Face(0, 0, 0);
                        while (fr.hasNext())
                        {
                            fr.next(f);
                            if (f.i < 0) throw new RuntimeException("Bad vertex index in face!");
                            if (f.j < 0) throw new RuntimeException("Bad vertex index in face!");
                            if (f.k < 0) throw new RuntimeException("Bad vertex index in face!");
                            if (f.i >= numVertices) throw new RuntimeException("Bad vertex index in face!");
                            if (f.j >= numVertices) throw new RuntimeException("Bad vertex index in face!");
                            if (f.k >= numVertices) throw new RuntimeException("Bad vertex index in face!");

                            f.writeToStream(ostream);
                            progress.tick();
                        }
                    }
                }
            }
            return scale;
        }
    }

    public static void main(String[] args) throws IOException
    {
        CommandLine cmd = parseArgs(args);
        int rescaleToSize = cmd.hasOption("size") ? Integer.parseInt(cmd.getOptionValue("size")) : DEFAULT_RESCALE_SIZE;
        String filename = cmd.getOptionValue("filename");
        String outputDirectory = cmd.getOptionValue("output");

        // checking
        if (rescaleToSize < 2) throw new IllegalArgumentException("Rescale size must not be smaller than 2 units");

        File inputFile = new File(filename);

        // output file stuff
        File outputDir = new File(new File(outputDirectory).getCanonicalPath());
        if (!outputDir.exists() && !outputDir.mkdirs())
            throw new IOException("Could not create output directory " + outputDir);
        File outputFile = new File(
            outputDir,
            String.format(
                "%s_rescaled_%d.ply", Useful.getFilenameWithoutExt(inputFile.getName()), rescaleToSize
            )
        );

        rescale(inputFile, outputFile, rescaleToSize, cmd.hasOption("swapyz"));
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

        options.addOption(new Option("z", "swapyz", false, "Swap Y and Z axes"));

        try
        {
            return clp.parse(options, args);
        }
        catch (ParseException e)
        {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("--filename <path> --output <path> --size <size>", options);
            System.exit(1);
            return null;
        }
    }
}
