package Bard.NLG.Usefulness;

import Bard.NLG.Tools;
import agenariskserver.Net;
import agenariskserver.Node;

import java.lang.reflect.Array;
import java.util.*;
import java.util.stream.Collectors;

public class Analyser {

    // --- --- --- Fields
    public final Net net;
    public final String target;
    public final String targetState;
    public final Map<String, String> evidences;

    // --- --- --- Constructor

    public Analyser(Net net, String target, String targetState, Map<String, String> evidences){
        this.net = net;
        this.target = target;
        this.targetState = targetState;
        this.evidences = Tools.freeze(evidences);
    }

    // --- --- --- Public methods

    /**
     * Returns the "useful" evidences.
     */
    public Set<String> getUseful() throws Exception {

        // --- --- --- Data:
        Set<String> ev = evidences.keySet();
        HashMap<Set<String>, Double> deltas = new HashMap<>();

        // --- --- --- Compute permutations (powerSet):
        // --- Start with empty set, i.e. no evidences.
        net.clearAllEvidence();
        net.compile();
        Node tNode = net.getNode(target);
        Double tNode0 = tNode.getBelief(targetState);
        deltas.put(Collections.emptySet(), 0d);

        // --- Add evidences one by one with copy:
        // Step 1 {} (+) A ==> {}, {A}
        // Step 2 {}, {A} (+) B => {}, {A}, {B}, {A, B}
        // Step 3 {}, {A}, {B}, {A, B} (+) C => {}, {A}, {B}, {A, B}, {C}, {A, C}, {B, C}, {A, B, C}
        // etc... As expected, the size of the set is doubling at each step (|P(X)| = 2^|X|)
        for(String newEv: ev) {
            // Warning: copy needed as we are updating "deltas" while iterating.
            Set<Set<String>> currentSets = Tools.freeze(deltas.keySet());
            for(Set<String> cs: currentSets){
                // Copy and add:
                Set<String> newSet = new HashSet<>(cs);
                newSet.add(newEv);

                // --- --- --- Computations
                // WARNING: clear all evidences!
                net.clearAllEvidence();
                // Then set all current evidences
                for(String nodeName: newSet){
                    String stateName = evidences.get(nodeName);
                    net.getNode(nodeName).setEvidenceState(stateName);
                }
                net.compile();
                Double delta = Math.abs(net.getNode(target).getBelief(targetState)-tNode0);

                // --- --- --- Update the map
                deltas.put(newSet, delta);
            }
        }

        // --- --- --- Magic stuff here ?

        Map<String, double[]> res = new HashMap<>();

        for(String evName: evidences.keySet()){

            double [] a = new double[2];
            double wiAcc = 0;
            double woAcc = 0;
            int wiNb = 0;
            int woNb = 0;

            for(Map.Entry<Set<String>, Double> entry: deltas.entrySet()){
                if (entry.getKey().contains(evName)) {
                    wiAcc += entry.getValue();
                    wiNb++;
                } else {
                    woAcc += entry.getValue();
                    woNb++;
                }
            }

            a[0] = wiAcc/wiNb;
            a[1] = woAcc/(woNb);

            System.out.println(evName + " WITH " + a[0] + " WITHOUT " + a[1]);

            res.put(evName, a);

        }





/*


        // --- Printing


            // --- --- --- ALL
            for(int i = 0; i<=evidences.size(); ++i){
                final Double d = (double) i;
                System.out.println("--- --- --- SIZE " + i + " --- --- ---");
                Map<String, Double> v = deltas.entrySet().stream().filter(e -> e.getKey().size() == d).collect(
                        Collectors.toMap(e ->e.getKey().stream().sorted().collect(Collectors.toList()).toString(), Map.Entry::getValue)
                );
                v.forEach((key, val) -> System.out.println(key + " => " + val));
            }



            // --- --- --- For a given node
            String interest = "Radar Report";

            Map<Set<String>, Double> with = deltas.entrySet().stream()
                    .filter(e -> e.getKey().contains(interest))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            Map<Set<String>, Double> without = deltas.entrySet().stream()
                    .filter(e -> !e.getKey().contains(interest))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            System.out.println("\n\n");
            for(int i = 0; i<=evidences.size(); ++i){
                final Double d = (double) i;
                System.out.println("--- --- --- WITH ++ SIZE " + i + " --- --- ---");
                Map<String, Double> v =
                        with.entrySet().stream()
                                .filter(e -> e.getKey().size() == d)
                                .collect(Collectors.toMap(
                                        e ->e.getKey().stream().sorted().collect(Collectors.toList()).toString(),
                                        Map.Entry::getValue));
                v.forEach((key, val) -> System.out.println(key + " => " + val));
            }

            System.out.println("\n\n");

            for(int i = 0; i<=evidences.size(); ++i){
                final Double d = (double) i;
                System.out.println("--- --- --- WITHOUT ++ SIZE " + i + " --- --- ---");
                Map<String, Double> v =
                        without.entrySet().stream()
                                .filter(e -> e.getKey().size() == d)
                                .collect(Collectors.toMap(
                                        e ->e.getKey().stream().sorted().collect(Collectors.toList()).toString(),
                                        Map.Entry::getValue));
                v.forEach((key, val) -> System.out.println(key + " => " + val));
            }

            // Sorting "WITH"
            List<Map.Entry<Set<String>, Double>> withSorted =
                    with.entrySet().stream()
                            .sorted(Comparator.comparing((Map.Entry<Set<String>, Double> e)->e.getValue()).reversed())
                            .collect(Collectors.toList());

            System.out.println("\n\n");
            System.out.println("WITH");
            withSorted.forEach( e -> System.out.println(e.getKey() + " => " + e.getValue()));

            // Sorting "WITHOUT"
            List<Map.Entry<Set<String>, Double>> withoutSorted =
                    without.entrySet().stream()
                            .sorted(Comparator.comparing((Map.Entry<Set<String>, Double> e)->e.getValue()).reversed())
                            .collect(Collectors.toList());

            System.out.println("\n\n");
            System.out.println("WITHOUT");
            withoutSorted.forEach( e -> System.out.println(e.getKey() + " => " + e.getValue()));

*/















        return Collections.emptySet();

    }

}
