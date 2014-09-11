package org.uct.cs.simplify.file_builder;

import org.uct.cs.simplify.ply.header.PLYHeader;
import org.uct.cs.simplify.ply.reader.ImprovedPLYReader;
import org.uct.cs.simplify.ply.utilities.BoundsFinder;
import org.uct.cs.simplify.util.XBoundingBox;

import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;

public class PackagedHierarchicalNode
{
    private final int numVertices;
    private final int numFaces;
    private final XBoundingBox boundingBox;
    private final File linkedFile;

    private PackagedHierarchicalNode parent;
    private ArrayList<PackagedHierarchicalNode> children;

    public PackagedHierarchicalNode(XBoundingBox bb, int numV, int numF, File linkedFile)
    {
        this.numFaces = numF;
        this.numVertices = numV;
        this.boundingBox = bb;
        this.linkedFile = linkedFile;
        this.parent = null;
        this.children = new ArrayList<>();
    }

    public PackagedHierarchicalNode(File linkedFile) throws IOException
    {
        ImprovedPLYReader r = new ImprovedPLYReader(new PLYHeader(linkedFile));
        this.numFaces = r.getHeader().getElement("face").getCount();
        this.numVertices = r.getHeader().getElement("vertex").getCount();
        this.boundingBox = BoundsFinder.getBoundingBox(r);
        this.linkedFile = linkedFile;
        this.parent = null;
        this.children = new ArrayList<>();
    }

    public void addChild(PackagedHierarchicalNode node)
    {
        this.children.add(node);
        node.setParent(this);
    }

    public ArrayList<PackagedHierarchicalNode> getChildren()
    {
        return this.children;
    }

    public int getNumVertices()
    {
        return numVertices;
    }

    public int getNumFaces()
    {
        return numFaces;
    }

    public XBoundingBox getBoundingBox()
    {
        return boundingBox;
    }

    public File getLinkedFile()
    {
        return linkedFile;
    }

    public void setParent(PackagedHierarchicalNode parent)
    {
        this.parent = parent;
    }

    public String toJSON(int id, Integer parentID)
    {
        return "{" +
            String.format("\"id\":%d,", id) +
            String.format("\"parent_id\":%s,", (parentID == null) ? "null" : parentID) +
            String.format("\"num_faces\":%d,", this.numFaces) +
            String.format("\"num_vertices\":%d,", this.numVertices) +
            String.format("\"block_offset\":%d,", 0) +
            String.format("\"block_length\":%d,", 0) +
            String.format("\"min_x\":%f,", this.boundingBox.getMinX()) +
            String.format("\"min_y\":%f,", this.boundingBox.getMinY()) +
            String.format("\"min_z\":%f,", this.boundingBox.getMinZ()) +
            String.format("\"max_x\":%f,", this.boundingBox.getMaxX()) +
            String.format("\"max_y\":%f,", this.boundingBox.getMaxY()) +
            String.format("\"max_z\":%f", this.boundingBox.getMaxZ()) +
            "}";
    }

    public static String buildJSONHierarchy(PackagedHierarchicalNode root)
    {
        int id = 0;
        HashMap<PackagedHierarchicalNode, Integer> nodeIds = new HashMap<>();
        ArrayDeque<PackagedHierarchicalNode> processQueue = new ArrayDeque<>();
        processQueue.add(root);
        while (!processQueue.isEmpty())
        {
            PackagedHierarchicalNode current = processQueue.removeFirst();
            nodeIds.put(current, id++);
            processQueue.addAll(current.getChildren());
        }

        StringBuilder s = new StringBuilder("[");
        boolean first = true;
        for (PackagedHierarchicalNode node : nodeIds.keySet())
        {
            if (first)
                first = false;
            else
                s.append(",");
            s.append(node.toJSON(nodeIds.get(node), (node.parent == null) ? null : nodeIds.get(node.parent)));
        }
        return s + "]";
    }
}