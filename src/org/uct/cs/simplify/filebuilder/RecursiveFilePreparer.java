package org.uct.cs.simplify.filebuilder;

import org.uct.cs.simplify.ply.header.PLYHeader;
import org.uct.cs.simplify.simplifier.SimplificationFactory;
import org.uct.cs.simplify.simplifier.SimplifierWrapper;
import org.uct.cs.simplify.splitter.NodeSplitter;
import org.uct.cs.simplify.splitter.memberships.IMembershipBuilder;
import org.uct.cs.simplify.splitter.stopcondition.IStoppingCondition;
import org.uct.cs.simplify.stitcher.NaiveMeshStitcher;
import org.uct.cs.simplify.util.Outputter;
import org.uct.cs.simplify.util.ProgressReporter;
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
        SimplificationFactory simplificationFactory,
        ProgressReporter progressReporter
    )
        throws IOException, InterruptedException
    {
        return prepare(inputNode, 0, stopCondition, splitType, simplificationFactory, new ProgressSegment(), progressReporter);
    }

    public static PHFNode prepare(
        PHFNode inputNode,
        int depth,
        IStoppingCondition stopCondition,
        IMembershipBuilder splitType,
        SimplificationFactory simplificationFactory,
        ProgressSegment progress,
        ProgressReporter progressReporter
    )
        throws IOException, InterruptedException
    {
        // stopping condition
        if (stopCondition.met(depth, inputNode.getNumFaces()))
        {
            // simply copy the node and return
            PHFNode outputNode = new PHFNode(inputNode.getLinkedFile());
            outputNode.setDepth(depth);
            progressReporter.report(progress.end);
            return outputNode;
        }
        else
        {
            // split current node into a list of subnodes
            ArrayList<PHFNode> childNodes = NodeSplitter.split(inputNode, splitType);

            TempFileManager.release(inputNode.getLinkedFile());

            // pre process child nodes
            List<PHFNode> processedNodes = new ArrayList<>(childNodes.size());
            List<ProgressSegment> progressSegments = progress.split(childNodes.size());
            List<File> processedFiles = new ArrayList<>(processedNodes.size());
            int maxDepth = -1;

            for (int i = 0; i < childNodes.size(); i++)
            {
                PHFNode n = prepare(
                    childNodes.get(i),
                    depth + 1,
                    stopCondition,
                    splitType,
                    simplificationFactory,
                    progressSegments.get(i),
                    progressReporter
                );

                maxDepth = Math.max(n.getDepthOfDeepestChild(), maxDepth);
                processedNodes.add(n);
                processedFiles.add(n.getLinkedFile());
            }

            Outputter.debugf("Stitching %d files%n", processedFiles.size());
            File stitchedModel = NaiveMeshStitcher.stitch(processedFiles);

            // load details of stitched model
            PLYHeader stitchedHeader = new PLYHeader(stitchedModel);

            // calculate target number of faces based on target simplification ratio
            long totalFaces = stitchedHeader.getElement("face").getCount();
            float ratio = simplificationFactory.getSimplificationRatioForDepth(depth, maxDepth, totalFaces);
            long targetFaces = (long) (totalFaces * ratio);

            // simplify file
            Outputter.info3f("Simplify from %d to %d faces.%n", totalFaces, targetFaces);
            File simplifiedFile = SimplifierWrapper.simplify(stitchedModel, targetFaces);

            // delete stitched model
            if (processedFiles.size() > 1) TempFileManager.release(stitchedModel);

            // build output file
            PHFNode outputNode = new PHFNode(simplifiedFile);
            outputNode.addChildren(processedNodes);
            outputNode.setDepth(depth);

            // print & report & return
            Outputter.info1f("Simplified from %d to %d faces. (depth: %d) (ratio: %f)%n", totalFaces, outputNode.getNumFaces(), depth, outputNode.getNumFaces() / (float) totalFaces);
            progressReporter.report(progress.end);
            return outputNode;
        }
    }

    private static class ProgressSegment
    {
        private double start, end;

        public ProgressSegment()
        {
            this(0, 1);
        }

        public ProgressSegment(double start, double end)
        {
            this.start = start;
            this.end = end;
        }


        public List<ProgressSegment> split(int nths)
        {
            List<ProgressSegment> o = new ArrayList<>(nths);
            double diff = (end - start) / nths;
            for (int i = 0; i < nths; i++)
            {
                o.add(new ProgressSegment(start + i * diff, start + diff + i * diff));
            }
            return o;
        }
    }

}
