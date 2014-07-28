package org.uct.cs.simplify;

import org.apache.commons.cli.*;
import org.uct.cs.simplify.ply.datatypes.DataType;
import org.uct.cs.simplify.ply.header.PLYElement;
import org.uct.cs.simplify.ply.header.PLYHeader;
import org.uct.cs.simplify.ply.header.PLYListProperty;
import org.uct.cs.simplify.ply.header.PLYProperty;
import org.uct.cs.simplify.ply.reader.*;
import org.uct.cs.simplify.ply.utilities.BoundsFinder;
import org.uct.cs.simplify.ply.utilities.OctetFinder;
import org.uct.cs.simplify.util.*;
import org.uct.cs.simplify.util.Timer;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.*;

public class Splitter
{
    private static final int BYTE = 0xFF;
    private static final int DEFAULT_BYTEOSBUF_SIZE = 524288;
    private static final int DEFAULT_BYTEOSBUF_TAIL = 16;
    private static final int DEFAULT_RESCALE_SIZE = 1024;
    private static final int DEFAULT_NUM_SPLIT_LEVELS = 2;

    public static void run(File inputFile, File outputDir) throws IOException
    {
        // this scans the target file and works out start and end ranges
        ImprovedPLYReader reader = new ImprovedPLYReader(new PLYHeader(inputFile));

        File scaledFile = new File(outputDir,
                String.format("%s_rescaled_%d.ply", Useful.getFilenameWithoutExt(inputFile.getName()), DEFAULT_RESCALE_SIZE)
        );

        ScaleAndRecenter.run(reader, scaledFile, DEFAULT_RESCALE_SIZE);
        ArrayDeque<Pair<File, Integer>> processQueue = new ArrayDeque<>();
        processQueue.add(new Pair<>(scaledFile, 0));

        while(!processQueue.isEmpty())
        {
            Pair<File, Integer> processEntry = processQueue.removeFirst();
            File processFile = processEntry.getFirst();
            int processDepth = processEntry.getSecond();

            // now switch to rescaled version
            reader = new ImprovedPLYReader(new PLYHeader(processFile));

            // calculate vertex memberships
            OctetFinder.Octet[] memberships = calculateVertexMemberships(reader);

            for (OctetFinder.Octet current : OctetFinder.Octet.values())
            {
                System.out.println();
                File octetFaceFile = new File(outputDir, String.format("%s_%s", Useful.getFilenameWithoutExt(processFile.getName()), current));

                LinkedHashMap<Integer, Integer> new_vertex_indices = new LinkedHashMap<>(reader.getHeader().getElement("vertex").getCount() / 8);
                int num_faces = gatherOctetFaces(reader, memberships, current, octetFaceFile, new_vertex_indices);
                int num_vertices = new_vertex_indices.size();

                PLYHeader newHeader = constructNewHeader(num_faces, num_vertices);

                File octetFile = new File(outputDir, String.format("%s_%s.ply", Useful.getFilenameWithoutExt(processFile.getName()), current));

                try (FileOutputStream fostream = new FileOutputStream(octetFile))
                {
                    fostream.write((newHeader + "\n").getBytes());

                    try (MemoryMappedVertexReader vr = new MemoryMappedVertexReader(reader))
                    {
                        try (ByteArrayOutputStream bostream = new ByteArrayOutputStream(DEFAULT_BYTEOSBUF_SIZE))
                        {
                            Vertex v;
                            ByteBuffer bb = ByteBuffer.wrap(new byte[3 * DataType.FLOAT.getByteSize()]);
                            bb.order(ByteOrder.LITTLE_ENDIAN);
                            try (ProgressBar pb = new ProgressBar(String.format("%s: Writing Vertices", current), new_vertex_indices.size()))
                            {
                                for (int i : new_vertex_indices.keySet())
                                {
                                    pb.tick();
                                    v = vr.get(i);
                                    bb.putFloat(v.x);
                                    bb.putFloat(v.y);
                                    bb.putFloat(v.z);

                                    bostream.write(bb.array());
                                    bb.clear();

                                    if (bostream.size() > DEFAULT_BYTEOSBUF_SIZE - DEFAULT_BYTEOSBUF_TAIL)
                                    {
                                        fostream.write(bostream.toByteArray());
                                        bostream.reset();
                                    }
                                }
                                if (bostream.size() > 0) fostream.write(bostream.toByteArray());

                            }
                        }
                    }

                    try (FileChannel fc = new FileInputStream(octetFaceFile).getChannel())
                    {
                        fostream.getChannel().transferFrom(fc, fostream.getChannel().position(), fc.size());
                    }

                    if (!octetFaceFile.delete())
                        throw new IOException("File not deleted " + octetFaceFile.getAbsolutePath());
                }

                if (processDepth < DEFAULT_NUM_SPLIT_LEVELS)
                {
                    processQueue.addLast(new Pair<>(octetFile, processDepth+1));
                }
            }
        }
    }

    private static PLYHeader constructNewHeader(int num_faces, int num_vertices)
    {
        List<PLYElement> elements = new ArrayList<>();
        PLYElement eVertex = new PLYElement("vertex", num_vertices);
        eVertex.addProperty(new PLYProperty("x", DataType.FLOAT));
        eVertex.addProperty(new PLYProperty("y", DataType.FLOAT));
        eVertex.addProperty(new PLYProperty("z", DataType.FLOAT));
        elements.add(eVertex);
        PLYElement eFace = new PLYElement("face", num_faces);
        eFace.addProperty(new PLYListProperty("vertex_indices", DataType.INT, DataType.UCHAR));
        elements.add(eFace);
        return new PLYHeader(elements);
    }

    private static int gatherOctetFaces(
            ImprovedPLYReader reader,
            OctetFinder.Octet[] memberships,
            OctetFinder.Octet current,
            File octetFaceFile,
            Map<Integer, Integer> output
    ) throws IOException
    {
        int num_faces_in_octet = 0;
        try (ProgressBar progress = new ProgressBar(String.format("%s : Scanning & Writing Faces", current), reader.getHeader().getElement("face").getCount()))
        {
            try (MemoryMappedFaceReader faceReader = new MemoryMappedFaceReader(reader))
            {
                try (FileOutputStream fostream = new FileOutputStream(octetFaceFile))
                {
                    try (ByteArrayOutputStream bostream = new ByteArrayOutputStream(DEFAULT_BYTEOSBUF_SIZE))
                    {
                        Face face;
                        int current_vertex_index = 0;

                        while (faceReader.hasNext())
                        {
                            progress.tick();
                            face = faceReader.next();

                            if (face.getVertices().stream().anyMatch(v -> memberships[ v ] == current))
                            {
                                num_faces_in_octet += 1;
                                bostream.write((byte) face.getNumVertices());
                                for (int vertex_index : face.getVertices())
                                {
                                    if (!output.containsKey(vertex_index))
                                    {
                                        output.put(vertex_index, current_vertex_index);
                                        current_vertex_index += 1;
                                    }
                                    littleEndianWrite(bostream, output.get(vertex_index));
                                }
                            }
                            if (bostream.size() > DEFAULT_BYTEOSBUF_SIZE - DEFAULT_BYTEOSBUF_TAIL)
                            {
                                fostream.write(bostream.toByteArray());
                                bostream.reset();
                            }
                        }
                        if (bostream.size() > 0) fostream.write(bostream.toByteArray());
                    }
                }
            }
        }
        return num_faces_in_octet;
    }

    private static void littleEndianWrite(ByteArrayOutputStream stream, int i)
    {
        stream.write((i) & BYTE);
        stream.write((i >> 8) & BYTE);
        stream.write((i >> (8*2)) & BYTE);
        stream.write((i >> (8*3)) & BYTE);

    }

    private static OctetFinder.Octet[] calculateVertexMemberships(ImprovedPLYReader reader) throws IOException
    {
        OctetFinder ofinder = new OctetFinder(BoundsFinder.getBoundingBox(reader));

        try (MemoryMappedVertexReader vr = new MemoryMappedVertexReader(reader))
        {
            try (ProgressBar pb = new ProgressBar("Calculating Memberships", vr.getCount()))
            {
                int c = vr.getCount();
                OctetFinder.Octet[] memberships = new OctetFinder.Octet[ c ];
                Vertex v;
                for (int i = 0; i < c; i++)
                {
                    pb.tick();
                    v = vr.get(i);
                    memberships[ i ] = ofinder.getOctet(v.x, v.y, v.z);
                }
                return memberships;
            }
        }
    }

    @SuppressWarnings("unused")
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
