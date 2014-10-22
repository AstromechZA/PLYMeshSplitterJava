package org.uct.cs.simplify.filebuilder;

import org.uct.cs.simplify.util.*;

import java.io.*;
import java.nio.channels.FileChannel;
import java.util.List;
import java.util.Map;

public class PHFBuilder
{
    public static String compile(PHFNode tree, File outputFile, Map<String, String> jsonPairs, ProgressReporter progressReporter) throws IOException
    {
        try (StateHolder.StateWrap ignored = new StateHolder.StateWrap("compiling"))
        {
            Outputter.info3f("Compressing Hierarchical tree into %s%n", outputFile);
            File tempBlockFile = TempFileManager.provide(Useful.getFilenameWithoutExt(outputFile.getName()));

            List<PHFNode> nodes = tree.collectAllNodes();
            Outputter.info2f("Writing %d nodes to %s%n", nodes.size(), tempBlockFile.getPath());

            progressReporter.changeTask("Compiling", true);

            int max_depth = 0;
            try (BufferedOutputStream ostream = new BufferedOutputStream(new FileOutputStream(tempBlockFile)))
            {
                long position = 0;
                int nodeIndex = 0;
                int numNodes = nodes.size();
                for (PHFNode node : nodes)
                {
                    nodeIndex++;

                    Outputter.info1f("%d/%d Writing %s to %s%n", nodeIndex, nodes.size(), node.getLinkedFile().getPath(), tempBlockFile.getPath());

                    PLYDataCompressor.CompressionResult r = PLYDataCompressor.compress(node.getLinkedFile(), ostream);

                    long length = r.getLengthOfFaces() + r.getLengthOfVertices();
                    node.setBlockOffset(position);
                    node.setBlockLength(length);
                    position += length;
                    max_depth = Math.max(max_depth, node.getDepth());

                    progressReporter.report(nodeIndex / (float) numNodes);
                }
            }

            jsonPairs.put("vertex_colour", "true");
            jsonPairs.put("nodes", PHFNode.buildJSONHierarchy(tree));
            jsonPairs.put("max_depth", "" + max_depth);

            StringBuilder sb = new StringBuilder(1000);
            sb.append('{');
            boolean needsComma = false;
            for (Map.Entry<String, String> entry : jsonPairs.entrySet())
            {
                if (needsComma) sb.append(',');
                sb.append(String.format("\"%s\":%s", entry.getKey(), entry.getValue()));
                needsComma = true;
            }
            sb.append('}');
            String jsonheader = sb.toString();

            Outputter.info1f("%nWriting '%s' ..%n", outputFile.getPath());
            progressReporter.changeTask("Writing final file", true);
            try (FileOutputStream fostream = new FileOutputStream(outputFile))
            {
                int l = jsonheader.length();
                Outputter.debugf("Header length: " + l);
                Useful.writeIntLE(fostream, l);
                fostream.write(jsonheader.getBytes());

                Outputter.debugf("Writing header (%s)%n", Useful.formatBytes(l));

                try (
                    FileChannel fcOUT = fostream.getChannel();
                    FileChannel fcIN = new FileInputStream(tempBlockFile).getChannel()
                )
                {
                    Outputter.debugf("Writing data (%s)%n", Useful.formatBytes(fcIN.size()));

                    long length = fcIN.size();
                    long position = fcOUT.position();

                    long partLength = length / 100;

                    for (int i = 0; i < 99; i++)
                    {
                        fcOUT.transferFrom(fcIN, position, partLength);
                        position += partLength;
                        progressReporter.report(i / 100.0f);
                    }

                    fcOUT.transferFrom(fcIN, position, partLength + length % partLength);
                    progressReporter.report(1);
                }
            }

            TempFileManager.release(tempBlockFile);

            return jsonheader;
        }
    }
}
