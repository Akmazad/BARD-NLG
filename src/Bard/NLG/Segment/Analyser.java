/**
 * This file is part of the BARD project
 * Monash Univeristy, Melbourne, Australia
 * 2018
 *
 * @author Dr. Matthieu Herrmann
 * Analyser for the segment computation. Main class.
 */

package Bard.NLG.Segment;

import Bard.NLG.Segment.Graph.Graph;
import Bard.NLG.Segment.Graph.Node;
import Bard.NLG.Segment.Graph.Path;

import static Bard.NLG.Tools.*;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Analyser extends Graph.Builder {

    // --- --- --- Public methods

    public List<RawSegment> getRawSegments() {
        return getSegments().stream().map(Segment::asRawSegment).collect(Collectors.toList());
    }

    public List<Segment> getSegments() {

        // Check the internal state of the graph builder.
        // If no nodes or no edges, return an empty list.
        if (nodes.isEmpty() || edges.isEmpty()) {
            return Collections.emptyList();
        }

        // Else build the graph and try to reduce it.
        log("Building graph... ");
        Graph inputGraph = build();
        log("... Done!");


        // --- --- --- ANALYSIS STARTS HERE

        // --- --- --- STEP 1: REDUCE THE GRAPH
        log("Building REDUCED graph... ");
        Optional<Graph> reducedGraph = inputGraph.getReducedGraph();
        log("... Done!");

        if (!reducedGraph.isPresent()) {
            // Could not reduce it...
            return Collections.emptyList();
        } else {
            Graph graph = reducedGraph.get();

            // --- --- --- STEP 2: CREATE THE ASSOCIATED "PATH TREE"
            PathTreeNode tree = PathTreeNode.mkRoot(graph, graph.target);
            for (Path p : graph.freePaths) {
                tree = tree.applyPath(p);
            }

            // --- --- --- STEP 3: REMOVING DUPLICATED BRANCHES/HANDLING LOOPS
            // --- A) Detect the loops.
            // WARNING: translation of the "old" scala version. New algo needed. This is dirty.
            List<PathTreeNode> myTrees = separate_old(graph, tree);
            // --- B) Postifx run & filter duplicated and evidences
            // WARNING: removing duplicated -> this is how we deal right now with triangles...
            List<Node> myOrderedTargets = new ArrayList<>();
            myTrees.forEach(t -> {
                myOrderedTargets.addAll(t.postFix(Function.identity()).stream().filter(n -> !n.isEvidence()).distinct().collect(Collectors.toList()));
            });

            // --- --- --- STEP 4: Get the markov blanket
            List<Segment> myOrderedMB = myOrderedTargets.stream().map(n -> n.getMarkovBlanket(graph)).collect(Collectors.toList());
            // A bit of logging
            log("mytrees:\n" + myTrees + "\n");
            log("myOrderedTarget:\n" + myOrderedTargets + "\n");
            log("Initial segments:");
            myOrderedMB.forEach(seg -> log(seg));

            // --- --- --- STEP 5: Remove stuff
            Set<Node> targetSet = new HashSet<>();
            List<Segment> orderedSegments = new ArrayList<>();

            myOrderedMB.forEach(mb -> {
                // --- Remove previous targets
                Segment s = mb.remove(targetSet);
                // --- Extends the accumulator:
                targetSet.add(s.target);
                // --- Extends the list, in the good order == add front
                orderedSegments.add(0, s);
            });

            log("\nFinal segments:");
            orderedSegments.forEach(seg -> log(seg));
            return orderedSegments;
        }
    }


    // --- --- --- Separation of the tree in several trees. OLD VERSION

    // Loop finding - Old version
    private Map<Set<Node>, List<Node>> findCELoop_old(PathTreeNode ptn) {

        // Get all common effect in sub branches (getCE is recursive)
        Map<Node, Set<Node>> mapCESet = new HashMap<>();
        for (Map.Entry<Node, PathTreeNode> e : ptn.children.entrySet()) {
            mapCESet.put(e.getKey(), e.getValue().getAllCE());
        }

        // Reverse the map: a set of common effect give us the list of affected children.
        Map<Set<Node>, List<Node>> revMap = new HashMap<>();
        for (Map.Entry<Node, Set<Node>> e : mapCESet.entrySet()) {
            // Get the current list or create a new empty list and add the key in the list
            List<Node> currentList = revMap.getOrDefault(e.getValue(), new ArrayList<>());
            currentList.add(e.getKey());
            revMap.put(e.getValue(), currentList);
        }

        // Only keep the set larger than 1
        return revMap.entrySet().stream().filter(e -> e.getKey().size() > 1).
                collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }


    // Create an ordered list of Path tree node used if a cut needs to be made
    // The "base" is the first item of the list, removed parts come after.
    public List<PathTreeNode> separate_old(Graph graph, PathTreeNode ptn) {
        Map<Set<Node>, List<Node>> CEMap = findCELoop_old(ptn);
        // Separation if CEMap is non empty
        if (!CEMap.isEmpty()) {
            // --- Collect the "separation points"
            Map<Node, List<Node>> sepPoints = new HashMap<>();
            CEMap.forEach((key, values) -> {
                // Only consider list of size > 1
                // List = 1 is more than on common effect for one child.
                if (values.size() <= 1) {
                    // --- --- --- Skip if size == 1, but still perform an extra control
                    if (values.isEmpty()) {
                        shouldNotHappen("Share common effect with no values?");
                    }
                } else {
                    if (values.size() > 2) {
                        shouldNotHappen("Loop breaking limited to 2 CE... " + values);
                    }
                    // For now, limited to loop with 2 CE
                    Node first = values.get(0);
                    Node last = values.get(1);
                    // Get the path between the items and drop them as they are extremities:
                    List<Node> treePath = ptn.DFS(first, last);
                    if (!treePath.isEmpty()) {
                        Path path = Path.build(graph, treePath);
                        Optional<Node> cutNode = path.findPL();
                        if (cutNode.isPresent()) {
                            log("<<<Separate: " + cutNode + " for path " + path + ">>>");
                            sepPoints.put(cutNode.get(), path.nodeList);
                        } else {
                            shouldNotHappen("Could not find a cutting point in a 2 CE Loop");
                        }
                    }
                }
            });
            // --- Rebuild the tree while removing the breakpoints:
            PathTreeNode baseTree = ptn.copyFilter(sepPoints.keySet());
            // --- Add the "breakpoint"
            // Find each subtree and remove the "common nodes"
            List<PathTreeNode> res = new ArrayList<>();
            res.add(baseTree);
            sepPoints.forEach((node, listNode) -> {
                Optional<PathTreeNode> opt = ptn.findSubTree(node);
                if (opt.isPresent()) {
                    res.add(opt.get().copyFilter(new HashSet<>(listNode)));
                } else {
                    shouldNotHappen("Subtree not found for " + node.id);
                }
            });

            return res;

        } else {
            // No separation
            return Collections.singletonList(ptn);
        }
    }


}
