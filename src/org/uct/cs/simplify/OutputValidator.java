package org.uct.cs.simplify;

import org.apache.commons.cli.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.uct.cs.simplify.util.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OutputValidator
{
    public static final int BYTES_PER_VERTEX = 12;
    public static final int BYTES_PER_FACE = 12;

    public static void run(File file, boolean verbose) throws IOException
    {
        try (ReliableBufferedInputStream istream = new ReliableBufferedInputStream(new FileInputStream(file)))
        {
            long streamLength = file.length();
            int headerLength = Useful.readIntLE(istream);
            String jsonHeader = Useful.readString(istream, headerLength);
            Outputter.info1f("header length: %s%n", headerLength);

            JSONObject o = new JSONObject(jsonHeader);

            boolean hasVertexColour = o.getBoolean("vertex_colour");
            Outputter.info1f("vertex_colour: %b%n", hasVertexColour);

            JSONArray nodesJ = o.getJSONArray("nodes");
            Outputter.info1f("number of nodes: %d%n", nodesJ.length());

            SomeNode[] nodes = new SomeNode[ nodesJ.length() ];
            for (int i = 0; i < nodesJ.length(); i++)
            {
                SomeNode n = (new SomeNode(nodesJ.getJSONObject(i)));
                nodes[ n.id ] = n;
            }

            long currentPosition = 0;
            try (ProgressBar pb = new ProgressBar("Checking Nodes", nodes.length))
            {
                for (SomeNode node : nodes)
                {
                    check(currentPosition, node.blockOffset);

                    long l = node.numVertices * BYTES_PER_VERTEX + ((hasVertexColour) ? node.numVertices * 3 : 0) +
                        node.numFaces * BYTES_PER_FACE;
                    check(node.blockLength, l);

                    for (long i = 0; i < node.numVertices; i++)
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
                        for (long i = 0; i < node.numVertices; i++)
                        {
                            istream.read();
                            istream.read();
                            istream.read();
                        }
                    }

                    for (long i = 0; i < node.numFaces; i++)
                    {
                        int f = Useful.readIntLE(istream);
                        int g = Useful.readIntLE(istream);
                        int h = Useful.readIntLE(istream);

                        checkLt(f, node.numVertices);
                        checkLt(g, node.numVertices);
                        checkLt(h, node.numVertices);
                    }
                    currentPosition += node.blockLength;

                    if (node.parentId >= 0)
                    {
                        nodes[ node.parentId ].children.add(node);
                    }

                    pb.tick();
                }
            }
            check(istream.available(), 0);

            check(streamLength, currentPosition + 4 + jsonHeader.length());

            Outputter.info3ln("All checks passed successfully");

            if (verbose)
            {
                HashMap<Integer, NumberSummary> faceSummaries = new HashMap<>();
                HashMap<Integer, NumberSummary> vertexSummaries = new HashMap<>();
                HashMap<Integer, NumberSummary> simpSummaries = new HashMap<>();
                NumberSummary leafSizeFaces = new NumberSummary(nodes.length);
                NumberSummary leafSizeVertices = new NumberSummary(nodes.length);
                NumberSummary leafDepth = new NumberSummary(nodes.length);

                for (SomeNode node : nodes)
                {
                    if (node.children.size() > 0)
                    {
                        long total = 0;
                        for (SomeNode child : node.children)
                        {
                            total += child.numFaces;
                        }
                        double ratio = node.numFaces / (double) total;
                        if (simpSummaries.containsKey(node.depth))
                        {
                            simpSummaries.get(node.depth).add(ratio);
                        }
                        else
                        {
                            simpSummaries.put(node.depth, new NumberSummary(10, ratio));
                        }
                    }
                    else
                    {
                        leafDepth.add(node.depth);
                        leafSizeFaces.add(node.numFaces);
                        leafSizeVertices.add(node.numVertices);
                    }

                    if (faceSummaries.containsKey(node.depth))
                    {
                        faceSummaries.get(node.depth).add(node.numFaces);
                    }
                    else
                    {
                        faceSummaries.put(node.depth, new NumberSummary(10, node.numFaces));
                    }
                    if (vertexSummaries.containsKey(node.depth))
                    {
                        vertexSummaries.get(node.depth).add(node.numFaces);
                    }
                    else
                    {
                        vertexSummaries.put(node.depth, new NumberSummary(10, node.numVertices));
                    }
                }

                Outputter.info1ln("Tree Analysis:");
                Outputter.info1f("Nodes: %d%n", nodesJ.length());
                Outputter.info1f("Leaf nodes: %d%n", leafDepth.count);
                Outputter.info1f("Leaf Depth: Min: %f %n", leafDepth.min);
                Outputter.info1f("Leaf Depth: P50: %f %n", leafDepth.p50);
                Outputter.info1f("Leaf Depth: Max: %f %n", leafDepth.max);
                Outputter.info1f("Leaf Depth: Mean: %f %n", leafDepth.mean);
                Outputter.info1f("Leaf Size Faces: Min: %f %n", leafSizeFaces.min);
                Outputter.info1f("Leaf Size Faces: P25: %f %n", leafSizeFaces.p25);
                Outputter.info1f("Leaf Size Faces: P50: %f %n", leafSizeFaces.p50);
                Outputter.info1f("Leaf Size Faces: P75: %f %n", leafSizeFaces.p75);
                Outputter.info1f("Leaf Size Faces: Max: %f %n", leafSizeFaces.max);
                Outputter.info1f("Leaf Size Faces: Mean: %f %n", leafSizeFaces.mean);
                Outputter.info1f("Leaf Size Faces: StdDev: %f %n%n", leafSizeFaces.calculateStdDev());
                Outputter.info1f("Leaf Size Vertices: Min: %f %n", leafSizeVertices.min);
                Outputter.info1f("Leaf Size Vertices: P25: %f %n", leafSizeVertices.p25);
                Outputter.info1f("Leaf Size Vertices: P50: %f %n", leafSizeVertices.p50);
                Outputter.info1f("Leaf Size Vertices: P75: %f %n", leafSizeVertices.p75);
                Outputter.info1f("Leaf Size Vertices: Max: %f %n", leafSizeVertices.max);
                Outputter.info1f("Leaf Size Vertices: Mean: %f %n", leafSizeVertices.mean);
                Outputter.info1f("Leaf Size Vertices: StdDev: %f %n%n", leafSizeVertices.calculateStdDev());

                Outputter.info1ln("Face Summaries per depth:");
                for (Map.Entry<Integer, NumberSummary> entry : faceSummaries.entrySet())
                {
                    Outputter.info1f("%2d Faces | nodes: %4d | min: %8.0f | max: %8.0f | median: %11.2f | mean: %11.2f | total: %.0f %n",
                        entry.getKey(),
                        entry.getValue().count,
                        entry.getValue().min,
                        entry.getValue().max,
                        entry.getValue().p50,
                        entry.getValue().mean,
                        entry.getValue().total
                    );
                }
                for (Map.Entry<Integer, NumberSummary> entry : vertexSummaries.entrySet())
                {
                    Outputter.info1f("%2d Vertices | nodes: %4d | min: %8.0f | max: %8.0f | median: %11.2f | mean: %11.2f | total: %.0f %n",
                        entry.getKey(),
                        entry.getValue().count,
                        entry.getValue().min,
                        entry.getValue().max,
                        entry.getValue().p50,
                        entry.getValue().mean,
                        entry.getValue().total
                    );
                }

                for (Map.Entry<Integer, NumberSummary> entry : simpSummaries.entrySet())
                {
                    Outputter.info1f(" Simp %d | simplification ratios: | min: %5.5f | max: %5.5f | median: %5.5f | mean: %5.5f %n",
                        entry.getKey(),
                        entry.getValue().min,
                        entry.getValue().max,
                        entry.getValue().p50,
                        entry.getValue().mean
                    );
                }
            }
        }
    }

    public static void main(String[] args) throws IOException
    {
        CommandLine cmd = getCommandLine(args);
        run(new File(cmd.getOptionValue("input")), true);
    }

    private static void check(byte a, byte b)
    {
        if (a != b) throw new RuntimeException(String.format("%s != %s", a, b));
    }

    private static void check(long a, long b)
    {
        if (a != b) throw new RuntimeException(String.format("%s != %s", a, b));
    }

    private static void checkLt(long a, long b)
    {
        if (a >= b) throw new RuntimeException(String.format("%s >= %s", a, b));
    }

    private static void checkInRange(double a, double v, double b)
    {
        if (v > b || v < a) throw new RuntimeException(String.format("%s is out of range %s..%s", v, a, b));
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
            Outputter.errorf("%s : %s%n%n", e.getClass().getName(), e.getMessage());
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("--input <path>", options);
            System.exit(1);
            return null;
        }
    }

    private static class SomeNode
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
        public final int depth;
        public final List<SomeNode> children = new ArrayList<>();

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
            this.depth = o.getInt("depth");
        }
    }
}
