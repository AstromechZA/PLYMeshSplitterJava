package org.uct.cs.simplify.filebuilder;

import org.uct.cs.simplify.model.BoundsFinder;
import org.uct.cs.simplify.ply.reader.PLYReader;
import org.uct.cs.simplify.util.XBoundingBox;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class PackagedHierarchicalNode
{
    private final ArrayList<PackagedHierarchicalNode> children;
    private long numVertices;
    private long numFaces;
    private XBoundingBox boundingBox;
    private File linkedFile;
    private PackagedHierarchicalNode parent;
    private long blockOffset, blockLength;
    private int depth;

    public PackagedHierarchicalNode(XBoundingBox bb, long numV, long numF, File f)
    {
        this.numFaces = numF;
        this.numVertices = numV;
        this.boundingBox = bb;
        this.linkedFile = f;
        this.parent = null;
        this.children = new ArrayList<>(8);
        this.blockOffset = 0;
        this.blockLength = 0;
        this.depth = 0;
    }

    public PackagedHierarchicalNode(File f) throws IOException
    {
        PLYReader r = new PLYReader(f);
        this.numFaces = r.getHeader().getElement("face").getCount();
        this.numVertices = r.getHeader().getElement("vertex").getCount();
        this.boundingBox = BoundsFinder.getBoundingBox(r);
        this.linkedFile = f;
        this.parent = null;
        this.children = new ArrayList<>(8);
        this.blockOffset = 0;
        this.blockLength = 0;
        this.depth = 0;
    }

    @SuppressWarnings("CollectionWithoutInitialCapacity")
    public static String buildJSONHierarchy(PackagedHierarchicalNode root)
    {
        int id = 0;
        HashMap<PackagedHierarchicalNode, Integer> nodeIds = new HashMap<>();
        for (PackagedHierarchicalNode node : root.collectAllNodes())
        {
            nodeIds.put(node, id++);
        }

        StringBuilder s = new StringBuilder("[");
        boolean first = true;
        for (PackagedHierarchicalNode node : nodeIds.keySet())
        {
            if (first)
                first = false;
            else
                s.append(',');
            s.append(node.toJSON(nodeIds.get(node), (node.parent == null) ? null : nodeIds.get(node.parent)));
        }
        return s + "]";
    }

    public void addChild(PackagedHierarchicalNode node)
    {
        this.children.add(node);
        node.parent = this;
    }

    public List<PackagedHierarchicalNode> getChildren()
    {
        return this.children;
    }

    public long getNumVertices()
    {
        return this.numVertices;
    }

    public long getNumFaces()
    {
        return this.numFaces;
    }

    public XBoundingBox getBoundingBox()
    {
        return this.boundingBox;
    }

    public void setBoundingBox(XBoundingBox boundingBox)
    {
        this.boundingBox = boundingBox;
    }

    public File getLinkedFile()
    {
        return this.linkedFile;
    }

    public void setBlockOffset(long blockOffset)
    {
        this.blockOffset = blockOffset;
    }

    public void setBlockLength(long blockLength)
    {
        this.blockLength = blockLength;
    }

    public String toJSON(int id, Integer parentID)
    {
        return '{' +
            String.format("\"id\":%d,", id) +
            String.format("\"parent_id\":%s,", (parentID == null) ? "null" : parentID) +
            String.format("\"num_faces\":%d,", this.numFaces) +
            String.format("\"num_vertices\":%d,", this.numVertices) +
            String.format("\"file\":\"%s\",", this.linkedFile.getAbsolutePath().replace("\\", "\\\\")) +
            String.format("\"block_offset\":%d,", this.blockOffset) +
            String.format("\"block_length\":%d,", this.blockLength) +
            String.format("\"min_x\":%f,", this.boundingBox.getMinX()) +
            String.format("\"min_y\":%f,", this.boundingBox.getMinY()) +
            String.format("\"min_z\":%f,", this.boundingBox.getMinZ()) +
            String.format("\"max_x\":%f,", this.boundingBox.getMaxX()) +
            String.format("\"max_y\":%f,", this.boundingBox.getMaxY()) +
            String.format("\"max_z\":%f", this.boundingBox.getMaxZ()) +
                '}';
    }

    public int countSelfPlusDescendants()
    {
        int c = 1;
        for (PackagedHierarchicalNode child : this.children)
        {
            c += child.countSelfPlusDescendants();
        }
        return c;
    }

    public int getDepth()
    {
        return this.depth;
    }

    public void setDepth(int depth)
    {
        this.depth = depth;
    }

    public boolean hasChildren()
    {
        return !this.children.isEmpty();
    }

    public void addChildren(List<PackagedHierarchicalNode> children)
    {
        children.forEach(this::addChild);
    }

    public List<PackagedHierarchicalNode> collectAllNodes()
    {
        List<PackagedHierarchicalNode> o = new ArrayList<>(100);

        Deque<PackagedHierarchicalNode> q = new ArrayDeque<>(100);

        o.add(this);
        q.add(this);

        while (!q.isEmpty())
        {
            PackagedHierarchicalNode n = q.removeFirst();
            o.addAll(n.children);
            q.addAll(n.children);
        }

        return o;
    }
}
