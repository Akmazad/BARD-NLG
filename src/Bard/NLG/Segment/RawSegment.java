/**
 * This file is part of the BARD project
 * Monash Univeristy, Melbourne, Australia
 * 2018
 *
 * @author Dr. Matthieu Herrmann
 * Raw segment are "stringified" version of the segment.
 */

package Bard.NLG.Segment;

import org.json.JSONObject;
import org.json.JSONString;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class RawSegment implements JSONString {

    /** Target of the segment */
    public final String target;

    /** Causal nodes == parents of the target */
    public final Set<String> causal;

    /** AntiCausal nodes == children of the target */
    public final Set<String> antiCausal;

    /** Common Effect node == children of the target with set of alternate causes */
    public final Map<String, Set<String>> commonEffect;

    /** Constructor: copy everything */
    public RawSegment(String target, Set<String> causal, Set<String> antiCausal, Map<String, Set<String>> commonEffect){
        this.target = target;
        this.causal = new HashSet<>(causal);
        this.antiCausal = new HashSet<>(antiCausal);
        this.commonEffect = new HashMap<>();
        commonEffect.forEach( (key, set) -> this.commonEffect.put(key, new HashSet<String>(set)));
    }

    @Override
    public String toString() {
        return "RawSegment{" +
                "target='" + target + '\'' +
                ", causal=" + causal +
                ", antiCausal=" + antiCausal +
                ", commonEffect=" + commonEffect +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RawSegment that = (RawSegment) o;

        if (!target.equals(that.target)) return false;
        if (!causal.equals(that.causal)) return false;
        if (!antiCausal.equals(that.antiCausal)) return false;
        return commonEffect.equals(that.commonEffect);
    }

    @Override
    public int hashCode() {
        int result = target.hashCode();
        result = 31 * result + causal.hashCode();
        result = 31 * result + antiCausal.hashCode();
        result = 31 * result + commonEffect.hashCode();
        return result;
    }

    // --- --- --- JSON

    public String toJSONString(){
        String[] fields = new String[]{"target", "causal", "antiCausal", "commonEffect"};
        return new JSONObject(this, fields).toString();
    }

    static public RawSegment fromJSON(JSONObject obj){
        String target = obj.getString("target");

        Set<String> causal = obj.getJSONArray("causal").toList().stream().map( o -> (String)o).collect(Collectors.toSet());

        Set<String> antiCausal = obj.getJSONArray("antiCausal").toList().stream().map( o -> (String)o).collect(Collectors.toSet());

        Map<String, Set<String>> m = new HashMap<>();
        JSONObject jsonMap = obj.getJSONObject("commonEffect");
        for(String k: jsonMap.keySet()){
            Set<String> v = jsonMap.getJSONArray(k).toList().stream().map( o -> (String)o).collect(Collectors.toSet());
            m.put(k, v);
        }

        return new RawSegment(target, causal, antiCausal, m);

    }
}