package org.uct.cs.simplify.stitcher;

import gnu.trove.map.TDoubleIntMap;
import gnu.trove.map.hash.TDoubleIntHashMap;
import org.uct.cs.simplify.model.*;
import org.uct.cs.simplify.ply.header.PLYElement;
import org.uct.cs.simplify.ply.header.PLYHeader;
import org.uct.cs.simplify.ply.reader.ElementDimension;
import org.uct.cs.simplify.ply.reader.PLYReader;
import org.uct.cs.simplify.util.Pair;
import org.uct.cs.simplify.util.TempFileManager;
import org.uct.cs.simplify.util.Useful;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.List;

public class NaiveMeshStitcher
{
    public static final int COPYBYTES_BUF_SIZE = 4096;
    private static final int DEFAULT_BYTEOSBUF_SIZE = 524288;
    private static final int DEFAULT_BYTEOSBUF_TAIL = 16;

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

        TDoubleIntHashMap mesh1Vertices = buildMesh1VertexMap(reader1, mesh1vertices.getCount());

        writeMesh1ToTempFiles(file1, vertexFile, faceFile, reader1);

        VertexStitchResult stitchResult = getStitchTransform(vertexFile, reader2, mesh1Vertices, mesh1vertices.getCount(), mesh2vertices.getCount());

        writeMesh2FacesStitched(faceFile, reader2, stitchResult.getStitchTransform());

//        System.out.printf("mesh1v : %d%n", mesh1vertices.getCount());
//        System.out.printf("mesh2v : %d%n", mesh2vertices.getCount());
//        System.out.printf("stitched : %d%n", stitchResult.getStitchedCount());
//        System.out.printf("mesh1f : %d%n", mesh1faces.getCount());
//        System.out.printf("mesh2f : %d%n", mesh2faces.getCount());

        int numVertices = mesh1vertices.getCount() + mesh2vertices.getCount() - stitchResult.getStitchedCount();
        int numFaces = mesh1faces.getCount() + mesh2faces.getCount();

        VertexAttrMap outVam = new VertexAttrMap(mesh1vertices);
        return writeFinalPLYModel(outputFile, vertexFile, faceFile, numVertices, numFaces, outVam);
    }

    private static VertexStitchResult getStitchTransform(
        File vertexFile, PLYReader reader2, TDoubleIntMap mesh1VertexMap, int startingIndex, int mesh2NumVertices
    ) throws IOException
    {
        int[] mesh2VertexIndices = new int[ mesh2NumVertices ];
        int stitched = 0;
        try (
            FileOutputStream fostream = new FileOutputStream(vertexFile, true);
            ByteArrayOutputStream bostream = new ByteArrayOutputStream(DEFAULT_BYTEOSBUF_SIZE);
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

                    Useful.writeIntLE(bostream, Float.floatToRawIntBits(v.x));
                    Useful.writeIntLE(bostream, Float.floatToRawIntBits(v.y));
                    Useful.writeIntLE(bostream, Float.floatToRawIntBits(v.z));

                    if (bostream.size() > DEFAULT_BYTEOSBUF_SIZE - DEFAULT_BYTEOSBUF_TAIL)
                    {
                        fostream.write(bostream.toByteArray());
                        bostream.reset();
                    }
                }
            }
            if (bostream.size() > 0) fostream.write(bostream.toByteArray());
        }
        return new VertexStitchResult(stitched, mesh2VertexIndices);
    }

    private static void writeMesh2FacesStitched(File faceFile, PLYReader reader, int[] indexTransform) throws IOException
    {
        try (
            MemoryMappedFaceReader fr = new MemoryMappedFaceReader(reader);
            FileOutputStream fostream = new FileOutputStream(faceFile, true);
            ByteArrayOutputStream bostream = new ByteArrayOutputStream(DEFAULT_BYTEOSBUF_SIZE)
        )
        {
            Face face;
            while (fr.hasNext())
            {
                face = fr.next();
                bostream.write((byte) 3);
                Useful.writeIntLE(bostream, indexTransform[face.i]);
                Useful.writeIntLE(bostream, indexTransform[face.j]);
                Useful.writeIntLE(bostream, indexTransform[face.k]);

                if (bostream.size() > DEFAULT_BYTEOSBUF_SIZE - DEFAULT_BYTEOSBUF_TAIL)
                {
                    fostream.write(bostream.toByteArray());
                    bostream.reset();
                }
            }
            if (bostream.size() > 0) fostream.write(bostream.toByteArray());
        }
    }

    private static void writeMesh1ToTempFiles(File file1, File vertexFile, File faceFile, PLYReader reader1) throws IOException
    {
        // write vertices to tempfile1
        ElementDimension vertexE = reader1.getElementDimension("vertex");
        copyFileBytesToFile(file1, vertexE.getOffset(), vertexE.getLength(), vertexFile);
        // write faces to tempfile2
        ElementDimension faceE = reader1.getElementDimension("face");
        copyFileBytesToFile(file1, faceE.getOffset(), faceE.getLength(), faceFile);
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

    private static void copyFileBytesToFile(File inputFile, Long offset, Long length, File outputFile) throws IOException
    {
        try (
            RandomAccessFile rafIN = new RandomAccessFile(inputFile, "r");
            FileChannel fcIN = rafIN.getChannel();
            RandomAccessFile rafOUT = new RandomAccessFile(outputFile, "rw");
            FileChannel fcOUT = rafOUT.getChannel()
        )
        {
            fcIN.position(offset);

            int bufsize = COPYBYTES_BUF_SIZE;
            long div = length / bufsize;
            int rem = (int) (length % bufsize);

            ByteBuffer temp = ByteBuffer.allocate(bufsize);
            for (long i = 0; i < div; i++)
            {
                fcIN.read(temp);
                temp.flip();
                while (temp.hasRemaining()) fcOUT.write(temp);
                temp.clear();
            }
            if (rem > 0)
            {
                temp = ByteBuffer.allocate(rem);
                fcIN.read(temp);
                temp.flip();
                while (temp.hasRemaining()) fcOUT.write(temp);
            }
        }
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
