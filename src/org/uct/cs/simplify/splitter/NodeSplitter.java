package org.uct.cs.simplify.splitter;

import it.unimi.dsi.fastutil.io.FastBufferedOutputStream;
import org.uct.cs.simplify.filebuilder.PackagedHierarchicalNode;
import org.uct.cs.simplify.model.*;
import org.uct.cs.simplify.ply.header.PLYHeader;
import org.uct.cs.simplify.ply.reader.PLYReader;
import org.uct.cs.simplify.splitter.memberships.IMembershipBuilder;
import org.uct.cs.simplify.splitter.memberships.MembershipBuilderResult;
import org.uct.cs.simplify.util.*;

import java.io.*;
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
        System.out.printf(
            "Splitting %s into %d subnodes%n", parent.getLinkedFile().getPath(), membershipBuilder.getSplitRatio()
        );
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
        try (FastBufferedOutputStream fostream = new FastBufferedOutputStream(new FileOutputStream(subNodeFile)))
        {
            try (MemoryMappedVertexReader vr = new MemoryMappedVertexReader(reader))
            {
                Vertex v;

                VertexAttrMap vam = vr.getVam();
                PLYHeader newHeader = PLYHeader.constructHeader(result.numVertices, result.numFaces, vam);
                fostream.write((newHeader + "\n").getBytes());

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

                        Useful.writeFloatLE(fostream, v.x);
                        Useful.writeFloatLE(fostream, v.y);
                        Useful.writeFloatLE(fostream, v.z);

                        if (vam.hasColour)
                        {
                            fostream.write(v.r);
                            fostream.write(v.g);
                            fostream.write(v.b);
                        }

                        if (vam.hasAlpha)
                        {
                            fostream.write(v.a);
                        }
                    }
                }
            }
        }

        try (
            FileChannel fcIN = new FileInputStream(tempFaceFile).getChannel();
            FileChannel fcOUT = new FileOutputStream(subNodeFile, true).getChannel()
        )
        {
            fcIN.transferTo(0, fcIN.size(), fcOUT);
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
            ByteArrayOutputStream bostream = new ByteArrayOutputStream(DEFAULT_BYTEOSBUF_SIZE);
            ProgressBar pb = new ProgressBar("Gathering Vertices & Writing Faces", faceReader.getCount())
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
                    Useful.writeIntLE(bostream, vertexIndexMap.get(face.i));

                    if (!vertexIndexMap.containsKey(face.j))
                    {
                        vertexIndexMap.put(face.j, currentVertexIndex);
                        currentVertexIndex += 1;
                    }
                    Useful.writeIntLE(bostream, vertexIndexMap.get(face.j));

                    if (!vertexIndexMap.containsKey(face.k))
                    {
                        vertexIndexMap.put(face.k, currentVertexIndex);
                        currentVertexIndex += 1;
                    }
                    Useful.writeIntLE(bostream, vertexIndexMap.get(face.k));

                }

                if (bostream.size() > DEFAULT_BYTEOSBUF_SIZE - DEFAULT_BYTEOSBUF_TAIL)
                {
                    fostream.write(bostream.toByteArray());
                    bostream.reset();
                }

                pb.tick();
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
