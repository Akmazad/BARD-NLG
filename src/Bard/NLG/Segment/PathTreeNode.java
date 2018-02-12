/**
 * This file is part of the BARD project
 * Monash Univeristy, Melbourne, Australia
 * 2018
 *
 * @author Dr. Matthieu Herrmann
 * Segment computation Path -> Tree. Paths are assembled in a tree, the target being the root and the evidences
 * the leaves. That tree may be cut to handle loops, yeilding a forest.
 * Walking the forest gives us a list of intermediate target to be explained - See the analyser.
 */

package Bard.NLG.Segment;

import Bard.NLG.Segment.Graph.Graph;
import Bard.NLG.Segment.Graph.Node;
import Bard.NLG.Segment.Graph.Path;

import static Bard.NLG.Segment.PathTreeNode.Status.*;
import static Bard.NLG.Tools.*;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PathTreeNode {

    // --- --- --- Inner type

    /**
     * PathTreeNode Status
     */
    enum Status {
        TARGET,
        CAUSAL,             // ... Simple → previous ... Target=root
        ANTICAUSAL,         // ... Simple ← previous ... Target=root
        EVCAUSAL,           // EV → previous ... Target=root
        EVANTICAUSAL,       // EV ← previous ... Target=root
        CECAUSAL,           // [ … → CE ← …] → previous ... Target=root
        CEANTICAUSAL,       // [ … → CE ← …] ← previous ... Target=root
        CEEVCAUSAL,         // [ … → CE[EV] ← …] → previous ... Target=root
        CEEVANTICAUSAL      // [ … → CE[EV] ← …] ← previous ... Target=root
    }

    // --- --- --- Fields

    /**
     * BN node associated to this tree node. Immutable.
     */
    public final Node node;

    /**
     * The set of children for the current node. Immutable.
     */
    public final Map<Node, PathTreeNode> children;

    /**
     * Status of the this path tree node. See STATUS. Immutable.
     */
    public final Status status;

    /**
     * Parent node. Not available if the status is TARGET.
     */
    public final PathTreeNode parent;

    /**
     * Graph associated to this tree node. Immutable.
     */
    public final Graph graph;

    // --- --- --- Constructor

    /**
     * Private constructor. Use the factory node "mkRoot" to create the first node, than use the "applyPath" method
     * with path to create the tree.
     */
    private PathTreeNode(Graph g, Node n, Map<Node, PathTreeNode> c, Status s, PathTreeNode p) {
        graph = g;
        node = n;
        children = c;
        status = s;
        parent = p;
    }

    /**
     * Create a new tree root from the node n in the graph g. The node MUST be a target node.
     */
    public static PathTreeNode mkRoot(Graph g, Node n) {
        if (n.isTarget()) {
            return new PathTreeNode(g, n, Collections.emptyMap(), TARGET, null);
        } else {
            shouldNotHappen("Creating a root with a non target node (" + n.id + ").");
            return null; // Satisfy flow analysis
        }
    }

    /**
     * Copy this and filter out some subtrees identified by their node.
     * Note: the root/call receiver node can not be filtered out!
     * The checking start with the children of the root/call receiver node
     */
    public PathTreeNode copyFilter(Set<Node> filter) {
        Map<Node, PathTreeNode> filtered_children = new HashMap<>();
        children.forEach((k, v) -> {
            if (!filter.contains(k)) { // Only keep the node if not found in the filter
                PathTreeNode nv = v.copyFilter(filter);
                filtered_children.put(k, nv);
            }
        });
        return new PathTreeNode(graph, node, filtered_children, status, parent);
    }


    // --- --- --- Public methods

    /**
     * Check if this node is from a common effect node.
     */
    public boolean isCE() {
        return node.isCE(graph);
    }

    /**
     * Gather recursively the set of common effect node in this node and its children.
     */
    public Set<Node> getAllCE() {
        Set<Node> set = new HashSet<>();
        if (isCE()) {
            set.add(node);
        }
        children.forEach((k, v) -> {
            set.addAll(v.getAllCE());
        });
        return freeze(set);
    }

    /**
     * Apply a path on the current node.
     * The first node of the path must match this.node.
     * In particular, when building a tree (root=)Target -> ... -> (leaf=)Evidence,
     * This method should be applyPath on the root node with a path [Target ... ... Evidence].
     * Note: because we are shaving the path, it must be free (see Path.shave)
     * Return a new path (or this if no change was made). This is never modified.
     */
    public PathTreeNode applyPath(Path path) {
        if (path.first.equals(node)) {
            return path.shave().map(

                    // --- --- --- Either.left: Shave returned a Path:
                    (Path p) -> {
                        Node n = p.first;
                        PathTreeNode child;
                        if (children.containsKey(n)) {
                            // --- Subnode exists: recurse on it with the rest of the path
                            child = children.get(n).applyPath(p);
                        } else {
                            // --- Subnode does not exist: build it and recurse with the rest of the path
                            child = mkNode(n).applyPath(p);
                        }
                        // Update our children map
                        Map<Node, PathTreeNode> nchildren = new HashMap<>(children);
                        nchildren.put(n, child);
                        return new PathTreeNode(graph, node, nchildren, status, parent);
                    },

                    // --- --- --- Either.right: Shave returned a Node ==> last node of the path, end of recursion.
                    (Node n) -> {
                        if (children.containsKey(n)) {
                            // --- Node already in the tree, we are done.
                            return this;
                        } else {
                            // --- Create and add the last node
                            Map<Node, PathTreeNode> nchildren = new HashMap<>(children);
                            nchildren.put(n, mkNode(n));
                            return new PathTreeNode(graph, node, nchildren, status, parent);
                        }
                    }
            ); // END OF MAP EITHER
        } else {
            shouldNotHappen("Extending a TreePathNode with a wrong path");
            return this; // Satisfy flow analysis
        }
    }


    /**
     * Find a path between two nodes, DFS in the current tree
     * ONLY RETURNS THE FIRST FOUND PATH
     */
    public List<Node> DFS(Node from, Node to) {
        if (node.equals(from)) {
            return DFS(to);
        } else {

            Map<Node, List<Node>> paths = new HashMap<>();
            children.forEach((k, v) -> {
                List<Node> nv = v.DFS(from, to);
                if (!nv.isEmpty()) {
                    paths.put(k, nv);
                }
            });

            List<Node> res = Collections.emptyList();
            if (!paths.isEmpty()) {
                res = new ArrayList<>(paths.entrySet().stream().findAny().get().getValue());
            }

            return res;
        }
    }


    /**
     * Helper for the DFS function, to be called when we have the first 'from' node.
     * Find a path to the node 'to'.
     * Note: find **any path**!
     */
    private List<Node> DFS(Node to) {
        if (node == to) {
            return Collections.singletonList(node);
        } else {

            Map<Node, List<Node>> paths = new HashMap<>();
            children.forEach((k, v) -> {
                List<Node> nv = v.DFS(to);
                if (!nv.isEmpty()) {
                    paths.put(k, nv);
                }
            });

            List<Node> res = Collections.emptyList();
            if (!paths.isEmpty()) {
                res = new ArrayList<>(paths.entrySet().stream().findAny().get().getValue());
                res.add(0, node);
            }

            return res;
        }
    }

    /**
     * Find a subtree.
     * Note: starting with the children, so finding the subtree of 'this' with 'this.node' will fail.
     */
    public Optional<PathTreeNode> findSubTree(Node wantedRoot) {
        if (children.containsKey(wantedRoot)) {
            return Optional.of(children.get(wantedRoot));
        } else {
            return children.entrySet().stream()
                    .flatMap(e -> e.getValue().findSubTree(wantedRoot).map(Stream::of).orElseGet(Stream::empty))
                    .findAny();
        }
    }

    /**
     * Postfix run, with accumulation in a list
     */
    public <T> List<T> postFix(Function<Node, T> fun) {
        T val = fun.apply(node);
        ArrayList<T> res = new ArrayList<>();
        res.add(val);
        children.forEach((k, v) -> {
            res.addAll(v.postFix(fun));
        });
        return res;
    }


    // --- --- --- Mondanités contingentes

    @Override
    public String toString() {
        return toString(Collections.emptyList());
    }


    // --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- ---
    // --- PRIVATE - PRIVATE - PRIVATE - PRIVATE - PRIVATE - PRIVATE - PRIVATE - PRIVATE - PRIVATE - PRIVATE - PRIVATE
    // --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- ---

    /**
     * Create a new PathTreeNode for the Node n, in the current graph.
     * This method SHOULD NOT be called with a target node. Use mkRoot in that case.
     */
    private PathTreeNode mkNode(Node n) {

        // --- Gather some data
        PathTreeNode parent = this;
        boolean isCE = n.isCE(graph);
        boolean isCausal = n.hasChild(graph, parent.node);

        // --- Determine the status of this node
        Status s;
        if (n.isTarget()) {
            shouldNotHappen("Constructing a inner node with the target");
            s = TARGET; // Satisfy flow analysis
        } else if (n.isEvidence()) {
            if (isCE) { // CommonEffect + EVidence
                s = isCausal ? CEEVCAUSAL : CEEVANTICAUSAL;
            } else { // Only EVidence
                s = isCausal ? EVCAUSAL : EVANTICAUSAL;
            }
        } else if (n.isSimple()) {
            if (isCE) { // Simple CommonEffect
                s = isCausal ? CECAUSAL : CEANTICAUSAL;
            } else { // Simple
                s = isCausal ? CAUSAL : ANTICAUSAL;
            }
        } else {
            shouldNotHappen("Constructing a inner node with an unknown status.");
            s = TARGET; // Satisfy flow analysis
        }

        // --- Build time
        return new PathTreeNode(graph, n, Collections.emptyMap(), s, parent);
    }


    /**
     * Helper for the toString method.
     */
    private String toString(List<PathTreeNode> stack) {
        if (children.isEmpty()) {
            // IF we are at a leaf, print the branch
            StringBuffer sb = new StringBuffer("\n");
            for (int i = 0; i < stack.size(); ++i) {
                sb.append(toStringStatus(stack.get(i)));
                sb.append(" ");
            }
            // Do not forget "this"
            sb.append(toStringStatus(this));
            return sb.toString();
        } else {
            // Build the stack
            List<PathTreeNode> nstack = new ArrayList<>(stack);
            nstack.add(this);
            return children.entrySet().stream().map(ptn -> ptn.getValue().toString(nstack)).collect(Collectors.joining("\n"));
        }
    }

    /**
     * Helper for the toString method.
     */
    private static String toStringStatus(PathTreeNode ptn) {
        String name = ptn.node.id;
        switch (ptn.status) {
            case TARGET:
                return name;
            case CAUSAL:
                return "(<= " + name + ")";
            case ANTICAUSAL:
                return "(=> " + name + ")";
            case EVCAUSAL:
                return "{<= " + name + "}";
            case EVANTICAUSAL:
                return "{=> " + name + "}";
            case CECAUSAL:
                return "[<= " + name + "]";
            case CEANTICAUSAL:
                return "[=> " + name + "]";
            case CEEVCAUSAL:
                return "||<= " + name + "||";
            case CEEVANTICAUSAL:
                return "||=> " + name + "||";
            default:
                shouldNotHappen("Unknown path status");
                return ""; // Satisfy flow analysis
        }
    }
}
