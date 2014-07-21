package org.uct.cs.simplify.ply.header;

import java.util.ArrayList;
import java.util.List;

/**
 * PLYElement
 * <p>
 * An element in a PLYHeader indicates an object name and a count. eg: "element vertex 30000"
 */
public class PLYElement
{
    private final String name;
    private final int count;
    private final List<PLYPropertyBase> properties;
    private Integer itemSize;

    public PLYElement(String name, int count)
    {
        this.name = name;
        this.count = count;
        this.itemSize = 0;
        this.properties = new ArrayList<>();
    }

    public static PLYElement FromString(String s)
    {
        s = s.trim();
        String[] parts = s.split(" ");
        return new PLYElement(parts[ 1 ], Integer.parseInt(parts[ 2 ]));
    }

    public void addProperty(PLYPropertyBase p)
    {
        this.properties.add(p);
        if (this.itemSize != null)
        {
            if (p instanceof PLYListProperty)
            {
                this.itemSize = null;
            }
            else if (p instanceof PLYProperty)
            {
                this.itemSize += ((PLYProperty) p).getTypeReader().bytesAtATime();
            }
        }
    }

    public String getName()
    {
        return this.name;
    }

    public int getCount()
    {
        return this.count;
    }

    public Integer getItemSize()
    {
        return this.itemSize;
    }

    public List<PLYPropertyBase> getProperties()
    {
        return this.properties;
    }

    @Override
    public String toString()
    {
        return "PLYElement(" + this.name + ")[" + this.count + "]";
    }

}

