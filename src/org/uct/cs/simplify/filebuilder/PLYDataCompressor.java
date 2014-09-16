package org.uct.cs.simplify.filebuilder;

import it.unimi.dsi.fastutil.io.FastBufferedOutputStream;
import org.uct.cs.simplify.model.Face;
import org.uct.cs.simplify.model.MemoryMappedFaceReader;
import org.uct.cs.simplify.model.MemoryMappedVertexReader;
import org.uct.cs.simplify.model.Vertex;
import org.uct.cs.simplify.ply.header.PLYElement;
import org.uct.cs.simplify.ply.reader.PLYReader;
import org.uct.cs.simplify.util.Pair;
import org.uct.cs.simplify.util.Useful;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

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
    public static CompressionResult compress(File inputFile, File outputFile) throws IOException
    {
        return compress(new PLYReader(inputFile), outputFile);
    }

    public static CompressionResult compress(PLYReader reader, File outputFile) throws IOException
    {
        PLYElement vertexE = reader.getHeader().getElement("vertex");
        PLYElement faceE = reader.getHeader().getElement("face");
        long vbuffersize = vertexE.getCount() * (4*3 + 4*3 + 4);
        long fbuffersize = faceE.getCount() * (4*3);

        try(FastBufferedOutputStream fostream = new FastBufferedOutputStream(new FileOutputStream(outputFile)))
        {
            try(MemoryMappedVertexReader vr = new MemoryMappedVertexReader(reader))
            {
                Vertex v;
                for (int i = 0; i < vertexE.getCount(); i++)
                {
                    v = vr.get(i);
                    // write location
                    Useful.littleEndianWrite(fostream, Float.floatToIntBits(v.x));
                    Useful.littleEndianWrite(fostream, Float.floatToIntBits(v.y));
                    Useful.littleEndianWrite(fostream, Float.floatToIntBits(v.z));

                    // write fake normal (this gets overrided in stage 2)
                    Useful.littleEndianWrite(fostream, 0);
                    Useful.littleEndianWrite(fostream, 0);
                    Useful.littleEndianWrite(fostream, 1);

                    // write fake colour information
                    fostream.write(128);
                    fostream.write(128);
                    fostream.write(128);
                    fostream.write(255);
                }
                try(MemoryMappedFaceReader fr = new MemoryMappedFaceReader(reader))
                {
                    Face f;
                    while(fr.hasNext())
                    {
                        f = fr.next();
                        Useful.littleEndianWrite(fostream, f.i);
                        Useful.littleEndianWrite(fostream, f.j);
                        Useful.littleEndianWrite(fostream, f.k);
                    }
                }
            }


        }

        return new CompressionResult(vbuffersize, fbuffersize);
    }

    /** Class containing lengths of the vertices and faces regions. A simple wrapper around Pair<long, long> */
    public static class CompressionResult extends Pair<Long, Long>
    {
        public CompressionResult(long a, long b) { super(a, b); }

        public long getLengthOfVertices() { return this.getFirst(); }

        public long getLengthOfFaces() { return this.getSecond(); }
    }
}
