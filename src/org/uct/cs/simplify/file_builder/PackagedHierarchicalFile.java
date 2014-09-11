package org.uct.cs.simplify.file_builder;

import org.uct.cs.simplify.ply.header.PLYHeader;
import org.uct.cs.simplify.util.XBoundingBox;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class PackagedHierarchicalFile
{
    private ArrayList<PackagedHierarchicalNode> nodeList = new ArrayList<>();
    ;
    private PackagedHierarchicalNode root = null;

    public PackagedHierarchicalNode add(Integer parentID, File linkedFile)
    {
        throw new NotImplementedException();
    }

    public PackagedHierarchicalNode add(Integer parentID, File linkedFile, XBoundingBox bb) throws IOException
    {
        PLYHeader header = new PLYHeader(linkedFile);

        int numV = header.getElement("vertex").getCount();
        int numF = header.getElement("face").getCount();

        int newID = this.nodeList.size();
        PackagedHierarchicalNode newNode = new PackagedHierarchicalNode(newID, null, bb, numV, numF, linkedFile);

        if (parentID == null)
        {
            if (this.root != null) throw new IllegalArgumentException("Hierarchical file already has root nodes!");
            this.root = newNode;
        }
        else
        {
            PackagedHierarchicalNode parent = this.nodeList.get(parentID);
            parent.addChild(newNode);
        }
        this.nodeList.add(newNode);
        return newNode;
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

        for (PackagedHierarchicalNode n : this.nodeList)
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
}
