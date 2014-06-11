package org.uct.cs.simplify;

import javafx.geometry.BoundingBox;
import javafx.geometry.Point3D;
import org.apache.commons.cli.*;
import org.uct.cs.simplify.ply.header.PLYHeader;
import org.uct.cs.simplify.ply.reader.ImprovedPLYReader;
import org.uct.cs.simplify.ply.utilities.BoundsFinder;
import org.uct.cs.simplify.util.MemRecorder;
import org.uct.cs.simplify.util.Timer;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;

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

        try (Timer ignored = new Timer("Processed"))
        {
            File inputFile = new File(filename);

            // output file stuff
            File outputDir = new File(new File(outputDirectory).getCanonicalPath());
            if (!outputDir.exists() && !outputDir.mkdirs())
                throw new IOException("Could not create output directory " + outputDir);
            File outputFile = new File(outputDir, String.format("%s_rescaled_%d.ply", getFilenameWithoutExt(inputFile.getName()), rescaleToSize));

            // memory logger
            MemRecorder mRecorder = null;
            if (cmd.hasOption("memlog"))
            {
                File memFile = new File(outputDir, String.format("memlog_%d.dat", System.currentTimeMillis()));
                System.out.printf("Logging memory usage to %s%n", memFile);
                mRecorder = new MemRecorder(memFile, 100);
            }

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
            double scale = (rescaleToSize / 2.0) / meshHalfSize;
            Point3D translate = new Point3D(-center.getX(), -center.getY(), -center.getZ());

            // debug
            System.out.printf("Input File: %s%n", inputFile);
            System.out.printf("Output File: %s%n", outputFile);
            System.out.printf("X: %f → %f%n", bb.getMinX(), bb.getMaxX());
            System.out.printf("Y: %f → %f%n", bb.getMinY(), bb.getMaxY());
            System.out.printf("Z: %f → %f%n", bb.getMinZ(), bb.getMaxZ());
            System.out.printf("Center: %f, %f, %f%n", center.getX(), center.getY(), center.getZ());
            System.out.printf("Scale ratio: %f%n", scale);

            try (RandomAccessFile rafIN = new RandomAccessFile(inputFile, "r"))
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

                            int percentN = numVertices / 100;
                            int tenPercentN = numVertices / 10;
                            System.out.printf("Progress: (each dot indicates %d vertices)%n", percentN);

                            float x, y, z;
                            for (int n = 0; n < numVertices; n++)
                            {
                                if (n % percentN == 0) System.out.print(".");
                                if ((n + 1) % tenPercentN == 0) System.out.print(" ");

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
                            }

                            // copy remainder
                            long fileRemainder = inputFile.length() - vertexElementBegin - vertexElementLength;
                            copyNBytes(fcIN, fcOUT, fileRemainder);

                            System.out.println();
                        }
                    }
                }
            }

            if (mRecorder != null) mRecorder.close();
        }
        catch (IOException | InterruptedException e)
        {
            e.printStackTrace();
        }

    }

    private static void copyNBytes(FileChannel input, FileChannel output, long n) throws IOException
    {
        if (n == 0) return;

        int bufsize = 4096;
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

    private static String getFilenameWithoutExt(String fn)
    {
        return fn.substring(0, fn.lastIndexOf('.'));
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
