package org.uct.cs.simplify.splitter;

import org.uct.cs.simplify.filebuilder.PHFNode;
import org.uct.cs.simplify.splitter.memberships.IMembershipBuilder;
import org.uct.cs.simplify.splitter.splitrules.ISplitRule;

import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;

public class HierarchicalSplitter
{
    @SuppressWarnings("CollectionWithoutInitialCapacity")
    public static PHFNode split(File inputFile, File outputDir, ISplitRule rule, IMembershipBuilder membershipBuilder) throws IOException
    {
        System.out.printf("Intput File: %s%n", inputFile.getAbsolutePath());
        System.out.printf("Output Directory: %s%n", outputDir.getAbsolutePath());

        ArrayDeque<PHFNode> processQueue = new ArrayDeque<>();
        PHFNode root = new PHFNode(inputFile);
        processQueue.add(root);
        while (!processQueue.isEmpty())
        {
            PHFNode currentNode = processQueue.removeFirst();

            ArrayList<PHFNode> children = NodeSplitter.split(
                currentNode,
                membershipBuilder
            );

            for (PHFNode child : children)
            {
                currentNode.addChild(child);
                if (rule.canSplit(child)) processQueue.add(child);
            }
        }
        return root;
    }
}
