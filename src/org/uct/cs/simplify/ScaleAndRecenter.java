package org.uct.cs.simplify;

import javafx.geometry.BoundingBox;
import javafx.geometry.Point3D;
import org.apache.commons.cli.*;
import org.uct.cs.simplify.ply.header.PLYHeader;
import org.uct.cs.simplify.ply.reader.ImprovedPLYReader;
import org.uct.cs.simplify.ply.utilities.BoundsFinder;
import org.uct.cs.simplify.util.MemStatRecorder;
import org.uct.cs.simplify.util.ProgressBar;
import org.uct.cs.simplify.util.Timer;
import org.uct.cs.simplify.util.Useful;

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

    public static BoundingBox run(File inputFile, File outputFile, int size) throws  IOException
    {
        // this scans the target file and works out start and end ranges
        ImprovedPLYReader reader = new ImprovedPLYReader(new PLYHeader(inputFile));

        return run(reader, outputFile, size);
    }

    public static BoundingBox run(ImprovedPLYReader reader, File outputFile, int size) throws IOException
    {
        // first have to identify bounds in order to work out ranges and center
        BoundingBox bb = BoundsFinder.getBoundingBox(reader);
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

        try (RandomAccessFile rafIN = new RandomAccessFile(reader.getFile(), "r"))
        {
            try (FileChannel fcIN = rafIN.getChannel())
            {
                try (RandomAccessFile rafOUT = new RandomAccessFile(outputFile, "rw"))
                {
                    try (FileChannel fcOUT = rafOUT.getChannel())
                    {
                        // copy beginning
                        long vertexElementBegin = reader.getElementDimension("vertex").getFirst();
                        long vertexElementLength = reader.getElementDimension("vertex").getSecond();
                        copyNBytes(fcIN, fcOUT, vertexElementBegin);

                        int blockSize = reader.getHeader().getElement("vertex").getItemSize();
                        int numVertices = reader.getHeader().getElement("vertex").getCount();

                        ByteBuffer blockBufferIN = ByteBuffer.allocateDirect(blockSize);
                        ByteBuffer blockBufferOUT = ByteBuffer.allocateDirect(blockSize);
                        blockBufferIN.order(ByteOrder.LITTLE_ENDIAN);
                        blockBufferOUT.order(ByteOrder.LITTLE_ENDIAN);

                        try(ProgressBar progress = new ProgressBar("Rescaling", numVertices))
                        {

                            float x, y, z;
                            for (int n = 0; n < numVertices; n++)
                            {
                                fcIN.read(blockBufferIN);
                                blockBufferIN.flip();
                                x = (float) ((blockBufferIN.getFloat() + translate.getX()) * scale);
                                y = (float) ((blockBufferIN.getFloat() + translate.getY()) * scale);
                                z = (float) ((blockBufferIN.getFloat() + translate.getZ()) * scale);

                                blockBufferOUT.putFloat(x);
                                blockBufferOUT.putFloat(y);
                                blockBufferOUT.putFloat(z);

                                blockBufferOUT.put(blockBufferIN);
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
                }
            }
        }

        double sx = (bb.getMinX() - center.getX()) * scale;
        double sy = (bb.getMinY() - center.getY()) * scale;
        double sz = (bb.getMinZ() - center.getZ()) * scale;
        double ex = (bb.getMaxX() - center.getX()) * scale;
        double ey = (bb.getMaxY() - center.getY()) * scale;
        double ez = (bb.getMaxZ() - center.getZ()) * scale;

        return new BoundingBox(sx,sy,sz,ex-sx,ey-sy,ez-sz);
    }

    @SuppressWarnings("unused")
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
            File outputFile = new File(outputDir,
                    String.format("%s_rescaled_%d.ply", Useful.getFilenameWithoutExt(inputFile.getName()), rescaleToSize)
            );

            run(inputFile, outputFile, rescaleToSize);
        }
        catch (IOException | InterruptedException e)
        {
            e.printStackTrace();
        }
    }

    private static void copyNBytes(FileChannel input, FileChannel output, long n) throws IOException
    {
        if (n == 0) return;

        int bufsize = COPYBYTES_BUF_SIZE;
        long div = n / bufsize;
        int rem = (int) (n % bufsize);

        ByteBuffer temp = ByteBuffer.allocate(bufsize);
        for (long i = 0; i < div; i++)
        {
            input.read(temp);
            temp.flip();
            while (temp.hasRemaining()) output.write(temp);
            temp.clear();
        }

        if (rem > 0)
        {
            temp = ByteBuffer.allocate(rem);
            input.read(temp);
            temp.flip();
            while (temp.hasRemaining()) output.write(temp);
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

        Option o4 = new Option("m", "memlog", false, "Log memory usage to file (memlog.dat)");
        options.addOption(o4);

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
