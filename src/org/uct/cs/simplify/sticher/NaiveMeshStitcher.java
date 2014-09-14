package org.uct.cs.simplify.sticher;

import gnu.trove.map.hash.TObjectIntHashMap;
import org.uct.cs.simplify.ply.datatypes.Vertex;
import org.uct.cs.simplify.ply.header.PLYHeader;
import org.uct.cs.simplify.ply.reader.ElementDimension;
import org.uct.cs.simplify.ply.reader.MemoryMappedVertexReader;
import org.uct.cs.simplify.ply.reader.PLYReader;
import org.uct.cs.simplify.util.TempFile;
import org.uct.cs.simplify.util.Useful;

import java.io.File;
import java.io.IOException;

public class NaiveMeshStitcher
{
    public static PLYHeader stitch(File file1, File file2, File outputFile) throws IOException
    {

        String outputFileBase = Useful.getFilenameWithoutExt(outputFile.getName());
        TempFile vertexFile = new TempFile(outputFile.getParent(), outputFileBase + "_v.temp");
        TempFile faceFile = new TempFile(outputFile.getParent(), outputFileBase + "_f.temp");

        PLYReader reader1 = new PLYReader(file1);
        int mesh1NumVertices = reader1.getHeader().getElement("vertex").getCount();
        TObjectIntHashMap<Vertex> mesh1Vertices = new TObjectIntHashMap<>(mesh1NumVertices);

        try (MemoryMappedVertexReader vr = new MemoryMappedVertexReader(reader1))
        {
            for (int i = 0; i < mesh1NumVertices; i++)
            {
                mesh1Vertices.put(vr.get(i), i);
            }
        }

        // write vertices to tempfile1
        ElementDimension vertexE = reader1.getElementDimension("vertex");
        copyFileBytesToFile(file1, vertexE.getOffset(), vertexE.getLength(), vertexFile);
        // write faces to tempfile2
        ElementDimension faceE = reader1.getElementDimension("face");
        copyFileBytesToFile(file1, faceE.getOffset(), faceE.getLength(), faceFile);

        PLYReader reader2 = new PLYReader(file2);
        int mesh2NumVertices = reader2.getHeader().getElement("vertex").getCount();
        int[] mesh2VertexIndices = new int[ mesh2NumVertices ];

        int index = mesh1NumVertices;
        try (MemoryMappedVertexReader vr = new MemoryMappedVertexReader(reader2))
        {
            Vertex v;
            for (int i = 0; i < mesh2NumVertices; i++)
            {
                v = vr.get(i);
                if (mesh1Vertices.containsKey(v))
                {
                    mesh2VertexIndices[ i ] = mesh1Vertices.get(v);
                }
                else
                {
                    mesh2VertexIndices[ i ] = index++;
                    // write vertex to vertex file
                }
            }
        }

        // run through faces
        // for each vertex
        // pull new index from G
        // write to tempfile2

        // combine tempfiles into final file

        return null;
    }

    private static void copyFileBytesToFile(File inputFile, Long offset, Long length, File outputFile)
    {

    }
}
