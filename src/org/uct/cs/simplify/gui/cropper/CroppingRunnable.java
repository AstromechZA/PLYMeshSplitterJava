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
        this.progress = new ProgressBarProgressReporter(progressBar, "Cropping");
    }

    @Override
    public void run()
    {
        boolean success = false;

        try (StatRecorder ignored = new StatRecorder())
        {
            PLYReader reader = new PLYReader(this.inputFile);
            long numExcludedVertices = 0;
            long numVertices = reader.getHeader().getElement("vertex").getCount();
            long numExcludedFaces = 0;
            long numFaces = reader.getHeader().getElement("face").getCount();
            CompactBitArray isExcluded = new CompactBitArray(1, numVertices);
            TIntIntHashMap vertexIndexMap = new TIntIntHashMap((int) (numVertices / 2));

            BufferedImage bi = this.blueprint.output;
            int[] pixels = ((DataBufferInt) bi.getRaster().getDataBuffer()).getData();
            int w = bi.getWidth();

            File tempdatafile = TempFileManager.provide();

            try (BufferedOutputStream ostream = new BufferedOutputStream(new FileOutputStream(tempdatafile)))
            {

                this.progress.changeTask("Cropping Vertices", false);

                try (ReliableBufferedVertexReader vr = new ReliableBufferedVertexReader(reader))
                {
                    long vertexIndex = 0;
                    long currentIndex = 0;
                    Vertex v = new Vertex(0, 0, 0);
                    while (vr.hasNext())
                    {
                        vr.next(v);

                        float x = this.blueprint.av.getPrimaryAxisValue(v);
                        float y = this.blueprint.av.getSecondaryAxisValue(v);

                        boolean excluded = false;
                        for (LineBase line : hullLines)
                        {
                            if (line.doesExclude(x, y))
                            {
                                isExcluded.set(vertexIndex, 1);
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
                            vertexIndexMap.put((int) (vertexIndex), (int) (currentIndex++));
                            v.writeToStream(ostream, vr.getVam());
                        }

                        this.progress.report((vertexIndex / (float) numVertices) * 0.5f);
                        vertexIndex++;
                    }
                }

                this.progress.changeTask("Cropping Faces", false);

                try (StreamingFaceReader fr = new CleverFastBuffedFaceReader(reader))
                {
                    Face f = new Face(0, 0, 0);
                    long i = 0;
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
                        this.progress.report(0.5f + (i / (float) numFaces) * 0.5f);
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

            this.progress.changeTask("Writing file...", true);

            try (FileOutputStream fostream = new FileOutputStream(outputFile))
            {
                fostream.write((newHeader + "\n").getBytes());

                try (FileChannel channelOut = fostream.getChannel())
                {
                    long position = channelOut.position();
                    try (FileChannel channelIn = new FileInputStream(tempdatafile).getChannel())
                    {
                        long length = channelIn.size();
                        long partLength = length / 100;

                        for (int i = 0; i < 99; i++)
                        {
                            channelOut.transferFrom(channelIn, position, partLength);
                            position += partLength;
                            this.progress.report(i / 100.0f);
                        }

                        channelOut.transferFrom(channelIn, position, partLength + length % partLength);
                        this.progress.report(1);
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
