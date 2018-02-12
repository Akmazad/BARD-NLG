/**
 * This file is part of the BARD project
 * Monash Univeristy, Melbourne, Australia
 * 2018
 *
 * @author Dr. Matthieu Herrmann
 * The segment computation maintains its own graph representing a Bayesian Network layout.
 * This class represents the nodes of the graph.
 */

package Bard.NLG.Segment.Graph;

import Bard.NLG.Segment.Segment;
import Bard.NLG.Tools;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class Node {

    // --- --- --- Internal type

    /** Enumeration representing the status of a node: target, evidence or simple (i.e. not target neither evidence) */
    public enum STATUS {
        TARGET,
        SIMPLE,
        EVIDENCE
    }

    // --- --- --- Fields

    /***
     * Node's id. Should be unique in the graph.
     */
    public final String id;

    /**
     * Node's status: target, evidence or "simple" (i.e. other).
     * WARNING: only one target per graph is allowed!
     */
    public final STATUS status;

    // --- --- --- Constructor

    public Node(String id, STATUS status) {
        this.id = id;
        this.status = status;
    }

    // --- --- --- Node query methods

    /** For a node N, gets its children N->CHILDREN */
    public Set<Node> getChildren(Graph g) {
        return g.getChildren(this);
    }

    /** For a node N, gets its parents PARENTS -> N */
    public Set<Node> getParents(Graph g) {
        return g.getParents(this);
    }

    /** For a node N, gets both its parents and children PARENTS -> N <- CHILDREN */
    public Set<Node> getAdjacent(Graph g) {
        Set<Node> allNodes = new HashSet<>(getChildren(g));
        allNodes.addAll(getParents(g));
        return allNodes;
    }

    /** For a node N, gets its children and all its children's children */
    public Set<Node> getDescendants(Graph g) {
        Set<Node> children = new HashSet<>(getChildren(g));
        Set<Node> childrenChildren = children.stream().flatMap(c -> c.getDescendants(g).stream()).collect(Collectors.toSet());
        children.addAll(childrenChildren);
        return children;
    }

    /** Check this node N has queryNode as a child N -> Child ? */
    public boolean hasChild(Graph g, Node queryNode) {
        return getChildren(g).contains(queryNode);
    }

    /** Check this node N has queryNode as a parent Parent -> N ? */
    public boolean hasParent(Graph g, Node queryNode) {
        return getParents(g).contains(queryNode);
    }

    /** For a node N, gets its markov blanket */
    public Segment getMarkovBlanket(Graph g) {
        return g.getMarkovBlanket(this);
    }

    /** Test if a node N is in a common effect position */
    public boolean isCE(Graph g) {
        return g.isCE(this);
    }

    /** Test if a node is a target node */
    public boolean isTarget() {
        return status == STATUS.TARGET;
    }

    /** Test if a node is an evidence node */
    public boolean isEvidence() {
        return status == STATUS.EVIDENCE;
    }

    /** Test if a node is a simple nnode */
    public boolean isSimple() {
        return status == STATUS.SIMPLE;
    }


    // --- --- --- Mondanités contingentes

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Node node = (Node) o;
        return Objects.equals(id, node.id) &&
                status == node.status;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, status);
    }


    @Override
    public String toString() {
        String s;
        switch(status){
            case SIMPLE: s = "Simple"; break;
            case TARGET: s = "Target"; break;
            case EVIDENCE: s = "Evidence"; break;
            default: s=null;
                Tools.shouldNotHappen("Unknown node status: " + status);
        }
        return s + "(" + id + ")";
    }
}

