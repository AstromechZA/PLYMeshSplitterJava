package org.uct.cs.simplify.stitcher;

import gnu.trove.map.TDoubleIntMap;
import gnu.trove.map.hash.TDoubleIntHashMap;
import it.unimi.dsi.fastutil.io.FastBufferedOutputStream;
import org.uct.cs.simplify.model.*;
import org.uct.cs.simplify.ply.header.PLYElement;
import org.uct.cs.simplify.ply.header.PLYHeader;
import org.uct.cs.simplify.ply.reader.ElementDimension;
import org.uct.cs.simplify.ply.reader.PLYReader;
import org.uct.cs.simplify.util.Pair;
import org.uct.cs.simplify.util.TempFileManager;
import org.uct.cs.simplify.util.Useful;

import java.io.*;
import java.nio.channels.FileChannel;
import java.util.List;

public class NaiveMeshStitcher
{
    public static PLYHeader stitch(File file1, File file2, File outputFile) throws IOException
    {
        String outputFileBase = Useful.getFilenameWithoutExt(outputFile.getName());
        File vertexFile = TempFileManager.provide(outputFileBase + "_v");
        File faceFile = TempFileManager.provide(outputFileBase + "_f");

        PLYReader reader1 = new PLYReader(file1);
        PLYReader reader2 = new PLYReader(file2);
        PLYElement mesh1vertices = reader1.getHeader().getElement("vertex");
        PLYElement mesh1faces = reader1.getHeader().getElement("face");
        PLYElement mesh2vertices = reader2.getHeader().getElement("vertex");
        PLYElement mesh2faces = reader2.getHeader().getElement("face");

        VertexAttrMap outVam = new VertexAttrMap(mesh1vertices);

        TDoubleIntHashMap mesh1Vertices = buildMesh1VertexMap(reader1, mesh1vertices.getCount());

        writeMesh1ToTempFiles(file1, vertexFile, faceFile, reader1);

        VertexStitchResult stitchResult = getStitchTransform(vertexFile, reader2, mesh1Vertices, mesh1vertices.getCount(), mesh2vertices.getCount(), outVam);

        writeMesh2FacesStitched(faceFile, reader2, stitchResult.getStitchTransform());

        int numVertices = mesh1vertices.getCount() + mesh2vertices.getCount() - stitchResult.getStitchedCount();
        int numFaces = mesh1faces.getCount() + mesh2faces.getCount();

        return writeFinalPLYModel(outputFile, vertexFile, faceFile, numVertices, numFaces, outVam);
    }

    private static VertexStitchResult getStitchTransform(
            File vertexFile,
            PLYReader reader2,
            TDoubleIntMap mesh1VertexMap,
            int startingIndex,
            int mesh2NumVertices,
            VertexAttrMap outVam
    ) throws IOException
    {
        int[] mesh2VertexIndices = new int[ mesh2NumVertices ];
        int stitched = 0;
        try (
            FastBufferedOutputStream fostream = new FastBufferedOutputStream(new FileOutputStream(vertexFile, true));
            MemoryMappedVertexReader vr = new MemoryMappedVertexReader(reader2)
        )
        {
            Vertex v;
            for (int i = 0; i < mesh2NumVertices; i++)
            {
                v = vr.get(i);
                if (mesh1VertexMap.containsKey(v.getHash()))
                {
                    mesh2VertexIndices[i] = mesh1VertexMap.get(v.getHash());
                    stitched++;
                }
                else
                {
                    mesh2VertexIndices[ i ] = startingIndex++;

                    Useful.writeIntLE(fostream, Float.floatToRawIntBits(v.x));
                    Useful.writeIntLE(fostream, Float.floatToRawIntBits(v.y));
                    Useful.writeIntLE(fostream, Float.floatToRawIntBits(v.z));

                    if (outVam.hasColour)
                    {
                        fostream.write(v.r);
                        fostream.write(v.g);
                        fostream.write(v.b);
                    }

                    if (outVam.hasAlpha)
                    {
                        fostream.write(v.a);
                    }
                }
            }
        }
        return new VertexStitchResult(stitched, mesh2VertexIndices);
    }

    private static void writeMesh2FacesStitched(File faceFile, PLYReader reader, int[] indexTransform) throws IOException
    {
        try (
            MemoryMappedFaceReader fr = new MemoryMappedFaceReader(reader);
            FastBufferedOutputStream fostream = new FastBufferedOutputStream(new FileOutputStream(faceFile, true))
        )
        {
            Face face;
            while (fr.hasNext())
            {
                face = fr.next();
                fostream.write((byte) 3);
                Useful.writeIntLE(fostream, indexTransform[face.i]);
                Useful.writeIntLE(fostream, indexTransform[face.j]);
                Useful.writeIntLE(fostream, indexTransform[face.k]);
            }
        }
    }

    private static void writeMesh1ToTempFiles(File inputFile, File vertexFile, File faceFile, PLYReader reader) throws IOException
    {
        ElementDimension vertexE = reader.getElementDimension("vertex");
        ElementDimension faceE = reader.getElementDimension("face");

        try(FileChannel fcIN = new RandomAccessFile(inputFile, "r").getChannel())
        {
            try(FileChannel fcOUT = new FileOutputStream(vertexFile).getChannel())
            {
                fcIN.transferTo(vertexE.getOffset(), vertexE.getLength(), fcOUT);
            }

            try(FileChannel fcOUT = new FileOutputStream(faceFile).getChannel())
            {
                fcIN.transferTo(faceE.getOffset(), faceE.getLength(), fcOUT);
            }
        }
    }

    private static TDoubleIntHashMap buildMesh1VertexMap(PLYReader reader1, int mesh1NumVertices) throws IOException
    {
        TDoubleIntHashMap mesh1Vertices = new TDoubleIntHashMap(mesh1NumVertices);
        try (MemoryMappedVertexReader vr = new MemoryMappedVertexReader(reader1))
        {
            for (int i = 0; i < mesh1NumVertices; i++)
            {
                mesh1Vertices.put(vr.get(i).getHash(), i);
            }
        }
        return mesh1Vertices;
    }

    private static PLYHeader writeFinalPLYModel(
        File outputFile, File vertexFile, File faceFile, int numVertices, int numFaces, VertexAttrMap outVam
    ) throws IOException
    {
        PLYHeader newHeader = PLYHeader.constructHeader(numVertices, numFaces, outVam);

        try (FileOutputStream fostream = new FileOutputStream(outputFile))
        {
            fostream.write((newHeader + "\n").getBytes());

            try (FileChannel fcOUT = fostream.getChannel())
            {
                long position = fcOUT.position();

                try (FileChannel fc = new FileInputStream(vertexFile).getChannel())
                {
                    long length = fc.size();
                    fcOUT.transferFrom(fc, position, length);
                    position += length;
                }
                try (FileChannel fc = new FileInputStream(faceFile).getChannel())
                {
                    long length = fc.size();
                    fcOUT.transferFrom(fc, position, length);
                }
            }
        }

        return new PLYHeader(outputFile);
    }

    public static File stitch(List<File> files) throws IOException
    {
        File last = files.get(0);
        for (int i = 1; i < files.size(); i++)
        {
            File temp = TempFileManager.provide("phf", ".ply");
            File current = files.get(i);
            System.out.printf("Stitching %s and %s into %s%n", last, current, temp);
            NaiveMeshStitcher.stitch(last, current, temp);
            last = temp;
        }

        return last;
    }

    private static class VertexStitchResult extends Pair<Integer, int[]>
    {
        public VertexStitchResult(Integer f, int[] s)
        {
            super(f, s);
        }

        public int getStitchedCount()
        {
            return this.getFirst();
        }

        public int[] getStitchTransform()
        {
            return this.getSecond();
        }
    }
}
