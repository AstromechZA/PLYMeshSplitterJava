package org.uct.cs.simplify.splitter;

import javafx.geometry.Point3D;
import org.uct.cs.simplify.file_builder.PackagedHierarchicalFile;
import org.uct.cs.simplify.file_builder.PackagedHierarchicalNode;
import org.uct.cs.simplify.ply.datatypes.DataType;
import org.uct.cs.simplify.ply.header.PLYHeader;
import org.uct.cs.simplify.ply.reader.*;
import org.uct.cs.simplify.ply.utilities.BoundsFinder;
import org.uct.cs.simplify.ply.utilities.OctetFinder;
import org.uct.cs.simplify.util.ProgressBar;
import org.uct.cs.simplify.util.TempFile;
import org.uct.cs.simplify.util.Useful;
import org.uct.cs.simplify.util.XBoundingBox;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.ArrayDeque;
import java.util.LinkedHashMap;
import java.util.Map;

public class OctreeSplitter
{

    private static final int DEFAULT_BYTEOSBUF_SIZE = 524288;
    private static final int DEFAULT_BYTEOSBUF_TAIL = 16;
    private static final int DEFAULT_MODEL_SIZE = 1024;
    private static final int DEFAULT_MODEL_SIZE_H = DEFAULT_MODEL_SIZE / 2;
    private static final int DEFAULT_CUTOFF_VERTEX_COUNT = 200_000;

    private final File outputDir;
    private final File inputFile;
    private final XBoundingBox boundingBox;

    public OctreeSplitter(File inputFile, File outputDir) throws IOException
    {
        this.inputFile = inputFile;
        this.outputDir = outputDir;
        this.boundingBox = this.prepare();
    }

    private XBoundingBox prepare() throws IOException
    {
        return BoundsFinder.getBoundingBox(new ImprovedPLYReader(new PLYHeader(this.inputFile)));
    }

    public PackagedHierarchicalFile run() throws IOException
    {
        // empty queue for processing
        ArrayDeque<PackagedHierarchicalNode> processQueue = new ArrayDeque<>();

        PackagedHierarchicalFile outputHierarchy = new PackagedHierarchicalFile();
        PackagedHierarchicalNode root = outputHierarchy.add(null, this.inputFile, this.boundingBox);

        // add first element
        processQueue.add(root);
        while (!processQueue.isEmpty())
        {
            PackagedHierarchicalNode currentNode = processQueue.removeFirst();

            String processFileBase = Useful.getFilenameWithoutExt(currentNode.getLinkedFile().getName());

            // now switch to rescaled version
            ImprovedPLYReader reader = new ImprovedPLYReader(new PLYHeader(currentNode.getLinkedFile()));

            OctetFinder.Octet[] memberships = calculateVertexMemberships(reader, currentNode.getBoundingBox().getCenter());
            for (OctetFinder.Octet currentOctet : OctetFinder.Octet.values())
            {
                String tempfilepath = String.format("%s_%s.temp", processFileBase, currentOctet);
                try (TempFile temporaryFaceFile = new TempFile(this.outputDir, tempfilepath))
                {
                    LinkedHashMap<Integer, Integer> vertexMap = new LinkedHashMap<>(currentNode.getNumVertices() / 8);
                    int num_faces = gatherOctetFaces(reader, memberships, currentOctet, temporaryFaceFile, vertexMap);
                    if (num_faces > 0)
                    {
                        File octetFile = new File(this.outputDir, String.format("%s_%s.ply", processFileBase, currentOctet));

                        writeOctetPLYModel(reader, currentOctet, temporaryFaceFile, vertexMap, num_faces, octetFile);

                        PackagedHierarchicalNode n = outputHierarchy.add(currentNode.getID(), octetFile, currentNode.getBoundingBox().getSubBB(currentOctet));

                        if (n.getNumVertices() > DEFAULT_CUTOFF_VERTEX_COUNT) processQueue.addLast(n);
                    }
                }
            }
        }
        return outputHierarchy;
    }

    private static void writeOctetPLYModel(
        ImprovedPLYReader reader,
        OctetFinder.Octet currentOctet,
        File octetFaceFile,
        LinkedHashMap<Integer, Integer> vertexMap,
        int numFaces,
        File octetFile
    ) throws IOException
    {
        PLYHeader newHeader = PLYHeader.constructBasicHeader(vertexMap.size(), numFaces);

        try (FileOutputStream fostream = new FileOutputStream(octetFile))
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
                        String.format("%s: Writing Vertices", currentOctet),
                        vertexMap.size()
                    )
                )
                {
                    for (int i : vertexMap.keySet())
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

            try (FileChannel fc = new FileInputStream(octetFaceFile).getChannel())
            {
                fostream.getChannel().transferFrom(fc, fostream.getChannel().position(), fc.size());
            }
        }
    }

    private static int gatherOctetFaces(
        ImprovedPLYReader reader,
        OctetFinder.Octet[] memberships,
        OctetFinder.Octet current,
        File octetFaceFile,
        Map<Integer, Integer> vertexMap
    ) throws IOException
    {
        int num_faces_in_octet = 0;
        try (
            ProgressBar progress = new ProgressBar(
                String.format("%s : Scanning & Writing Faces", current),
                reader.getHeader().getElement("face").getCount()
            );
            MemoryMappedFaceReader faceReader = new MemoryMappedFaceReader(reader);
            FileOutputStream fostream = new FileOutputStream(octetFaceFile);
            ByteArrayOutputStream bostream = new ByteArrayOutputStream(DEFAULT_BYTEOSBUF_SIZE)
        )
        {
            Face face;
            int current_vertex_index = 0;

            while (faceReader.hasNext())
            {
                progress.tick();
                face = faceReader.next();

                if (face.getVertices().stream().anyMatch(v -> memberships[ v ] == current))
                {
                    num_faces_in_octet += 1;
                    bostream.write((byte) face.getNumVertices());
                    for (int vertex_index : face.getVertices())
                    {
                        if (!vertexMap.containsKey(vertex_index))
                        {
                            vertexMap.put(vertex_index, current_vertex_index);
                            current_vertex_index += 1;
                        }
                        littleEndianWrite(bostream, vertexMap.get(vertex_index));
                    }
                }
                if (bostream.size() > DEFAULT_BYTEOSBUF_SIZE - DEFAULT_BYTEOSBUF_TAIL)
                {
                    fostream.write(bostream.toByteArray());
                    bostream.reset();
                }
            }
            if (bostream.size() > 0) fostream.write(bostream.toByteArray());
        }
        return num_faces_in_octet;
    }

    private static void littleEndianWrite(ByteArrayOutputStream stream, int i)
    {
        stream.write((i) & 0xFF);
        stream.write((i >> 8) & 0xFF);
        stream.write((i >> (8 * 2)) & 0xFF);
        stream.write((i >> (8 * 3)) & 0xFF);

    }

    private static OctetFinder.Octet[] calculateVertexMemberships(
        ImprovedPLYReader reader, Point3D splitPoint
    ) throws IOException
    {
        OctetFinder ofinder = new OctetFinder(splitPoint);

        try (
            MemoryMappedVertexReader vr = new MemoryMappedVertexReader(reader);
            ProgressBar pb = new ProgressBar("Calculating Memberships", vr.getCount())
        )
        {
            int c = vr.getCount();
            OctetFinder.Octet[] memberships = new OctetFinder.Octet[ c ];
            Vertex v;
            for (int i = 0; i < c; i++)
            {
                pb.tick();
                v = vr.get(i);
                memberships[ i ] = ofinder.getOctet(v.x, v.y, v.z);
            }
            return memberships;
        }
    }


}
