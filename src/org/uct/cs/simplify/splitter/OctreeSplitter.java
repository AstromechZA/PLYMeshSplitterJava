package org.uct.cs.simplify.splitter;

import javafx.geometry.Point3D;
import org.uct.cs.simplify.file_builder.PackagedHierarchicalNode;
import org.uct.cs.simplify.ply.datatypes.DataType;
import org.uct.cs.simplify.ply.header.PLYHeader;
import org.uct.cs.simplify.ply.reader.*;
import org.uct.cs.simplify.ply.utilities.OctetFinder;
import org.uct.cs.simplify.util.ProgressBar;
import org.uct.cs.simplify.util.TempFile;
import org.uct.cs.simplify.util.Useful;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public class OctreeSplitter implements ISplitter
{
    private static final int DEFAULT_BYTEOSBUF_SIZE = 524288;
    private static final int DEFAULT_BYTEOSBUF_TAIL = 16;

    @Override
    public ArrayList<PackagedHierarchicalNode> split(PackagedHierarchicalNode parent, File outputDir) throws IOException
    {
        ArrayList<PackagedHierarchicalNode> output = new ArrayList<>();

        String processFileBase = Useful.getFilenameWithoutExt(parent.getLinkedFile().getName());

        ImprovedPLYReader reader = new ImprovedPLYReader(new PLYHeader(parent.getLinkedFile()));

        OctetFinder.Octet[] memberships = calculateVertexMemberships(reader, parent.getBoundingBox().getCenter());
        for (OctetFinder.Octet currentOctet : OctetFinder.Octet.values())
        {
            String tempfilepath = String.format("%s_%s.temp", processFileBase, currentOctet);
            try (TempFile temporaryFaceFile = new TempFile(outputDir, tempfilepath))
            {
                LinkedHashMap<Integer, Integer> vertexMap = new LinkedHashMap<>(parent.getNumVertices() / 8);
                int num_faces = gatherOctetFaces(reader, memberships, currentOctet, temporaryFaceFile, vertexMap);
                if (num_faces > 0)
                {
                    File octetFile = new File(outputDir, String.format("%s_%s.ply", processFileBase, currentOctet));

                    writeOctetPLYModel(reader, currentOctet, temporaryFaceFile, vertexMap, num_faces, octetFile);

                    output.add(new PackagedHierarchicalNode(parent.getBoundingBox().getSubBB(currentOctet), vertexMap.size(), num_faces, octetFile));
                }
            }
        }
        return output;
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
