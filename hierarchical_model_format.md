Octet Tree Model Format
=======================
A schema for storing hierarchical level of details of a model in a single file. This is a json array, with each element
being a block. Each block holds its own id and its parent id.

## High level schema
The file is comprised of 3 major parts:

### 1. header_length
32 bit signed integer equal to the number of bytes in the following header.
### 2. header
JSON string with the following format:
```
{
    "scale_ratio": how the model was rescaled to fit the 1024x1024x1024 box.
    "vertex_colour": true/false, (do the vertices have colour information following them) 
    "nodes": [
        {
            "id": <block_id>,
            "parent_id": <block_id of parent>,
            "num_faces": <number of faces in node>,
            "num_vertices": <number of vertices in node>,
            "block_offset": <offset from the beginning of the data section>,
            "block_length": <length of the data block>,
            "depth": <depth of the node in the tree>,
            "min_x": <min x value of a vertex in the node>,
            "max_x": <max x value of a vertex in the node>,
            "min_y": <min y value of a vertex in the node>,
            "max_y": <max y value of a vertex in the node>,
            "min_z": <min z value of a vertex in the node>,
            "max_z": <max z value of a vertex in the node>,
            "leaf": <true/false is this a leaf node?>
        },
        {
            ...
        }
    ]
}
``` 
### 3. data region
File region containing data blocks. Each data block is a number of arrays:
* vertex array
Array of floats, each group of 3 being one vertex
* colour array (if vertex_colour was specified)
Array of bytes, each group of 4 bytes containing Red, Green, Blue, Alpha
* face array
Array of integers, each group of 3 being the vertex indices of a triangular face

## Facts
- node.block_length == node.num_vertices * 12 + node.num_faces * 12 (+ node.num_vertices * 3 if vertex_colour was true)
