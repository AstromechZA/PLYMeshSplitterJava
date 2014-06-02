package org.uct.cs.simplify.ply;

import java.util.ArrayList;

/**
 * PLYElement
 * <p>
 * An element in a PLYHeader indicates an object name and a count. eg: "element vertex 30000"
 */
public class PLYElement
{
    private String name;
    private int count;
    private Integer fileByteOffset;
    private ArrayList<PLYProperty> properties;
    private Integer lengthInBytes;

    public PLYElement(String name, int count)
    {
        this.name = name;
        this.count = count;

        fileByteOffset = null; // unknown, can be set
        lengthInBytes = 0;
        properties = new ArrayList<>();
    }

    public static PLYElement FromString(String s)
    {
        s = s.trim();
        String[] parts = s.split(" ");
        return new PLYElement(parts[ 1 ], Integer.parseInt(parts[ 2 ]));
    }

    public void addProperty(PLYProperty p)
    {
        properties.add(p);
        recalculateLength();
    }

    private void recalculateLength()
    {
        int total = 0;
        for (PLYProperty p : properties)
        {
            if (p.isList())
            {
                lengthInBytes = null;
                return;
            }
            else
            {
                total += p.getByteLength();
            }
        }
        lengthInBytes = total * count;
    }

    public String getName()
    {
        return name;
    }

    public Integer getFileByteOffset()
    {
        return fileByteOffset;
    }

    public void setFileByteOffset(Integer fileByteOffset)
    {
        this.fileByteOffset = fileByteOffset;
    }

    public int getCount()
    {
        return count;
    }

    public ArrayList<PLYProperty> getProperties()
    {
        return properties;
    }

    public Integer getLengthInBytes()
    {
        return lengthInBytes;
    }

    @Override
    public String toString()
    {
        return "PLYElement(" + name + ")[" + count + "]" + ((fileByteOffset != null) ? " @ " + fileByteOffset : "");
    }

}

