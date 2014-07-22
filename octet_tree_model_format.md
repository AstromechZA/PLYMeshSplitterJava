Octet Tree Model Format
=======================
A schema for storing hierarchical level of details of a model in a single file.

## High level schema
- header_length <int32 4bytes>
- header_json (header_length bytes)
- data

### header_json
    [
        {
            id: <block_id>,
            block_offset: <absolute_byte_offset>,
            block_length: <num_bytes>,
            children: [
                <block_id>,                 # +x+y+z
                <block_id>,                 # +x+y-z
                <block_id>,                 # +x-y+z
                <block_id>,                 # +x-y-z
                <block_id>,                 # -x+y+z
                null,                       # -x+y-z    (null indicates empty block)
                <block_id>,                 # -x-y+z
                <block_id>                  # -x-y-z
            ]
        },
        ...
    ]

### data
`data` contains the PLY format model blocks. Use the header to get the block offsets and lengths in order to read this efficiently.