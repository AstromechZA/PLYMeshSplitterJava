package org.uct.cs.simplify.stitcher;

import gnu.trove.map.hash.TObjectIntHashMap;
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

        TObjectIntHashMap mesh1Vertices = buildMesh1VertexMap(reader1, mesh1vertices.getCount());

        writeMesh1ToTempFiles(file1, vertexFile, faceFile, reader1);

        VertexStitchResult stitchResult = getStitchTransform(vertexFile, reader2, mesh1Vertices, mesh1vertices.getCount(), mesh2vertices.getCount(), outVam);

        writeMesh2FacesStitched(faceFile, reader2, stitchResult.getStitchTransform());

        int numVertices = mesh1vertices.getCount() + mesh2vertices.getCount() - stitchResult.getStitchedCount();
        int numFaces = mesh1faces.getCount() + mesh2faces.getCount();

        System.out.printf("Stitched %d vertices.%n", stitchResult.getStitchedCount());

        PLYHeader h = writeFinalPLYModel(outputFile, vertexFile, faceFile, numVertices, numFaces, outVam);

        TempFileManager.release(vertexFile);
        TempFileManager.release(faceFile);

        return h;
    }

    private static VertexStitchResult getStitchTransform(
            File vertexFile,
            PLYReader reader2,
            TObjectIntHashMap mesh1VertexMap,
            int startingIndex,
            int mesh2NumVertices,
            VertexAttrMap outVam
    ) throws IOException
    {
        int[] mesh2VertexIndices = new int[ mesh2NumVertices ];
        int stitched = 0;
        try (
            BufferedOutputStream ostream = new BufferedOutputStream(new FileOutputStream(vertexFile, true));
            MemoryMappedVertexReader vr = new MemoryMappedVertexReader(reader2)
        )
        {
            Vertex v;
            for (int i = 0; i < mesh2NumVertices; i++)
            {
                v = vr.get(i);
                if (mesh1VertexMap.containsKey(v))
                {
                    mesh2VertexIndices[ i ] = mesh1VertexMap.get(v);
                    stitched++;
                }
                else
                {
                    mesh2VertexIndices[ i ] = startingIndex++;
                    v.writeToStream(ostream, outVam);
                }
            }
        }
        return new VertexStitchResult(stitched, mesh2VertexIndices);
    }

    private static void writeMesh2FacesStitched(File faceFile, PLYReader reader, int[] indexTransform) throws IOException
    {
        try (
            MemoryMappedFaceReader fr = new MemoryMappedFaceReader(reader);
            BufferedOutputStream fostream = new BufferedOutputStream(new FileOutputStream(faceFile, true))
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

    @SuppressWarnings("unchecked")
    private static TObjectIntHashMap buildMesh1VertexMap(PLYReader reader1, int mesh1NumVertices) throws IOException
    {
        TObjectIntHashMap mesh1Vertices = new TObjectIntHashMap(mesh1NumVertices);
        try (MemoryMappedVertexReader vr = new MemoryMappedVertexReader(reader1))
        {
            for (int i = 0; i < mesh1NumVertices; i++)
            {
                mesh1Vertices.put(vr.get(i), i);
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
        int num = files.size();
        for (int i = 1; i < num; i++)
        {
            File temp = TempFileManager.provide("phf", ".ply");
            File current = files.get(i);
            System.out.printf("Stitching %s and %s into %s%n", last, current, temp);
            NaiveMeshStitcher.stitch(last, current, temp);

            // if 'last' is a tempfile, and not the final one.. delete it
            if (i > 1 && i < (num - 1)) TempFileManager.release(last);

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
