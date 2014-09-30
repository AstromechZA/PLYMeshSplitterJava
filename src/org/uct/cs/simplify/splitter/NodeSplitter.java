package org.uct.cs.simplify.splitter;

import org.uct.cs.simplify.filebuilder.PHFNode;
import org.uct.cs.simplify.model.Face;
import org.uct.cs.simplify.model.MemoryMappedFaceReader;
import org.uct.cs.simplify.model.MemoryMappedVertexReader;
import org.uct.cs.simplify.model.VertexAttrMap;
import org.uct.cs.simplify.ply.header.PLYHeader;
import org.uct.cs.simplify.ply.reader.PLYReader;
import org.uct.cs.simplify.splitter.memberships.IMembershipBuilder;
import org.uct.cs.simplify.splitter.memberships.MembershipBuilderResult;
import org.uct.cs.simplify.util.CompactBitArray;
import org.uct.cs.simplify.util.TempFileManager;
import org.uct.cs.simplify.util.Useful;

import java.io.*;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.LinkedHashMap;

public class NodeSplitter
{
    private static final int DEFAULT_BYTEOSBUF_SIZE = 524288;
    private static final int DEFAULT_BYTEOSBUF_TAIL = 16;

    public static ArrayList<PHFNode> split(
        PHFNode parent,
        IMembershipBuilder membershipBuilder
    ) throws IOException
    {
        System.out.printf(
            "Splitting %s into %d subnodes%n", parent.getLinkedFile().getPath(), membershipBuilder.getSplitRatio()
        );
        // output object for subnodes

        // build reader object
        PLYReader reader = new PLYReader(parent.getLinkedFile());

        System.out.println("Calculating subnode memberships..");

        MembershipBuilderResult mr = membershipBuilder.build(reader, parent.getBoundingBox());

        System.out.println("Subnode memberships created.");

        ArrayList<PHFNode> output = new ArrayList<>(mr.subNodes.size());
        for (int nodeID : mr.subNodes.keys())
        {
            System.out.printf("Creating subnode %d%n", nodeID);
            File tempFaceFile = TempFileManager.provide("faces");
            GatheringResult result = gatherVerticesAndWriteFaces(reader, mr.memberships, tempFaceFile, nodeID);
            if (result.numFaces > 0)
            {
                File subNodeFile = TempFileManager.provide("node", ".ply");

                writeSubnodePLYModel(reader, subNodeFile, tempFaceFile, result);

                PHFNode child = new PHFNode(mr.subNodes.get(nodeID), result.numVertices, result.numFaces, subNodeFile);
                child.setDepth(parent.getDepth() + 1);
                output.add(child);
            }
            TempFileManager.release(tempFaceFile);
        }

        return output;
    }

    private static void writeSubnodePLYModel(PLYReader reader, File subNodeFile, File tempFaceFile, GatheringResult result) throws IOException
    {
        try (BufferedOutputStream fostream = new BufferedOutputStream(new FileOutputStream(subNodeFile)))
        {
            try (MemoryMappedVertexReader vr = new MemoryMappedVertexReader(reader))
            {
                VertexAttrMap vam = vr.getVam();
                PLYHeader newHeader = PLYHeader.constructHeader(result.numVertices, result.numFaces, vam);
                fostream.write((newHeader + "\n").getBytes());

                for (int i : result.vertexIndexMap.keySet())
                {
                    vr.get(i).writeToStream(fostream, vam);
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
