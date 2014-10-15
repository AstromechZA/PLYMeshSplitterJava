package org.uct.cs.simplify;

import org.apache.commons.cli.*;
import org.uct.cs.simplify.filebuilder.PHFBuilder;
import org.uct.cs.simplify.filebuilder.PHFNode;
import org.uct.cs.simplify.filebuilder.RecursiveFilePreparer;
import org.uct.cs.simplify.ply.header.PLYHeader;
import org.uct.cs.simplify.splitter.memberships.IMembershipBuilder;
import org.uct.cs.simplify.splitter.memberships.MultiwayVariableKDTreeMembershipBuilder;
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
    public static final int FACES_PER_LEAF = 50_000;
    private static final int RESCALE_SIZE = 1024;

    public static String run(
        File inputFile,
        File outputFile,
        boolean keepNodes,
        boolean swapYZ,
        IMembershipBuilder membershipBuilder,
        IProgressReporter reporter
    )
        throws IOException, InterruptedException
    {
        IStoppingCondition stopCondition;
        float simplificationRatio;

        Outputter.info1f("Using membership builder: %s%n", membershipBuilder.getClass().getName());

        if (membershipBuilder.isBalanced())
        {
            float facesPerSimplification = FACES_PER_LEAF * membershipBuilder.getSplitRatio();

            long numFaces = new PLYHeader(inputFile).getElement("face").getCount();
            float numLeaves = (numFaces / facesPerSimplification) * membershipBuilder.getSplitRatio();
            int numLevels = (int) Math.ceil(Math.log(numLeaves) / Math.log(membershipBuilder.getSplitRatio()));

            simplificationRatio = (float) Math.pow(facesPerSimplification / numFaces, 1.0f / (numLevels - 1));

            Outputter.info1f("Calculated Tree Depth: %d (%d splits)%n", numLevels + 1, numLevels);
            Outputter.info1f("Calculated preffered simplification ratio: %f%n", simplificationRatio);

            stopCondition = new DepthStoppingCondition(numLevels);
        }
        else
        {
            simplificationRatio = (float) 1 / membershipBuilder.getSplitRatio();

            Outputter.info1ln("Calculated Tree Depth: N/A (face limit controlled)");
            Outputter.info1f("Calculated preffered simplification ratio: %f%n", simplificationRatio);

            stopCondition = new LowerFaceBoundStoppingCondition(FACES_PER_LEAF);
        }

        return run(inputFile, outputFile, keepNodes, swapYZ, membershipBuilder, simplificationRatio, stopCondition, reporter);
    }

    public static String run(
        File inputFile,
        File outputFile,
        boolean keepNodes,
        boolean swapYZ,
        IMembershipBuilder membershipBuilder,
        float simplificationRatio,
        IStoppingCondition stopCondition,
        IProgressReporter progressReporter
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
        double scaleRatio = ScaleAndRecenter.run(inputFile, scaledFile, RESCALE_SIZE, swapYZ);

        // build tree
        PHFNode tree = RecursiveFilePreparer.prepare(new PHFNode(scaledFile), stopCondition, membershipBuilder, simplificationRatio, progressReporter);

        // additional json keys
        Map<String, String> additionalJSON = new HashMap<>();
        additionalJSON.put("scale_ratio", "" + scaleRatio);

        // compile into output file
        String jsonHeader = PHFBuilder.compile(tree, outputFile, additionalJSON);
        Outputter.info3f("Processing complete. Final file: %s%n", outputFile);

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
            new MultiwayVariableKDTreeMembershipBuilder(4),
            new StdOutProgressReporter()
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
            formatter.printHelp("filebuilder --input <path> --output <path>", options);
            System.exit(1);
            return null;
        }
    }


}
