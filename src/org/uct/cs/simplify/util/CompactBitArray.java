package org.uct.cs.simplify.util;

public class CompactBitArray
{
    private static final int INTEGER_SIZE = 32;

    private final int bits;
    private final long length;
    private final int blockLength;
    private final int blocksPerInt;
    private final int[] data;
    private final int bitMask;

    public CompactBitArray(int bits, long length)
    {
        // for compactness and performance reasons we only support blocks of size 1, 2, 4, 8, 16 bits. For this reason,
        // <bits> should be smaller than or equal to 8
        if (bits < 1 || bits > INTEGER_SIZE)
            throw new IllegalArgumentException("Bits argument must be between 1 and 16 (inclusive).");
        this.bits = bits;
        this.length = length;
        this.blockLength = (int) Math.pow(2, Math.ceil(Math.log(bits) / Math.log(2)));
        this.blocksPerInt = INTEGER_SIZE / this.blockLength;

        // now we setup
        int numBlocks = (int) Math.ceil(((long) this.blockLength * this.length) / (double) INTEGER_SIZE);
        this.data = new int[ numBlocks ];
        this.bitMask = (1 << this.bits) - 1;
    }

    public long size()
    {
        return this.length;
    }

    public int getBits()
    {
        return this.bits;
    }

    public int get(long i)
    {
        // first identify which index we need
        long blockIndex = i / this.blocksPerInt;
        if (blockIndex >= this.data.length) throw new IndexOutOfBoundsException();
        int block = this.data[ (int) blockIndex ];

        // get positional index
        int positionIndex = (int) (i % this.blocksPerInt) * this.blockLength;
        return (block >> positionIndex) & this.bitMask;
    }

    public void set(long i, int v)
    {
        // first identify which index we need
        long blockIndex = i / this.blocksPerInt;
        if (blockIndex >= this.data.length) throw new IndexOutOfBoundsException();
        int block = this.data[ (int) blockIndex ];

        // get positional index
        int positionIndex = (int) (i % this.blocksPerInt) * this.blockLength;

        int wiper = this.bitMask << positionIndex;
        int oldData = ~(~block | wiper);
        int newData = (v & this.bitMask) << positionIndex;
        this.data[ (int) blockIndex ] = oldData | newData;
    }
}
