package org.uct.cs.simplify;

import org.apache.commons.cli.*;
import org.uct.cs.simplify.ply.header.PLYHeader;
import org.uct.cs.simplify.ply.reader.*;
import org.uct.cs.simplify.ply.utilities.BoundsFinder;
import org.uct.cs.simplify.ply.utilities.OctetFinder;
import org.uct.cs.simplify.util.MemStatRecorder;
import org.uct.cs.simplify.util.Timer;
import org.uct.cs.simplify.util.Useful;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;

public class Splitter
{

    private static final int DEFAULT_RESCALE_SIZE = 1024;

    public static void run(File inputFile, File outputDir) throws IOException
    {
        // this scans the target file and works out start and end ranges
        ImprovedPLYReader reader = new ImprovedPLYReader(new PLYHeader(inputFile));

        File scaledFile = new File(outputDir,
                String.format("%s_rescaled_%d.ply", Useful.getFilenameWithoutExt(inputFile.getName()), DEFAULT_RESCALE_SIZE)
        );

        System.out.println("Rescaling and Centering...");

        ScaleAndRecenter.run(reader, scaledFile, DEFAULT_RESCALE_SIZE);

        System.out.println("Done.");

        // now switch to rescaled version
        reader = new ImprovedPLYReader(new PLYHeader(scaledFile));

        try(Timer ignored = new Timer("Split")) {

            // calculate vertex memberships
            OctetFinder.Octet[] memberships = calculateVertexMemberships(reader);

            OctetFinder.Octet current = OctetFinder.Octet.XYZ;
            HashMap<Integer, Integer> new_vertex_indexes = new HashMap<>();

            File octetFaceFile = new File(outputDir,
                    String.format("%s_%s", Useful.getFilenameWithoutExt(scaledFile.getName()), current)
            );

            try (MemoryMappedFaceReader fr = new MemoryMappedFaceReader(reader)) {
                try (RandomAccessFile rafOUT = new RandomAccessFile(octetFaceFile, "rw")) {

                    Face f;
                    int current_vertex_index = 1;
                    while (fr.hasNext()) {
                        f = fr.next();

                        boolean inside = false;
                        for (int i : f.getVertices()) {
                            if (memberships[i] == current) {
                                inside = true;
                                break;
                            }
                        }

                        if (inside) {

                            ByteBuffer buffer = ByteBuffer.allocate(1 + 4 * f.getNumVertices());
                            buffer.order(ByteOrder.LITTLE_ENDIAN);
                            buffer.put((byte) f.getNumVertices());
                            for (int i : f.getVertices()) {
                                if (!new_vertex_indexes.containsKey(i)) {
                                    new_vertex_indexes.put(i, current_vertex_index);
                                    current_vertex_index += 1;
                                }
                                buffer.putInt(new_vertex_indexes.get(i));
                            }
                            rafOUT.write(buffer.array());
                        }
                    }
                }
            }

            System.out.println(reader.getHeader().toString());

            // write header to file2
            // write new_vertexes to file2
            // write file1 to file2
            // doone
        }
    }

    private static OctetFinder.Octet[] calculateVertexMemberships(ImprovedPLYReader reader) throws IOException
    {
        OctetFinder ofinder = new OctetFinder(BoundsFinder.getBoundingBox(reader));

        try (MemoryMappedVertexReader vr = new MemoryMappedVertexReader(reader))
        {
            int c = vr.getCount();
            OctetFinder.Octet[] memberships = new OctetFinder.Octet[c];
            Vertex v;
            for (int i = 0; i < c; i++)
            {
                v = vr.get(i);
                memberships[i] = ofinder.getOctet(v.x, v.y, v.z);
            }
            return memberships;
        }
    }

    public static void main(String[] args)
    {
        CommandLine cmd = getCommandLine(args);

        try (Timer ignored = new Timer("Total"); MemStatRecorder ignored2 = new MemStatRecorder())
        {
            File file = new File(cmd.getOptionValue("filename"));
            File outputDir = new File(new File(cmd.getOptionValue("output")).getCanonicalPath());
            if (!outputDir.exists() && !outputDir.mkdirs())
                throw new IOException("Could not create output directory " + outputDir);

            run(file, outputDir);
        }
        catch (IOException | InterruptedException e)
        {
            e.printStackTrace();
        }
    }

    /**
     * Parses the given string array as an options array and returns a CommandLine instance
     * containing the results. If required options were missing or an error occured, it will print
     * usage to standard out and System.exit(1).
     *
     * @param args String[] containing program arguments
     * @return CommandLine instance containing results
     */
    private static CommandLine getCommandLine(String[] args)
    {
        CommandLineParser clp = new BasicParser();

        Options options = new Options();

        Option o1 = new Option("f", "filename", true, "path to PLY model to process");
        o1.setRequired(true);
        options.addOption(o1);

        Option o2 = new Option("o", "output", true, "Destination directory of models");
        o2.setRequired(true);
        options.addOption(o2);

        Option o3 = new Option("d", "depth", true, "number of levels to split to");
        o3.setType(Short.class);
        options.addOption(o3);

        Option o4 = new Option("s", "scaleTo", true, "scale the model to fit a cube of the given size");
        o4.setType(Short.class);
        options.addOption(o4);

        CommandLine cmd;
        try
        {
            cmd = clp.parse(options, args);
            return cmd;
        }
        catch (ParseException e)
        {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("PLYMeshSplitterJ --filename <path>", options);
            System.exit(1);
            return null;
        }
    }

}
