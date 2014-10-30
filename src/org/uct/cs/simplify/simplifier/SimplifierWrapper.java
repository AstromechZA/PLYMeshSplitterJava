package org.uct.cs.simplify.simplifier;

import org.uct.cs.simplify.util.OSDetect;
import org.uct.cs.simplify.util.Outputter;
import org.uct.cs.simplify.util.TempFileManager;
import org.uct.cs.simplify.util.XBoundingBox;

import java.io.*;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

public class SimplifierWrapper
{
    private static final String PATH_TO_EXECUTABLE = "simplifier/" + (OSDetect.isWindows ? "tridecimator_win.exe" : "tridecimator_unix");

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
        Outputter.info2f("Simplifying %s to %s (faces: %d)...%n", input.getPath(), tt.getPath(), numFaces);
        Runtime r = Runtime.getRuntime();

        List<String> argsList = new ArrayList<>();

        String path = SimplifierWrapper.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        String decodedPath = URLDecoder.decode(path.substring(0, path.lastIndexOf('/')), "UTF-8") + '/';

        argsList.add(decodedPath + PATH_TO_EXECUTABLE);
        argsList.add("" + 0);
        argsList.add(input.getAbsolutePath());
        argsList.add(tt.getAbsolutePath());
        argsList.add("" + numFaces);
        argsList.add("-P");

        if (preserveBoundary)
        {
            argsList.add("-By");
            argsList.add("" + bb.getMinX());
            argsList.add("" + bb.getMaxX());
            argsList.add("" + bb.getMinY());
            argsList.add("" + bb.getMaxY());
            argsList.add("" + bb.getMinZ());
            argsList.add("" + bb.getMaxZ());
        }

        String[] args = argsList.toArray(new String[ argsList.size() ]);
        Process proc = r.exec(args);

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
                String line;
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
