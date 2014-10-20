package org.uct.cs.simplify.splitter;

import gnu.trove.iterator.TIntIterator;
import org.uct.cs.simplify.filebuilder.PHFNode;
import org.uct.cs.simplify.model.*;
import org.uct.cs.simplify.ply.header.PLYHeader;
import org.uct.cs.simplify.ply.reader.PLYReader;
import org.uct.cs.simplify.splitter.memberships.IMembershipBuilder;
import org.uct.cs.simplify.splitter.memberships.MembershipBuilderResult;
import org.uct.cs.simplify.util.*;

import java.io.*;
import java.nio.channels.FileChannel;
import java.util.ArrayList;

public class NodeSplitter
{
    private static final int DEFAULT_BYTEOSBUF_SIZE = 524288;
    private static final int DEFAULT_BYTEOSBUF_TAIL = 16;

    public static ArrayList<PHFNode> split(
        PHFNode parent,
        IMembershipBuilder membershipBuilder
    ) throws IOException
    {
        Outputter.info1f(
            "Splitting %s into %d subnodes%n", parent.getLinkedFile().getPath(), membershipBuilder.getSplitRatio()
        );
        // output object for subnodes

        // build reader object
        PLYReader reader = new PLYReader(parent.getLinkedFile());

        Outputter.info1ln("Calculating subnode memberships..");

        MembershipBuilderResult mr = membershipBuilder.build(reader, parent.getBoundingBox());

        Outputter.debugln("Subnode memberships created.");

        ArrayList<PHFNode> output = new ArrayList<>(mr.subNodes.size());
        for (int nodeID : mr.subNodes.keys())
        {
            Outputter.debugf("Creating subnode %d%n", nodeID);
            File tempFaceFile = TempFileManager.provide("faces");
            GatheringResult result = gatherVerticesAndWriteFaces(reader, mr.memberships, tempFaceFile, nodeID);
            if (result.numFaces > 0)
            {
                File subNodeFile = TempFileManager.provide("node", ".ply");

                writeSubnodePLYModel(reader, subNodeFile, tempFaceFile, result);

                PHFNode child = new PHFNode(mr.subNodes.get(nodeID), result.numVertices, result.numFaces, subNodeFile);
                child.setDepth(parent.getDepth() + 1);
                output.add(child);
                System.out.println(result.numFaces);
            }
            TempFileManager.release(tempFaceFile);
        }

        return output;
    }

    private static void writeSubnodePLYModel(PLYReader reader, File subNodeFile, File tempFaceFile, GatheringResult result) throws IOException
    {
        try (BufferedOutputStream fostream = new BufferedOutputStream(new FileOutputStream(subNodeFile)))
        {
            try (FastBufferedVertexReader vr = new FastBufferedVertexReader(reader))
            {
                Vertex v = new Vertex(0, 0, 0);
                VertexAttrMap vam = vr.getVam();
                PLYHeader newHeader = PLYHeader.constructHeader(result.numVertices, result.numFaces, vam);
                fostream.write((newHeader + "\n").getBytes());

                TIntIterator iter = result.vertexIndexMap.getKeyList().iterator();
                while (iter.hasNext())
                {
                    int i = iter.next();
                    if (i < vr.getIndex()) vr.reset();
                    vr.skipForwardTo(i);
                    vr.next(v);
                    v.writeToStream(fostream, vam);
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
        IntIntHashMapWithKeyList vertexIndexMap = new IntIntHashMapWithKeyList((int) (memberships.size() / Math.pow(2, memberships.getBits())));
        try (
            StreamingFaceReader faceReader = new FastBufferedFaceReader(reader);
            FileOutputStream fostream = new FileOutputStream(tempfile);
            ByteArrayOutputStream bostream = new ByteArrayOutputStream(DEFAULT_BYTEOSBUF_SIZE)
        )
        {
            Face face = new Face(0, 0, 0);
            int numVerticesOfFaceInSubnode;
            long numFacesInSubnode = 0;
            while (faceReader.hasNext())
            {
                faceReader.next(face);
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
        public final long numFaces;
        public final int numVertices;
        public final IntIntHashMapWithKeyList vertexIndexMap;

        public GatheringResult(long numFacesInSubnode, IntIntHashMapWithKeyList vertexIndexMap)
        {
            this.numFaces = numFacesInSubnode;
            this.numVertices = vertexIndexMap.size();
            this.vertexIndexMap = vertexIndexMap;
        }
    }
}
