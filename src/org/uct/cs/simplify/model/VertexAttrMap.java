package org.uct.cs.simplify.model;

import org.uct.cs.simplify.ply.header.PLYElement;
import org.uct.cs.simplify.ply.header.PLYProperty;
import org.uct.cs.simplify.ply.header.PLYPropertyBase;

/**
 VertexAttrMap is used as a link between the flexible PLYHeader and the inflexible internal Vertex class. VertexAttrMap
 analyses the vertex PLYElement to determine the location of the XYZ and RGBA elements (if they exist).
 <p>
 WARNING: this will break if the vertex contais list properties. (This shouldn't ever happen)
 */
public class VertexAttrMap
{
    public final boolean hasColour, hasAlpha;
    public final int byteLength;
    public int xOffset = -1;
    public int yOffset = -1;
    public int zOffset = -1;
    public int redOffset = -1;
    public int greenOffset = -1;
    public int blueOffset = -1;
    public int alphaOffset = -1;

    /**
     Constructor

     @param vertexElement the PLYHeader Element description for the 'vertex' element;
     */
    public VertexAttrMap(PLYElement vertexElement) throws IllegalArgumentException
    {
        int position = 0;
        this.byteLength = vertexElement.getItemSize();
        for (PLYPropertyBase base : vertexElement.getProperties())
        {
            // only scan the PLYProperty types
            if (base instanceof PLYProperty)
            {
                switch (base.getName())
                {
                    case "x":
                        this.xOffset = position;
                        break;
                    case "y":
                        this.yOffset = position;
                        break;
                    case "z":
                        this.zOffset = position;
                        break;
                    case "red":
                        this.redOffset = position;
                        break;
                    case "green":
                        this.greenOffset = position;
                        break;
                    case "blue":
                        this.blueOffset = position;
                        break;
                    case "alpha":
                        this.alphaOffset = position;
                        break;
                }

                // move position forward by the correct number of bytes
                position += ((PLYProperty) base).getType().getByteSize();
            } else
            {
                throw new IllegalArgumentException("Error: Vertex Element contains List property!");
            }
        }

        // work out if there are successful colour and alpha descriptions
        this.hasColour = this.redOffset > -1 && this.greenOffset > -1 && this.blueOffset > -1;
        this.hasAlpha = this.alphaOffset > -1;
    }


}
