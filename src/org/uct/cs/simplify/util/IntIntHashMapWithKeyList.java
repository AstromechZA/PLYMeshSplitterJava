package org.uct.cs.simplify.util;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;

public class IntIntHashMapWithKeyList extends TIntIntHashMap implements TIntIntMap
{
    private final TIntArrayList keyList;

    public IntIntHashMapWithKeyList(int initialCapacity)
    {
        super(initialCapacity);
        keyList = new TIntArrayList(initialCapacity);
    }

    @Override
    public int put(int key, int value)
    {
        int r = super.put(key, value);
        keyList.add(key);
        return r;
    }

    public TIntArrayList getKeyList()
    {
        return keyList;
    }
}
