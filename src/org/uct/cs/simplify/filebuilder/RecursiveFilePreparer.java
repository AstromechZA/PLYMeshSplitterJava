package org.uct.cs.simplify.filebuilder;

import org.uct.cs.simplify.ply.header.PLYHeader;
import org.uct.cs.simplify.simplifier.SimplifierWrapper;
import org.uct.cs.simplify.splitter.NodeSplitter;
import org.uct.cs.simplify.splitter.memberships.IMembershipBuilder;
import org.uct.cs.simplify.splitter.stopcondition.IStoppingCondition;
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
    public static PHFNode prepare(
        PHFNode inputNode,
        IStoppingCondition stopCondition,
        IMembershipBuilder splitType,
        float finalRatio,
        IProgressReporter progressReporter
    )
    throws IOException, InterruptedException
    {
        return prepare(inputNode, 0, stopCondition, splitType, finalRatio, 0, 1, progressReporter);
    }

    public static PHFNode prepare(
        PHFNode inputNode,
        int depth,
        IStoppingCondition stopCondition,
        IMembershipBuilder splitType,
        float finalRatio,
        float startProgress,
        float endProgress,
        IProgressReporter progressReporter
    )
    throws IOException, InterruptedException
    {
        // stopping condition
        if (stopCondition.met(depth, inputNode.getNumFaces()))
        {
            // simply copy the node and return
            PHFNode outputNode = new PHFNode(inputNode.getLinkedFile());
            outputNode.setDepth(depth);
            progressReporter.report(endProgress);
            return outputNode;
        }
        else
        {
            // split current node into a list of subnodes
            ArrayList<PHFNode> childNodes = NodeSplitter.split(inputNode, splitType);

            TempFileManager.release(inputNode.getLinkedFile());

            // pre process child nodes
            List<PHFNode> processedNodes = new ArrayList<>(childNodes.size());
            float diffProgress = (endProgress - startProgress) / childNodes.size();
            float childProgress = startProgress;
            for (PHFNode childNode : childNodes)
            {
                processedNodes.add(prepare(
                    childNode,
                    depth + 1,
                    stopCondition,
                    splitType,
                    finalRatio,
                    childProgress,
                    childProgress + diffProgress,
                    progressReporter
                ));
                childProgress += diffProgress;
            }

            // collect list of files for stitching
            List<File> processedFiles = new ArrayList<>(processedNodes.size());
            int maxDepth = -1;
            for (PHFNode n : processedNodes)
            {
                maxDepth = Math.max(n.getDepthOfDeepestChild(), maxDepth);
                processedFiles.add(n.getLinkedFile());
            }
            File stitchedModel = NaiveMeshStitcher.stitch(processedFiles);

            // load details of stitched model
            PLYHeader stitchedHeader = new PLYHeader(stitchedModel);

            // calculate target number of faces based on target simplification ratio
            long totalFaces = stitchedHeader.getElement("face").getCount();
            // nth root of finalRatio (equal spread of simplification throughout tree)
            float localSimplificationRatio = (float) Math.pow(finalRatio, 1.0f / maxDepth);
            long targetFaces = (long) (totalFaces * localSimplificationRatio);

            // simplify file
            File simplifiedFile = SimplifierWrapper.simplify(stitchedModel, targetFaces, inputNode.getBoundingBox());

            // delete stitched model
            TempFileManager.release(stitchedModel);

            // build output file
            PHFNode outputNode = new PHFNode(simplifiedFile);
            outputNode.addChildren(processedNodes);
            outputNode.setDepth(depth);

            // print & report & return
            Outputter.info1f("Simplified from %d to %d faces. (depth: %d) (ratio: %f)%n", totalFaces, outputNode.getNumFaces(), depth, outputNode.getNumFaces() / (float) totalFaces);
            progressReporter.report(endProgress);
            return outputNode;
        }
    }

}
