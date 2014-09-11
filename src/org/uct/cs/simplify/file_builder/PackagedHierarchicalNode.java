package org.uct.cs.simplify.file_builder;

import org.uct.cs.simplify.ply.header.PLYHeader;
import org.uct.cs.simplify.ply.reader.ImprovedPLYReader;
import org.uct.cs.simplify.ply.utilities.BoundsFinder;
import org.uct.cs.simplify.util.XBoundingBox;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class PackagedHierarchicalNode
{
    private final int numVertices;
    private final int numFaces;
    private final XBoundingBox boundingBox;
    private final File linkedFile;

    private ArrayList<PackagedHierarchicalNode> children;

    public PackagedHierarchicalNode(XBoundingBox bb, int numV, int numF, File linkedFile)
    {
        this.numFaces = numF;
        this.numVertices = numV;
        this.boundingBox = bb;
        this.linkedFile = linkedFile;
        this.children = new ArrayList<>();
    }

    public PackagedHierarchicalNode(File linkedFile) throws IOException
    {
        ImprovedPLYReader r = new ImprovedPLYReader(new PLYHeader(linkedFile));
        this.numFaces = r.getHeader().getElement("face").getCount();
        this.numVertices = r.getHeader().getElement("vertex").getCount();
        this.boundingBox = BoundsFinder.getBoundingBox(r);
        this.linkedFile = linkedFile;
        this.children = new ArrayList<>();
    }

    public void addChild(PackagedHierarchicalNode node)
    {
        this.children.add(node);
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
}
