package org.uct.cs.simplify.file_builder;

import org.uct.cs.simplify.util.TempFile;
import org.uct.cs.simplify.util.Useful;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayDeque;

public class FileBuilder
{
    public static File compile(PackagedHierarchicalNode tree, File outputFile) throws IOException
    {
        String tempF = Useful.getFilenameWithoutExt(outputFile.getName()) + ".temp";
        try (TempFile temp = new TempFile(outputFile.getParent(), tempF))
        {
            try (FileChannel fcOUT = new FileOutputStream(temp).getChannel())
            {
                ArrayDeque<PackagedHierarchicalNode> processQueue = new ArrayDeque<PackagedHierarchicalNode>();
                processQueue.add(tree);

                long position = 0;
                while (!processQueue.isEmpty())
                {
                    PackagedHierarchicalNode current = processQueue.removeFirst();

                    System.out.printf("Writing %s to %s%n", current.getLinkedFile().getPath(), temp.getPath());
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

            try (FileOutputStream fostream = new FileOutputStream(outputFile))
            {
                int l = jsonheader.length();
                fostream.write(new byte[]{
                    (byte) (l & 0x255),
                    (byte) ((l >> 8) & 0x255),
                    (byte) ((l >> (8 * 2)) & 0x255),
                    (byte) ((l >> (8 * 3)) & 0x255)
                });
                fostream.write(jsonheader.getBytes());

                try (
                    FileChannel fcOUT = fostream.getChannel();
                    FileChannel fcIN = new FileInputStream(temp).getChannel()
                )
                {
                    fcOUT.transferFrom(fcIN, fcOUT.position(), fcIN.size());
                }
            }
        }
        return outputFile;
    }
}
