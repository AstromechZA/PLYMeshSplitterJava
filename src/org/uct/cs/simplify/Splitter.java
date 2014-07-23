package org.uct.cs.simplify;

import javafx.geometry.Point3D;
import org.apache.commons.cli.*;
import org.uct.cs.simplify.ply.datatypes.DataTypes;
import org.uct.cs.simplify.ply.header.PLYElement;
import org.uct.cs.simplify.ply.header.PLYHeader;
import org.uct.cs.simplify.ply.header.PLYListProperty;
import org.uct.cs.simplify.ply.header.PLYProperty;
import org.uct.cs.simplify.ply.reader.*;
import org.uct.cs.simplify.ply.utilities.OctetFinder;
import org.uct.cs.simplify.util.MemStatRecorder;
import org.uct.cs.simplify.util.ProgressBar;
import org.uct.cs.simplify.util.Timer;
import org.uct.cs.simplify.util.Useful;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Splitter
{
    private static final int DEFAULT_BYTEOSBUF_SIZE = 512 * 1024;
    private static final int DEFAULT_RESCALE_SIZE = 1024;

    public static void run(File inputFile, File outputDir) throws IOException
    {
        // this scans the target file and works out start and end ranges
        ImprovedPLYReader reader = new ImprovedPLYReader(new PLYHeader(inputFile));

        File scaledFile = new File(outputDir,
                String.format("%s_rescaled_%d.ply", Useful.getFilenameWithoutExt(inputFile.getName()), DEFAULT_RESCALE_SIZE)
        );

        ScaleAndRecenter.run(reader, scaledFile, DEFAULT_RESCALE_SIZE);

        // now switch to rescaled version
        reader = new ImprovedPLYReader(new PLYHeader(scaledFile));

        // calculate vertex memberships
        OctetFinder.Octet[] memberships = calculateVertexMemberships(reader);

        for (OctetFinder.Octet current : OctetFinder.Octet.values())
        {
            System.out.println();
            File octetFaceFile = new File(outputDir, String.format("%s_%s", Useful.getFilenameWithoutExt(scaledFile.getName()), current));

            LinkedHashMap<Integer, Integer> new_vertex_indices = new LinkedHashMap<>(reader.getHeader().getElement("vertex").getCount() / 4);
            int num_faces = gatherOctetFaces(reader, memberships, current, octetFaceFile, new_vertex_indices);
            int num_vertices = new_vertex_indices.size();

            PLYHeader newHeader = constructNewHeader(num_faces, num_vertices);

            File octetFile = new File(outputDir, String.format("%s_%s.ply", Useful.getFilenameWithoutExt(scaledFile.getName()), current));

            try (FileOutputStream fostream = new FileOutputStream(octetFile))
            {
                fostream.write((newHeader + "\n").getBytes());

                try (MemoryMappedVertexReader vr = new MemoryMappedVertexReader(reader))
                {
                    try (ByteArrayOutputStream bostream = new ByteArrayOutputStream(DEFAULT_BYTEOSBUF_SIZE))
                    {
                        Vertex v;
                        ByteBuffer bb = ByteBuffer.wrap(new byte[ 3 * 4 ]);
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

                                bb.flip();
                                bostream.write(bb.array());
                                bb.flip();
                                bb.clear();

                                if (bostream.size() > DEFAULT_BYTEOSBUF_SIZE - 16)
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
    }

    private static PLYHeader constructNewHeader(int num_faces, int num_vertices)
    {
        List<PLYElement> elements = new ArrayList<>();
        PLYElement eVertex = new PLYElement("vertex", num_vertices);
        eVertex.addProperty(new PLYProperty("x", DataTypes.DataType.FLOAT));
        eVertex.addProperty(new PLYProperty("y", DataTypes.DataType.FLOAT));
        eVertex.addProperty(new PLYProperty("z", DataTypes.DataType.FLOAT));
        elements.add(eVertex);
        PLYElement eFace = new PLYElement("face", num_faces);
        eFace.addProperty(new PLYListProperty("vertex_indices", DataTypes.DataType.INT, DataTypes.DataType.UCHAR));
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
                            if (bostream.size() > DEFAULT_BYTEOSBUF_SIZE - 16)
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
        stream.write((i >> 0) & 0xFF);
        stream.write((i >> 8) & 0xFF);
        stream.write((i >> 16) & 0xFF);
        stream.write((i >> 24) & 0xFF);

    }

    private static OctetFinder.Octet[] calculateVertexMemberships(ImprovedPLYReader reader) throws IOException
    {
        OctetFinder ofinder = new OctetFinder(new Point3D(0, 0, 0));

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
