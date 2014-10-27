package org.uct.cs.simplify.model;

import org.uct.cs.simplify.ply.header.PLYListProperty;
import org.uct.cs.simplify.ply.header.PLYProperty;
import org.uct.cs.simplify.ply.header.PLYPropertyBase;
import org.uct.cs.simplify.ply.reader.PLYReader;
import org.uct.cs.simplify.util.Useful;

import java.io.IOException;

public class CleverFaceReader extends SimpleFaceReader
{
    public CleverFaceReader(PLYReader reader) throws IOException
    {
        super(reader);
    }

    @Override
    public void next(Face f) throws IOException
    {
        for (PLYPropertyBase base : this.faceElement.getProperties())
        {
            if (base.getName().equals("vertex_indices"))
            {
                istream.read();
                f.i = Useful.readIntLE(istream);
                f.j = Useful.readIntLE(istream);
                f.k = Useful.readIntLE(istream);
            }
            else if (base instanceof PLYListProperty)
            {
                PLYListProperty listProperty = (PLYListProperty) base;
                int l = (int) listProperty.getLengthTypeReader().read(istream);
                for (int j = 0; j < l; j++)
                {
                    listProperty.getTypeReader().read(istream);
                }
            }
            else if (base instanceof PLYProperty)
            {
                ((PLYProperty) base).getTypeReader().read(istream);
            }
        }

        this.index += 1;
    }
}
