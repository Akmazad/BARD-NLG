/** This file is part of the BARD project
 * Monash Univeristy, Melbourne, Australia
 * 2018
 * @author Dr. Matthieu Herrmann
 *
 * The segment computation maintains its own graph representing a Bayesian Network layout.
 * This class represents the edges of the graph.
 */

package Bard.NLG.Segment.Graph;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class Edge {

    // --- --- --- Fields

    /** Edge's source node */
    public final Node source;

    /** Edge's target node */
    public final Node target;

    // --- --- --- Constructor

    /** Build an edges from a source node to a target node */
    public Edge(Node source, Node target){
        this.source = source;
        this.target = target;
    }

    // --- --- --- Methods

    /** Get the edge's nodes as a set {source, target} */
    public Set<Node> getNodes(){
        return new HashSet<>(Arrays.asList(source, target));
    }

    /** Test if the edges is made of n1 and n2, regardless of the direction */
    public boolean isLink(Node n1, Node n2){
        return (source.equals(n1) && target.equals(n2)) || (source.equals(n2) && target.equals(n1));
    }

    // --- --- --- Mondanités contingentes

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Edge edge = (Edge) o;
        return Objects.equals(source, edge.source) &&
                Objects.equals(target, edge.target);
    }

    @Override
    public int hashCode() {
        return Objects.hash(source, target);
    }
}
