package org.uct.cs.simplify.ply;

public class PLYProperty
{
    private static int[] magicTypeBytes = { 1, 1, 0, 0, 0, 0, 0, 4, 8, 0, 0, 0, 4, 1, 0, 0, 2, 0, 0, 4, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2 };
    private String name;
    private String listLengthDataType;
    private String dataType;
    private boolean isList;
    private Integer byteLength;

    private PLYProperty(String name, String dataType)
    {
        this.name = name;
        this.dataType = dataType;
        this.isList = false;
        this.byteLength = bytesInType(dataType);
    }

    public PLYProperty(String name, String dataType, String listLengthDataType)
    {
        this.name = name;
        this.dataType = dataType;
        this.listLengthDataType = listLengthDataType;
        this.isList = true;
        this.byteLength = null;
    }

    public static PLYProperty FromString(String s)
    {
        s = s.trim();
        String[] parts = s.split(" ");
        if (parts[ 1 ].equals("list"))
        {
            return new PLYProperty(parts[ 4 ], parts[ 3 ], parts[ 2 ]);
        }
        else
        {
            return new PLYProperty(parts[ 2 ], parts[ 1 ]);
        }
    }

    public String getName() { return name; }

    public boolean isList() { return isList; }

    public String getDataType() { return dataType; }

    public Integer getByteLength() { return byteLength; }

    public String getListLengthDataType() { return listLengthDataType; }

    @Override
    public String toString()
    {
        if (isList)
        {
            return "PLYProperty(" + dataType + "[" + listLengthDataType + "] " + name + ")";
        }
        else
        {
            return "PLYProperty(" + dataType + " " + name + ")";
        }
    }

    public static int bytesInType(String s)
    {
        int index = s.charAt(0) + s.charAt(1) - 203;
        return magicTypeBytes[ index ];
    }

}
