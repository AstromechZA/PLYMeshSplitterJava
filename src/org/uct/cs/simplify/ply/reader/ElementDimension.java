package org.uct.cs.simplify.ply.reader;

import org.uct.cs.simplify.util.Pair;

public class ElementDimension extends Pair<Long, Long>
{

    public ElementDimension(Long f, Long s)
    {
        super(f, s);
    }

    public Long getOffset()
    {
        return this.getFirst();
    }

    public Long getLength()
    {
        return this.getSecond();
    }

}
