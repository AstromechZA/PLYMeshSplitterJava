package org.uct.cs.simplify.ply.header;

import org.uct.cs.simplify.model.VertexAttrMap;
import org.uct.cs.simplify.ply.datatypes.DataType;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("CollectionWithoutInitialCapacity")
public class PLYHeader
{
    private static final String HEADER_REGEXP = "^[\\s\\S]*?end_header[\\s\\S]*?\n";
    private static final int EXPECTED_HEADER_LENGTH = 1024;
    private final File file;
    private PLYFormat format;
    private LinkedHashMap<String, PLYElement> elements;
    private int dataOffset;

    public PLYHeader(File f) throws IOException
    {
        this.file = f;
        try (RandomAccessFile raf = new RandomAccessFile(this.file, "r"))
        {
            FileChannel inChannel = raf.getChannel();
            ByteBuffer buffer = ByteBuffer.allocate(EXPECTED_HEADER_LENGTH);

            if (inChannel.read(buffer) > 0)
            {
                String headerContent = new String(buffer.array(), "ASCII");
                this.constructFromString(headerContent);
            }
        }
        if (this.format != PLYFormat.LITTLE_ENDIAN)
            throw new RuntimeException("Input file must be in binary Little-Endian format!");
    }

    public PLYHeader(Iterable<PLYElement> elements)
    {
        this.file = null;
        this.format = PLYFormat.LITTLE_ENDIAN;
        this.elements = new LinkedHashMap<>();
        for (PLYElement e : elements)
        {
            this.elements.put(e.getName(), e);
        }
        this.dataOffset = this.toString().length();
    }

    public File getFile()
    {
        return this.file;
    }

    public LinkedHashMap<String, PLYElement> getElements()
    {
        return this.elements;
    }

    public PLYElement getElement(String name)
    {
        return this.elements.get(name);
    }

    private static String isolateHeader(String input) throws BadHeaderException
    {
        Pattern p = Pattern.compile(HEADER_REGEXP, Pattern.MULTILINE | Pattern.UNIX_LINES);
        Matcher m = p.matcher(input);
        if (!m.find()) throw new BadHeaderException("PLY header could not be found");
        return m.group();
    }

    private static PLYFormat parseFormat(String input)
    {
        String input1 = input.trim().toLowerCase();
        if (input1.startsWith("format"))
        {
            if (input1.contains("ascii")) return PLYFormat.ASCII;
            if (input1.contains("little")) return PLYFormat.LITTLE_ENDIAN;
            if (input1.contains("big")) return PLYFormat.BIG_ENDIAN;
        }
        throw new BadHeaderException("Unidentified PLYFormat");
    }

    private void constructFromString(String input)
    {
        input = isolateHeader(input);

        Scanner headScan = new Scanner(input);

        // read PLY
        headScan.nextLine();

        this.format = parseFormat(headScan.nextLine());

        this.elements = parseElements(headScan);

        this.dataOffset = input.length();
    }

    public static LinkedHashMap<String, PLYElement> parseElements(Scanner s)
    {
        // output object
        LinkedHashMap<String, PLYElement> out = new LinkedHashMap<>();

        PLYElement current = null;
        String line;
        while (s.hasNextLine() && ((line = s.nextLine()) != null))
        {
            // skip comments
            if (line.startsWith("comment")) continue;
            if (line.startsWith("obj_info")) continue;

            // element line shows the start of a new element block
            if (line.startsWith("element"))
            {
                if (current != null) out.put(current.getName(), current);
                current = PLYElement.FromString(line);
            }
            // property line
            else if (line.startsWith("property"))
            {
                if (current != null) current.addProperty(PLYPropertyBase.FromString(line));
            }
        }
        if (current != null) out.put(current.getName(), current);

        return out;
    }

    public int getDataOffset()
    {
        return this.dataOffset;
    }


    public String toString()
    {
        StringBuilder sb = new StringBuilder(EXPECTED_HEADER_LENGTH);
        sb.append(String.format("ply%n"));
        sb.append(String.format("format binary_little_endian 1.0%n"));
        for (PLYElement element : elements.values())
        {
            sb.append(element);
        }
        sb.append("end_header");
        return sb.toString();
    }

    public static PLYHeader constructHeader(long numVertices, long numFaces, VertexAttrMap vam)
    {
        List<PLYElement> elements = new ArrayList<>(7);
        PLYElement eVertex = new PLYElement("vertex", numVertices);
        eVertex.addProperty(new PLYProperty("x", DataType.FLOAT));
        eVertex.addProperty(new PLYProperty("y", DataType.FLOAT));
        eVertex.addProperty(new PLYProperty("z", DataType.FLOAT));
        if (vam != null)
        {
            if (vam.hasColour)
            {
                eVertex.addProperty(new PLYProperty("red", DataType.UCHAR));
                eVertex.addProperty(new PLYProperty("green", DataType.UCHAR));
                eVertex.addProperty(new PLYProperty("blue", DataType.UCHAR));
            }
            if (vam.hasAlpha)
            {
                eVertex.addProperty(new PLYProperty("alpha", DataType.UCHAR));
            }
        }
        elements.add(eVertex);
        PLYElement eFace = new PLYElement("face", numFaces);
        eFace.addProperty(new PLYListProperty("vertex_indices", DataType.UINT, DataType.UCHAR));
        elements.add(eFace);
        return new PLYHeader(elements);
    }

    public enum PLYFormat
    {
        ASCII, BIG_ENDIAN, LITTLE_ENDIAN
    }

    public static class BadHeaderException extends RuntimeException
    {
        public BadHeaderException(String s)
        {
            super(s);
        }
    }

}
