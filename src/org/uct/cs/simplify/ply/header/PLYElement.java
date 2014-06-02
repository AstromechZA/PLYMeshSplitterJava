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
    private String name;
    private int count;
    private List<PLYPropertyBase> properties;

    public PLYElement(String name, int count)
    {
        this.name = name;
        this.count = count;

        properties = new ArrayList<>();
    }

    public static PLYElement FromString(String s)
    {
        s = s.trim();
        String[] parts = s.split(" ");
        return new PLYElement(parts[ 1 ], Integer.parseInt(parts[ 2 ]));
    }

    public void addProperty(PLYPropertyBase p)
    {
        properties.add(p);
    }

    public String getName()
    {
        return name;
    }

    public int getCount()
    {
        return count;
    }

    public List<PLYPropertyBase> getProperties()
    {
        return properties;
    }

    @Override
    public String toString()
    {
        return "PLYElement(" + name + ")[" + count + "]";
    }

}

