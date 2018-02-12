package Bard.NLG.Generator;

import org.json.JSONObject;
import org.json.JSONString;

import java.util.Objects;

public class NodeInfo implements JSONString {

    enum DIRECTION {
        INCREASE,
        NEUTRAL,
        DECREASE
    };

    // --- --- --- Fields

    public final String nodeID;

    public final String nodeName;

    public final String stateName;

    public final double prior;

    public final double posterior;

    public final boolean isEvidence;

//    public final boolean isTarget;

    // --- --- --- Constructor

    public NodeInfo(String nodeID, String nodeName, String stateName, double prior, double posterior, boolean isEvidence, boolean isTarget) {
        this.nodeID = nodeID;
        this.nodeName = nodeName;
        this.stateName = stateName;
        this.prior = prior;
        this.posterior = posterior;
        this.isEvidence = isEvidence;
//        this.isTarget = isTarget;
    }

    // --- --- --- Getters

    public String getNodeID() {
        return nodeID;
    }

    public String getNodeName() {
        return nodeName;
    }

    public String getStateName() {
        return stateName;
    }

    public double getPrior() {
        return prior;
    }

    public double getPosterior() {
        return posterior;
    }

    public boolean isEvidence() {
        return isEvidence;
    }

    public DIRECTION getDirection() {
        if(posterior > prior){
            return DIRECTION.INCREASE;
        } else if (posterior == prior) {
            return DIRECTION.NEUTRAL;
        } else {
            return DIRECTION.DECREASE;
        }
    }

//    public boolean isTarget() {
//        return isTarget;
//    }

    // --- --- --- Equal & hashcode

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NodeInfo nodeInfo = (NodeInfo) o;
        return Double.compare(nodeInfo.prior, prior) == 0 &&
                Double.compare(nodeInfo.posterior, posterior) == 0 &&
                isEvidence == nodeInfo.isEvidence &&
//                isTarget == nodeInfo.isTarget &&
                Objects.equals(nodeID, nodeInfo.nodeID) &&
                Objects.equals(nodeName, nodeInfo.nodeName) &&
                Objects.equals(stateName, nodeInfo.stateName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nodeID, nodeName, stateName, prior, posterior, isEvidence);//, isTarget);
    }


    // --- --- --- JSON


    public String toJSONString() {
        String[] fields = new String[]{
                "nodeID", "nodeName",
                "stateName",
                "prior", "posterior",
                "isEvidence"//, "isTarget"
        };

        return new JSONObject(this, fields).toString();
    }

    static public NodeInfo fromJSON(JSONObject obj) {
        String nodeID = obj.getString("nodeID");
        String nodeName = obj.getString("nodeName");
        String stateName = obj.getString("stateName");
        double prior = obj.getDouble("prior");
        double posterior = obj.getDouble("posterior");
        boolean isEv = obj.getBoolean("isEvidence");
        boolean isTarget = obj.getBoolean("isTarget");
        return new NodeInfo(nodeID, nodeName, stateName, prior, posterior, isEv, isTarget);
    }
}
