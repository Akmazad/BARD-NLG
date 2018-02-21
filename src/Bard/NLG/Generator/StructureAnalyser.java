/**
 * This file is part of the BARD project
 * Monash Univeristy, Melbourne, Australia
 * 2018
 *
 * @author Dr. Matthieu Herrmann
 */

package Bard.NLG.Generator;

import Bard.NLG.Tools;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Comparator.comparing;

public class StructureAnalyser {

    // --- --- --- Inner classes

    /** A rule is a list of antecedents and a list of consequents.
     * Note that the order inside the list matters:
     *  * antecedents are ordered according to their appearance order when generating the rules
     *  * consequents are ordered according to their "depth"
     */
    public static class Rule {

        // --- --- --- Fields

        // Immutable set
        public final List<String> sources;

        // Immutable set
        public final List<String> targets;

        // --- --- --- Constructor

        public Rule(Set<String> sources, List<String> targets, Map<String, Integer> keyOrder) {

            // Always same order for the keys
            this.sources = Tools.freeze(sources.stream().sorted(comparing(keyOrder::get)).collect(Collectors.toList()));

            // We want to order the target from "smallest" to "largest" in the amount of node the allow to reach...
            this.targets = Tools.freeze(targets);
        }

        // --- --- --- Mondanités contingentes

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Rule rules = (Rule) o;
            return Objects.equals(sources, rules.sources) &&
                    Objects.equals(targets, rules.targets);
        }

        @Override
        public int hashCode() {

            return Objects.hash(sources, targets);
        }

        @Override
        public String toString() {
            String src;
            if (sources.isEmpty()) {
                src = "∅";
            } else {
                src = sources.stream().collect(Collectors.joining(" ∧ "));
            }
            return src + " ⇒ " + targets.stream().collect(Collectors.joining(" ∧ "));
        }
    }

    /** A causal edge represent a directed link from the BN. */
    public static class CausalEdge {

        // --- --- --- Fields

        public final String source;
        public final String target;

        // --- --- --- Constructor

        public CausalEdge(String source, String target) {
            this.source = source;
            this.target = target;
        }

        // --- --- --- Equals & HashCode & toString

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            } else if (o == null || getClass() != o.getClass()) {
                return false;
            } else {
                CausalEdge that = (CausalEdge) o;
                return source.equals(that.source) && target.equals(that.target);
            }
        }

        @Override
        public int hashCode() {
            return Objects.hash(source, target);
        }

        @Override
        public String toString() {
            return "causalEdge{" +
                    "source='" + source + '\'' +
                    ", target='" + target + '\'' +
                    '}';
        }
    }

    // --- --- --- Fields

    /**
     * Input set of causal edges. Immutable.
     */
    public final Set<CausalEdge> edges;

    /**
     * Mapping (target, {set of parents}). Immutable
     * Contains an entry for every node, even the node without any parent (associated to the empty set).
     * The set of key is the set of all nodes.
     */
    public final Map<String, Set<String>> tgt_parents;

    /**
     * Relation mapping ({sources}, {targets}). Immutable.
     * Target ar ordered from the low depth to the height depth.
     */
    public final Map<Set<String>, List<String>> rules_map;

    /**
     * The "depth" of each node, i.e. how many node we can reach from it. Immutable
     */
    public final Map<String, Integer> node_depth;

    /**
     * "Depth Comparator"
     */
    public final Comparator<String> depthCmp;

    /**
     * Antecedent multiplication coefficient use for sorting.
     * The number of "antecedent" is prevalent when sorting rules.
     * As the "sorting score" of a rule is just one number, we need to translate a 2 dimension criteria
     * (arity of antecedents, arity of descendants) in one dimension.
     * Hence, antecedentCoef is the maximum arity of descendants.
     */
    public final int antecedentCoef;


    // --- --- --- Constructor

    public StructureAnalyser(Set<CausalEdge> e) {
        // --- --- --- Freeze input set
        edges = Tools.freeze(e);


        // --- --- --- Init tgt_parents:
        // --- Create an empty set per node
        Map<String, Set<String>> tgt_parents_ = new HashMap<>();
        for (CausalEdge ce : edges) {
            Stream.of(ce.target, ce.source).forEach((String nodeName) -> {
                if (!tgt_parents_.containsKey(nodeName)) {
                    tgt_parents_.put(nodeName, new HashSet<>());
                }
            });
        }

        // --- Build the set per node
        for (CausalEdge ce : edges) {
            // Init gives us fully initialized map.
            Set<String> antecedents = tgt_parents_.get(ce.target);
            antecedents.add(ce.source);
        }

        tgt_parents = Tools.freezeMapSet(tgt_parents_);

        // --- --- --- Get the antecedentCoef
        antecedentCoef = tgt_parents.values().stream().mapToInt(Set::size).max().orElse(1);


        // --- --- --- Depth of node
        Map<String, Integer> node_depth_ = new HashMap<>();
        for (String n : tgt_parents.keySet()) {
            int d = depth(n);
            node_depth_.put(n, d);
        }

        node_depth = Tools.freeze(node_depth_);

        // --- Associated comparator: take the depth of a node
        depthCmp = Comparator.comparingInt(node_depth::get);


        // --- --- --- Init the relation map (retro mapping)
        // --- Build the map
        Map<Set<String>, List<String>> rules_map_ = new HashMap<>();
        for (Map.Entry<String, Set<String>> entry : tgt_parents.entrySet()) {
            String tgt = entry.getKey();
            Set<String> antecedents = entry.getValue();

            List<String> consequents;
            if (rules_map_.containsKey(antecedents)) {
                consequents = rules_map_.get(antecedents);
            } else {
                consequents = new ArrayList<>();
                rules_map_.put(antecedents, consequents);
            }
            consequents.add(tgt);
        }

        // --- Sort consequents. Note: in-place sorting!
        rules_map_.forEach((key, value) -> value.sort(depthCmp));
        rules_map = Tools.freeze(rules_map_);

    }

    // --- --- --- Helpers

    /**
     * See how far a node is reaching.
     */
    private int depth(String node) {
        Set<String> children = edges.stream().filter(e -> e.source.equals(node)).map(e -> e.target).collect(Collectors.toSet());
        int result = children.size();
        result += children.stream().map(this::depth).mapToInt(Integer::intValue).sum();
        return result;
    }

    /**
     * Add a list in a stack, reverse order.
     * A bit hacky, but java does not offer any easy built-in way to do this.
     * Anyway, we are using ArrayList so this is not terrible.
     */
    private void pushList(List<String> l, Deque<String> s) {
        for (int i = l.size() - 1; i >= 0; --i) {
            s.push(l.get(i));
        }
    }

    // --- --- --- Public Methods

    public List<Rule> getRules() {

        // --- --- --- Global Data
        // Will contains the final result
    	List<Rule> result = new ArrayList<>();
    	if(!this.rules_map.isEmpty()) {
    		// Working mutable copy of rules_map: we will remove rules from that map.
    		Map<Set<String>, List<String>> working_rules_map = new HashMap<>(this.rules_map);
    		// Allow to always show the antecedents in the same order
    		Map<String, Integer> keyOrder = new HashMap<>();

    		// --- --- --- Local Data
    		// Stack of nodes to be added progressively in the "available" set
    		Deque<String> stack_nodeToBeAdded = new ArrayDeque<>();
    		// Set of nodes available to trigger a rule
    		Set<String> available = new HashSet<>();

    		// --- --- --- Sorting
    		// --- Init. Note: push => reverse order, so start with "height" depth here!
    		working_rules_map.get(Collections.emptySet()).forEach(stack_nodeToBeAdded::push);
    		working_rules_map.remove(Collections.emptySet());
    		getRules(result, working_rules_map, keyOrder, available, stack_nodeToBeAdded);
    	}
        return result;
    }


    /**
     * Private helper, working code, for the getRule function.
     */
    private void getRules(
            // "Global"
            List<Rule> result,
            Map<Set<String>, List<String>> working_rules_map,
            Map<String, Integer> keyOrder,
            // "Local"
            Set<String> available,
            Deque<String> stack_nodeToBeAdded
    ) {
        // --- While we have node to add in the "available" set, continue.
        while (!stack_nodeToBeAdded.isEmpty()) {
            String top = stack_nodeToBeAdded.pop();
            available.add(top);

            // Continue while we can trigger new rules
            boolean goOn = true;
            while (goOn) {

                // Try to find a matching rules
                Optional<Map.Entry<Set<String>, List<String>>> opkv =
                        working_rules_map.entrySet().stream()
                                .filter(e -> available.containsAll(e.getKey()))
                                // Get the "smallest" arity first, penalizing more the keys (and relation in a key)
                                .min(Comparator.comparing(o -> o.getKey().size()*antecedentCoef + o.getValue().size()));


                if (opkv.isPresent()) {
                    // Get info
                    Set<String> key = opkv.get().getKey();
                    List<String> val = opkv.get().getValue();

                    // Update the key order with new key only.
                    key.forEach(k -> keyOrder.putIfAbsent(k, keyOrder.size()));

                    // Create the rule
                    Rule r = new Rule(key, val, keyOrder);
                    result.add(r);

                    // --- Now, we want to continue exploring "locally"
                    Set<String> local_available = new HashSet<>(key);   // What allowed to trigger the rule is available
                    Deque<String> local_stack = new ArrayDeque<>();     // New nodes go in the stack
                    pushList(val, local_stack);

                    // Remove the rule from the set BEFORE recursive call:
                    working_rules_map.remove(key);

                    // Call "locally"
                    getRules(result, working_rules_map, keyOrder, local_available, local_stack);

                    // Get the nodes made "locally" available
                    available.addAll(local_available);

                } else {
                    goOn = false;
                }
            }
        } // End while ! stack.IsEmpty
    }
}
