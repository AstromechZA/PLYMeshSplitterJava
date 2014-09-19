Octet Tree Model Format
=======================
A schema for storing hierarchical level of details of a model in a single file. This is a json array, with each element
being a block. Each block holds its own id and its parent id.

## High level schema
- header_length <int32 4bytes>
- header_json (header_length bytes)
- data

### header_json
    [
        {
            block_id: 0
            parent: nil
            num_faces: <number of faces in block>
            num_vertices: <number of vertices in block>
            block_length: <byte length of PLY file block>
            block_offset: <pointer to beginning of PLY file block>
            min_x: <minimum x value of bounding box>
            min_y: <minimum y value of bounding box>
            min_z: <minimum z value of bounding box>
            max_x: <maximum x value of bounding box>
            max_y: <maximum y value of bounding box>
            max_z: <maximum z value of bounding box>
        },
        {
            block_id: 1
            parent: 0
            num_faces: <number of faces in block>
            num_vertices: <number of vertices in block>
            block_length: <byte length of PLY file block>
            block_offset: <pointer to beginning of PLY file block>
            min_x: <minimum x value of bounding box>
            min_y: <minimum y value of bounding box>
            min_z: <minimum z value of bounding box>
            max_x: <maximum x value of bounding box>
            max_y: <maximum y value of bounding box>
            max_z: <maximum z value of bounding box>
        }
        ...
    ]

### data
`data` contains the PLY format model blocks. Use the header to get the block offsets and lengths in order to read this efficiently.
