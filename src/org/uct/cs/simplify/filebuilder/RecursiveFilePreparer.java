package org.uct.cs.simplify.filebuilder;

import org.uct.cs.simplify.ply.header.PLYHeader;
import org.uct.cs.simplify.simplifier.SimplifierWrapper;
import org.uct.cs.simplify.splitter.NodeSplitter;
import org.uct.cs.simplify.splitter.memberships.VariableKDTreeMembershipBuilder;
import org.uct.cs.simplify.stitcher.NaiveMeshStitcher;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class RecursiveFilePreparer
{
    private static final int DEFAULT_MIN_FACES = 100_000;

    public static PackagedHierarchicalNode buildNodes(PackagedHierarchicalNode inputNode, int maxdepth)
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
            List<PackagedHierarchicalNode> processedNodes = new ArrayList<>(childNodes.size());
            for (PackagedHierarchicalNode childNode : childNodes)
            {
                processedNodes.add(prepare(childNode, depth + 1, maxdepth));
            }

            List<File> processedFiles = new ArrayList<>(processedNodes.size());
            for (PackagedHierarchicalNode n : processedNodes)
            {
                processedFiles.add(n.getLinkedFile());
            }

            File stitchedModel = NaiveMeshStitcher.stitch(processedFiles);

            PLYHeader stitchedHeader = new PLYHeader(stitchedModel);
            long totalFaces = stitchedHeader.getElement("face").getCount();
            long targetFaces = Math.max(totalFaces / childNodes.size(), DEFAULT_MIN_FACES);

            File simplifiedModel = SimplifierWrapper.simplify(stitchedModel, targetFaces, false);

            PackagedHierarchicalNode outputNode = new PackagedHierarchicalNode(simplifiedModel);
            outputNode.setBoundingBox(inputNode.getBoundingBox());
            outputNode.addChildren(processedNodes);

            System.out.printf("Simplified from %d to %d faces.%n", totalFaces, outputNode.getNumFaces());

            return outputNode;
        }
    }

}
