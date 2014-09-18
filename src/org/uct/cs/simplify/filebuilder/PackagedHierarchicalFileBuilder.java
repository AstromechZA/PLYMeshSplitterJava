package org.uct.cs.simplify.filebuilder;

import org.uct.cs.simplify.util.TempFileManager;
import org.uct.cs.simplify.util.Useful;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayDeque;

public class PackagedHierarchicalFileBuilder
{
    public static File compile(PackagedHierarchicalNode tree, File outputFile) throws IOException
    {
        File tempBlockFile = TempFileManager.provide(Useful.getFilenameWithoutExt(outputFile.getName()));
        try (FileChannel fcOUT = new FileOutputStream(tempBlockFile).getChannel())
        {
            ArrayDeque<PackagedHierarchicalNode> processQueue = new ArrayDeque<>();
            processQueue.add(tree);

            long position = 0;
            while (!processQueue.isEmpty())
            {
                PackagedHierarchicalNode current = processQueue.removeFirst();

                System.out.printf("Writing %s to %s%n", current.getLinkedFile().getPath(), tempBlockFile.getPath());
                try (FileChannel fcIN = new FileInputStream(current.getLinkedFile()).getChannel())
                {
                    long length = fcIN.size();
                    fcOUT.transferFrom(fcIN, position, length);
                    current.setBlockOffset(position);
                    current.setBlockLength(length);
                    position += length;
                }

                for (PackagedHierarchicalNode node : current.getChildren())
                {
                    processQueue.add(node);
                }
            }
        }

        String jsonheader = PackagedHierarchicalNode.buildJSONHierarchy(tree);
        System.out.printf("%nWriting '%s' ..%n", outputFile.getPath());
        try (FileOutputStream fostream = new FileOutputStream(outputFile))
        {
            int l = jsonheader.length();
            System.out.println("Header length: " + l);
            Useful.littleEndianWrite(fostream, l);
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
}
