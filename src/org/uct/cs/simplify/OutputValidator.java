package org.uct.cs.simplify;

import org.apache.commons.cli.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.uct.cs.simplify.util.ProgressBar;
import org.uct.cs.simplify.util.Useful;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class OutputValidator
{
    public static void main(String[] args) throws IOException
    {
        CommandLine cmd = getCommandLine(args);

        try (BufferedInputStream istream = new BufferedInputStream(new FileInputStream(new File(cmd.getOptionValue("input")))))
        {
            int streamLength = istream.available();
            int headerLength = Useful.readIntLE(istream);
            String jsonHeader = Useful.readString(istream, headerLength);
            System.out.printf("header length: %s%n", headerLength);

            JSONObject o = new JSONObject(jsonHeader);

            boolean hasVertexColour = o.getBoolean("vertex_colour");
            System.out.printf("vertex_colour: %b%n", hasVertexColour);

            JSONArray nodesJ = o.getJSONArray("nodes");
            System.out.printf("number of nodes: %d%n", nodesJ.length());

            List<SomeNode> nodes = new ArrayList<>();
            for (int i = 0; i < nodesJ.length(); i++)
            {
                nodes.add(new SomeNode(nodesJ.getJSONObject(i)));
            }

            Collections.sort(nodes);

            long currentPosition = 0;
            try (ProgressBar pb = new ProgressBar("Checking Nodes", nodes.size()))
            {
                for (SomeNode node : nodes)
                {
                    check(currentPosition, node.blockOffset);

                    check(node.blockLength, node.numVertices * 16 + node.numFaces * 12);

                    for (int i = 0; i < node.numVertices; i++)
                    {
                        float x = Useful.readFloatLE(istream);
                        float y = Useful.readFloatLE(istream);
                        float z = Useful.readFloatLE(istream);

                        checkInRange(node.min_x - 1, x, node.max_x + 1);
                        checkInRange(node.min_y - 1, y, node.max_y + 1);
                        checkInRange(node.min_z - 1, z, node.max_z + 1);
                    }

                    if (hasVertexColour)
                    {
                        for (int i = 0; i < node.numVertices; i++)
                        {
                            byte r = (byte) istream.read();
                            byte g = (byte) istream.read();
                            byte b = (byte) istream.read();
                            byte a = (byte) istream.read();

                            check(r, g);
                            check(g, b);
                            check(a, (byte) 255);
                        }
                    }

                    for (int i = 0; i < node.numFaces; i++)
                    {
                        int f = Useful.readIntLE(istream);
                        int g = Useful.readIntLE(istream);
                        int h = Useful.readIntLE(istream);

                        checkLt(f, node.numVertices);
                        checkLt(g, node.numVertices);
                        checkLt(h, node.numVertices);
                    }
                    currentPosition += node.blockLength;
                    pb.tick();
                }
            }
            check(istream.available(), 0);

            check(streamLength, currentPosition + 4 + jsonHeader.length());

            System.out.println("All checks passed successfully");
        }
    }

    private static boolean check(byte a, byte b)
    {
        if (a != b) throw new RuntimeException(String.format("%s != %s", a, b));
        return true;
    }

    private static boolean check(long a, long b)
    {
        if (a != b) throw new RuntimeException(String.format("%s != %s", a, b));
        return true;
    }

    private static boolean checkLt(long a, long b)
    {
        if (a >= b) throw new RuntimeException(String.format("%s >= %s", a, b));
        return true;
    }

    private static boolean checkInRange(double a, double v, double b)
    {
        if (v > b || v < a) throw new RuntimeException(String.format("%s is out of range %s..%s", v, a, b));
        return true;
    }

    private static CommandLine getCommandLine(String[] args)
    {
        CommandLineParser clp = new BasicParser();

        Options options = new Options();

        Option inputFile = new Option("i", "input", true, "path to first PLY file");
        inputFile.setRequired(true);
        options.addOption(inputFile);

        CommandLine cmd;
        try
        {
            cmd = clp.parse(options, args);
            return cmd;
        }
        catch (ParseException e)
        {
            System.out.printf("%s : %s%n%n", e.getClass().getName(), e.getMessage());
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("outputvalidator --input <path>", options);
            System.exit(1);
            return null;
        }
    }

    private static class SomeNode implements Comparable<SomeNode>
    {
        public final int id;
        public final int parentId;
        public final long numFaces;
        public final long numVertices;
        public final long blockLength;
        public final long blockOffset;
        public final double min_x;
        public final double max_x;
        public final double min_y;
        public final double max_y;
        public final double min_z;
        public final double max_z;

        public SomeNode(JSONObject o)
        {
            this.id = o.getInt("id");
            this.parentId = o.isNull("parent_id") ? -1 : o.getInt("parent_id");
            this.numFaces = o.getLong("num_faces");
            this.numVertices = o.getLong("num_vertices");
            this.blockLength = o.getLong("block_length");
            this.blockOffset = o.getLong("block_offset");
            this.min_x = o.getDouble("min_x");
            this.max_x = o.getDouble("max_x");
            this.min_y = o.getDouble("min_y");
            this.max_y = o.getDouble("max_y");
            this.min_z = o.getDouble("min_z");
            this.max_z = o.getDouble("max_z");
        }

        @Override
        public int compareTo(SomeNode o)
        {
            return Long.compare(this.blockOffset, o.blockOffset);
        }
    }
}
