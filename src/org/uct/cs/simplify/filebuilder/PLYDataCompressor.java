package org.uct.cs.simplify.filebuilder;

import org.uct.cs.simplify.util.Pair;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

/**
 * This class is designed to convert the given model from PLY format to a terser binary form.
 * The resulting file contains 2 binary regions, one encoding the Vertices and the other encoding the Faces.
 * <p>
 * Vertices are of the form:
 * X (float), Y (float), Z (float), nX (float), nY (float), nZ (float), colour (int)
 * <p>
 * Edges are of the form:
 * I (integer), J (integer), K (integer)
 * where I, J, and K are vertex indices.
 * <p>
 * A Vertex is 4*3 + 4*3 + 4 == 28 bytes
 * ( 10_000_000 vertices is 267 MB )
 * <p>
 * All binary data is LITTLE ENDIAN.
 * <p>
 * No header containing lengths or offsets is supplied.
 */
public class PLYDataCompressor
{
    public CompressionResult compress(File plymodel, File outputFile) throws IOException
    {
        try (
            // open file channel to output file
            FileChannel fc = new FileOutputStream(outputFile).getChannel();
        )
        {
            return null;
        }
    }

    /** Class containing lengths of the vertices and faces regions. A simple wrapper around Pair<long, long> */
    public class CompressionResult extends Pair<Long, Long>
    {
        public CompressionResult(long a, long b, File o) { super(a, b); }

        public long getLengthOfVertices() { return this.getFirst(); }

        public long getLengthOfFaces() { return this.getSecond(); }
    }
}
