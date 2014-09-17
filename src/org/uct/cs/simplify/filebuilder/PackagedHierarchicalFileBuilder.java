package org.uct.cs.simplify.filebuilder;

import org.uct.cs.simplify.ply.header.PLYHeader;
import org.uct.cs.simplify.stitcher.NaiveMeshStitcher;
import org.uct.cs.simplify.util.TempFileManager;
import org.uct.cs.simplify.util.Useful;

import java.io.*;
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


    public static void prepare(PackagedHierarchicalNode node) throws IOException
    {
        if (node.hasChildren())
        {
            ArrayList<PackagedHierarchicalNode> children = node.getChildren();

            for (PackagedHierarchicalNode c : children)
            {
                if (c.hasChildren())
                {
                    prepare(c);

                    File tt = TempFileManager.provide("simp", ".ply");
                    System.out
                        .printf("Simplifying %s to %s%n", c.getLinkedFile().getAbsolutePath(), tt.getAbsolutePath());
                    Runtime r = Runtime.getRuntime();
                    Process proc = r.exec(
                        String.format(
                            "./simplifier/SimplFy %s %s 100000 -By -Ty",
                            c.getLinkedFile().getAbsolutePath(),
                            tt.getAbsolutePath()
                        )
                    );

                    BufferedReader stdOut = new BufferedReader(new InputStreamReader(proc.getInputStream()));
                    String s;
                    while ((s = stdOut.readLine()) != null)
                    {
                        System.out.println(s);
                    }
                    c.setLinkedFile(tt);
                    PLYHeader h = new PLYHeader(tt);
                    c.setNumFaces(h.getElement("face").getCount());
                    c.setNumVertices(h.getElement("vertex").getCount());
                }
            }

            File last = children.get(0).getLinkedFile();
            for (int i = 1; i < children.size(); i++)
            {
                File temp = TempFileManager.provide("phf", ".ply");

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
