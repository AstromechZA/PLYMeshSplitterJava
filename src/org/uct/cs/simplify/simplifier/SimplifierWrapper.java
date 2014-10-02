package org.uct.cs.simplify.simplifier;

import org.uct.cs.simplify.util.OSDetect;
import org.uct.cs.simplify.util.Outputter;
import org.uct.cs.simplify.util.TempFileManager;
import org.uct.cs.simplify.util.XBoundingBox;

import java.io.*;

public class SimplifierWrapper
{
    private static final String PATH_TO_EXECUTABLE = "./simplifier/" + (OSDetect.isWindows ? "tridecimator_win.exe" : "tridecimator_unix");

    public static File simplify(File input, long numFaces)
    throws IOException, InterruptedException
    {
        return simplify(input, numFaces, false, null);
    }

    public static File simplify(File input, long numFaces, XBoundingBox bb)
    throws IOException, InterruptedException
    {
        return simplify(input, numFaces, true, bb);
    }

    public static File simplify(File input, long numFaces, boolean preserveBoundary, XBoundingBox bb)
    throws IOException, InterruptedException
    {
        File tt = TempFileManager.provide("simp", ".ply");
        Outputter.info2f("Simplifying %s to %s...%n", input.getPath(), tt.getPath());
        Runtime r = Runtime.getRuntime();

        String flags = " -P";
        if (preserveBoundary)
        {
            flags += String.format(
                " -By %f %f %f %f %f %f", bb.getMinX(), bb.getMaxX(), bb.getMinY(), bb.getMaxY(), bb.getMinZ(),
                bb.getMaxZ()
            );
        }

        String inputS = input.getAbsolutePath();
        String outputS = tt.getAbsolutePath();
        if (inputS.contains(" ")) inputS = "\"" + inputS + "\"";
        if (outputS.contains(" ")) outputS = "\"" + outputS + "\"";

        Process proc = r.exec(
            String.format(
                "%s 0 %s %s %d" + flags,
                PATH_TO_EXECUTABLE,
                inputS,
                outputS,
                numFaces
            )
        );

        StreamGobbler stdOutGobble = new StreamGobbler(proc.getInputStream());
        StreamGobbler errorGobble = new StreamGobbler(proc.getErrorStream());

        stdOutGobble.run();
        errorGobble.run();

        int code = proc.waitFor();

        stdOutGobble.join();
        errorGobble.join();

        if (code != 0)
        {
            throw new UnknownError("Simplify failed! : " + stdOutGobble + errorGobble);
        }
        return tt;
    }

    private static class StreamGobbler extends Thread
    {
        private final StringBuilder sb;
        private final InputStream stream;

        public StreamGobbler(InputStream in)
        {
            this.sb = new StringBuilder(1000);
            this.stream = in;
        }

        public void run()
        {
            try
            {
                InputStreamReader reader = new InputStreamReader(this.stream);
                BufferedReader br = new BufferedReader(reader);
                String line ;
                while ((line = br.readLine()) != null)
                {
                    this.sb.append(line);
                    this.sb.append('\n');
                }
            }
            catch (IOException ioe)
            {
                ioe.printStackTrace();
            }
        }

        public String toString()
        {
            return this.sb.toString();
        }
    }
}
