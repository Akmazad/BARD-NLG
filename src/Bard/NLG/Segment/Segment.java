/**
 * This file is part of the BARD project
 * Monash Univeristy, Melbourne, Australia
 * 2018
 *
 * @author Dr. Matthieu Herrmann
 * Final product of the segment computation: a modified markov blanket of a node.
 * A segment is essentialy a markov blanket, by some node may be removed.
 */


package Bard.NLG.Segment;

import Bard.NLG.Segment.Graph.Node;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Segment {

    // --- --- --- Fields

    /**
     * Target node of the segment. Immutable
     */
    public final Node target;

    /**
     * Set of causal nodes of the segment. Immutable
     */
    public final Set<Node> causal;

    /**
     * Set of antiCausal nodes of the segment. Immutable
     */
    public final Set<Node> antiCausal;

    /**
     * Map of (common effect, immutable set of alternate causes) of the segment. Immutable
     */
    public final Map<Node, Set<Node>> commonEffect;

    // --- --- --- Constructor

    /**
     * Create a new segment for the target with the specified collection of causal, anticausal and common effect nodes.
     * Internally copies and freezes the collections.
     */
    public Segment(Node target, Set<Node> causal, Set<Node> antiCausal, Map<Node, Set<Node>> commonEffect) {
        this.target = target;
        this.causal = Collections.unmodifiableSet(new HashSet<>(causal));
        this.antiCausal = Collections.unmodifiableSet(new HashSet<>(antiCausal));
        // Freezing commonEffect is a bit longer...
        this.commonEffect = Collections.unmodifiableMap(commonEffect.entrySet().stream().collect(
                Collectors.toMap(Map.Entry::getKey, kv -> Collections.unmodifiableSet(kv.getValue()))
        ));
    }

    // --- --- --- Public methods

    /**
     * Return a new segment where the specified nodes have been removed.
     * If a common effect is removed, the alternate causes are also remove.
     * If a common effect does not have alternate causes anymore, it becomes an anticause
     */
    public Segment remove(Set<Node> removeUs) {
        Set<Node> c = new HashSet<>(causal);
        c.removeAll(removeUs);

        Set<Node> ac = new HashSet<>(antiCausal);
        ac.removeAll(removeUs);

        Map<Node, Set<Node>> ceMap = new HashMap<>();
        commonEffect.forEach((Node ce, Set<Node> altC) -> {
            if (removeUs.contains(ce)) {
                /* Nothing: we skip it, i.e. we do not add it in ceMap */
            } else {
                // The common effect is not removed: check the alternate causes
                Set<Node> newAltC = new HashSet<>(altC);
                newAltC.removeAll(removeUs);
                if (newAltC.isEmpty()) {
                    // No alternate causes left: the common effect becomes a simple anti cause
                    ac.add(ce);
                } else {
                    // We still have some alternate causes: update the map
                    ceMap.put(ce, newAltC);
                }
            }
        });

        return new Segment(target, c, ac, ceMap);
    }

    /**
     * Get the number of node in a segment
     */
    public int size() {
        Set<Node> setNodesCE = commonEffect.entrySet().stream()
                .flatMap( e-> Stream.concat(Stream.of(e.getKey()), e.getValue().stream()) ).collect(Collectors.toSet());
        return (1 + causal.size() + antiCausal.size() + setNodesCE.size());//  '1' : count the target
    }

    /**
     * Conversion into a raw segment
     */

    public RawSegment asRawSegment(){

        Map<String, Set<String>> ce = commonEffect.entrySet().stream().collect(Collectors.toMap(
                e -> e.getKey().id,
                e -> e.getValue().stream().map(n -> n.id).collect(Collectors.toSet())
        ));

        return new RawSegment(target.id,
                causal.stream().map(n->n.id).collect(Collectors.toSet()),
                antiCausal.stream().map(n->n.id).collect(Collectors.toSet()),
                ce);

    }

    // --- --- --- Mondanités contingentes

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Segment segment = (Segment) o;
        return Objects.equals(target, segment.target) &&
                Objects.equals(causal, segment.causal) &&
                Objects.equals(antiCausal, segment.antiCausal) &&
                Objects.equals(commonEffect, segment.commonEffect);
    }

    @Override
    public int hashCode() {
        return Objects.hash(target, causal, antiCausal, commonEffect);
    }

    @Override
    public String toString() {
        return target.id + ":" +
                " [C] " + causal.stream().map(n -> n.id).collect(Collectors.joining(", ")) +
                " [AC] " + antiCausal.stream().map(n -> n.id).collect(Collectors.joining(", ")) +
                " [CE] " + commonEffect.entrySet().stream()
                .map(kv -> "(" + kv.getKey().id + ", " + kv.getValue().stream().map(n->n.id).collect(Collectors.joining(", ")) + ")")
                .collect(Collectors.joining(", ")) +
                " [Size: " + size() + "]";
    }
}