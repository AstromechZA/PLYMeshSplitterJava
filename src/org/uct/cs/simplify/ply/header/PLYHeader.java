package org.uct.cs.simplify.ply.header;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.LinkedHashMap;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PLYHeader
{

    private static final int expectedHeaderLength = 1024;

    private PLYFormat format;
    private LinkedHashMap<String, PLYElement> elements;
    private int dataOffset;
    private final File file;

    public PLYHeader(File f) throws IOException
    {
        this.file = f;
        try (RandomAccessFile raf = new RandomAccessFile(this.file, "r"))
        {
            FileChannel inChannel = raf.getChannel();
            ByteBuffer buffer = ByteBuffer.allocate(expectedHeaderLength);

            if (inChannel.read(buffer) > 0)
            {
                String headerContent = new String(buffer.array(), "ASCII");
                this.constructFromString(headerContent);
            }
        }
    }

    public PLYHeader(String headerContent)
    {
        this.file = null;
        this.constructFromString(headerContent);
    }

    public File getFile()
    {
        return this.file;
    }

    public PLYFormat getFormat()
    {
        return this.format;
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
        Pattern p = Pattern.compile("[\\s\\S]*?end_header.*?\n", Pattern.MULTILINE | Pattern.UNIX_LINES);
        Matcher m = p.matcher(input);
        if (!m.find()) throw new BadHeaderException("PLY header could not be found");
        return m.group();
    }

    private static PLYFormat parseFormat(String input)
    {
        input = input.trim().toLowerCase();
        if (input.startsWith("format"))
        {
            if (input.contains("ascii")) return PLYFormat.ASCII;
            if (input.contains("little")) return PLYFormat.LITTLE_ENDIAN;
            if (input.contains("big")) return PLYFormat.BIG_ENDIAN;
        }
        throw new BadHeaderException("Unidentified PLYFormat");
    }

    private void constructFromString(String input)
    {
        input = isolateHeader(input);

        Scanner headScan = new Scanner(input);

        // read PLY
        headScan.nextLine();

        // read PLY format identifier from the string
        this.format = parseFormat(headScan.nextLine());

        // read the list of elements from scanner
        this.elements = parseElements(headScan);

        this.dataOffset = input.length();
    }

    public static LinkedHashMap<String, PLYElement> parseElements(Scanner s)
    {
        // output object
        LinkedHashMap<String, PLYElement> out = new LinkedHashMap<>();

        PLYElement current = null;
        String line;
        while (s.hasNextLine() && (line = s.nextLine()) != null)
        {
            // skip comments
            if (line.startsWith("comment")) continue;

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
        StringBuilder sb = new StringBuilder(1000);
        sb.append(String.format("ply%n"));
        sb.append(String.format("format binary_little_endian 1.0%n"));
        for (PLYElement e : this.elements.values())
        {
            sb.append(e.toString());
        }
        sb.append("end_header");
        return sb.toString();
    }

    public static enum PLYFormat
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
