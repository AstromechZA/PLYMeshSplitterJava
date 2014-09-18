package org.uct.cs.simplify.splitter;

import org.uct.cs.simplify.filebuilder.PackagedHierarchicalNode;
import org.uct.cs.simplify.model.Face;
import org.uct.cs.simplify.model.MemoryMappedFaceReader;
import org.uct.cs.simplify.model.MemoryMappedVertexReader;
import org.uct.cs.simplify.model.Vertex;
import org.uct.cs.simplify.ply.datatypes.DataType;
import org.uct.cs.simplify.ply.header.PLYHeader;
import org.uct.cs.simplify.ply.reader.PLYReader;
import org.uct.cs.simplify.splitter.memberships.IMembershipBuilder;
import org.uct.cs.simplify.splitter.memberships.MembershipBuilderResult;
import org.uct.cs.simplify.util.*;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.LinkedHashMap;

public class NodeSplitter
{
    private static final int DEFAULT_BYTEOSBUF_SIZE = 524288;
    private static final int DEFAULT_BYTEOSBUF_TAIL = 16;

    public static ArrayList<PackagedHierarchicalNode> split(
        PackagedHierarchicalNode parent,
        IMembershipBuilder membershipBuilder
    ) throws IOException
    {
        // output object for subnodes
        ArrayList<PackagedHierarchicalNode> output = new ArrayList<>();

        // build reader object
        PLYReader reader = new PLYReader(parent.getLinkedFile());

        MembershipBuilderResult mr = membershipBuilder.build(reader, parent.getBoundingBox());
        for (int nodeID : mr.subNodes.keys())
        {
            File tempFaceFile = TempFileManager.provide();
            GatheringResult result = gatherVerticesAndWriteFaces(reader, mr.memberships, tempFaceFile, nodeID);
            if (result.numFaces > 0)
            {
                File subNodeFile = TempFileManager.provide("node", ".ply");

                writeSubnodePLYModel(reader, subNodeFile, tempFaceFile, result);

                PackagedHierarchicalNode child = new PackagedHierarchicalNode(mr.subNodes.get(nodeID), result.numVertices, result.numFaces, subNodeFile);
                child.setDepth(parent.getDepth() + 1);
                output.add(child);
            }
            if (tempFaceFile.exists()) tempFaceFile.delete();
        }

        return output;
    }

    private static void writeSubnodePLYModel(PLYReader reader, File subNodeFile, File tempFaceFile, GatheringResult result) throws IOException
    {
        PLYHeader newHeader = PLYHeader.constructBasicHeader(result.numVertices, result.numFaces);

        try (FileOutputStream fostream = new FileOutputStream(subNodeFile))
        {
            fostream.write((newHeader + "\n").getBytes());

            try (
                MemoryMappedVertexReader vr = new MemoryMappedVertexReader(reader);
                ByteArrayOutputStream bostream = new ByteArrayOutputStream(DEFAULT_BYTEOSBUF_SIZE)
            )
            {
                Vertex v;
                ByteBuffer bb = ByteBuffer.wrap(new byte[ 3 * DataType.FLOAT.getByteSize() ]);
                bb.order(ByteOrder.LITTLE_ENDIAN);
                try (
                    ProgressBar pb = new ProgressBar(
                        String.format("%s: Writing Vertices", Useful.getFilenameWithoutExt(subNodeFile.getName())),
                        result.numVertices
                    )
                )
                {
                    for (int i : result.vertexIndexMap.keySet())
                    {
                        pb.tick();
                        v = vr.get(i);
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
                    if (bostream.size() > 0) fostream.write(bostream.toByteArray());
                }
            }

            try (FileChannel fc = new FileInputStream(tempFaceFile).getChannel())
            {
                fostream.getChannel().transferFrom(fc, fostream.getChannel().position(), fc.size());
            }
        }
    }

    private static CompactBitArray getSubnodeMemberships(PLYReader reader, XBoundingBox bb) throws IOException
    {
        int num_vertices = reader.getHeader().getElement("vertex").getCount();
        CompactBitArray output = new CompactBitArray(num_vertices);

        try (MemoryMappedVertexReader vr = new MemoryMappedVertexReader(reader))
        {
            if (bb.getWidth() > bb.getHeight() && bb.getWidth() > bb.getDepth())
                putSubnodeMembershipsX(vr, output, bb.getCenter().getX());
            else if (bb.getHeight() > bb.getWidth() && bb.getHeight() > bb.getDepth())
                putSubnodeMembershipsY(vr, output, bb.getCenter().getY());
            else
                putSubnodeMembershipsZ(vr, output, bb.getCenter().getZ());
        }

        return output;
    }

    private static void putSubnodeMembershipsX(
        MemoryMappedVertexReader vReader, CompactBitArray bitArray, double splitPoint
    )
    {
        long l = bitArray.size();
        for (int i = 0; i < l; i++)
        {
            if (vReader.get(i).x > splitPoint) bitArray.set(i, 1);
        }
    }

    private static void putSubnodeMembershipsY(
        MemoryMappedVertexReader vReader, CompactBitArray bitArray, double splitPoint
    )
    {
        long l = bitArray.size();
        for (int i = 0; i < l; i++)
        {
            if (vReader.get(i).y > splitPoint) bitArray.set(i, 1);
        }
    }

    private static void putSubnodeMembershipsZ(
        MemoryMappedVertexReader vReader, CompactBitArray bitArray, double splitPoint
    )
    {
        long l = bitArray.size();
        for (int i = 0; i < l; i++)
        {
            if (vReader.get(i).z > splitPoint) bitArray.set(i, 1);
        }
    }

    private static GatheringResult gatherVerticesAndWriteFaces(
        PLYReader reader,
        CompactBitArray memberships,
        File tempfile,
        int subnode
    ) throws IOException
    {
        int currentVertexIndex = 0;
        LinkedHashMap<Integer, Integer> vertexIndexMap = new LinkedHashMap<>((int) (memberships.size() / Math.pow(2, memberships.getBits())));
        try (
            MemoryMappedFaceReader faceReader = new MemoryMappedFaceReader(reader);
            FileOutputStream fostream = new FileOutputStream(tempfile);
            ByteArrayOutputStream bostream = new ByteArrayOutputStream(DEFAULT_BYTEOSBUF_SIZE)
        )
        {
            Face face;
            int numVerticesOfFaceInSubnode;
            int numFacesInSubnode = 0;
            while (faceReader.hasNext())
            {
                face = faceReader.next();
                numVerticesOfFaceInSubnode = 0;

                if (memberships.get(face.i) == subnode) numVerticesOfFaceInSubnode += 1;
                if (memberships.get(face.j) == subnode) numVerticesOfFaceInSubnode += 1;
                if (memberships.get(face.k) == subnode) numVerticesOfFaceInSubnode += 1;

                if (numVerticesOfFaceInSubnode > 1)
                {
                    numFacesInSubnode++;
                    bostream.write((byte) 3);

                    if (!vertexIndexMap.containsKey(face.i))
                    {
                        vertexIndexMap.put(face.i, currentVertexIndex);
                        currentVertexIndex += 1;
                    }
                    Useful.littleEndianWrite(bostream, vertexIndexMap.get(face.i));

                    if (!vertexIndexMap.containsKey(face.j))
                    {
                        vertexIndexMap.put(face.j, currentVertexIndex);
                        currentVertexIndex += 1;
                    }
                    Useful.littleEndianWrite(bostream, vertexIndexMap.get(face.j));

                    if (!vertexIndexMap.containsKey(face.k))
                    {
                        vertexIndexMap.put(face.k, currentVertexIndex);
                        currentVertexIndex += 1;
                    }
                    Useful.littleEndianWrite(bostream, vertexIndexMap.get(face.k));

                }

                if (bostream.size() > DEFAULT_BYTEOSBUF_SIZE - DEFAULT_BYTEOSBUF_TAIL)
                {
                    fostream.write(bostream.toByteArray());
                    bostream.reset();
                }
            }
            if (bostream.size() > 0) fostream.write(bostream.toByteArray());
            return new GatheringResult(numFacesInSubnode, vertexIndexMap);
        }
    }


    private static class GatheringResult
    {
        public final int numFaces;
        public final int numVertices;
        public final LinkedHashMap<Integer, Integer> vertexIndexMap;

        public GatheringResult(int numFacesInSubnode, LinkedHashMap<Integer, Integer> vertexIndexMap)
        {
            this.numFaces = numFacesInSubnode;
            this.numVertices = vertexIndexMap.size();
            this.vertexIndexMap = vertexIndexMap;
        }
    }
}
