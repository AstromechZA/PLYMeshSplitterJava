package org.uct.cs.simplify;

import javafx.geometry.Point3D;
import org.apache.commons.cli.*;
import org.uct.cs.simplify.model.BoundsFinder;
import org.uct.cs.simplify.model.Vertex;
import org.uct.cs.simplify.model.VertexAttrMap;
import org.uct.cs.simplify.ply.header.PLYElement;
import org.uct.cs.simplify.ply.header.PLYHeader;
import org.uct.cs.simplify.ply.reader.PLYReader;
import org.uct.cs.simplify.util.ProgressBar;
import org.uct.cs.simplify.util.StatRecorder;
import org.uct.cs.simplify.util.Useful;
import org.uct.cs.simplify.util.XBoundingBox;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;

public class ScaleAndRecenter
{
    private static final int DEFAULT_RESCALE_SIZE = 1024;

    public static XBoundingBox run(File inputFile, File outputFile, int size, boolean swapYZ) throws IOException
    {
        // this scans the target file and works out start and end ranges
        PLYReader reader = new PLYReader(inputFile);

        return run(reader, outputFile, size, swapYZ);
    }

    public static XBoundingBox run(PLYReader reader, File outputFile, int size, boolean swapYZ) throws IOException
    {
        // first have to identify bounds in order to work out ranges and center
        XBoundingBox bb = BoundsFinder.getBoundingBox(reader);
        Point3D center = new Point3D(
            (bb.getMinX() + bb.getMaxX()) / 2,
            (bb.getMinY() + bb.getMaxY()) / 2,
            (bb.getMinZ() + bb.getMaxZ()) / 2
        );

        // mesh size is the maximum axis
        double meshHalfSize = Math.abs(
            Math.max(
                Math.max(
                    bb.getMaxX() - center.getX(),
                    bb.getMaxY() - center.getY()
                ),
                bb.getMaxZ() - center.getZ()
            )
        );
        double scale = size / (2 * meshHalfSize);
        Point3D translate = new Point3D(-center.getX(), -center.getY(), -center.getZ());

        // debug
        System.out.println("Rescaling and Centering...");
        System.out.printf("%s -> %s%n", reader.getFile(), outputFile);
        System.out.printf("Scale ratio: %f%n", scale);
        System.out.printf("Swapping YZ axis: %s%n", swapYZ);

        try (FileChannel fcIN = new FileInputStream(reader.getFile()).getChannel())
        {
            PLYElement vertexE = reader.getHeader().getElement("vertex");
            PLYElement faceE = reader.getHeader().getElement("face");
            int numVertices = vertexE.getCount();
            int numFaces = faceE.getCount();
            long vertexElementBegin = reader.getElementDimension("vertex").getOffset();
            long vertexElementLength = reader.getElementDimension("vertex").getLength();

            try (BufferedOutputStream bufostream = new BufferedOutputStream(new FileOutputStream(outputFile)))
            {
                VertexAttrMap vam = new VertexAttrMap(vertexE);
                // construct new header
                PLYHeader header = PLYHeader.constructHeader(numVertices, numFaces, vam);

                bufostream.write((header + "\n").getBytes());

                ByteBuffer blockBufferIN = ByteBuffer.allocateDirect(vertexE.getItemSize());
                blockBufferIN.order(ByteOrder.LITTLE_ENDIAN);

                try (ProgressBar progress = new ProgressBar("Rescaling Vertices", numVertices))
                {
                    fcIN.position(vertexElementBegin);
                    Vertex v;
                    for (int n = 0; n < numVertices; n++)
                    {
                        fcIN.read(blockBufferIN);
                        blockBufferIN.flip();

                        v = new Vertex(blockBufferIN, vam);
                        v.transform(translate, (float)scale);
                        if (swapYZ) v.swapYZ();
                        v.writeToStream(bufostream, vam);

                        blockBufferIN.clear();
                        progress.tick();
                    }
                }
            }

            try (FileChannel fcOUT = new FileOutputStream(outputFile, true).getChannel())
            {
                long fileRemainder = reader.getFile().length() - vertexElementBegin - vertexElementLength;
                fcIN.transferTo(fcIN.position(), fileRemainder, fcOUT);
            }
        }

        double minX = (bb.getMinX() - center.getX()) * scale;
        double minY = (bb.getMinY() - center.getY()) * scale;
        double minZ = (bb.getMinZ() - center.getZ()) * scale;
        double lenX = (bb.getMaxX() - center.getX()) * scale - minX;
        double lenY = (bb.getMaxY() - center.getY()) * scale - minY;
        double lenZ = (bb.getMaxZ() - center.getZ()) * scale - minZ;

        if (swapYZ)
        {
            double t = lenY;
            lenY = lenZ;
            lenZ = t;

            t = minY;
            minY = minZ;
            minZ = t;
        }

        return new XBoundingBox(minX, minY, minZ, lenX, lenY, lenZ);
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
            formatter.printHelp("ScaleAndRecenter --filename <path> --output <path> --size <size>", options);
            System.exit(1);
            return null;
        }
    }
}
