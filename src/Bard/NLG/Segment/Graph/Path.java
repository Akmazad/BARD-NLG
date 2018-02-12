/**
 * This file is part of the BARD project
 * Monash Univeristy, Melbourne, Australia
 * 2018
 *
 * @author Dr. Matthieu Herrmann
 * The segment computation maintains its own graph representing a Bayesian Network layout.
 * This class represents a path in a graph.
 */

package Bard.NLG.Segment.Graph;

import static Bard.NLG.Tools.*;

import java.util.*;
import java.util.prefs.NodeChangeListener;
import java.util.stream.Collectors;


public class Path {

    // --- --- --- Inner type

    /**
     * Path Status
     */
    enum STATUS {
        /**
         * Path is not blocked - The field 'CEIndex' contains the index of common effects.
         */
        FREE,
        /**
         * Path is blocked by a chain - The field 'blocking' contains the blocking evidence.
         */
        CHAIN,
        /**
         * Path is blocked by a common cause - The field 'blocking' contains the blocking evidence.
         */
        CC,
        /**
         * Path is blocked because no common effect was found.
         */
        NOCE
    }


    // --- --- --- Fields

    /**
     * First node of the path. Immutable.
     */
    public final Node first;

    /**
     * First node of the path. Immutable.
     */
    public final Node last;

    /**
     * Inner nodes of the path, i.e. other than first and last.
     */
    public final List<Node> inners;

    /**
     * Set of nodes. Immutable.
     */
    public final Set<Node> nodeSet;

    /**
     * Ordered list of nodes composing the path. Immutable.
     */
    public final List<Node> nodeList;

    /**
     * The length of the path, i.e. the number of link and NOT the number of nodes!
     * A-B-C as 3 nodes but is of length 2, and in general, length = nodeList.size()-1
     */
    public final int length;

    /**
     * Set of edges. Immutable.
     */
    public final List<Edge> edges;

    /**
     * Status of the path. See the STATUS enum.
     */
    public final STATUS status;

    /**
     * STATUS - CEIndex: contains the index of common effects when the path is free.
     * Empty list if status != FREE.
     * Immutable.
     */
    public final List<Integer> CEIndex;

    /**
     * Mapping CEIndex on nodes. Immutable set.
     */
    public final Set<Node> CENodes;

    /**
     * STATUS - blocking:
     * Contains the blocking evidence when the path is blocked by a 'chain' or 'common cause' condition.
     * null if status != CC or status != CHAIN.
     * Immutable.
     */
    public final Node blocking;

    /**
     * The graph the path lives in.
     */
    public final Graph graph;


    // --- --- --- Constructors

    /**
     * Private constructor.
     * Can only be called with a complete set of info, and in particular a computed STATUS.
     * See the static function 'build'.
     *
     * @param g     Graph the path lives in
     * @param f     First node of the path
     * @param ins   Inner nodes of the path
     * @param l     Last node of the path
     * @param s     Status of the path
     * @param ceidx Index of common effects (see STATUS)
     * @param bl    Blocking node (see STATUS)
     */
    private Path(Graph g, Node f, List<Node> ins, Node l, STATUS s, List<Integer> ceidx, Node bl) {
        graph = g;

        // Store the nodes
        first = f;
        inners = freeze(ins);
        last = l;

        // Create the nodeList from the above nodes, and the nodeSet
        ArrayList<Node> list = new ArrayList<>(ins);
        list.add(0, first);   // add first
        list.add(last);         // add last
        nodeList = freeze(list);
        nodeSet = freeze(new HashSet<Node>(nodeList));

        // Lenght of the path
        length = 1 + inners.size();

        // Store the status info and compute the mapping on CEIndex -> CENodes
        status = s;
        blocking = bl;
        CEIndex = ceidx;
        CENodes = freeze(CEIndex.stream().map(nodeList::get).collect(Collectors.toSet()));

        // Get the list of all edges
        ArrayList<Edge> allEdges = new ArrayList<Edge>();
        for (int idx = 0; idx < length; ++idx) {
            Node n = nodeList.get(idx);
            Node next = nodeList.get(idx + 1);
            if (n.hasParent(g, next)) {
                allEdges.add(new Edge(next, n));
            } else {
                allEdges.add(new Edge(n, next));

            }
        }
        edges = freeze(allEdges);

    }

    /**
     * Build a path from a list of node, and use the graph to test if it is free or blocked.
     * Must contains at least 2 nodes.
     */
    public static Path build(Graph g, List<Node> nodes) {
        if (nodes.size() < 2) {
            shouldNotHappen("Creating a path with less than two nodes (" + nodes.toString() + ")");
        }

        // Analyse if the path is blocked in the context of the graph
        CBRes cbres = computeBlocked(g, nodes);

        // Build the path
        return new Path(g,
                nodes.get(0),
                nodes.subList(1, nodes.size() - 1), // [INCLUSIVE, EXCLUSIVE[
                nodes.get(nodes.size() - 1),
                cbres.status,
                cbres.CEIndex,
                cbres.blocking
        );
    }


    // --- --- --- Public methods

    /**
     * Get the set of nodes. Return an immutable set.
     */
    public Set<Node> getNodeSet() {
        return nodeSet;
    }

    /**
     * Get the list of edges. Return an immutable list.
     */
    public List<Edge> getEdges() {
        return edges;
    }

    /**
     * Test if a path is blocked.
     */
    public boolean isBlocked() {
        // FREE is the only status that does not represent a blocked path
        return status != STATUS.FREE;
    }

    /**
     * Test if a path is free.
     */
    public boolean isFree() {
        return status == STATUS.FREE;
    }

    /**
     * Test if a path has some common effects
     */
    public boolean hasCE() {
        if (status == STATUS.FREE) {
            return !CEIndex.isEmpty();
        } else {
            return false;
        }
    }

    /**
     * Check if a given node index is the index of a node with common effect in the path
     */
    public boolean isCEIndex(int idx) {
        return CEIndex.contains(idx);
    }

    /**
     * Get the set of common effect nodes. Return an immutable set.
     */
    public Set<Node> getCENodes() {
        return CENodes;
    }

    /**
     * Shave a path, i.e. remove its first item. If the path only contains two node, return the last node.
     * Warning: the path must be free! Shaving a blocked path does not make sens.
     */
    public Either<Path, Node> shave() {
        if (isFree()) {
            if (inners.isEmpty()) {
                return Either.right(last);
            } else {
                Node head = inners.get(0);
                List<Node> tail = freeze(inners.subList(1, inners.size()));
                // Adjust the common effect indexes (substract 1n remove the one < 0)
                List<Integer> ceIdx = CEIndex.stream().map(i -> i - 1).filter(i -> i >= 0).collect(Collectors.toList());
                // Return the shaved path
                return Either.left(new Path(graph, head, tail, last, status, ceIdx, blocking));
            }
        } else {
            shouldNotHappen("Path shaving is only applicable on free paths.");
            return null;    // Satisfy flow analysis
        }
    }

    /**
     * Find a purely causal node on the path, ie TARGET .... N <- PurelyCausal -> M ... EVIDENCE
     */
    public Optional<Node> findPL() {
        if (nodeList.size() > 2) {
            // Starts with the second node, start before the last.
            for (int idx = 1; idx < nodeList.size() - 1; ++idx) {
                Node nBefore = nodeList.get(idx - 1);
                Node n = nodeList.get(idx);
                Node nAfter = nodeList.get(idx + 1);
                if (n.hasChild(graph, nBefore) && n.hasChild(graph, nAfter)) {
                    return Optional.of(n);
                }
            }
        }
        return Optional.empty();
    }

    // --- --- --- Mondanités contingentes

    @Override
    public String toString() {
        return toStringBC_pfx() + " " + toStringPath();
    }


    // --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- ---
    // --- PRIVATE - PRIVATE - PRIVATE - PRIVATE - PRIVATE - PRIVATE - PRIVATE - PRIVATE - PRIVATE - PRIVATE - PRIVATE
    // --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- ---

    /**
     * Helper class for the result of the computeBlocked function
     */
    private static class CBRes {
        public final STATUS status;
        public final Node blocking;
        public final List<Integer> CEIndex;

        /**
         * For CHAIN blocking situations.
         */
        public static CBRes CHAIN(Node bl) {
            return new CBRes(STATUS.CHAIN, bl, Collections.emptyList());
        }

        /**
         * For CC blocking situation.
         */
        public static CBRes CC(Node bl) {
            return new CBRes(STATUS.CC, bl, Collections.emptyList());
        }

        /**
         * For NOCE blocking situation.
         */
        public static CBRes NOCE() {
            return new CBRes(STATUS.NOCE, null, Collections.emptyList());
        }

        /**
         * For FREE situation, with common effect indexes.
         */
        public static CBRes FREE(List<Integer> l) {
            return new CBRes(STATUS.FREE, null, freeze(l));
        }

        /**
         * Private constructor. Use factory static functions.
         */
        public CBRes(STATUS s, Node bl, List<Integer> l) {
            status = s;
            blocking = bl;
            CEIndex = l;
        }
    }


    /**
     * USE FOR CONSTRUCTION ONLY
     * For a graph and the path represented as a list of nodes,
     * compute the blocking condition and update the internal status.
     */
    private static CBRes computeBlocked(Graph g, List<Node> nodeList) {

        // Declare a list to store the indexes of node affected by a common effect
        List<Integer> CEIndex = new ArrayList<Integer>();

        // We only consider intermediate node, i.e. path longer than 2
        // [0] | -- [1] -- .... -- [n-1] -- | [n]
        // This also assure us that there is always a 'previous' and a 'next' node in the path.
        for (int idx = 1; idx < nodeList.size() - 1; ++idx) {
            // --- --- --- Get the node
            Node N = nodeList.get(idx);
            Node prev = nodeList.get(idx - 1);
            Node next = nodeList.get(idx + 1);

            // --- --- --- 'Chain' and 'Common Cause'
            if (N.isEvidence()) {
                //  'Chain' condition:  prev -> N -> next  OR  prev <- N <- next
                if (isChain(g, prev, N, next) || isChain(g, next, N, prev)) {
                    return CBRes.CHAIN(N);
                }
                // 'Common cause' condition: prev <- N -> next
                if (isCC(g, prev, N, next)) {
                    return CBRes.CC(N);
                }
            }

            // --- --- --- 'Common effect' condition: prev -> N[descendant] <-next
            // First, detect the opportunity of a common effect, i.e. the shape prev -> N <-next
            if (canCE(g, prev, N, next)) {
                if (N.isEvidence() || anyDescendantHasEvidence(g, N)) {
                    // Path "freed" by evidence (store the index as a common effect index)
                    CEIndex.add(idx);
                } else {
                    // Else, path is blocked by "no Common Effect"
                    return CBRes.NOCE();
                }
            }

        } // End for( idx ...)

        // Path is free: return the index of nodes "unblocked" by common effect
        return CBRes.FREE(CEIndex);
    } // END computeBlocked


    /**
     * Helper for the "computeBlocked" function:
     * Check chain between 3 nodes: prev -> center -> next
     */
    private static boolean isChain(Graph g, Node prev, Node center, Node next) {
        return prev.hasChild(g, center) && center.hasChild(g, next);
    }

    /**
     * Helper for the "computeBlocked" function
     * Check common cause between 3 nodes (center causes the two other): prev <- center -> next
     */
    private static boolean isCC(Graph g, Node prev, Node center, Node next) {
        return center.hasChild(g, prev) && center.hasChild(g, next);
    }

    /**
     * Helper for the "computeBlocked" function
     * Check if the topology may lead to a common effect (center is caused by the two other): prev -> center <- next
     */
    private static boolean canCE(Graph g, Node prev, Node center, Node next) {
        return center.hasParent(g, prev) && center.hasParent(g, next);
    }

    /**
     * Helper for the "computeBlocked" function
     * Check if any descendant of n has evidence
     */
    private static boolean anyDescendantHasEvidence(Graph g, Node n) {
        return n.getDescendants(g).stream().anyMatch(Node::isEvidence);
    }


    /**
     * Helper for the toString function.
     */
    private String toStringBC_pfx() {
        switch (status) {
            case FREE:
                return CEIndex.isEmpty() ? "[Free          ]" : "[Free - CE     ]";
            case CHAIN:
                return "[Blocked: Chain]";
            case CC:
                return "[Blocked: CC   ]";
            case NOCE:
                return "[Blocked: noCE ]";
            default:
                shouldNotHappen("Unexpected path status code: " + status);
                return null; // Satisfy flow analysis.
        }
    }

    /**
     * Helper for the toString function.
     */
    private String toStringPath() {
        StringBuffer b = new StringBuffer();

        // Put the first node that always exists (can't have a common effect)
        b.append(first.id);

        // Start at the second node (as we look "before us"), until the last
        for (int idx = 1; idx < nodeList.size(); ++idx) {
            Node prev = nodeList.get(idx - 1);
            Node N = nodeList.get(idx);
            // Print oriented arrows
            if (N.hasParent(graph, prev)) {
                b.append(" -> ");
            } else {
                b.append(" <- ");
            }
            // Print the node
            b.append(N.id);
            // Add the common effect indicator
            if (isCEIndex(idx)) {
                b.append("*");
            }
        }

        return b.toString();
    }

}
