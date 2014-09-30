package org.uct.cs.simplify.filebuilder;

import org.uct.cs.simplify.util.TempFileManager;
import org.uct.cs.simplify.util.Useful;

import java.io.*;
import java.nio.channels.FileChannel;
import java.util.List;

public class PHFBuilder
{
    public static String compile(PHFNode tree, File outputFile) throws IOException
    {
        System.out.printf("Compressing Hierarchical tree into %s%n", outputFile);
        File tempBlockFile = TempFileManager.provide(Useful.getFilenameWithoutExt(outputFile.getName()));

        List<PHFNode> nodes = tree.collectAllNodes();
        System.out.printf("Writing %d nodes to %s%n", nodes.size(), tempBlockFile.getPath());

        int max_depth = 0;
        try (BufferedOutputStream ostream = new BufferedOutputStream(new FileOutputStream(tempBlockFile)))
        {
            long position = 0;
            for (PHFNode node : nodes)
            {
                System.out.printf("Writing %s to %s%n", node.getLinkedFile().getPath(), tempBlockFile.getPath());

                PLYDataCompressor.CompressionResult r = PLYDataCompressor.compress(node.getLinkedFile(), ostream);

                long length = r.getLengthOfFaces() + r.getLengthOfVertices();
                node.setBlockOffset(position);
                node.setBlockLength(length);
                position += length;
                max_depth = Math.max(max_depth, node.getDepth());
            }
        }

        String jsonheader = "{" +
            String.format("\"vertex_colour\":%b,", true) +
            String.format("\"nodes\":%s,", PHFNode.buildJSONHierarchy(tree)) +
            String.format("\"max_depth\":%d", max_depth) +
            "}";

        System.out.printf("%nWriting '%s' ..%n", outputFile.getPath());
        try (FileOutputStream fostream = new FileOutputStream(outputFile))
        {
            int l = jsonheader.length();
            System.out.println("Header length: " + l);
            Useful.writeIntLE(fostream, l);
            fostream.write(jsonheader.getBytes());

            System.out.printf("Writing header (%s)%n", Useful.formatBytes(l));

            try (
                FileChannel fcOUT = fostream.getChannel();
                FileChannel fcIN = new FileInputStream(tempBlockFile).getChannel()
            )
            {
                System.out.printf("Writing data (%s)%n", Useful.formatBytes(fcIN.size()));
                fcOUT.transferFrom(fcIN, fcOUT.position(), fcIN.size());
            }
        }

        TempFileManager.release(tempBlockFile);

        return jsonheader;
    }
}
