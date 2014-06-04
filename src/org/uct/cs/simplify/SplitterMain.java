package org.uct.cs.simplify;

import org.apache.commons.cli.*;
import org.uct.cs.simplify.ply.header.PLYHeader;
import org.uct.cs.simplify.ply.reader.ImprovedPLYReader;
import org.uct.cs.simplify.util.MemRecorder;
import org.uct.cs.simplify.util.Timer;

import java.io.File;
import java.io.IOException;

public class SplitterMain
{

    public static void main(String[] args)
    {
        CommandLine cmd = getCommandLine(args);

        try (MemRecorder m = new MemRecorder(new File("C:\\Users\\Ben\\o.dat"), 50); Timer ignored = new Timer("Entire read"))
        {
            File file = new File(cmd.getOptionValue("f"));

            // == construct & setup PLYReader
            // this scans the target file and works out start and end ranges
            PLYHeader header = new PLYHeader(file);
            ImprovedPLYReader r = new ImprovedPLYReader(header, file);

            System.out.println(r.getElementDimension("vertex").getFirst());
            System.out.println(r.getElementDimension("face").getFirst());


        }
        catch (IOException | InterruptedException e)
        {
            e.printStackTrace();
        }
    }

    /**
     * Parses the given string array as an options array and returns a CommandLine instance
     * containing the results. If required options were missing or an error occured, it will print
     * usage to standard out and System.exit(1).
     *
     * @param args String[] containing program arguments
     * @return CommandLine instance containing results
     */
    private static CommandLine getCommandLine(String[] args)
    {
        CommandLineParser clp = new BasicParser();

        Options options = new Options();

        Option o1 = new Option("f", "filename", true, "path to PLY model to process");
        o1.setRequired(true);
        options.addOption(o1);

        Option o2 = new Option("d", "depth", true, "number of levels to split to");
        o2.setType(Short.class);
        options.addOption(o2);

        Option o3 = new Option("s", "scaleTo", true, "scale the model to fit a cube of the given size");
        o3.setType(Short.class);
        options.addOption(o3);

        CommandLine cmd;
        try
        {
            cmd = clp.parse(options, args);
            return cmd;
        }
        catch (ParseException e)
        {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("PLYMeshSplitterJ --filename <path>", options);
            System.exit(1);
            return null;
        }
    }

}
