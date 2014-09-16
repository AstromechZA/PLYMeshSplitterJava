package org.uct.cs.simplify;

import org.uct.cs.simplify.filebuilder.PLYDataCompressor;
import org.uct.cs.simplify.util.MemStatRecorder;
import org.uct.cs.simplify.util.Timer;

import java.io.File;
import java.io.IOException;

public class Compressor
{
    public static void main(String[] args) throws IOException, InterruptedException
    {
        File input = new File("temp/compressor/test.ply");
        File output = new File("temp/compressor/o.dat");

        try(MemStatRecorder ignored = new MemStatRecorder(); Timer t = new Timer())
        {
            PLYDataCompressor.compress(input, output);
        }
    }
}
