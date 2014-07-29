package org.uct.cs.simplify;

import javafx.geometry.BoundingBox;
import javafx.geometry.Point3D;
import org.apache.commons.cli.*;
import org.uct.cs.simplify.ply.datatypes.DataType;
import org.uct.cs.simplify.ply.header.PLYHeader;
import org.uct.cs.simplify.ply.reader.*;
import org.uct.cs.simplify.ply.utilities.OctetFinder;
import org.uct.cs.simplify.util.*;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.ArrayDeque;
import java.util.LinkedHashMap;
import java.util.Map;

public class Splitter
{
    private static final int BYTE = 0xFF;
    private static final int DEFAULT_BYTEOSBUF_SIZE = 524288;
    private static final int DEFAULT_BYTEOSBUF_TAIL = 16;
    private static final int DEFAULT_MODEL_SIZE = 1024;

    public static void run(File inputFile, File outputDir, int maxDepth, boolean swapYZ) throws IOException
    {
        // this scans the target file and works out start and end ranges
        ImprovedPLYReader reader = new ImprovedPLYReader(new PLYHeader(inputFile));

        File scaledFile = new File(
                outputDir,
                String.format(
                        "%s_rescaled_%d.ply", Useful.getFilenameWithoutExt(inputFile.getName()), DEFAULT_MODEL_SIZE
                )
        );

        BoundingBox finalBoundingBox = ScaleAndRecenter.run(reader, scaledFile, DEFAULT_MODEL_SIZE, swapYZ);

        System.out.printf("%f -> %f%n", finalBoundingBox.getMinX(), finalBoundingBox.getMaxX());
        System.out.printf("%f -> %f%n", finalBoundingBox.getMinY(), finalBoundingBox.getMaxY());
        System.out.printf("%f -> %f%n", finalBoundingBox.getMinZ(), finalBoundingBox.getMaxZ());

        ArrayDeque<Triple<File, Integer, Point3D>> processQueue = new ArrayDeque<>();
        processQueue.add(new Triple<>(scaledFile, 1, Point3D.ZERO));

        while (!processQueue.isEmpty())
        {
            Triple<File, Integer, Point3D> processEntry = processQueue.removeFirst();
            File processFile = processEntry.getFirst();
            int processDepth = processEntry.getSecond();
            Point3D splitPoint = processEntry.getThird();

            String processFileBase = Useful.getFilenameWithoutExt(processFile.getName());

            // now switch to rescaled version
            reader = new ImprovedPLYReader(new PLYHeader(processFile));
            int average_vertices_per_octet = reader.getHeader().getElement("vertex").getCount() / 8;

            OctetFinder.Octet[] memberships = calculateVertexMemberships(reader, splitPoint);

            for (OctetFinder.Octet currentOctet : OctetFinder.Octet.values())
            {
                try (
                        TempFile temporaryFaceFile = new TempFile(
                                outputDir, String.format(
                                "%s_%s.temp", processFileBase, currentOctet
                        )
                        )
                )
                {
                    LinkedHashMap<Integer, Integer> new_vertex_indices = new LinkedHashMap<>(
                            average_vertices_per_octet
                    );
                    int num_faces = gatherOctetFaces(
                            reader, memberships, currentOctet, temporaryFaceFile, new_vertex_indices
                    );

                    File octetFile = new File(outputDir, String.format("%s_%s.ply", processFileBase, currentOctet));

                    writeOctetPLYModel(
                            reader, currentOctet, temporaryFaceFile, new_vertex_indices, num_faces, octetFile
                    );

                    if (processDepth < maxDepth)
                    {
                        processQueue.addLast(
                                new Triple<>(
                                        octetFile,
                                        processDepth + 1,
                                        currentOctet.calculateCenterBasedOn(splitPoint, processDepth, finalBoundingBox)
                                )
                        );
                    }
                }
            }
        }
    }

    private static void writeOctetPLYModel(
            ImprovedPLYReader reader,
            OctetFinder.Octet currentOctet,
            File octetFaceFile,
            LinkedHashMap<Integer, Integer> vertexMap,
            int numFaces,
            File octetFile
    ) throws IOException
    {
        PLYHeader newHeader = PLYHeader.constructBasicHeader(vertexMap.size(), numFaces);

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
                    try (
                            ProgressBar pb = new ProgressBar(
                                    String.format("%s: Writing Vertices", currentOctet), vertexMap.size()
                            )
                    )
                    {
                        for (int i : vertexMap.keySet())
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
        }
    }

    private static int gatherOctetFaces(
            ImprovedPLYReader reader,
            OctetFinder.Octet[] memberships,
            OctetFinder.Octet current,
            File octetFaceFile,
            Map<Integer, Integer> vertexMap
    ) throws IOException
    {
        int num_faces_in_octet = 0;
        try (
                ProgressBar progress = new ProgressBar(
                        String.format("%s : Scanning & Writing Faces", current),
                        reader.getHeader().getElement("face").getCount()
                )
        )
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

                            if (face.getVertices().stream().anyMatch(v -> memberships[v] == current))
                            {
                                num_faces_in_octet += 1;
                                bostream.write((byte) face.getNumVertices());
                                for (int vertex_index : face.getVertices())
                                {
                                    if (!vertexMap.containsKey(vertex_index))
                                    {
                                        vertexMap.put(vertex_index, current_vertex_index);
                                        current_vertex_index += 1;
                                    }
                                    littleEndianWrite(bostream, vertexMap.get(vertex_index));
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
        stream.write((i >> (8 * 2)) & BYTE);
        stream.write((i >> (8 * 3)) & BYTE);

    }

    private static OctetFinder.Octet[] calculateVertexMemberships(
            ImprovedPLYReader reader, Point3D splitPoint
    ) throws IOException
    {
        OctetFinder ofinder = new OctetFinder(splitPoint);

        try (MemoryMappedVertexReader vr = new MemoryMappedVertexReader(reader))
        {
            try (ProgressBar pb = new ProgressBar("Calculating Memberships", vr.getCount()))
            {
                int c = vr.getCount();
                OctetFinder.Octet[] memberships = new OctetFinder.Octet[c];
                Vertex v;
                for (int i = 0; i < c; i++)
                {
                    pb.tick();
                    v = vr.get(i);
                    memberships[i] = ofinder.getOctet(v.x, v.y, v.z);
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

            int depth = Integer.parseInt(cmd.getOptionValue("depth"));
            if (depth < 2 || depth > 8) throw new IllegalArgumentException("Splitting depth must be between 1 and 9!");

            run(file, outputDir, depth, cmd.hasOption("swapyz"));
        }
        catch (IOException | InterruptedException | IllegalArgumentException e)
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

        Option opFilename = new Option("f", "filename", true, "path to PLY model to process");
        opFilename.setRequired(true);
        options.addOption(opFilename);

        Option opOutput = new Option("o", "output", true, "Destination directory of models");
        opOutput.setRequired(true);
        options.addOption(opOutput);

        Option opDepth = new Option("d", "depth", true, "Number of levels to split to");
        opDepth.setType(Short.class);
        options.addOption(opDepth);

        Option opSwapYZ = new Option("yz", "swapyz", false, "Swap the Y and Z axis during preprocessing");
        options.addOption(opSwapYZ);

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
