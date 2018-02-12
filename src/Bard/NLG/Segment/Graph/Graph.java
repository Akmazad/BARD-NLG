/**
 * This file is part of the BARD project
 * Monash Univeristy, Melbourne, Australia
 * 2018
 *
 * @author Dr. Matthieu Herrmann
 * The segment computation maintains its own graph representing a Bayesian Network layout.
 * This class represents the graph.
 */

package Bard.NLG.Segment.Graph;

import Bard.NLG.Segment.Segment;
import Bard.NLG.Tools;

import static Bard.NLG.Tools.*;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;


public class Graph {

    // --- --- --- Fields

    // --- Inputs

    /**
     * Set of nodes. Immutable.
     */
    public final Set<Node> nodes;

    /**
     * Set of edges. Immutable.
     */
    public final Set<Edge> edges;

    // --- Computed

    /**
     * For a node, gives its parents  PARENTS -> N.
     * All node are present, associated with an empty set if needed. Immutable.
     */
    public final Map<Node, Set<Node>> myParents;

    /**
     * For a node, gives its children N -> CHILDREN.
     * All node are present, associated with an empty set if needed. Immutable.
     */
    public final Map<Node, Set<Node>> myChildren;

    /**
     * Target node. Immutable.
     */
    public final Node target;

    /**
     * Set of evidences. Immutable.
     */
    public final Set<Node> evidences;

    /**
     * All directed path from target to evidence.
     * Indexed by all the evidences. Immutable.
     */
    public final Map<Node, Set<Path>> evidencePaths;

    /**
     * Set of all the paths. Immutable.
     */
    public final Set<Path> allPaths;

    /**
     * Set of all blocked paths. Immutable.
     */
    public final Set<Path> blockedPaths;

    /**
     * Set of all free paths. Immutable.
     */
    public final Set<Path> freePaths;

    /**
     * Set of common effect nodes present in all the free paths. Immutable.
     */
    public final Set<Node> ceNodes;


    // --- --- --- Constructor

    // Warning: the constructor is "heavy" and does a some computation for the graph.
    // DO NOT UPDATE INTERNAL OBJETCS AFTER CONSTRUCTION

    /**
     * Create a new graph from a set of nodes and a set of edges.
     * The set of nodes should contains one and only one target, and should be a superset of the set of nodes use
     * by the edges (i.e. disconnected nodes are allowed).
     */
    public Graph(Set<Node> nodes_, Set<Edge> edges_) {

        // --- Freezes the input
        this.nodes = freeze(nodes_);
        this.edges = freeze(edges_);

        // --- Compute the myParents_tmp and myChildren relations.
        Map<Node, Set<Node>> myParents_tmp = new HashMap<>();
        Map<Node, Set<Node>> myChildren_tmp = new HashMap<>();

        // Init with empty sets. Guarantee that the get() calls below are valid
        // (if the set of edges and the nodeMap are consistent)
        nodes.forEach(n -> {
            myParents_tmp.put(n, new HashSet<>());
            myChildren_tmp.put(n, new HashSet<>());
        });

        // Edge source -> target
        //      parent -> child
        edges.forEach(e -> {
            Node parent = e.source;
            Node child = e.target;
            myParents_tmp.get(child).add(parent);
            myChildren_tmp.get(parent).add(child);
        });

        // Freeze
        myParents = freezeMapSet(myParents_tmp);
        myChildren = freezeMapSet(myChildren_tmp);

        // --- Look for the target node
        // Should always exists... Else crash.
        Optional<Node> mbT = nodes.stream().filter(Node::isTarget).findFirst();
        if (mbT.isPresent()) {
            target = mbT.get();
        } else {
            target = null; // Satisfy the compiler initialization analysis...
            shouldNotHappen("Constructing a BN graph without a target.");
        }

        // --- Look for the evidence nodes
        evidences = freeze(nodes.stream().filter(Node::isEvidence).collect(Collectors.toSet()));

        // Warning: calling with this before contstruction is complete.
        // Works because the path only rely on the "myParents" and "myChildren" relations.
        // --- Compute at all the path between the target and the evidences
        evidencePaths = freezeMapSet(evidences.stream().collect(Collectors.toMap(
                Function.identity(),
                (Node ev) -> DFS(target, ev).stream().map(list -> Path.build(this, list)).collect(Collectors.toSet())))
        );

        // --- Gather all the paths, blocked paths, free paths
        allPaths = freeze(evidencePaths.values().stream().flatMap(Set::stream).collect(Collectors.toSet()));
        blockedPaths = freeze(allPaths.stream().filter(Path::isBlocked).collect(Collectors.toSet()));
        freePaths = freeze(allPaths.stream().filter(Path::isFree).collect(Collectors.toSet()));

        // --- Gather common effect node
        ceNodes = freeze(freePaths.stream().flatMap(p -> p.getCENodes().stream()).collect(Collectors.toSet()));
    }


    // --- --- --- Node query methods

    /**
     * For a node N, gets its children N -> CHILDREN
     */
    public Set<Node> getChildren(Node n) {
        return myChildren.get(n);
    }

    /**
     * For a node N, gets its parents PARENTS -> N
     */
    public Set<Node> getParents(Node n) {
        return myParents.get(n);
    }

    /**
     * Check if a node is a "common effect" nodes (only consider free path)
     */
    public boolean isCE(Node n) {
        return ceNodes.contains(n);
    }


    // --- --- --- Markov Blanket
    /* Markov blanket: all parents, all children, all children's parents (but the blanket's target)
                       causal       anticausal    common effect
     */

    /**
     * Compute the markov blanket of a graph's node.
     */
    public Segment getMarkovBlanket(Node target) {
        Set<Node> causal = getParents(target);
        Set<Node> antiCausal = getChildren(target).stream().filter(n -> !isCE(n)).collect(Collectors.toSet());
        Map<Node, Set<Node>> commonEffect =
                getChildren(target).stream().filter(this::isCE).collect(Collectors.toMap(
                        Function.identity(),
                        n -> getParents(n).stream().filter(np -> !np.equals(target)).collect(Collectors.toSet())));
        return new Segment(target, causal, antiCausal, commonEffect);
    }

    // --- --- --- Reduced graph

    /**
     * Compute a reduced graph based on the free paths only.
     * Return Optionnal.empty() if a reduced graph cannot be computed.
     */
    public Optional<Graph> getReducedGraph() {

        // --- Do a bit of analysis if the logger is on
        if (Tools.loggerFlag) {
            // --- Nodes analysis
            Set<Node> pathNodes = allPaths.stream().flatMap(p -> p.getNodeSet().stream()).collect(Collectors.toSet());
            Set<Node> freeNodes = freePaths.stream().flatMap(p -> p.getNodeSet().stream()).collect(Collectors.toSet());
            Set<Node> blockedNodes = new HashSet<>(pathNodes);
            blockedNodes.removeAll(freeNodes);

            // --- Evidence analysis
            Set<Node> freeEvidences = freeNodes.stream().filter(Node::isEvidence).collect(Collectors.toSet());
            Set<Node> blockedEvidences = new HashSet<>(evidences);
            blockedEvidences.removeAll(freeEvidences);

            // --- Print
            log("GRAPH ANALYSIS:");
            log("--- PATHS ---");
            log("All    :\n" + allPaths.stream().map(Path::toString).collect(Collectors.joining("\n ", "\n ", "\n ")));
            log("Free   :\n" + freePaths.stream().map(Path::toString).collect(Collectors.joining("\n ", "\n ", "\n ")));
            log("Blocked:\n" + blockedPaths.stream().map(Path::toString).collect(Collectors.joining("\n ", "\n ", "\n ")));
            log("--- NODES ---");
            log("All        : " + nodes.stream().map(Node::toString).collect(Collectors.joining(" ")));
            log("On paths   : " + pathNodes.stream().map(Node::toString).collect(Collectors.joining(" ")));
            log(" |- Free   : " + freeNodes.stream().map(Node::toString).collect(Collectors.joining(" ")));
            log(" |- Blocked: " + blockedNodes.stream().map(Node::toString).collect(Collectors.joining(" ")));
            log("--- EVIDENCES ---");
            log("All    : " + evidences.stream().map(Node::toString).collect(Collectors.joining(" ")));
            log("Free   : " + freeEvidences.stream().map(Node::toString).collect(Collectors.joining(" ")));
            log("Blocked: " + blockedEvidences.stream().map(Node::toString).collect(Collectors.joining(" ")));
        }

        // --- Create the new reduced graph
        Set<Edge> rEdges = freePaths.stream().flatMap(p -> p.getEdges().stream()).collect(Collectors.toSet());
        Set<Node> rNodes = rEdges.stream().flatMap(e -> e.getNodes().stream()).collect(Collectors.toSet());

        if (rNodes.isEmpty()) {
            return Optional.empty();
        } else {
            return Optional.of(new Graph(rNodes, rEdges));
        }
    }


    // --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- ---
    // --- INNER CLASS: BUILDER
    // --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- ---

    /**
     * Graph building class.
     */
    public static class Builder {

        // --- --- --- Fields

        public final Map<String, Node> nodes;

        public final Set<Edge> edges;

        // --- --- --- Constructor

        public Builder() {
            nodes = new HashMap<>();
            edges = new HashSet<>();
        }

        // --- --- --- Adding nodes in the graph

        private Optional<String> addNode(Node node) {
            if (nodes.containsKey(node.id)) {
                return Optional.of("Duplicated node " + node.id);
            } else {
                nodes.put(node.id, node);
                return Optional.empty();
            }
        }

        public Optional<String> addTarget(String nodeId) {
            return addNode(new Node(nodeId, Node.STATUS.TARGET));
        }

        public Optional<String> addEvidence(String nodeId) {
            return addNode(new Node(nodeId, Node.STATUS.EVIDENCE));
        }

        public Optional<String> addSimple(String nodeId) {
            return addNode(new Node(nodeId, Node.STATUS.SIMPLE));
        }

        // --- --- --- Adding edges in the graph

        private boolean existsLink(Node src, Node tgt) {
            return edges.stream().anyMatch(e -> e.isLink(src, tgt));
        }

        public Optional<String> addEdge(String source, String target) {
            Node src = nodes.get(source);
            Node tgt = nodes.get(target);
            // --- Checking...
            if (src == null) {
                return Optional.of("Source node " + source + " does not exist.");
            } else if (tgt == null) {
                return Optional.of("Target node " + source + " does not exist.");
            } else if (existsLink(src, tgt)) {
                return Optional.of("An edge between " + source + " and " + target + " already exists.");
            } else { // ... OK!
                edges.add(new Edge(src, tgt));
                return Optional.empty();
            }
        }

        // --- --- --- Build the graph

        public Graph build() {
            return new Graph(new HashSet<>(nodes.values()), edges);
        }
    }

    // --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- ---
    // --- PRIVATE - PRIVATE - PRIVATE - PRIVATE - PRIVATE - PRIVATE - PRIVATE - PRIVATE - PRIVATE - PRIVATE - PRIVATE
    // --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- ---


    // Return a List where start is the first node and target the last one
    private Set<List<Node>> DFS(Node start, Node target) {
        Deque<Node> stack = new ArrayDeque<Node>();
        return DFS(start, target, stack);
    }

    // Depth Firs Search
    private Set<List<Node>> DFS(Node current, Node target, Deque<Node> stack) {
        stack.push(current);
        Set<List<Node>> ret = new HashSet<>();

        // If we reach the destination, create a path (for now, a list of nodes)
        if (current.equals(target)) {
            List<Node> item = new ArrayList<>(stack);
            Collections.reverse(item);
            ret.add(item);
        } else {
            // Else, recursion on adjacent node that are not already in the path
            current.getAdjacent(this).forEach(n -> {
                if (!stack.contains(n)) {
                    ret.addAll(DFS(n, target, stack));
                }
            });
        }

        stack.pop();
        return ret;
    }

}