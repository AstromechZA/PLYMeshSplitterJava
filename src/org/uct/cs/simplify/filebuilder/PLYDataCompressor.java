package org.uct.cs.simplify.filebuilder;

import org.uct.cs.simplify.model.Face;
import org.uct.cs.simplify.model.MemoryMappedFaceReader;
import org.uct.cs.simplify.model.MemoryMappedVertexReader;
import org.uct.cs.simplify.model.Vertex;
import org.uct.cs.simplify.ply.header.PLYElement;
import org.uct.cs.simplify.ply.reader.PLYReader;
import org.uct.cs.simplify.util.Pair;
import org.uct.cs.simplify.util.Useful;

import java.io.*;

/**
 This class is designed to convert the given model from PLY format to a terser binary form. The resulting file
 contains 2
 binary regions, one encoding the Vertices and the other encoding the Faces.
 <p>
 Vertices are of the form: X (float), Y (float), Z (float), nX (float), nY (float), nZ (float), colour (int)
 <p>
 Edges are of the form: I (integer), J (integer), K (integer) where I, J, and K are vertex indices.
 <p>
 A Vertex is 4*3 + 4*3 + 4 == 28 bytes ( 10_000_000 vertices is 267 MB )
 <p>
 All binary data is LITTLE ENDIAN.
 <p>
 No header containing lengths or offsets is supplied.
 */
public class PLYDataCompressor
{
    private static final int BYTES_PER_VERTEX = 4 * 3 + 4;
    private static final int BYTES_PER_FACE = 4 * 3;

    public static CompressionResult compress(File inputFile, File outputFile) throws IOException
    {
        try (BufferedOutputStream ostream = new BufferedOutputStream(new FileOutputStream(outputFile)))
        {
            return compress(new PLYReader(inputFile), ostream);
        }
    }

    public static CompressionResult compress(PLYReader reader, File outputFile) throws IOException
    {
        try (BufferedOutputStream ostream = new BufferedOutputStream(new FileOutputStream(outputFile)))
        {
            return compress(reader, ostream);
        }
    }

    public static CompressionResult compress(File inputFile, OutputStream ostream) throws IOException
    {
        return compress(new PLYReader(inputFile), ostream);
    }

    public static CompressionResult compress(PLYReader reader, OutputStream ostream) throws IOException
    {
        PLYElement vertexE = reader.getHeader().getElement("vertex");
        PLYElement faceE = reader.getHeader().getElement("face");
        long vbuffersize = vertexE.getCount() * BYTES_PER_VERTEX;
        long fbuffersize = faceE.getCount() * BYTES_PER_FACE;

        try (MemoryMappedVertexReader vr = new MemoryMappedVertexReader(reader))
        {
            Vertex v;
            for (int i = 0; i < vertexE.getCount(); i++)
            {
                v = vr.get(i);
                Useful.writeIntLE(ostream, Float.floatToRawIntBits(v.x));
                Useful.writeIntLE(ostream, Float.floatToRawIntBits(v.y));
                Useful.writeIntLE(ostream, Float.floatToRawIntBits(v.z));
            }
            for (int i = 0; i < vertexE.getCount(); i++)
            {
                v = vr.get(i);
                ostream.write(v.r);
                ostream.write(v.g);
                ostream.write(v.b);
                ostream.write(v.a);
            }

            try (MemoryMappedFaceReader fr = new MemoryMappedFaceReader(reader))
            {
                Face f;
                while (fr.hasNext())
                {
                    f = fr.next();
                    Useful.writeIntLE(ostream, f.i);
                    Useful.writeIntLE(ostream, f.j);
                    Useful.writeIntLE(ostream, f.k);
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
