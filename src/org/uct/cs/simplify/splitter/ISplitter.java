package org.uct.cs.simplify.splitter;

import org.uct.cs.simplify.file_builder.PackagedHierarchicalNode;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public interface ISplitter
{
    public ArrayList<PackagedHierarchicalNode> split(PackagedHierarchicalNode parent, File outputDir) throws IOException;
}
