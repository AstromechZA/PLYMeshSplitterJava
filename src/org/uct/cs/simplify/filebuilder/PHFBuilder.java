package org.uct.cs.simplify.filebuilder;

import org.uct.cs.simplify.util.TempFileManager;
import org.uct.cs.simplify.util.Useful;

import java.io.*;
import java.nio.channels.FileChannel;

public class PHFBuilder
{
    public static File compile(PHFNode tree, File outputFile, CompilationMode mode) throws IOException
    {
        System.out.printf("Compressing Hierarchical tree into %s using mode: %s%n", outputFile, mode);
        File tempBlockFile = TempFileManager.provide(Useful.getFilenameWithoutExt(outputFile.getName()));

        if (mode == CompilationMode.PLY_MODEL)
        {
            try (FileChannel fcOUT = new FileOutputStream(tempBlockFile).getChannel())
            {
                long position = 0;
                for (PHFNode node : tree.collectAllNodes())
                {
                    System.out.printf("Writing %s to %s%n", node.getLinkedFile().getPath(), tempBlockFile.getPath());
                    try (FileChannel fcIN = new FileInputStream(node.getLinkedFile()).getChannel())
                    {
                        long length = fcIN.size();
                        fcOUT.transferFrom(fcIN, position, length);
                        node.setBlockOffset(position);
                        node.setBlockLength(length);
                        position += length;
                    }
                }
            }
        }
        else if (mode == CompilationMode.COMPRESSED_ARRAY)
        {
            try (BufferedOutputStream ostream = new BufferedOutputStream(new FileOutputStream(tempBlockFile)))
            {
                long position = 0;
                for (PHFNode node : tree.collectAllNodes())
                {
                    System.out.printf("Writing %s to %s%n", node.getLinkedFile().getPath(), tempBlockFile.getPath());

                    PLYDataCompressor.CompressionResult r = PLYDataCompressor.compress(node.getLinkedFile(), ostream);

                    long length = r.getLengthOfFaces() + r.getLengthOfVertices();
                    node.setBlockOffset(position);
                    node.setBlockLength(length);
                    position += length;
                }
            }
        }
        else
        {
            throw new RuntimeException("No compression mode provided!");
        }

        String jsonheader = PHFNode.buildJSONHierarchy(tree);
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

        return outputFile;
    }

    public enum CompilationMode
    {
        PLY_MODEL, COMPRESSED_ARRAY
    }
}
