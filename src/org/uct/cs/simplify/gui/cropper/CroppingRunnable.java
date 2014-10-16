package org.uct.cs.simplify.gui.cropper;

import gnu.trove.map.hash.TIntIntHashMap;
import org.uct.cs.simplify.blueprint.BluePrintGenerator.BlueprintGeneratorResult;
import org.uct.cs.simplify.cropping.LineBase;
import org.uct.cs.simplify.gui.util.ProgressBarProgressReporter;
import org.uct.cs.simplify.model.*;
import org.uct.cs.simplify.ply.header.PLYHeader;
import org.uct.cs.simplify.ply.reader.PLYReader;
import org.uct.cs.simplify.util.CompactBitArray;
import org.uct.cs.simplify.util.ICompletionListener;
import org.uct.cs.simplify.util.StatRecorder;
import org.uct.cs.simplify.util.TempFileManager;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.*;
import java.nio.channels.FileChannel;
import java.util.ArrayList;

public class CroppingRunnable implements Runnable
{
    private final ProgressBarProgressReporter progress;
    private final File inputFile, outputFile;
    private final ICompletionListener listener;
    private final ArrayList<LineBase> hullLines;
    private final BlueprintGeneratorResult blueprint;

    public CroppingRunnable(JProgressBar progressBar, File inputFile, File outputFile, ICompletionListener listener, ArrayList<LineBase> hullLines, BlueprintGeneratorResult blueprint)
    {
        this.inputFile = inputFile;
        this.outputFile = outputFile;
        this.listener = listener;
        this.hullLines = hullLines;
        this.blueprint = blueprint;
        this.progress = new ProgressBarProgressReporter(progressBar);
    }

    @Override
    public void run()
    {
        boolean success = false;

        try (StatRecorder ignored = new StatRecorder())
        {
            PLYReader reader = new PLYReader(this.inputFile);
            int numExcludedVertices = 0;
            int numVertices = reader.getHeader().getElement("vertex").getCount();
            int numExcludedFaces = 0;
            int numFaces = reader.getHeader().getElement("face").getCount();
            CompactBitArray isExcluded = new CompactBitArray(1, numVertices);
            TIntIntHashMap vertexIndexMap = new TIntIntHashMap(numVertices / 2);

            BufferedImage bi = this.blueprint.output;
            int[] pixels = ((DataBufferInt) bi.getRaster().getDataBuffer()).getData();
            int w = bi.getWidth();

            File tempdatafile = TempFileManager.provide();

            try (BufferedOutputStream ostream = new BufferedOutputStream(new FileOutputStream(tempdatafile)))
            {
                try (MemoryMappedVertexReader vr = new MemoryMappedVertexReader(reader))
                {
                    int currentIndex = 0;
                    Vertex v = new Vertex(0, 0, 0);
                    for (int i = 0; i < numVertices; i++)
                    {
                        vr.get(i, v);

                        float x = this.blueprint.av.getPrimaryAxisValue(v);
                        float y = this.blueprint.av.getSecondaryAxisValue(v);

                        boolean excluded = false;
                        for (LineBase line : hullLines)
                        {
                            if (line.doesExclude(x, y))
                            {
                                isExcluded.set(i, 1);
                                numExcludedVertices++;
                                excluded = true;
                                break;
                            }
                        }
                        if (excluded)
                        {
                            int tx = (int) (this.blueprint.center + ((this.blueprint.av.getPrimaryAxisValue(v) - this.blueprint.centerPrimary) * this.blueprint.ratio));
                            int ty = (int) (this.blueprint.center - ((this.blueprint.av.getSecondaryAxisValue(v) - this.blueprint.centerSecondary) * this.blueprint.ratio));
                            int index = ty * w + tx;
                            pixels[ index ] = Color.red.getRGB();
                        }
                        else
                        {
                            vertexIndexMap.put(i, currentIndex++);
                            v.writeToStream(ostream, vr.getVam());
                        }

                        this.progress.report(((float) i / numVertices) * 0.5f);
                    }
                }

                try (StreamingFaceReader fr = new FastBufferedFaceReader(reader))
                {
                    Face f = new Face(0, 0, 0);
                    int i = 0;
                    while (fr.hasNext())
                    {
                        fr.next(f);

                        boolean excluded = isExcluded.get(f.i) > 0 || isExcluded.get(f.j) > 0 || isExcluded.get(f.k) > 0;
                        if (excluded)
                        {
                            numExcludedFaces++;
                        }
                        else
                        {
                            f.i = vertexIndexMap.get(f.i);
                            f.j = vertexIndexMap.get(f.j);
                            f.k = vertexIndexMap.get(f.k);
                            f.writeToStream(ostream);
                        }
                        i++;
                        this.progress.report(0.5f + ((float) i / numFaces) * 0.5f);
                    }
                }
            }

            System.out.printf("Vertices: %d -> %d (%d)%n", numVertices, numVertices - numExcludedVertices, -numExcludedVertices);
            System.out.printf("Faces: %d -> %d (%d)%n", numFaces, numFaces - numExcludedFaces, -numExcludedFaces);

            PLYHeader newHeader = PLYHeader.constructHeader(
                numVertices - numExcludedVertices,
                numFaces - numExcludedFaces,
                new VertexAttrMap(reader.getHeader().getElement("vertex"))
            );

            try (FileOutputStream fostream = new FileOutputStream(outputFile))
            {
                fostream.write((newHeader + "\n").getBytes());

                try (FileChannel fcOUT = fostream.getChannel())
                {
                    long position = fcOUT.position();
                    try (FileChannel fc = new FileInputStream(tempdatafile).getChannel())
                    {
                        long length = fc.size();
                        fcOUT.transferFrom(fc, position, length);
                    }
                }
            }

            this.progress.report(1);

            success = true;
        }
        catch (IOException | InterruptedException e)
        {
            e.printStackTrace();
        }

        try
        {
            TempFileManager.clear();
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
        if (this.listener != null) this.listener.callback(success);
    }
}
