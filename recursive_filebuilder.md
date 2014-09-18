## Recursive File Builder
(Recursively split , simplify, and rebuild a model)

Passes through <File, Depth>

process = (file, depth, maxdepth) ->
    if depth == maxdepth
        return build_tree_node(file, [])
    else
        files = split(file)
        file.delete
        child_nodes = [files.map f -> process(f, depth+1, maxdepth)]
        temp = stitch(child_nodes)
        temp = simplify(temp)
        return build_tree_node(temp, child_nodes)

tree = process(file, 0, maxdepth)

final = pack(tree)

## Monitoring Progress
calculate expected number of nodes:
let tree_depth = x      (number of levels)
let split = y           (number of children per node)
then num_nodes = (y^x) - 1

Pass through some Context variable? or make it static?

