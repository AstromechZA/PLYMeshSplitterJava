package org.uct.cs.simplify;

import javafx.geometry.Point3D;
import org.apache.commons.cli.*;
import org.uct.cs.simplify.model.BoundsFinder;
import org.uct.cs.simplify.model.Vertex;
import org.uct.cs.simplify.model.VertexAttrMap;
import org.uct.cs.simplify.ply.header.*;
import org.uct.cs.simplify.ply.reader.PLYReader;
import org.uct.cs.simplify.util.*;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class ScaleAndRecenter
{
    private static final int DEFAULT_RESCALE_SIZE = 1024;

    public static double run(File inputFile, File outputFile, int size, boolean swapYZ) throws IOException
    {
        // this scans the target file and works out start and end ranges
        PLYReader reader = new PLYReader(inputFile);

        return run(reader, outputFile, size, swapYZ);
    }

    public static double run(PLYReader reader, File outputFile, int targetSize, boolean swapYZ) throws IOException
    {
        // first have to identify bounds in order to work out ranges and center
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

        try (FileChannel fcIN = new FileInputStream(reader.getFile()).getChannel())
        {
            PLYElement vertexE = reader.getHeader().getElement("vertex");
            PLYElement faceE = reader.getHeader().getElement("face");
            long numVertices = vertexE.getCount();
            long numFaces = faceE.getCount();
            long vertexElementBegin = reader.getElementDimension("vertex").getOffset();
            long faceElementBegin = reader.getElementDimension("face").getOffset();
            long faceElementLength = reader.getElementDimension("face").getLength();

            try (BufferedOutputStream ostream = new BufferedOutputStream(new FileOutputStream(outputFile)))
            {
                VertexAttrMap vam = new VertexAttrMap(vertexE);
                // construct new header
                PLYHeader header = PLYHeader.constructHeader(numVertices, numFaces, vam);

                ostream.write((header + "\n").getBytes());

                fcIN.position(vertexElementBegin);
                ByteBuffer blockBufferIN = ByteBuffer.allocateDirect(vertexE.getItemSize());
                blockBufferIN.order(ByteOrder.LITTLE_ENDIAN);

                try (ProgressBar progress = new ProgressBar("Rescaling Vertices", numVertices))
                {
                    Vertex v = new Vertex(0, 0, 0);
                    for (long n = 0; n < numVertices; n++)
                    {
                        fcIN.read(blockBufferIN);
                        blockBufferIN.flip();

                        v.read(blockBufferIN, vam);
                        v.transform(translate, (float) scale);
                        if (swapYZ) v.swapYZ();
                        v.writeToStream(ostream, vam);

                        blockBufferIN.clear();
                        progress.tick();
                    }
                }

                fcIN.position(reader.getElementDimension("face").getOffset());
                MappedByteBuffer buffer = fcIN.map(FileChannel.MapMode.READ_ONLY, faceElementBegin, faceElementLength);
                buffer.order(ByteOrder.LITTLE_ENDIAN);

                try (ProgressBar progress = new ProgressBar("Filtering Face information", numFaces))
                {
                    for (long i = 0; i < numFaces; i++)
                    {
                        for (PLYPropertyBase base : faceE.getProperties())
                        {
                            if (base.getName().equals("vertex_indices"))
                            {
                                buffer.get();
                                ostream.write(3);
                                Useful.writeIntLE(ostream, buffer.getInt());
                                Useful.writeIntLE(ostream, buffer.getInt());
                                Useful.writeIntLE(ostream, buffer.getInt());
                            }
                            else if (base instanceof PLYListProperty)
                            {
                                PLYListProperty listProperty = (PLYListProperty) base;
                                int l = (int) listProperty.getLengthTypeReader().read(buffer);
                                for (int j = 0; j < l; j++)
                                {
                                    listProperty.getTypeReader().read(buffer);
                                }
                            }
                            else if (base instanceof PLYProperty)
                            {
                                ((PLYProperty) base).getTypeReader().read(buffer);
                            }
                        }
                        progress.tick();
                    }
                }
            }
        }

        return scale;
    }

    public static void main(String[] args)
    {
        try (StatRecorder ignored = new StatRecorder())
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

            run(inputFile, outputFile, rescaleToSize, cmd.hasOption("swapyz"));
        }
        catch (IOException | InterruptedException e)
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
