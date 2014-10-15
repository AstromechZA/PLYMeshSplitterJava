package org.uct.cs.simplify.filebuilder;

import org.uct.cs.simplify.model.BoundsFinder;
import org.uct.cs.simplify.ply.reader.PLYReader;
import org.uct.cs.simplify.util.XBoundingBox;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class PHFNode
{
    private final ArrayList<PHFNode> children = new ArrayList<>(8);
    private final long numVertices;
    private final long numFaces;
    private final XBoundingBox boundingBox;
    private final File linkedFile;

    private PHFNode parent;
    private long blockOffset;
    private long blockLength;
    private int depth = -1;

    public PHFNode(XBoundingBox bb, long numV, long numF, File f)
    {
        this.numFaces = numF;
        this.numVertices = numV;
        this.boundingBox = bb;
        this.linkedFile = f;
    }

    public PHFNode(File f) throws IOException
    {
        PLYReader r = new PLYReader(f);
        this.numFaces = r.getHeader().getElement("face").getCount();
        this.numVertices = r.getHeader().getElement("vertex").getCount();
        this.boundingBox = BoundsFinder.getBoundingBox(r);
        this.linkedFile = f;
    }

    public PHFNode(File f, XBoundingBox bb) throws IOException
    {
        PLYReader r = new PLYReader(f);
        this.numFaces = r.getHeader().getElement("face").getCount();
        this.numVertices = r.getHeader().getElement("vertex").getCount();
        this.boundingBox = bb;
        this.linkedFile = f;
    }

    @SuppressWarnings("CollectionWithoutInitialCapacity")
    public static String buildJSONHierarchy(PHFNode root)
    {
        int id = 0;
        HashMap<PHFNode, Integer> nodeIds = new HashMap<>();
        for (PHFNode node : root.collectAllNodes())
        {
            nodeIds.put(node, id++);
        }

        StringBuilder s = new StringBuilder("[");
        boolean first = true;
        for (PHFNode node : nodeIds.keySet())
        {
            if (first)
                first = false;
            else
                s.append(',');
            s.append(node.toJSON(nodeIds.get(node), (node.parent == null) ? null : nodeIds.get(node.parent)));
        }
        return s + "]";
    }

    public void addChild(PHFNode node)
    {
        this.children.add(node);
        node.parent = this;
    }

    public List<PHFNode> getChildren()
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
            //String.format("\"file\":\"%s\",", this.linkedFile.getAbsolutePath().replace("\\", "\\\\")) +
            String.format("\"block_offset\":%d,", this.blockOffset) +
            String.format("\"block_length\":%d,", this.blockLength) +
            String.format("\"depth\":%d,", this.depth) +
            String.format("\"min_x\":%f,", this.boundingBox.getMinX()) +
            String.format("\"min_y\":%f,", this.boundingBox.getMinY()) +
            String.format("\"min_z\":%f,", this.boundingBox.getMinZ()) +
            String.format("\"max_x\":%f,", this.boundingBox.getMaxX()) +
            String.format("\"max_y\":%f,", this.boundingBox.getMaxY()) +
            String.format("\"max_z\":%f", this.boundingBox.getMaxZ()) +
            '}';
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

    public void addChildren(List<PHFNode> children)
    {
        children.forEach(this::addChild);
    }

    public List<PHFNode> collectAllNodes()
    {
        List<PHFNode> o = new ArrayList<>(100);

        Deque<PHFNode> q = new ArrayDeque<>(100);

        o.add(this);
        q.add(this);

        while (!q.isEmpty())
        {
            PHFNode n = q.removeFirst();
            o.addAll(n.children);
            q.addAll(n.children);
        }

        return o;
    }

    public int getDepthOfDeepestChild()
    {
        int md = this.depth;
        for (PHFNode child : children)
        {
            md = Math.max(md, child.getDepthOfDeepestChild());
        }
        return md;
    }

}
