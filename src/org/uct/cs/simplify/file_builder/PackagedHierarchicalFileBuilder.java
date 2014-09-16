package org.uct.cs.simplify.file_builder;

import org.uct.cs.simplify.ply.header.PLYHeader;
import org.uct.cs.simplify.stitcher.NaiveMeshStitcher;
import org.uct.cs.simplify.util.TempFile;
import org.uct.cs.simplify.util.TempFileManager;
import org.uct.cs.simplify.util.Useful;
import org.uct.cs.simplify.util.XBoundingBox;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayDeque;
import java.util.ArrayList;

public class PackagedHierarchicalFileBuilder
{
    public static File compile(PackagedHierarchicalNode tree, File outputFile) throws IOException
    {
        prepare(tree);

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
            fostream.write(new byte[]{
                (byte) (l & 0x255),
                (byte) ((l >> 8) & 0x255),
                (byte) ((l >> (8 * 2)) & 0x255),
                (byte) ((l >> (8 * 3)) & 0x255)
            });
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

        ArrayDeque<PackagedHierarchicalNode> processQueue = new ArrayDeque<>();
        processQueue.add(tree);

        // force gc and wait, so we can delete locked files
        // (this is a remnant of mapped Byte buffers
        try
        {
            System.gc();
            Thread.sleep(1000);
        }
        catch (InterruptedException e)
        { /* nothing */ }

        while (!processQueue.isEmpty())
        {
            PackagedHierarchicalNode current = processQueue.removeFirst();
            if (current.getLinkedFile().exists())
            {
                System.out.printf("Removing %s .. ", current.getLinkedFile().getPath());
                System.out.printf("%s%n", current.getLinkedFile().delete());
            }
            for (PackagedHierarchicalNode node : current.getChildren())
            {
                processQueue.add(node);
            }
        }

        return outputFile;
    }


    public static void prepare(PackagedHierarchicalNode node) throws IOException
    {
        if (node.hasChildren())
        {
            ArrayList<PackagedHierarchicalNode> children = node.getChildren();

            for (PackagedHierarchicalNode c : children)
            {
                prepare(c);

                // TODO simplify child
            }

            if (node.getLinkedFile().exists()) node.getLinkedFile().delete();

            File last = children.get(0).getLinkedFile();
            for (int i = 1; i < children.size(); i++)
            {
                File temp = File.createTempFile("phf", ".tmp");
                temp.deleteOnExit();
                File current = children.get(1).getLinkedFile();
                System.out.printf("Stitching %s and %s into %s%n", last, current, temp);
                NaiveMeshStitcher.stitch(last, current, temp);

                last = temp;
            }

            node.setLinkedFile(last);
            PLYHeader h = new PLYHeader(last);
            node.setNumFaces(h.getElement("face").getCount());
            node.setNumVertices(h.getElement("vertex").getCount());
        }
    }


}
