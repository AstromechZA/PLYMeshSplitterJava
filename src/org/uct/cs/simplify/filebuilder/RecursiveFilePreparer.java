package org.uct.cs.simplify.filebuilder;

import org.uct.cs.simplify.simplifier.SimplifierWrapper;
import org.uct.cs.simplify.splitter.NodeSplitter;
import org.uct.cs.simplify.splitter.memberships.VariableKDTreeMembershipBuilder;
import org.uct.cs.simplify.stitcher.NaiveMeshStitcher;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class RecursiveFilePreparer
{
    public static PackagedHierarchicalNode prepare(PackagedHierarchicalNode inputNode, int maxdepth)
    throws IOException, InterruptedException
    {
        return prepare(inputNode, 0, maxdepth);
    }

    public static PackagedHierarchicalNode prepare(PackagedHierarchicalNode inputNode, int depth, int maxdepth)
    throws IOException, InterruptedException
    {
        // stopping condition
        if (depth == maxdepth)
        {
            // simply copy the node and return
            return new PackagedHierarchicalNode(
                inputNode.getBoundingBox(),
                inputNode.getNumVertices(),
                inputNode.getNumFaces(),
                inputNode.getLinkedFile()
            );
        } else
        {
            // split current node into a list of subnodes
            ArrayList<PackagedHierarchicalNode> childNodes = NodeSplitter.split(
                inputNode,
                new VariableKDTreeMembershipBuilder()
            );

            // pre process child nodes
            int totalFaces = 0;
            ArrayList<PackagedHierarchicalNode> processedNodes = new ArrayList<>();
            for (PackagedHierarchicalNode childNode : childNodes)
            {
                totalFaces += childNode.getNumFaces();
                processedNodes.add(prepare(childNode, depth + 1, maxdepth));
            }

            ArrayList<File> processedFiles = new ArrayList<>();
            for (PackagedHierarchicalNode n : processedNodes)
            {
                processedFiles.add(n.getLinkedFile());
            }

            File stitchedModel = NaiveMeshStitcher.stitch(processedFiles);

            int targetFaces = Math.max(totalFaces / childNodes.size(), 100_000);

            File simplifiedModel = SimplifierWrapper.simplify(stitchedModel, targetFaces);

            PackagedHierarchicalNode outputNode = new PackagedHierarchicalNode(simplifiedModel);
            outputNode.setBoundingBox(inputNode.getBoundingBox());
            outputNode.addChildren(processedNodes);
            return outputNode;
        }
    }

}
