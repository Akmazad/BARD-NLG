package Bard.NLG.Generator;

import Bard.NLG.Segment.RawSegment;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONString;

import java.util.*;
import java.util.stream.Collectors;

public class Segment implements JSONString {


    // --- --- --- Public fields

    public final RawSegment segment;

    public final Map<String, NodeInfo> mapInfo;

    // Supporting batch

    public final Set<String> supportingBatch;

    public final double supportingImpact;

    // Detracting batch

    public final Set<String> detractingBatch;

    public final double detractingImpact;


    // --- --- --- Constructor

    public Segment(RawSegment segment,
                   Set<String> supportingBatch, double supportingImpact,
                   Set<String> detractingBatch, double detractingImpact) {
        this.segment = segment;
        this.mapInfo = new HashMap<>();
        this.supportingBatch = new HashSet<>(supportingBatch);
        this.supportingImpact = supportingImpact;
        this.detractingBatch = new HashSet<>(detractingBatch);
        this.detractingImpact = detractingImpact;
    }

    private Segment(RawSegment segment,
                    Map<String, NodeInfo> mapInfo,
                    Set<String> supportingBatch, double supportingImpact,
                    Set<String> detractingBatch, double detractingImpact) {
        this.segment = segment;
        this.mapInfo = mapInfo;
        this.supportingBatch = new HashSet<>(supportingBatch);
        this.supportingImpact = supportingImpact;
        this.detractingBatch = new HashSet<>(detractingBatch);
        this.detractingImpact = detractingImpact;
    }


    // --- --- --- Methods

    public void put(String nodeId, NodeInfo info) {
        this.mapInfo.put(nodeId, info);
    }


    // --- --- --- Get data as NodeInfo

    /**
     * Return the target of the segment
     */
    public NodeInfo getTarget() {
        return mapInfo.get(segment.target);
    }

    /**
     * Return the set of all target's causal nodes (parents)
     */
    public Set<NodeInfo> getCausal() {
        return segment.causal.stream().map(mapInfo::get).collect(Collectors.toSet());
    }

    /**
     * Return the set of all target's "anticause nodes" ("children", "effects"), including common effect nodes
     */
    public Set<NodeInfo> getAllAntiCausal() {
        Set<String> allAC = (new HashSet<String>(segment.antiCausal));          // Simple anticauses (not common effect)
        allAC.addAll(segment.commonEffect.keySet());                            // Common effects
        return allAC.stream().map(mapInfo::get).collect(Collectors.toSet());
    }

    /**
     * Return the set of all target's "anticause nodes" ("children", "effect"), excluding common effect nodes
     */
    public Set<NodeInfo> getAntiCausal() {
        return segment.antiCausal.stream().map(mapInfo::get).collect(Collectors.toSet());
    }

    /**
     * Return the common effect relations
     */
    public Map<NodeInfo, Set<NodeInfo>> getCommonEffect() {
        return segment.commonEffect.entrySet().stream().collect(
                Collectors.toMap(
                        kv -> mapInfo.get(kv.getKey()),
                        kv -> kv.getValue().stream().map(mapInfo::get).collect(Collectors.toSet())
                )
        );
    }

    /**
     * Return all the anticauses - even the non common effect one - under the common effect format.
     */
    public Map<NodeInfo, Set<NodeInfo>> getAllEffect() {
        Map<NodeInfo, Set<NodeInfo>> map = getCommonEffect();
        for(NodeInfo ac: getAntiCausal()){
            map.put(ac, Collections.emptySet());
        }
        return map;
    }

    /**
     * Return all node infos
     */
    public Set<NodeInfo> getNodeInfos() {
        return new HashSet<>(mapInfo.values());
    }

    /**
     * Return the map ID - Name
     */
    public Map<String, String> getMapIdName() {
        return mapInfo.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().nodeName));
    }


    // --- --- --- Query supporting/detracting batch and impact

    /**
     * Test if a node is in the supporting batch
     */
    public boolean isSupporting(NodeInfo ni) {
        return (supportingBatch.contains(ni.nodeID) && (supportingImpact > 0));
    }

    /**
     * Test if a node is in the supporting batch && has no impact
     */
    public boolean isSupportingAndNoImpact(NodeInfo ni) {
        return (supportingBatch.contains(ni.nodeID) && (supportingImpact == 0));
    }

    /**
     * Test if a node is in the detracting batch
     */
    public boolean isDetracting(NodeInfo ni) {
        return (detractingBatch.contains(ni.nodeID) && (detractingImpact < 0));
    }

    /**
     * Test if a node is in the detracting batch && has no impact
     */
    public boolean isDetractingAndNoImpact(NodeInfo ni) {
        return (detractingBatch.contains(ni.nodeID) && (detractingImpact == 0));
    }

    /**
     * Test if a node has no impact
     */

    public boolean isNeutral(NodeInfo ni) {
        return isDetractingAndNoImpact(ni) || isSupportingAndNoImpact(ni);
    }



    // --- --- --- JSON

    /** Create a JSON string representation of the object */
    public String toJSONString() {
        JSONObject obj = new JSONObject(this, new String[]{
                "segment",
                "mapInfo",
                "supportingBatch",
                "supportingImpact",
                "detractingBatch",
                "detractingImpact"
        });
        return obj.toString(2);
    }


    /** Create a segment from a JSON string */
    static public Segment fromJSON(JSONObject obj) {
        RawSegment segment = RawSegment.fromJSON(obj.getJSONObject("segment"));

        Map<String, NodeInfo> mapInfo = new HashMap<>();
        JSONObject jsonMap = obj.getJSONObject("mapInfo");
        for (String k : jsonMap.keySet()) {
            NodeInfo v = NodeInfo.fromJSON(jsonMap.getJSONObject(k));
            mapInfo.put(k, v);
        }

        Set<String> SB = obj.getJSONArray("supportingBatch").toList().stream().map(o -> (String) o).collect(Collectors.toSet());
        double SI = obj.getDouble("supportingImpact");

        Set<String> DB = obj.getJSONArray("detractingBatch").toList().stream().map(o -> (String) o).collect(Collectors.toSet());
        double DI = obj.getDouble("detractingImpact");

        return new Segment(segment, mapInfo, SB, SI, DB, DI);
    }

    /** Create a String representation of a list of segments */
    static public String toStringJSON(List<Segment> ls) {
        JSONArray obj = new JSONArray(ls);
        return obj.toString();
    }

    /** Create a list of segment from a JSON string */
    static public List<Segment> fromJSON(JSONArray array) {
        ArrayList<Segment> ls = new ArrayList<>();
        for (Object o : array) {
            JSONObject jo = (JSONObject) o;
            Segment s = fromJSON(jo);
            ls.add(s);
        }
        return ls;
    }

}
