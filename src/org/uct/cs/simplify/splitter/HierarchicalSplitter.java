package org.uct.cs.simplify.splitter;

import org.uct.cs.simplify.filebuilder.PackagedHierarchicalNode;
import org.uct.cs.simplify.splitter.memberships.IMembershipBuilder;
import org.uct.cs.simplify.splitter.splitrules.ISplitRule;

import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;

public class HierarchicalSplitter
{
    public static PackagedHierarchicalNode split(File inputFile, File outputDir, ISplitRule rule, IMembershipBuilder membershipBuilder) throws IOException
    {
        System.out.printf("Intput File: %s%n", inputFile.getAbsolutePath());
        System.out.printf("Output Directory: %s%n", outputDir.getAbsolutePath());

        ArrayDeque<PackagedHierarchicalNode> processQueue = new ArrayDeque<>();
        PackagedHierarchicalNode root = new PackagedHierarchicalNode(inputFile);
        processQueue.add(root);
        while (!processQueue.isEmpty())
        {
            PackagedHierarchicalNode currentNode = processQueue.removeFirst();

            ArrayList<PackagedHierarchicalNode> children = NodeSplitter.split(
                currentNode,
                membershipBuilder
            );

            for (PackagedHierarchicalNode child : children)
            {
                currentNode.addChild(child);
                if (rule.canSplit(child)) processQueue.add(child);
            }
        }
        return root;
    }
}