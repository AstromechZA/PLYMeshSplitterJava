package org.uct.cs.simplify.splitter;

import org.uct.cs.simplify.file_builder.PackagedHierarchicalNode;
import org.uct.cs.simplify.ply.datatypes.DataType;
import org.uct.cs.simplify.ply.header.PLYHeader;
import org.uct.cs.simplify.ply.reader.*;
import org.uct.cs.simplify.splitter.memberships.IMembershipBuilder;
import org.uct.cs.simplify.splitter.memberships.MembershipBuilderResult;
import org.uct.cs.simplify.util.*;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public class HierarchicalSplitter
{
    private static final int DEFAULT_BYTEOSBUF_SIZE = 524288;
    private static final int DEFAULT_BYTEOSBUF_TAIL = 16;

    public static ArrayList<PackagedHierarchicalNode> split(
        PackagedHierarchicalNode parent,
        IMembershipBuilder membershipBuilder,
        File outputDir
    ) throws IOException
    {
        // output object for subnodes
        ArrayList<PackagedHierarchicalNode> output = new ArrayList<>();

        // calculate the base filename
        String processFileBase = Useful.getFilenameWithoutExt(parent.getLinkedFile().getName());

        // build reader object
        PLYReader reader = new PLYReader(parent.getLinkedFile());

        MembershipBuilderResult mr = membershipBuilder.build(reader, parent.getBoundingBox());
        for (Map.Entry<Integer, XBoundingBox> entry : mr.subNodes.entrySet())
        {
            int nodeID = entry.getKey();
            try (
                TempFile tempFaceFile = new TempFile(
                    outputDir,
                    String.format("%s_%s.temp", processFileBase, nodeID)
                )
            )
            {
                GatheringResult result = gatherVerticesAndWriteFaces(reader, mr.memberships, tempFaceFile, nodeID);
                if (result.numFaces > 0)
                {
                    File subNodeFile = new File(outputDir, String.format("%s_%s.ply", processFileBase, nodeID));

                    writeSubnodePLYModel(reader, subNodeFile, tempFaceFile, result);

                    output.add(
                        new PackagedHierarchicalNode(entry.getValue(), result.numVertices, result.numFaces, subNodeFile)
                    );
                }
            }
        }

        return output;
    }

    private static void writeSubnodePLYModel(
        PLYReader reader, File subNodeFile, TempFile tempFaceFile, GatheringResult result
    ) throws IOException
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
                ByteBuffer bb = ByteBuffer.wrap(new byte[3 * DataType.FLOAT.getByteSize()]);
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
        LinkedHashMap<Integer, Integer> vertexIndexMap = new LinkedHashMap<>();
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
                for (Integer i : face.getVertices())
                {
                    if (memberships.get(i) == subnode) numVerticesOfFaceInSubnode += 1;
                }

                if (numVerticesOfFaceInSubnode > 1)
                {
                    numFacesInSubnode++;
                    bostream.write((byte) face.getNumVertices());
                    for (int vertex_index : face.getVertices())
                    {
                        if (!vertexIndexMap.containsKey(vertex_index))
                        {
                            vertexIndexMap.put(vertex_index, currentVertexIndex);
                            currentVertexIndex += 1;
                        }
                        Useful.littleEndianWrite(bostream, vertexIndexMap.get(vertex_index));
                    }
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
