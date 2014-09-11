package org.uct.cs.simplify.file_builder;

import org.uct.cs.simplify.util.XBoundingBox;

import java.io.File;
import java.util.ArrayList;

public class PackagedHierarchicalNode
{
    private final int id;
    private final int numVertices;
    private final int numFaces;
    private final XBoundingBox boundingBox;
    private final File linkedFile;

    private PackagedHierarchicalNode parent;
    private ArrayList<PackagedHierarchicalNode> children;

    public PackagedHierarchicalNode(int newID, PackagedHierarchicalNode parent, XBoundingBox bb, int numV, int numF, File linkedFile)
    {
        this.id = newID;
        this.numFaces = numF;
        this.numVertices = numV;
        this.boundingBox = bb;
        this.linkedFile = linkedFile;

        this.parent = parent;
        this.children = new ArrayList<>();
    }

    public int addChild(PackagedHierarchicalNode newNode)
    {
        return this.id;
    }

    public PackagedHierarchicalNode getParent()
    {
        return this.parent;
    }

    public ArrayList<PackagedHierarchicalNode> getChildren()
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
