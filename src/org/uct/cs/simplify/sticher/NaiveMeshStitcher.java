package org.uct.cs.simplify.sticher;

import gnu.trove.map.hash.TDoubleIntHashMap;
import org.uct.cs.simplify.ply.datatypes.DataType;
import org.uct.cs.simplify.ply.datatypes.Face;
import org.uct.cs.simplify.ply.datatypes.Vertex;
import org.uct.cs.simplify.ply.header.PLYHeader;
import org.uct.cs.simplify.ply.reader.ElementDimension;
import org.uct.cs.simplify.ply.reader.MemoryMappedFaceReader;
import org.uct.cs.simplify.ply.reader.MemoryMappedVertexReader;
import org.uct.cs.simplify.ply.reader.PLYReader;
import org.uct.cs.simplify.util.TempFile;
import org.uct.cs.simplify.util.Useful;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;

public class NaiveMeshStitcher
{
    public static final int COPYBYTES_BUF_SIZE = 4096;
    private static final int DEFAULT_BYTEOSBUF_SIZE = 524288;
    private static final int DEFAULT_BYTEOSBUF_TAIL = 16;

    public static PLYHeader stitch(File file1, File file2, File outputFile) throws IOException
    {

        String outputFileBase = Useful.getFilenameWithoutExt(outputFile.getName());
        TempFile vertexFile = new TempFile(outputFile.getParent(), outputFileBase + "_v.temp");
        if (vertexFile.exists()) vertexFile.delete();
        TempFile faceFile = new TempFile(outputFile.getParent(), outputFileBase + "_f.temp");
        if (faceFile.exists()) faceFile.delete();

        PLYReader reader1 = new PLYReader(file1);
        int mesh1NumVertices = reader1.getHeader().getElement("vertex").getCount();
        TDoubleIntHashMap mesh1Vertices = new TDoubleIntHashMap(mesh1NumVertices);
        try (MemoryMappedVertexReader vr = new MemoryMappedVertexReader(reader1))
        {
            for (int i = 0; i < mesh1NumVertices; i++)
            {
                mesh1Vertices.put(vr.get(i).hashCode(), i);
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
        int stitchedVertices = 0;
        try (
            FileOutputStream fostream = new FileOutputStream(vertexFile, true);
            ByteArrayOutputStream bostream = new ByteArrayOutputStream(DEFAULT_BYTEOSBUF_SIZE);
            MemoryMappedVertexReader vr = new MemoryMappedVertexReader(reader2)
        )
        {
            Vertex v;
            ByteBuffer bb = ByteBuffer.wrap(new byte[ 3 * DataType.FLOAT.getByteSize() ]);
            bb.order(ByteOrder.LITTLE_ENDIAN);
            for (int i = 0; i < mesh2NumVertices; i++)
            {
                v = vr.get(i);
                if (mesh1Vertices.containsKey(v.hashCode()))
                {
                    mesh2VertexIndices[ i ] = mesh1Vertices.get(v.hashCode());
                    stitchedVertices++;
                }
                else
                {
                    mesh2VertexIndices[ i ] = index++;
                    bb.putFloat(v.x);
                    bb.putFloat(v.y);
                    bb.putFloat(v.z);
                    bostream.write(bb.array());
                    bb.clear();

                    if (bostream.size() > DEFAULT_BYTEOSBUF_SIZE - DEFAULT_BYTEOSBUF_TAIL)
                    {
                        fostream.write(bostream.toByteArray());
                        bostream.reset();
                    }
                }
            }
            if (bostream.size() > 0) fostream.write(bostream.toByteArray());
        }

        try (
            MemoryMappedFaceReader fr = new MemoryMappedFaceReader(reader2);
            FileOutputStream fostream = new FileOutputStream(faceFile, true);
            ByteArrayOutputStream bostream = new ByteArrayOutputStream(DEFAULT_BYTEOSBUF_SIZE)
        )
        {
            Face face;
            while (fr.hasNext())
            {
                face = fr.next();
                bostream.write((byte) face.getNumVertices());
                for (int i : face.getVertices().toArray())
                {
                    Useful.littleEndianWrite(bostream, mesh2VertexIndices[ i ]);
                }
                if (bostream.size() > DEFAULT_BYTEOSBUF_SIZE - DEFAULT_BYTEOSBUF_TAIL)
                {
                    fostream.write(bostream.toByteArray());
                    bostream.reset();
                }
            }
            if (bostream.size() > 0) fostream.write(bostream.toByteArray());
        }

        System.out.printf("mesh1v : %d%n", reader1.getHeader().getElement("vertex").getCount());
        System.out.printf("mesh2v : %d%n", reader2.getHeader().getElement("vertex").getCount());
        System.out.printf("stitched : %d%n", stitchedVertices);

        System.out.printf("mesh1f : %d%n", reader1.getHeader().getElement("face").getCount());
        System.out.printf("mesh2f : %d%n", reader2.getHeader().getElement("face").getCount());

        int numVertices = reader1.getHeader().getElement("vertex").getCount() +
            reader2.getHeader().getElement("vertex").getCount() -
            stitchedVertices;

        int numFaces = reader1.getHeader().getElement("face").getCount() +
            reader2.getHeader().getElement("face").getCount();

        return writeFinalPLYModel(outputFile, vertexFile, faceFile, numVertices, numFaces);
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

    private static PLYHeader writeFinalPLYModel(File outputFile, File vertexFile, File faceFile, int numVertices, int numFaces) throws IOException
    {
        PLYHeader newHeader = PLYHeader.constructBasicHeader(numVertices, numFaces);

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
}
