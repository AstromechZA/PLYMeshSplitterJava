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
