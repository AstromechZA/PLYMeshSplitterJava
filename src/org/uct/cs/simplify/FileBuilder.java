package org.uct.cs.simplify;

import org.apache.commons.cli.*;
import org.uct.cs.simplify.filebuilder.PHFBuilder;
import org.uct.cs.simplify.filebuilder.PHFNode;
import org.uct.cs.simplify.filebuilder.RecursiveFilePreparer;
import org.uct.cs.simplify.ply.header.PLYHeader;
import org.uct.cs.simplify.simplifier.SimplificationFactory;
import org.uct.cs.simplify.splitter.memberships.IMembershipBuilder;
import org.uct.cs.simplify.splitter.stopcondition.DepthStoppingCondition;
import org.uct.cs.simplify.splitter.stopcondition.IStoppingCondition;
import org.uct.cs.simplify.splitter.stopcondition.LowerFaceBoundStoppingCondition;
import org.uct.cs.simplify.util.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class FileBuilder
{
    public static String run(
        File inputFile,
        File outputFile,
        boolean keepNodes,
        boolean swapYZ,
        IMembershipBuilder membershipBuilder,
        ProgressReporter reporter
    )
        throws IOException, InterruptedException
    {
        IStoppingCondition stopCondition;

        Outputter.info1f("Using membership builder: %s%n", membershipBuilder.getClass().getName());

        long numFaces = new PLYHeader(inputFile).getElement("face").getCount();
        SimplificationFactory simplifier = new SimplificationFactory(numFaces, Constants.FACES_IN_ROOT, membershipBuilder.getSplitRatio());

        if (membershipBuilder.isBalanced())
        {
            float numLeaves = (numFaces / (float) Constants.MAX_FACES_PER_LEAF);
            int numLevels = (int) Math.ceil(Math.log(numLeaves) / Math.log(membershipBuilder.getSplitRatio()));

            Outputter.info1f("Calculated Tree Depth: %d (%d splits)%n", numLevels + 1, numLevels);

            for (int i = 0; i < numLevels; i++)
            {
                Outputter.info2f("Ratio for lvl %d: %f%n", i, simplifier.getSimplificationRatioForDepth(i, numLevels));
            }
            stopCondition = new DepthStoppingCondition(numLevels);
        }
        else
        {
            Outputter.info1ln("Calculated Tree Depth: N/A (unbalanced tree)");

            stopCondition = new LowerFaceBoundStoppingCondition(Constants.MAX_FACES_PER_LEAF);
        }


        return run(inputFile, outputFile, keepNodes, swapYZ, membershipBuilder, simplifier, stopCondition, reporter);
    }

    public static String run(
        File inputFile,
        File outputFile,
        boolean keepNodes,
        boolean swapYZ,
        IMembershipBuilder membershipBuilder,
        SimplificationFactory simplificationFactory,
        IStoppingCondition stopCondition,
        ProgressReporter progressReporter
    )
        throws IOException, InterruptedException
    {
        StatRecorder sr = new StatRecorder();
        // use the directory of the outputfile as the default output directory
        File outputDir = outputFile.getParentFile();

        // generate tempfiles in the outputdir
        TempFileManager.setWorkingDirectory(outputDir.toPath());

        // create scaled and recentered version of input
        File scaledFile = TempFileManager.provide("rescaled", ".ply");
        double scaleRatio = ScaleAndRecenter.run(inputFile, scaledFile, Constants.PHF_RESCALE_SIZE, swapYZ);

        // build tree
        PHFNode tree = RecursiveFilePreparer.prepare(new PHFNode(scaledFile), stopCondition, membershipBuilder, simplificationFactory, progressReporter);

        // additional json keys
        Map<String, String> additionalJSON = new HashMap<>();
        additionalJSON.put("scale_ratio", "" + scaleRatio);

        // compile into output file
        String jsonHeader = PHFBuilder.compile(tree, outputFile, additionalJSON, progressReporter);
        Outputter.info3f("Processing complete. Final file: %s%n", outputFile);

        try
        {
            OutputValidator.run(outputFile);
        }
        catch (RuntimeException e)
        {
            System.err.println("Validation Failed!");
            e.printStackTrace();
        }

        if (!keepNodes)
        {
            try
            {
                TempFileManager.clear();
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
        }

        sr.close();
        //sr.dump(new File(outputDir, "memdump"));
        return jsonHeader;
    }

    public static void main(String[] args) throws IOException, InterruptedException
    {
        CommandLine cmd = getCommandLine(args);
        File inputFile = new File(cmd.getOptionValue("input"));
        File outputFile = new File(cmd.getOptionValue("output"));
        File outputDir = outputFile.getParentFile();

        String jsonHeader = run(
            inputFile,
            outputFile,
            cmd.hasOption("keeptemp"),
            cmd.hasOption("swapyz"),
            Constants.MEMBERSHIP_BUILDER,
            new StdOutProgressReporter("Preprocessing")
        );

        if (cmd.hasOption("dumpjson"))
        {
            File headerFile = new File(outputDir, Useful.getFilenameWithoutExt(inputFile.getName()) + ".json");
            try (FileWriter fw = new FileWriter(headerFile))
            {
                fw.write(jsonHeader);
            }
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

        Option dumpJSON = new Option("j", "dumpjson", false, "Dump the JSON header into separate file");
        options.addOption(dumpJSON);


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
