package org.uct.cs.simplify.file_builder;

import org.uct.cs.simplify.ply.header.PLYHeader;
import org.uct.cs.simplify.util.XBoundingBox;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class PackagedHierarchicalFile
{
    private ArrayList<HierarchyNode> nodeList;

    public PackagedHierarchicalFile()
    {
        this.nodeList = new ArrayList<>();
    }

    public HierarchyNode add(Integer parentID, File linkedFile)
    {
        throw new NotImplementedException();
    }

    public HierarchyNode add(Integer parentID, File linkedFile, XBoundingBox bb) throws IOException
    {
        PLYHeader header = new PLYHeader(linkedFile);

        int numV = header.getElement("vertex").getCount();
        int numF = header.getElement("face").getCount();

        int newID = this.nodeList.size();

        if (parentID == null)
        {
            if (newID > 0) throw new IllegalArgumentException("Hierarchical file already has root nodes!");
            HierarchyNode newNode = new HierarchyNode(newID, null, bb, numV, numF, linkedFile);
            this.nodeList.add(newNode);
            return newNode;
        }
        else
        {
            HierarchyNode parent = this.nodeList.get(parentID);
            HierarchyNode newNode = new HierarchyNode(newID, parent, bb, numV, numF, linkedFile);
            this.nodeList.add(newNode);
            parent.addChild(newNode);
            return newNode;
        }
    }

    public String asJSON()
    {
        return this.asJSON(false);
    }

    public String asJSON(boolean indented)
    {
        StringBuilder sb = new StringBuilder();
        boolean isFirst = true;

        String sep1 = (indented) ? "\n\t" : "";
        String sep2 = (indented) ? "\n\t\t" : "";

        sb.append("[");

        for (HierarchyNode n : this.nodeList)
        {
            if (!isFirst) sb.append(",");

            sb.append(sep1).append("{");
            sb.append(sep2).append(String.format("\"id\":%d,", n.getID()));
            sb.append(sep2).append(String.format("\"parent_id\":%d,", (n.getParent() == null) ? null : n.getParent().getID()));
            sb.append(sep2).append(String.format("\"num_faces\":%d,", n.getNumFaces()));
            sb.append(sep2).append(String.format("\"num_vertices\":%d,", n.getNumVertices()));
            sb.append(sep2).append(String.format("\"block_offset\":%d,", 0));
            sb.append(sep2).append(String.format("\"block_length\":%d,", 0));
            sb.append(sep2).append(String.format("\"min_x\":%f,", n.getBoundingBox().getMinX()));
            sb.append(sep2).append(String.format("\"min_y\":%f,", n.getBoundingBox().getMinY()));
            sb.append(sep2).append(String.format("\"min_z\":%f,", n.getBoundingBox().getMinZ()));
            sb.append(sep2).append(String.format("\"max_x\":%f,", n.getBoundingBox().getMaxX()));
            sb.append(sep2).append(String.format("\"max_y\":%f,", n.getBoundingBox().getMaxY()));
            sb.append(sep2).append(String.format("\"max_z\":%f", n.getBoundingBox().getMaxZ()));
            sb.append(sep1).append("}");

            isFirst = false;
        }
        if (indented) sb.append("\n");
        sb.append("]");
        return sb.toString();
    }

    public static class HierarchyNode
    {
        private final int id;
        private final int numVertices;
        private final int numFaces;
        private final XBoundingBox boundingBox;
        private final File linkedFile;

        private HierarchyNode parent;
        private ArrayList<HierarchyNode> children;

        public HierarchyNode(int newID, HierarchyNode parent, XBoundingBox bb, int numV, int numF, File linkedFile)
        {
            this.id = newID;
            this.numFaces = numF;
            this.numVertices = numV;
            this.boundingBox = bb;
            this.linkedFile = linkedFile;

            this.parent = parent;
            this.children = new ArrayList<>();
        }

        public int addChild(HierarchyNode newNode)
        {
            return this.id;
        }

        public HierarchyNode getParent()
        {
            return this.parent;
        }

        public ArrayList<HierarchyNode> getChildren()
        {
            return this.children;
        }

        public int getID()
        {
            return id;
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
    }
}
