package org.uct.cs.simplify.splitter;

import org.uct.cs.simplify.file_builder.PackagedHierarchicalNode;
import org.uct.cs.simplify.ply.reader.Face;
import org.uct.cs.simplify.ply.reader.MemoryMappedFaceReader;
import org.uct.cs.simplify.ply.reader.MemoryMappedVertexReader;
import org.uct.cs.simplify.ply.reader.PLYReader;
import org.uct.cs.simplify.util.CompactBitArray;
import org.uct.cs.simplify.util.TempFile;
import org.uct.cs.simplify.util.Useful;
import org.uct.cs.simplify.util.XBoundingBox;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;

public class KDTreeSplitter implements ISplitter
{
    private static final int DEFAULT_BYTEOSBUF_SIZE = 524288;
    private static final int DEFAULT_BYTEOSBUF_TAIL = 16;

    @Override
    public ArrayList<PackagedHierarchicalNode> split(PackagedHierarchicalNode parent, File outputDir) throws IOException
    {
        // output object for subnodes
        ArrayList<PackagedHierarchicalNode> output = new ArrayList<>();

        // calculate the base filename
        String processFileBase = Useful.getFilenameWithoutExt(parent.getLinkedFile().getName());

        // build reader object
        PLYReader reader = new PLYReader(parent.getLinkedFile());

        // calculate subnode memberships
        CompactBitArray memberships = getSubnodeMemberships(reader, parent.getBoundingBox());

        int[] subnodes = new int[] { 0, 1 };
        for (int subnode : subnodes)
        {
            try (
                TempFile temporaryFaceFile = new TempFile(
                    outputDir, String.format("%s_%s.temp", processFileBase, subnode)
                )
            )
            {

                GatheringResult result = gatherVerticesAndWriteFaces(reader, memberships, temporaryFaceFile, subnode);
                if (result.numFaces > 0)
                {
                    File subNodeFile = new File(outputDir, String.format("%s_%s.ply", processFileBase, subnode));

                    // TODO 

                }
            }
        }

        return output;
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
            ByteArrayOutputStream bostream = new ByteArrayOutputStream(DEFAULT_BYTEOSBUF_SIZE);
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
