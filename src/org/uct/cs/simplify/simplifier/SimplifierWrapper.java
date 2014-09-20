package org.uct.cs.simplify.simplifier;

import org.uct.cs.simplify.util.TempFileManager;

import java.io.*;

public class SimplifierWrapper
{
    private static final String PATH_TO_EXECUTABLE = "./simplifier/SimplFy";

    public static File simplify(File input, int numFaces) throws IOException, InterruptedException
    {
        File tt = TempFileManager.provide("simp", ".ply");
        System.out.printf("Simplifying %s to %s...%n", input.getPath(), tt.getPath());
        Runtime r = Runtime.getRuntime();
        Process proc = r.exec(
            String.format(
                "%s %s %s %d -By",
                PATH_TO_EXECUTABLE,
                input.getAbsolutePath(),
                tt.getAbsolutePath(),
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
            throw new UnknownError("Simplify failed! : " + stdOutGobble.toString() + errorGobble.toString());
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
                InputStreamReader reader = new InputStreamReader((this.stream));
                BufferedReader br = new BufferedReader(reader);
                String line = null;
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
