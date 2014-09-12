package org.uct.cs.simplify;

import javafx.geometry.Point3D;
import org.apache.commons.cli.*;
import org.uct.cs.simplify.ply.datatypes.DataType;
import org.uct.cs.simplify.ply.header.PLYHeader;
import org.uct.cs.simplify.ply.reader.PLYReader;
import org.uct.cs.simplify.ply.utilities.BoundsFinder;
import org.uct.cs.simplify.util.*;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;

public class ScaleAndRecenter
{
    private static final int DEFAULT_RESCALE_SIZE = 1024;
    private static final int COPYBYTES_BUF_SIZE = 4096;

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
        System.out.printf("Input File: %s%n", reader.getFile());
        System.out.printf("Output File: %s%n", outputFile);
        System.out.printf("Scale ratio: %f%n", scale);
        System.out.printf("Swapping YZ axis: %s%n", swapYZ);

        try (
            RandomAccessFile rafIN = new RandomAccessFile(reader.getFile(), "r");
            FileChannel fcIN = rafIN.getChannel();
            RandomAccessFile rafOUT = new RandomAccessFile(outputFile, "rw");
            FileChannel fcOUT = rafOUT.getChannel()
        )
        {

            int numVertices = reader.getHeader().getElement("vertex").getCount();
            int numFaces = reader.getHeader().getElement("face").getCount();

            // construct new header
            PLYHeader header = PLYHeader.constructBasicHeader(numVertices, numFaces);
            fcOUT.write(ByteBuffer.wrap((header + "\n").getBytes()));

            long vertexElementBegin = reader.getElementDimension("vertex").getFirst();
            long vertexElementLength = reader.getElementDimension("vertex").getSecond();

            int blockSize = reader.getHeader().getElement("vertex").getItemSize();

            ByteBuffer blockBufferIN = ByteBuffer.allocateDirect(blockSize);
            ByteBuffer blockBufferOUT = ByteBuffer.allocateDirect(3 * DataType.FLOAT.getByteSize());
            blockBufferIN.order(ByteOrder.LITTLE_ENDIAN);
            blockBufferOUT.order(ByteOrder.LITTLE_ENDIAN);

            try (ProgressBar progress = new ProgressBar("Rescaling Vertices", numVertices))
            {
                fcIN.position(vertexElementBegin);
                float x, y, z, t;
                for (int n = 0; n < numVertices; n++)
                {
                    fcIN.read(blockBufferIN);
                    blockBufferIN.flip();
                    x = (float) ((blockBufferIN.getFloat() + translate.getX()) * scale);
                    y = (float) ((blockBufferIN.getFloat() + translate.getY()) * scale);
                    z = (float) ((blockBufferIN.getFloat() + translate.getZ()) * scale);

                    if (swapYZ)
                    {
                        t = z;
                        z = -y;
                        y = t;
                    }

                    blockBufferOUT.putFloat(x);
                    blockBufferOUT.putFloat(y);
                    blockBufferOUT.putFloat(z);

                    blockBufferOUT.flip();

                    fcOUT.write(blockBufferOUT);

                    blockBufferIN.clear();
                    blockBufferOUT.clear();

                    progress.tick();
                }
            }

            // copy remainder
            long fileRemainder = reader.getFile().length() - vertexElementBegin - vertexElementLength;
            copyNBytes(fcIN, fcOUT, fileRemainder);
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

    private static void copyNBytes(FileChannel input, FileChannel output, long n) throws IOException
    {
        if (n == 0) return;

        int bufsize = COPYBYTES_BUF_SIZE;
        long div = n / bufsize;
        int rem = (int) (n % bufsize);

        ProgressBar pb = new ProgressBar("Copying Edges", div + 1);
        ByteBuffer temp = ByteBuffer.allocate(bufsize);
        for (long i = 0; i < div; i++)
        {
            input.read(temp);
            temp.flip();
            while (temp.hasRemaining()) output.write(temp);
            temp.clear();
            pb.tick();
        }
        if (rem > 0)
        {
            temp = ByteBuffer.allocate(rem);
            input.read(temp);
            temp.flip();
            while (temp.hasRemaining()) output.write(temp);
            pb.tick();
        }
        pb.close();
    }

    public static void main(String[] args)
    {
        try (Timer ignored = new Timer(); MemStatRecorder ignored2 = new MemStatRecorder())
        {
            CommandLine cmd = parseArgs(args);
            int rescaleToSize = (
                cmd.hasOption("size") ? Integer.parseInt(cmd.getOptionValue("size")) : DEFAULT_RESCALE_SIZE
            );
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
