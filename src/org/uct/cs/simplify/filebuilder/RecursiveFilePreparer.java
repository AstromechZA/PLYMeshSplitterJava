package org.uct.cs.simplify.filebuilder;

import org.uct.cs.simplify.ply.header.PLYHeader;
import org.uct.cs.simplify.simplifier.SimplifierWrapper;
import org.uct.cs.simplify.splitter.NodeSplitter;
import org.uct.cs.simplify.splitter.memberships.IMembershipBuilder;
import org.uct.cs.simplify.splitter.memberships.VariableKDTreeMembershipBuilder;
import org.uct.cs.simplify.stitcher.NaiveMeshStitcher;
import org.uct.cs.simplify.util.IProgressReporter;
import org.uct.cs.simplify.util.Outputter;
import org.uct.cs.simplify.util.TempFileManager;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class RecursiveFilePreparer
{
    public static PHFNode prepare(PHFNode inputNode, int maxdepth, IProgressReporter progressReporter)
    throws IOException, InterruptedException
    {
        return prepare(inputNode, 0, maxdepth, 0, 1, progressReporter);
    }

    public static PHFNode prepare(
        PHFNode inputNode,
        int depth,
        int maxdepth,
        float start_progress,
        float end_progress,
        IProgressReporter progressReporter
    )
    throws IOException, InterruptedException
    {
        // stopping condition
        if (depth == maxdepth)
        {
            // simply copy the node and return
            PHFNode outputNode = new PHFNode(inputNode.getLinkedFile());
            outputNode.setDepth(depth);
            progressReporter.report(end_progress);
            return outputNode;
        }
        else
        {
            IMembershipBuilder splitType = new VariableKDTreeMembershipBuilder();

            // split current node into a list of subnodes
            ArrayList<PHFNode> childNodes = NodeSplitter.split(inputNode, splitType);

            // pre process child nodes
            List<PHFNode> processedNodes = new ArrayList<>(childNodes.size());
            float fromEnd = 1 / (float) (Math.pow(splitType.getSplitRatio(), maxdepth + 1) - 1);
            float diffProgress = (end_progress - fromEnd - start_progress) / childNodes.size();
            float childProgress = start_progress;
            for (PHFNode childNode : childNodes)
            {
                processedNodes.add(prepare(
                    childNode,
                    depth + 1,
                    maxdepth,
                    childProgress,
                    childProgress + diffProgress,
                    progressReporter
                ));
                childProgress += diffProgress;
            }

            List<File> processedFiles = new ArrayList<>(processedNodes.size());
            for (PHFNode n : processedNodes) processedFiles.add(n.getLinkedFile());

            TempFileManager.release(inputNode.getLinkedFile());

            File stitchedModel = NaiveMeshStitcher.stitch(processedFiles);

            PLYHeader stitchedHeader = new PLYHeader(stitchedModel);
            long totalFaces = stitchedHeader.getElement("face").getCount();
            long targetFaces = totalFaces / splitType.getSplitRatio();

            File simplifiedFile = SimplifierWrapper.simplify(stitchedModel, targetFaces, inputNode.getBoundingBox());

            TempFileManager.release(stitchedModel);

            PHFNode outputNode = new PHFNode(simplifiedFile);
            outputNode.addChildren(processedNodes);
            outputNode.setDepth(depth);

            Outputter.info1f("Simplified from %d to %d faces.%n", totalFaces, outputNode.getNumFaces());
            progressReporter.report(end_progress);
            return outputNode;
        }
    }

}
