package org.uct.cs.simplify;

import org.uct.cs.simplify.file_builder.PackagedHierarchicalNode;
import org.uct.cs.simplify.splitter.HierarchicalSplitter;
import org.uct.cs.simplify.util.Useful;

import java.io.File;
import java.io.IOException;

public class FileBuilder
{
    public static void main(String[] args) throws IOException
    {
        int rescaleTo = 1024;
        File inputFile = new File("temp/builder/GedePalace.ply");
        File outputDir = new File("temp/builder/");
        File outputFile = new File(inputFile.getParent(), Useful.getFilenameWithoutExt(inputFile.getName()) + ".phf");

        File scaledFile = new File(
            outputDir,
            String.format(
                "%s_rescaled_%d.ply",
                Useful.getFilenameWithoutExt(inputFile.getName()),
                rescaleTo
            )
        );
        ScaleAndRecenter.run(inputFile, scaledFile, rescaleTo, true);

        PackagedHierarchicalNode tree = HierarchicalSplitter.split(
            scaledFile,
            outputDir,
            HierarchicalSplitter.DepthControl.TREE_DEPTH_LIMIT
        );

        org.uct.cs.simplify.file_builder.FileBuilder.compile(tree, outputFile);
    }
}
