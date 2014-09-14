package org.uct.cs.simplify.splitter;

import org.uct.cs.simplify.ScaleAndRecenter;
import org.uct.cs.simplify.file_builder.PackagedHierarchicalNode;
import org.uct.cs.simplify.splitter.memberships.VariableKDTreeMembershipBuilder;
import org.uct.cs.simplify.util.Useful;

import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;

public class HierarchicalSplitter
{
    private static final int MINIMUM_VERTEX_COUNT = 100_000;
    private static final int DEPTH_LIMIT = 6;
    private static final int RESCALE_TO_FIT = 1024;

    public static PackagedHierarchicalNode split(File inputFile, File outputDir, DepthControl depthControl, boolean swapYZ) throws IOException
    {
        System.out.printf("Intput File: %s%n", inputFile.getAbsolutePath());
        System.out.printf("Output Directory: %s%n", outputDir.getAbsolutePath());

        File scaledFile = new File(
            outputDir,
            String.format(
                "%s_rescaled_%d.ply",
                Useful.getFilenameWithoutExt(inputFile.getName()),
                RESCALE_TO_FIT
            )
        );
        ScaleAndRecenter.run(inputFile, scaledFile, RESCALE_TO_FIT, swapYZ);

        ArrayDeque<PackagedHierarchicalNode> processQueue = new ArrayDeque<>();
        ArrayDeque<Integer> depthQueue = new ArrayDeque<>();
        PackagedHierarchicalNode root = new PackagedHierarchicalNode(scaledFile);
        processQueue.add(root);
        depthQueue.add(0);
        while (!processQueue.isEmpty())
        {
            PackagedHierarchicalNode currentNode = processQueue.removeFirst();
            int depth = depthQueue.removeFirst();
            ArrayList<PackagedHierarchicalNode> children = NodeSplitter
                .split(currentNode, new VariableKDTreeMembershipBuilder(), outputDir);
            for (PackagedHierarchicalNode child : children)
            {
                currentNode.addChild(child);

                if (depthControl == DepthControl.VERTEX_COUNT)
                {
                    if (child.getNumVertices() > MINIMUM_VERTEX_COUNT)
                    {
                        processQueue.add(child);
                        depthQueue.add(depth + 1);
                    }
                }
                else if (depth < DEPTH_LIMIT)
                {
                    processQueue.add(child);
                    depthQueue.add(depth + 1);
                }
            }
        }
        return root;
    }

    public enum DepthControl
    {
        VERTEX_COUNT, TREE_DEPTH_LIMIT
    }
}
