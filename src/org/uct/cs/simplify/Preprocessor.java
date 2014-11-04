package org.uct.cs.simplify;

import org.apache.commons.cli.*;
import org.uct.cs.simplify.filebuilder.PHFBuilder;
import org.uct.cs.simplify.filebuilder.PHFNode;
import org.uct.cs.simplify.filebuilder.RecursiveFilePreparer;
import org.uct.cs.simplify.ply.header.PLYHeader;
import org.uct.cs.simplify.simplifier.SimplificationFactory;
import org.uct.cs.simplify.splitter.memberships.MembershipBuilder;
import org.uct.cs.simplify.splitter.memberships.MultiwayVariableKDTreeMembershipBuilder;
import org.uct.cs.simplify.splitter.stopcondition.DepthStoppingCondition;
import org.uct.cs.simplify.splitter.stopcondition.IStoppingCondition;
import org.uct.cs.simplify.splitter.stopcondition.LowerFaceBoundStoppingCondition;
import org.uct.cs.simplify.treedrawer.TreeDrawer;
import org.uct.cs.simplify.util.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class Preprocessor
{
    public static String process(
        File inputFile,
        File outputFile,
        boolean keepNodes,
        boolean swapYZ,
        boolean treeImage,
        MembershipBuilder membershipBuilder,
        ProgressReporter progressReporter
    )
        throws IOException, InterruptedException
    {
        try (Timer sr = new Timer("Preprocessing"))
        {
            long numFaces = new PLYHeader(inputFile).getElement("face").getCount();

            if (numFaces < Constants.FACES_IN_ROOT)
            {
                throw new RuntimeException("The input model is too small and does not need to be processed! Please supply a model containing at least " + Constants.FACES_IN_ROOT + " faces.");
            }

            SimplificationFactory simplificationFactory = new SimplificationFactory(numFaces, Constants.FACES_IN_ROOT);

            if (membershipBuilder == null)
            {
                int splitRatio = determineBestSplitRatio(numFaces, 3, 5);
                membershipBuilder = new MultiwayVariableKDTreeMembershipBuilder(splitRatio);
            }
            Outputter.info1f("Using membership builder: %s with ratio %d%n", membershipBuilder.getClass().getName(), membershipBuilder.getSplitRatio());

            IStoppingCondition stopCondition;
            if (membershipBuilder.isBalanced())
            {
                float numLeaves = (numFaces / (float) Constants.MAX_FACES_PER_LEAF);
                int numLevels = (int) Math.ceil(Math.log(numLeaves) / Math.log(membershipBuilder.getSplitRatio()));

                Outputter.info1f("Calculated Tree Depth: %d (%d splits)%n", numLevels + 1, numLevels);
                for (int i = 0; i < numLevels; i++)
                {
                    Outputter.info2f("Ratio for lvl %d: %f%n", i, simplificationFactory.getSimplificationRatioForDepth(i, numLevels));
                }
                stopCondition = new DepthStoppingCondition(numLevels);
            }
            else
            {
                Outputter.info1ln("Calculated Tree Depth: N/A (unbalanced tree)");
                stopCondition = new LowerFaceBoundStoppingCondition(Constants.MAX_FACES_PER_LEAF);
            }

            // use the directory of the outputfile as the default output directory
            File outputDir = outputFile.getParentFile();

            // generate tempfiles in the outputdir
            TempFileManager.setWorkingDirectory(outputDir.toPath());

            // create scaled and recentered version of input
            File scaledFile = TempFileManager.provide("rescaled", ".ply");
            double scaleRatio = ScaleAndRecenter.rescale(inputFile, scaledFile, Constants.PHF_RESCALE_SIZE, swapYZ);

            // build tree
            PHFNode tree = RecursiveFilePreparer.prepare(new PHFNode(scaledFile), stopCondition, membershipBuilder, simplificationFactory, progressReporter);

            if (treeImage)
            {
                File o = new File(outputDir,
                    Useful.getFilenameWithoutExt(outputFile.getName()) + "_tree.png"
                );
                Outputter.info1f("Saving tree image to %s%n", o);
                BufferedImage bi = TreeDrawer.Draw(tree, 2048, 1024);
                ImageIO.write(bi, "png", o);
            }

            // additional json keys
            Map<String, String> additionalJSON = new HashMap<>();
            additionalJSON.put("scale_ratio", "" + scaleRatio);

            // compile into output file
            String jsonHeader = PHFBuilder.compile(tree, outputFile, additionalJSON, progressReporter);
            Outputter.info3f("Processing complete. Final file: %s%n", outputFile);

            Outputter.info1f("Input File Size: %d (%s)%n", inputFile.length(), Useful.formatBytes(inputFile.length()));
            Outputter.info1f("Output File Size: %d (%s)%n", outputFile.length(), Useful.formatBytes(outputFile.length()));

            if (!keepNodes)
            {
                TempFileManager.clear();
            }

            return jsonHeader;
        }
    }

    private static int determineBestSplitRatio(long numFaces, int lower, int upper)
    {
        float numLeaves = (numFaces / (float) Constants.MAX_FACES_PER_LEAF);
        long bestdiff = Integer.MAX_VALUE;
        int best = lower;
        for (int i = lower; i <= upper; i++)
        {
            int numLevels = (int) Math.round(Math.log(numLeaves) / Math.log(i));
            int actualNumLeaves = (int) Math.pow(i, numLevels);
            long actualFacesPerLeaf = (long) (numFaces / (float) actualNumLeaves);
            long diff = Math.abs(Constants.MAX_FACES_PER_LEAF - actualFacesPerLeaf);
            if (diff < bestdiff)
            {
                best = i;
                bestdiff = diff;
            }
        }
        return best;
    }


    public static void main(String[] args) throws IOException, InterruptedException
    {
        CommandLine cmd = getCommandLine(args);
        File inputFile = new File(cmd.getOptionValue("input"));
        File outputFile = new File(cmd.getOptionValue("output"));
        File outputDir = outputFile.getParentFile();

        if (cmd.hasOption("debug")) Outputter.setCurrentLevel(Outputter.DEBUG);

        MembershipBuilder mb = cmd.hasOption("hierarchy") ? MembershipBuilder.get(cmd.getOptionValue("hierarchy")) : null;

        if (outputFile.isDirectory())
        {
            throw new RuntimeException("The output file you specified is a directory! Please specify a file with the extension '.phf'");
        }
        if (!outputFile.getName().endsWith(".phf"))
        {
            throw new RuntimeException("Please specify a file with the extension '.phf'");
        }

        String jsonHeader = process(
            inputFile,
            outputFile,
            cmd.hasOption("keeptemp") || cmd.hasOption("dumpmem"),
            cmd.hasOption("swapyz"),
            cmd.hasOption("treeimage"),
            mb,
            new StdOutProgressReporter("Preprocessing")
        );

        if (cmd.hasOption("dumpjson"))
        {
            File headerFile = new File(outputDir, Useful.getFilenameWithoutExt(outputFile.getName()) + ".json");
            try (FileWriter fw = new FileWriter(headerFile))
            {
                fw.write(jsonHeader);
            }
        }

        try
        {
            OutputValidator.run(outputFile, cmd.hasOption("debug"));
        }
        catch (RuntimeException e)
        {
            System.err.println("Validation Failed!");
            e.printStackTrace();
        }
    }

    private static CommandLine getCommandLine(String[] args)
    {
        CommandLineParser clp = new BasicParser();

        Options options = new Options();

        Option inputFile = new Option("i", "input", true, "path to first PLY file");
        inputFile.setRequired(true);
        options.addOption(inputFile);

        Option outputFile = new Option("o", "output", true, "path to output directory");
        outputFile.setRequired(true);
        options.addOption(outputFile);

        Option keepTempFiles = new Option(
            "k", "keeptemp", false, "keep any temporary files generated during phf compilation"
        );
        options.addOption(keepTempFiles);

        Option swapYZ = new Option("s", "swapyz", false, "Rotate model 90 around X. (convert coordinate frame)");
        options.addOption(swapYZ);

        Option dumpJSON = new Option("j", "dumpjson", false, "Dump the JSON header into a separate file");
        options.addOption(dumpJSON);

        Option debug = new Option("d", "debug", false, "Debug output");
        options.addOption(debug);

        Option treeimage = new Option("t", "treeimage", false, "Dump image of the tree into a separate file");
        options.addOption(treeimage);

        Option hierarchy = new Option("h", "hierarchy", true, "Pick the hierarchical level of detail structure to be used." +
            " Choose one of: [octree, kdtree, vkdtree, mkdtreeN] where N is a number from 2 -> 8. Default: 'mkdtree4'.");
        options.addOption(hierarchy);

        CommandLine cmd;
        try
        {
            cmd = clp.parse(options, args);
            return cmd;
        }
        catch (ParseException e)
        {
            Outputter.errorf("%s : %s%n%n", e.getClass().getName(), e.getMessage());
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("--input <path> --output <path>", options);
            System.exit(1);
            return null;
        }
    }


}
