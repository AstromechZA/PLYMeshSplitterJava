package org.uct.cs.simplify.sticher;

import org.uct.cs.simplify.ply.header.PLYHeader;

import java.io.File;

public class NaiveMeshStitcher
{
    public static PLYHeader stitch(File file1, File file2, File outputFile)
    {
        // load in mesh1
        // create TObjectIntHashMap W (detect if vertices are already added, and what they're indexed to)

        // insert all of mesh1's vertices into W with incremening indices

        // write vertices to tempfile1
        // write faces to tempfile2

        // create TIntIntHashMap G

        // run through all vertices
        // if vertex in W
        // put transformed index into G
        // else
        // increment I, store in G,
        // write vertex into tempfile1

        // run through faces
        // for each vertex
        // pull new index from G
        // write to tempfile2

        // combine tempfiles into final file

        return null;
    }
}
