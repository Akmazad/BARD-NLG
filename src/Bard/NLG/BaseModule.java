package Bard.NLG;

//import norsys.netica.*;

import java.io.*;
import java.nio.file.Path;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Stack;
import java.util.Collections;
import java.util.Comparator;
import java.lang.Math;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.Set;
import org.json.JSONObject;

// AgenaRisk migration
import agenariskserver.*;
import Bard.NLG.Segment.*;
import Bard.NLG.Generator.*;
import Bard.NLG.Generator.Segment;

class NLGException extends Exception {
    public NLGException(String message) {
        super(message);
    }
}

public class BaseModule {

    // global variables
    private static Hashtable<String, String> CommonEffectNodes_and_theirPath = new Hashtable<String, String>();
    private static Hashtable backupConditionedList = new Hashtable<>();
    private static ArrayList<String> allBNnodes = new ArrayList<>();
    private static ArrayList<String> blockedEvidenceList = new ArrayList<>();
    public static ArrayList<ArrayList<String>> blockedEvidenceNodeInfoList = new ArrayList<>();
    private static Hashtable<String, Hashtable<String, Double>> originalPriorofAllNodes = new Hashtable<>();
    private static Hashtable<String, Hashtable<String, Double>> FinalBeliefofAllNodes = new Hashtable<>();
    private static String ultimateTargetNode = "";
    private static String ultimateTargetNodeState = "";
    private static String outputResponse_for_A_Target = "";
    private static TextGenerator_Az NLGtext_Az = new TextGenerator_Az();
    private static Map<String, String> semanticStates = new HashMap();
    private static Map<String, String> explainableStates = new HashMap<>();
    private static Net _net;
    private static double epsilon = 0.001;
    Path jsonPath = null;

    // ------- Wrapper function for running main NLG function 
    public String runNLG(JSONObject config, Path p) throws Exception {
        String filename = config.getString("netPath");
        Path netPath = p.getParent().resolve(filename);
        System.err.println(netPath);
        jsonPath = p;
        return runNLG(new Net(netPath.toString()), config);
    }
    
    // ------- Running NLG function (Main)
    public String runNLG(Net net, JSONObject config) throws Exception {
        _net = net;

        Hashtable conditionedNodeList = new Hashtable();
        ArrayList<ArrayList<String>> targetList = new ArrayList<>();

        Hashtable backgroundNodeList = new Hashtable();
        String queryNode = "";
        String queryNodeState = "";
        Graph _BNgraph = new Graph(_net);

        String verbosity = "";
        for (Object _node : config.getJSONArray("nodes")) {		// Go through all nodes defined in config
            JSONObject node = (JSONObject) _node;
            
            if (node.getBoolean("background")) {
                backgroundNodeList.put(node.getString("name"), true);	// Add background nodes to the right list
            }else {														// Add conditioned nodes to their own list
                HashMap tempMap = (HashMap) node.getJSONObject("conditioned").toMap();
                Hashtable tempTable = new Hashtable<>();
                tempTable.putAll(tempMap);
                conditionedNodeList.put(node.getString("name"), tempTable);
            }
        }

        for (Object _node : config.getJSONArray("target")) {			// putting target nodes into a list
            JSONObject node = (JSONObject) _node;
            ArrayList<String> tempList = new ArrayList<>();
            tempList.add(node.getString("name"));
            tempList.add(node.getString("state"));
            targetList.add(tempList);
        }
        verbosity = config.getString("verbosity");						// setting up the verbosity for explanation

        String FinalOutputString = "";									// final string that returns the whole NLG explanation for current scenario

        boolean _isConfigFileOK = CheckConsistency(conditionedNodeList, queryNode);		// checking if evidence nodes are in the Net
        if (!_isConfigFileOK) {
            Exception exp = new NLGException("Invalid Node Detected in the configuration File");
            throw exp;
        }

        DecimalFormat df = new DecimalFormat("#.#"); 					// setting up 1-decimal point for showing probability values
        df.setRoundingMode(RoundingMode.CEILING);

        backupConditionedList = new Hashtable<>(conditionedNodeList);	// backing up evidenceList
        allBNnodes = FindAllBNnodes();									

        // ===== Get ultimate prior and ultimate posteriori for all the nodes; Also, get rid of the blocked nodes */
        _net.clearAllEvidence();
        _net.compile();
        saveOriginalBeleifsofAllNodes("prior");
        set_findings_of_ConditionedNodes_3(conditionedNodeList, MakeNodeList(conditionedNodeList)); // this will
        saveOriginalBeleifsofAllNodes("posterior");

        semanticStates = GetSemanticStatesBN(); 						// semanticStates are used in the "Since" part
        explainableStates = GetExplainableStatesBN(conditionedNodeList, targetList); // for evidence-target nodes, states will be picked from the JSONobject, otherwise: True/Yes, or the state at the zero index
        _net.clearAllEvidence();
        _net.compile();
        // ===== End

        
        // ===== Response for Base Scenario (NO EVIDENCE - but at least one target)
        if (conditionedNodeList.size() == 0) {
            boolean adjectiveProb = true;

            if (targetList.size() > 0) {
                FinalOutputString += System.getProperty("line.separator").toString() + "In the absence of evidence, the prior "
                        + ((targetList.size() > 1) ? "probabilities of " : "probability of ");
                ArrayList<String> probList = new ArrayList<>();
                for (int i = 0; i < (targetList.size() - 1); i++) {
                    String targetNode = targetList.get(i).get(0);
                    String targetNodeState = targetList.get(i).get(1);

                    FinalOutputString += targetNode + "=" + targetNodeState + ", ";
                    Double probVal = _net.getNode(targetNode).getBelief(targetNodeState);
                    probVal = ((double) Math.round((probVal) * 1000.0) / 1000.0);

                    probList.add(df.format(probVal * 100.0) + "% (" + NLGtext_Az.PutVerbalWord_Az(probVal, false, adjectiveProb) + ")");
                }
                Double probVal2 = _net.getNode(targetList.get(targetList.size() - 1).get(0)).getBelief(targetList.get(targetList.size() - 1).get(1));
                probVal2 = ((double) Math.round((probVal2) * 1000.0) / 1000.0);
                probList.add(df.format(probVal2 * 100.0) + "% (" + NLGtext_Az.PutVerbalWord_Az(probVal2, false, adjectiveProb) + ")");

                if (targetList.size() > 1) {
                    FinalOutputString += "and " + targetList.get(targetList.size() - 1).get(0) + "=" + targetList.get(targetList.size() - 1).get(1) + " are ";
                    for (int i = 0; i < (probList.size() - 1); i++) {
                        FinalOutputString += (probList.get(i) + ", ");
                    }
                    FinalOutputString += "and " + probList.get(probList.size() - 1) + ", respectively." + System.getProperty("line.separator").toString();
                } else {
                    FinalOutputString += (targetList.get(0).get(0) + "=" + targetList.get(0).get(1) + " is " + probList.get(0) + "." + System.getProperty("line.separator").toString());
                }
            } else
                FinalOutputString = System.getProperty("line.separator").toString() + "Error: NLG can't produce a report on the current request." + System.getProperty("line.separator").toString();


            //------- customizing this for long verbosity
            if (verbosity.equals("long")) {
                //------------------- Text for BN structure + Probability Tables [with FAKE evidence for all nodes]
                Hashtable fake_conditionedNodeList = makeFakeConditionedNodeList(targetList, conditionedNodeList);
                boolean fake_it_Philip = true;
                String textforallTargets = "";
                for (ArrayList<String> qNodeInfo : targetList) {
                    ultimateTargetNode = qNodeInfo.get(0);
                    ultimateTargetNodeState = qNodeInfo.get(1);

                    //String bnText = getTextforBNstructure(fake_it_Philip, fake_conditionedNodeList, qNodeInfo.get(0), conditionedNodeList, _BNgraph);
                	String bnText = getTextforBNstructure(backupConditionedList);
                	textforallTargets += "<h1 class=\"target\">Target: " + escapeHtml(qNodeInfo.get(0)) + "</h1>\n <div class=\"summary\"> \n <h1>Summary</h1>\n" + escapeHtml(FinalOutputString) + bnText + "</div>";
                    re_InitializeGlobalVariable();                    // Re initialize all the global variables
                }
                return textforallTargets;
                // --------------------- End 
            }else
            	return FinalOutputString;									// Base Scenario: Return final texts
        }

        
        // Work-in-Progress for each target node
        List<String> JsonOutPutList = new ArrayList<String>();			// For outputting temporary results

        for (int i = 0; i < targetList.size(); i++) {					// For each target generate NLG texts

            outputResponse_for_A_Target = "";
            conditionedNodeList = (Hashtable) backupConditionedList.clone();

            queryNode = targetList.get(i).get(0);
            queryNodeState = targetList.get(i).get(1);

            re_InitializeGlobalVariable();         // Re initialize all the global variables

            conditionedNodeList = Find_UnBlocked_Evidence_Nodes(queryNode, _BNgraph, conditionedNodeList);
            blockedEvidenceNodeInfoList = CreatblockedNodeInfoList();

            _net.clearAllEvidence();
            _net.compile();

            ultimateTargetNode = queryNode;
            ultimateTargetNodeState = queryNodeState;

            // --------------------- Generate Summary [start] ------------------------

            HashMap<Hashtable<Node[], Double>, Hashtable<Node[], Hashtable<String, ArrayList<String>>>> _contents = ContentSelectionFor_NLG_report(conditionedNodeList, queryNode, queryNodeState, _BNgraph, "perc_change");
            Iterator iter = _contents.entrySet().iterator();
            Hashtable<Node[], Double> impactValues = null;
            Hashtable<Node[], Hashtable<String, ArrayList<String>>> d_connectedPaths = null;
            while (iter.hasNext()) {
                Map.Entry<Hashtable<Node[], Double>, Hashtable<Node[], Hashtable<String, ArrayList<String>>>> _entry = (Map.Entry<Hashtable<Node[], Double>, Hashtable<Node[], Hashtable<String, ArrayList<String>>>>) iter.next();
                impactValues = new Hashtable<>(_entry.getKey());
                d_connectedPaths = new Hashtable<>(_entry.getValue()); // this one will eventually be ChainList
            }
            ArrayList<Map.Entry<Node[], Double>> sortedImpactValuesofSubsets = sortHashTableValue(impactValues, "desc");            // get the hashtable sorted as an ArrayList

            _net.clearAllEvidence();
            _net.compile();
            String firstPreamble = GeneratePreamble(sortedImpactValuesofSubsets, conditionedNodeList, queryNode, queryNodeState, explainableStates);	// generate summary text (i.e. preamble)
            _net.clearAllEvidence();
            _net.compile();

            if (verbosity.equals("long")) {													// "Detailed" explanations texts are in HTML format
                outputResponse_for_A_Target += "<h1 class=\"target\">Target: " + escapeHtml(queryNode) + "</h1>\n";
                outputResponse_for_A_Target += "<div class=\"summary\">\n";
                outputResponse_for_A_Target += "<h1>Summary</h1>\n";

                outputResponse_for_A_Target += escapeHtml(firstPreamble) + "\n</div>";
            } else
                outputResponse_for_A_Target += firstPreamble;

            if (verbosity.equals("short")) {
                FinalOutputString += outputResponse_for_A_Target;                           // "Summary" explanations are in plain text format     
                continue;
            }
            // --------------------- End

            //------------------- Text for BN structure + Probability Tables [with FAKE evidence for all nodes]
            Hashtable fake_conditionedNodeList = makeFakeConditionedNodeList(targetList, conditionedNodeList);
            boolean fake_it_Philip = true;
            if (fake_it_Philip)
                //outputResponse_for_A_Target += getTextforBNstructure(fake_it_Philip, fake_conditionedNodeList, queryNode, conditionedNodeList, _BNgraph);
            	outputResponse_for_A_Target += getTextforBNstructure(backupConditionedList);
            // --------------------- End 

            // --------------------- Reasoning steps (for Detailed Explanation only) -------------------------------
            fake_it_Philip = false;
            fake_conditionedNodeList.clear();

            if (conditionedNodeList.size() > 0) {
                set_findings_of_ConditionedNodes_3(conditionedNodeList, MakeNodeList(conditionedNodeList));
                outputResponse_for_A_Target += System.getProperty("line.separator").toString();
                outputResponse_for_A_Target += System.getProperty("line.separator").toString();
                outputResponse_for_A_Target += System.getProperty("line.separator").toString();

                Analyser a = new Analyser();								// Analyser object for segment analysis
                MakeBNforMH(_net, conditionedNodeList, queryNode, a);		// Make a BN graph structure for segment analysis
                List<RawSegment> orderedSegmentList = a.getRawSegments(); 	// Get raw segments (with only structural information)
                
                // ---- Reconstructs all the raw segments to augment NLG info
                List<Segment> orderedSegmentListForNLG = ReConstructSegmentForNLG(conditionedNodeList, orderedSegmentList, semanticStates, explainableStates, fake_conditionedNodeList, "perc_change", _BNgraph, fake_it_Philip);
                JsonOutPutList.add(Segment.toStringJSON(orderedSegmentListForNLG));				// put temporary outputs into Jsonobject
                TextGenerator tg = new TextGenerator(orderedSegmentListForNLG, fake_it_Philip);	
                outputResponse_for_A_Target += tg.getText().replace("<br>", "");				// get NLG texts for "a" target
            }
            FinalOutputString += outputResponse_for_A_Target; // this "outputResponse_for_A_Target" holds "summary" and "detailed" explanations
            // --------------------- End
        }
        return FinalOutputString;							// return the final texts (either "summary" or "Detailed" explanation)
    }
   
    private String getTextforBNstructure(Hashtable conditionedNodeList) throws Exception{
		String retStr = "";
    	ArrayList<String> textsforEachNode = new ArrayList<>();
    	Set<NodeInfo> allNodeInfoList = new HashSet<>();
		
    	
		for(String bnNode:allBNnodes) {
			ArrayList<String> nodeNameListStr = new ArrayList<>(); 
			Arrays.asList(_net.getNode(bnNode).getChildren()).forEach(ni -> nodeNameListStr.add(ni.getShortName()));	
			if(nodeNameListStr.size() != 0) {
				String Str = "";
				if(nodeNameListStr.size() == 1) {
					Str += (bnNode + " can cause " + nodeNameListStr.get(0));
				}
				else if(nodeNameListStr.size() > 1) {
					Str += (bnNode + " can cause ");
					for(int i = 0; i < nodeNameListStr.size() - 1; i++) {
						Str += (nodeNameListStr.get(i) + ", ");
					}
					Str = Str.substring(0, Str.length()-2);
					Str += (" and " + nodeNameListStr.get(nodeNameListStr.size()-1));
				}
				
				textsforEachNode.add(Str);
			}
			
			allNodeInfoList.add(bnNode.equals(ultimateTargetNode) ? ConstructTargetNodeInfo(bnNode, semanticStates, explainableStates, conditionedNodeList): ConstructOtherNodeInfo(bnNode , semanticStates, explainableStates, conditionedNodeList));
			
		}
		
		TextGenerator tg = new TextGenerator();
		return tg.printBNTextwithCausalityOnly(textsforEachNode, allNodeInfoList);
	}

	private static ArrayList<ArrayList<String>> CreatblockedNodeInfoList() throws Exception {
        ArrayList<ArrayList<String>> retList = new ArrayList<>();
        for (String bNode : blockedEvidenceList) {
            ArrayList<String> tempList = new ArrayList<>();
            tempList.add(bNode);
            tempList.add(explainableStates.get(bNode));
            tempList.add("0.0");
            retList.add(tempList);
        }
        return retList;
    }

    private String getTextforBNstructure(boolean fake_it_Philip, Hashtable fake_conditionedNodeList, String queryNode, Hashtable conditionedNodeList, Graph _BNgraph) throws Exception {
        String retStr = "";
        Node[] bnNodes = _net.getNodes();
        int j = 0;
        for (Node node : bnNodes) {        // add fake node to the Agena BN: it helps to allow more paths to be free (i.e. d-connected)
            if (node.getParents().length >= 2) {
                String fakeNodeName = "ubgs92jh_" + j;        // ubgs92jh = fake nodeName initial
                Node tempNode = _net.addNode(fakeNodeName, new String[]{"True", "False"});
                tempNode.addParent(_net.getNode(node.getShortName()));
            }
            j++;
        }

        //--- Reuse segment analysis and TextGenerator code for getting Texts for whole BN structure as "Global Preamble"
        Analyser a = new Analyser();
        MakeBNforMH(_net, fake_conditionedNodeList, queryNode, a);

        List<RawSegment> fake_orderedSegmentList = a.getRawSegments();
        List<Segment> fake_orderedSegmentListForNLG = ReConstructSegmentForNLG(conditionedNodeList, fake_orderedSegmentList, semanticStates, explainableStates, fake_conditionedNodeList, "perc_change", _BNgraph, fake_it_Philip);
        TextGenerator fake_tg = new TextGenerator(fake_orderedSegmentListForNLG, fake_it_Philip);
        retStr += fake_tg.getText().replace("<br>", "");

        // ----- Remove Fake nodes that were created before
        bnNodes = _net.getNodes();
        for (Node node : bnNodes) {
            if (node.getShortName().startsWith("ubgs92jh")) {        // ubgs92jh = fake nodeName initial
                node.remove();
            }
            j++;
        }
        return retStr;
    }

    public String escapeHtml(String str) {
        return str
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private Hashtable makeFakeConditionedNodeList(ArrayList<ArrayList<String>> targetList, Hashtable conditionedNodeList) throws Exception {
        Hashtable retTable = new Hashtable<>();
        Node[] allBNnodes = _net.getNodes();
        for (Node node : allBNnodes) {
            if (isNodeOK_tobe_Fake_Evidence(node, targetList, conditionedNodeList)) {
                State[] allStates = node.getStates();
                Hashtable tempTable = new Hashtable<>();
                for (int i = 0; i < allStates.length; i++) {
                    double val = 0.0;
                    if (i == 0)
                        val = 1.0;
                    tempTable.put(allStates[i].getShortName(), val);
                }
                retTable.put(node.getShortName(), tempTable);
            }
        }
        return retTable;
    }

    private boolean isNodeOK_tobe_Fake_Evidence(Node node, ArrayList<ArrayList<String>> targetList, Hashtable conditionedNodeList) throws Exception {
        if (ultimateTargetNode.equals(node.getShortName()))
            return false;
        if ((node.getParents().length >= 2) && (node.getChildren().length == 0))        // hoping 'node' is CE node
            return true;
        else if ((node.getParents().length == 1) && (node.getChildren().length == 0)) // hoping 'node' is non-CE but a leaf
            return true;
        else if ((node.getParents().length == 0) && (node.getChildren().length == 1))// hoping 'node' is non-CE but a leaf
            return true;
        else            // hoping 'node' is non-CE and non-Terminal one, so not safe to be an evidence - can block a path
            return false;
    }

    private Map<String, String> GetExplainableStatesBN(Hashtable conditionedNodeList, ArrayList<ArrayList<String>> targetList) throws Exception {
        Map<String, String> retMap = new HashMap<String, String>();
        for (String nodeName : allBNnodes) {
            String pfState = "";

            if (conditionedNodeList.containsKey(nodeName) && (isHardEvidence((Hashtable) conditionedNodeList.get(nodeName)) != null)) {
                pfState = isHardEvidence((Hashtable) conditionedNodeList.get(nodeName));
                retMap.put(nodeName, pfState);
                continue;
            } else if (isInTargetList(nodeName, targetList) != null) {
                pfState = isInTargetList(nodeName, targetList);
                retMap.put(nodeName, pfState);
                continue;
            }

            State[] allStates = _net.getNode(nodeName).getStates();
            boolean trueOrYes = false;
            for (State st : allStates) {
                String stName = st.getShortName();
                if (stName.toLowerCase().equals("true") || stName.toLowerCase().equals("yes")) {
                    pfState = stName;
                    trueOrYes = true;
                    break;
                }
            }
            if (!trueOrYes) {
                pfState = allStates[0].getShortName();
            }
            retMap.put(nodeName, pfState);

        }
        return retMap;
    }

    private Map<String, String> GetSemanticStatesBN() throws Exception {
        Map<String, String> retMap = new HashMap<String, String>();
        for (String nodeName : allBNnodes) {
            String pfState = "";

            State[] allStates = _net.getNode(nodeName).getStates();
            boolean trueOrYes = false;
            for (State st : allStates) {
                String stName = st.getShortName();
                if (stName.toLowerCase().equals("true") || stName.toLowerCase().equals("yes")) {
                    pfState = stName;
                    trueOrYes = true;
                    break;
                }
            }
            if (!trueOrYes) {
                pfState = allStates[0].getShortName();
            }
            retMap.put(nodeName, pfState);
        }
        return retMap;
    }

    private static String isHardEvidence(Hashtable tempTable) {
        Enumeration<String> tempEnum = tempTable.keys();
        while (tempEnum.hasMoreElements()) {
            String stateName = tempEnum.nextElement();
            double probVal = (Double) tempTable.get(stateName);
            if (probVal == 1.0) {
                return stateName;
            }
        }
        return null;
    }

    private String isInTargetList(String nodeName, ArrayList<ArrayList<String>> targetList) {
        for (ArrayList<String> targetInfo : targetList) {
            if (targetInfo.get(0).equals(nodeName)) {
                return targetInfo.get(1);
            }
        }
        return null;
    }

    private ArrayList<String> FindAllBNnodes() throws Exception {
        ArrayList<String> retList = new ArrayList<>();
        Node[] bnNodes = _net.getNodes();
        for (Node n : bnNodes) {
            retList.add(n.getShortName());
        }
        return retList;
    }

    /*
     * This method is shared between getting "Real" and "Fake" (for texts of whole BN structure) impact analysis by changing a flag called: "fake_it_Philip"
     */
    private List<Segment> ReConstructSegmentForNLG(Hashtable conditionedNodeList, List<RawSegment> orderedSegmentList, Map<String, String> semanticStates, Map<String, String> explainableStates, Hashtable fake_conditionedNodeList, String impactChoice, Graph _BNGraph, boolean fake_it_Philip) throws Exception {
        List<Segment> retList = new ArrayList<>();
        for (RawSegment rawSeg : orderedSegmentList) {
            // add all the things that need to be added in the NLG segment            // do the impact analysis of a segment to find SB and DB with respective impact values
            List<String> segOtherNodeList = FindNodesInAsegment(rawSeg);
            Map<String, Map<Set<String>, Double>> SB_DB_infos = new HashMap<String, Map<Set<String>, Double>>();
            if (!fake_it_Philip)							// Do impact analysis for Real
                SB_DB_infos = impactAnalysisOfAsegment(rawSeg.target, segOtherNodeList, semanticStates, explainableStates, conditionedNodeList, impactChoice, _BNGraph);
            else {											// No impact analysis, but put something in "SB"-"DB" batches
                Set<String> SB = new HashSet<String>();
                Set<String> DB = new HashSet<String>();

                SB.add(segOtherNodeList.get(0));			// By default put the first node in the segment in the SB, and remaining in the DB
                for (int it = 1; it < segOtherNodeList.size(); it++)
                    DB.add(segOtherNodeList.get(it));

                Map<Set<String>, Double> tempMap = new HashMap<>();
                tempMap.put(SB, 999.0);
                SB_DB_infos.put("sb", tempMap);

                if (!DB.isEmpty()) {
                    tempMap = new HashMap<>();
                    tempMap.put(DB, -999.0);
                    SB_DB_infos.put("db", tempMap);
                } else {
                    tempMap = new HashMap<>();
                    SB_DB_infos.put("db", tempMap);
                }
            }

            Set<String> SB = new HashSet<>();
            Set<String> DB = new HashSet<>();

            List<Set<String>> SB_set_info = new ArrayList<Set<String>>(SB_DB_infos.get("sb").keySet());
            if (!SB_set_info.isEmpty()) {
                SB = SB_set_info.get(0);
            }
            List<Set<String>> DB_set_info = new ArrayList<Set<String>>(SB_DB_infos.get("db").keySet());
            if (!DB_set_info.isEmpty()) {
                DB = DB_set_info.get(0);
            }
            Double SB_impactVal = (SB.size() > 0) ? SB_DB_infos.get("sb").values().toArray(new Double[0])[0] : 0.0;
            Double DB_impactVal = (DB.size() > 0) ? SB_DB_infos.get("db").values().toArray(new Double[0])[0] : 0.0;

            Segment nlgSeg = new Segment(rawSeg, SB, SB_impactVal, DB, DB_impactVal);

            // add node information for the segment target
            NodeInfo targetNodeInfo = ConstructTargetNodeInfo(rawSeg.target, semanticStates, explainableStates, conditionedNodeList);
            nlgSeg.put(rawSeg.target, targetNodeInfo);

            // add node information for other nodes in the segment
            for (String oSegNode : segOtherNodeList) {
                NodeInfo oNinfo = ConstructOtherNodeInfo(oSegNode, semanticStates, explainableStates, conditionedNodeList);
                nlgSeg.put(oSegNode, oNinfo);
            }
            retList.add(nlgSeg);
        }
        return retList;
    }

    private NodeInfo ConstructOtherNodeInfo(String oSegNode, Map<String, String> semanticStates, Map<String, String> explainableStates, Hashtable conditionedNodeList) throws Exception {
        String state_Name = explainableStates.get(oSegNode);
        Double prior_prob = 0.0;
        Double posterior_prob = 0.0;
        if (!oSegNode.startsWith("ubgs92jh")) {
            prior_prob = (Double) RetriveProbfromBuffer(oSegNode, "prior").get(state_Name);
            posterior_prob = (Double) RetriveProbfromBuffer(oSegNode, "posterior").get(state_Name);
        } else {					// if this method is used by Fake nodes, then handle it in a special way
            prior_prob = 0.5;
            posterior_prob = 0.5;
        }
        boolean isEvidence = (conditionedNodeList.containsKey(oSegNode)) ? true : false;
        boolean isTarget = false;
        NodeInfo nodeInfo = new NodeInfo(oSegNode, oSegNode, state_Name, prior_prob, posterior_prob, isEvidence, isTarget); // nodeID = nodeName Now
        return nodeInfo;
    }

    private NodeInfo ConstructTargetNodeInfo(String target, Map<String, String> semanticStates, Map<String, String> explainableStates, Hashtable conditionedNodeList) throws Exception {
        String state_name = explainableStates.get(target);
        Double prior_prob = 0.0;
        Double posterior_prob = 0.0;
        if (!target.startsWith("ubgs92jh")) {
            prior_prob = (Double) RetriveProbfromBuffer(target, "prior").get(state_name);
            posterior_prob = (Double) RetriveProbfromBuffer(target, "posterior").get(state_name);
        } else {					// if this method is used by Fake nodes, then handle it in a special way
            prior_prob = 0.5;
            posterior_prob = 0.5;
        }
        boolean isEvidence = (conditionedNodeList.containsKey(target)) ? true : false;
        boolean isTarget = (ultimateTargetNode.equals(target)) ? true : false;
        NodeInfo nodeInfo = new NodeInfo(target, target, state_name, prior_prob, posterior_prob, isEvidence, isTarget); // nodeID = nodeName now
        return nodeInfo;
    }

    private Map<String, Map<Set<String>, Double>> impactAnalysisOfAsegment(String targetNode, List<String> segOtherNodeList, Map<String, String> semanticStates, Map<String, String> explainableStates, Hashtable conditionedNodeList, String impactChoice, Graph _BNGraph) throws Exception {
        Map<String, Map<Set<String>, Double>> retMap = new HashMap<>(); // (key-1: SB; value-1: SB_impact), (key-2: DB; value-2: DB_impact)
        String queryNodeName = targetNode;
        String queryNodeStateName = explainableStates.get(targetNode);

        if (segOtherNodeList.size() == 0)
            return retMap;
        else if (segOtherNodeList.size() == 1) {
            String evidenceNodeName = segOtherNodeList.get(0);
            String evidenceStateofInterest = explainableStates.get(evidenceNodeName);
            retMap = conductImpactAnalysisForSingleEdge(evidenceNodeName, evidenceStateofInterest, queryNodeName,
                    queryNodeStateName);
        } else if (segOtherNodeList.size() >= 2) {
            Hashtable U_conditionedNodeList = Construct_Updated_ConditionedList_for_a_NodeList_2(
                    (ArrayList<String>) segOtherNodeList);
            Hashtable P_conditionedNodeList = Construct_Prior_ConditionedList_for_a_NodeList(
                    (ArrayList<String>) segOtherNodeList);

            Node[][] allSubsetsOfEvidenceNodes = generateAllSubsets(MakeNodeList((ArrayList<String>) segOtherNodeList), queryNodeName, conditionedNodeList, _BNGraph);

            set_findings_of_ConditionedNodes_3(conditionedNodeList, MakeNodeList(conditionedNodeList)); // set again all main evidence nodes - this is because of "generateAllSubsets" function

            set_findings_of_ConditionedNodes_3(P_conditionedNodeList, MakeNodeList((ArrayList<String>) segOtherNodeList)); // resetting the CPTs of

            double priorValue = getTargetNodeBelief(queryNodeName, queryNodeStateName);
            priorValue = ((double) Math.round((priorValue) * 1000.0) / 1000.0);

            ArrayList<Map.Entry<Node[], Double>> magnitudeofSortedImpactValues = new ArrayList<>();            // instantiate impact vector (magnitude + direction)
            ArrayList<Map.Entry<Node[], String>> directionofSortedimpactValues = new ArrayList<>();

            if (impactChoice.equals("perc_change")) {
                Hashtable<Node[], Double> PercentageofTargetBeliefChangeofSubsets = FindPercentageofTargetBeliefChangeForAllSubsets(
                        allSubsetsOfEvidenceNodes, U_conditionedNodeList, P_conditionedNodeList, queryNodeName, queryNodeStateName, priorValue, (ArrayList<String>) segOtherNodeList);

                magnitudeofSortedImpactValues = sortHashTableValue(PercentageofTargetBeliefChangeofSubsets, "desc");
                directionofSortedimpactValues = findDirectionofVector(magnitudeofSortedImpactValues);
            }

            int topIndex = 0;

            Node[] SubsetofTopImpact = (Node[]) magnitudeofSortedImpactValues.get(topIndex).getKey();
            double impactValueofTopSubset = (double) magnitudeofSortedImpactValues.get(topIndex).getValue();
            impactValueofTopSubset = ((double) Math.round((impactValueofTopSubset) * 1000.0) / 1000.0);

            String impactDirectionofTopSubset = (String) directionofSortedimpactValues.get(topIndex).getValue();

            Map.Entry<Double, String> impactVectorofTheWholeSet = FindtheVectorforTheWholeSet(magnitudeofSortedImpactValues, segOtherNodeList);            // get the value of the interest for the whole input evidence list
            double valueforTheWholeSet = impactVectorofTheWholeSet.getKey();
            double posteriorValue = RetriveProbfromBuffer(queryNodeName, "posterior", conditionedNodeList);
            posteriorValue = ((double) Math.round((posteriorValue) * 1000.0) / 1000.0);
            valueforTheWholeSet = ((double) Math.round((valueforTheWholeSet) * 1000.0) / 1000.0);

            Set<String> SB = new HashSet<String>();
            Set<String> DB = new HashSet<String>();

            if (SubsetofTopImpact.length < segOtherNodeList.size()) {        // conflicting contribution
                Node[] remainingSubset = FindRemainingSubsets(SubsetofTopImpact, segOtherNodeList);
                if (impactDirectionofTopSubset.equals("increase")) {
                    SB = getNodeNames(SubsetofTopImpact);
                    Map<Set<String>, Double> tempMap = new HashMap<>();
                    tempMap.put(SB, impactValueofTopSubset);
                    retMap.put("sb", tempMap);

                    DB = getNodeNames(remainingSubset);
                    tempMap = new HashMap<>();
                    tempMap.put(DB, valueforTheWholeSet - impactValueofTopSubset);
                    retMap.put("db", tempMap);

                } else if (impactDirectionofTopSubset.equals("decrease")) {
                    SB = getNodeNames(remainingSubset);
                    Map<Set<String>, Double> tempMap = new HashMap<>();
                    tempMap.put(SB, valueforTheWholeSet - impactValueofTopSubset);
                    retMap.put("sb", tempMap);

                    DB = getNodeNames(SubsetofTopImpact);
                    tempMap = new HashMap<>();
                    tempMap.put(DB, impactValueofTopSubset);
                    retMap.put("db", tempMap);

                } else {        											// (impactDirection = no_change) ; insert empty set with 0.0 impact value
                    Map<Set<String>, Double> tempMap = new HashMap<>();
                    tempMap.put(SB, 0.0);
                    retMap.put("sb", tempMap);

                    tempMap = new HashMap<>();
                    tempMap.put(DB, 0.0);
                    retMap.put("db", tempMap);
                }
            } else {                                                        // non-conflicting contribution -- all impact comes from either SB or DB
                if (valueforTheWholeSet > 0) {
                    SB = getNodeNames(segOtherNodeList);
                    Map<Set<String>, Double> tempMap = new HashMap<>();
                    tempMap.put(SB, valueforTheWholeSet);
                    retMap.put("sb", tempMap);

                    tempMap = new HashMap<>();
                    tempMap.put(DB, 0.0);    								// insert empty set with 0.0 impact value
                    retMap.put("db", tempMap);
                } else if (valueforTheWholeSet < 0) {
                    Map<Set<String>, Double> tempMap = new HashMap<>();
                    tempMap.put(SB, 0.0);    								// insert empty set with 0.0 impact value
                    retMap.put("sb", tempMap);

                    DB = getNodeNames(segOtherNodeList);
                    tempMap = new HashMap<>();
                    tempMap.put(DB, valueforTheWholeSet);
                    retMap.put("db", tempMap);
                } else {
                    Map<Set<String>, Double> tempMap = new HashMap<>();
                    tempMap.put(SB, 0.0);
                    retMap.put("sb", tempMap);

                    tempMap = new HashMap<>();
                    tempMap.put(DB, 0.0); 									// insert empty set with 0.0 impact value
                    retMap.put("db", tempMap);
                }
            }
        }
        return retMap;
    }

    private Set<String> getNodeNames(List<String> nodeList) {
        Set<String> retSet = new HashSet<String>();
        for (String n : nodeList) {
            retSet.add(n);
        }
        return retSet;
    }

    private Set<String> getNodeNames(Node[] nodeList) {
        Set<String> retSet = new HashSet<String>();
        for (Node n : nodeList) {
            retSet.add(n.getShortName());
        }
        return retSet;
    }

    private Map<String, Map<Set<String>, Double>> conductImpactAnalysisForSingleEdge(String evidenceNodeName, String evidenceStateofInterest, String queryNodeName, String queryNodeStateName) throws Exception {
        Map<String, Map<Set<String>, Double>> retMap = new HashMap<String, Map<Set<String>, Double>>();

        ArrayList<String> allChilds = new ArrayList<>();
        allChilds.add(evidenceNodeName);
        Hashtable U_conditionedNodeList = Construct_Updated_ConditionedList_for_a_NodeList(allChilds);
        Hashtable P_conditionedNodeList = Construct_Prior_ConditionedList_for_a_NodeList(allChilds);

        set_findings_of_ConditionedNodes_3(P_conditionedNodeList, MakeNodeList(allChilds));
        double priorValue = (double) RetriveProbfromBuffer(queryNodeName, "prior").get(queryNodeStateName);

        set_findings_of_ConditionedNodes_3(U_conditionedNodeList, MakeNodeList(allChilds));
        double posteriorValue = (double) RetriveProbfromBuffer(queryNodeName, "posteriori").get(queryNodeStateName);
        double impactVal = posteriorValue - priorValue;
        Set<String> SB = new HashSet<String>();
        Set<String> DB = new HashSet<String>();

        if (impactVal > 0) {    // positive impact
            SB.add(evidenceNodeName);            // == put SB
            Map<Set<String>, Double> tempMap = new HashMap<>();
            tempMap.put(SB, impactVal);
            retMap.put("sb", tempMap);

            tempMap = new HashMap<>();
            tempMap.put(DB, 0.0);            // == put DB
            retMap.put("db", tempMap);

        } else if (impactVal < 0) {    // negative impact
            Map<Set<String>, Double> tempMap = new HashMap<>();
            tempMap.put(SB, 0.0);
            retMap.put("sb", tempMap);

            DB.add(evidenceNodeName);
            tempMap = new HashMap<>();
            tempMap.put(DB, impactVal);
            retMap.put("db", tempMap);
        } else {                    // NO impact (impactVal == 0)
            Map<Set<String>, Double> tempMap = new HashMap<>();
            tempMap.put(SB, 0.0);
            retMap.put("sb", tempMap);

            tempMap = new HashMap<>();
            tempMap.put(DB, 0.0);
            retMap.put("db", tempMap);
        }
        return retMap;
    }

    private List<String> FindNodesInAsegment(RawSegment rawSeg) {
        List<String> retList = new ArrayList<>(); 														// this list will contain distinct nodes in a segment
        Set<String> all_CE_nodes = rawSeg.commonEffect.keySet();
        for (String ceNode : all_CE_nodes) {
            if (!retList.contains(ceNode)) { 															// first insert ce nodes
                retList.add(ceNode);
            }
            Set<String> all_CE_depenedants = rawSeg.commonEffect.get(ceNode);
            for (String ceDepandant : all_CE_depenedants) {
                if (!retList.contains(ceDepandant)) { 													// then insert ce depandant nodes (aka alternate causes)
                    retList.add(ceDepandant);
                }
            }
        }
        for (String causalNode : rawSeg.causal) {
            if (!retList.contains(causalNode)) { 														// then add causal nodes
                retList.add(causalNode);
            }
        }
        for (String anti_causalNode : rawSeg.antiCausal) {
            if (!retList.contains(anti_causalNode)) { 													// then finally add anti-causal nodes
                retList.add(anti_causalNode);
            }
        }
        return retList;
    }

    private void MakeBNforMH(Net _net2, Hashtable conditionedNodeList, String queryNode, Analyser a) throws Exception {
        a.addTarget(queryNode);
        ArrayList<String> evidenceList = Collections.list(conditionedNodeList.keys());
        for (String ev : evidenceList) {
            a.addEvidence(ev);
        }
        Node[] allnodes = _net.getNodes();
        for (Node n1 : allnodes) {
            String sourceNodeID = n1.getShortName();
            a.addSimple(sourceNodeID); // all the node IDs are inserted
        }
        for (Node n1 : allnodes) {
            String sourceNodeID = n1.getShortName();
            Node[] children = n1.getChildren();
            for (Node n2 : children) {
                String targetNodeID = n2.getShortName();
                a.addEdge(sourceNodeID, targetNodeID); // add the edges
            }
        }
    }

    private void re_InitializeGlobalVariable() {
        CommonEffectNodes_and_theirPath = new Hashtable<String, String>();
        blockedEvidenceList = new ArrayList<>();
        blockedEvidenceNodeInfoList = new ArrayList<>();
        ultimateTargetNode = "";
        ultimateTargetNodeState = "";
        outputResponse_for_A_Target = "";
        NLGtext_Az = new TextGenerator_Az();
    }

    private static void saveOriginalBeleifsofAllNodes(String bufferName) throws Exception {
        Node[] allNodes = _net.getNodes();
        for (int i = 0; i < allNodes.length; i++) {
            Node _node = allNodes[i];
            String nodeName = allNodes[i].getShortName();
            Hashtable<String, Double> priorTable = new Hashtable<>();
            State[] allStates = _node.getStates();
            for (State st : allStates) {
                String stateName = st.getShortName();
                double probVal = _node.getBelief(stateName);
                probVal = ((double) Math.round((probVal) * 1000.0) / 1000.0);
                priorTable.put(stateName, probVal);
            }
            if (bufferName.equals("prior"))
                originalPriorofAllNodes.put(nodeName, priorTable);
            else
                FinalBeliefofAllNodes.put(nodeName, priorTable);
        }
    }

    private static String Find_State_of_Interest_for_intermediate_Node(String nodeName) throws Exception {
        Hashtable _currentNodesCPT = getTargetNodeCPT(nodeName);
        String stateName = "";

        double maxVal = -0.0;
        Enumeration<String> tempEnum = _currentNodesCPT.keys();
        while (tempEnum.hasMoreElements()) {
            String _state = tempEnum.nextElement();
            double val = (Double) _currentNodesCPT.get(_state);
            if (val > maxVal)
                stateName = _state;
        }
        return stateName;
    }

    private static Hashtable RetriveProbfromBuffer(String queryNodeName, String bufferName) throws Exception {
        Enumeration<String> tempEnum = originalPriorofAllNodes.keys();
        while (tempEnum.hasMoreElements()) {
            String nodeName = tempEnum.nextElement();
            if (nodeName.equals(queryNodeName)) {
                if (bufferName.equals("prior"))
                    return originalPriorofAllNodes.get(nodeName);
                else
                    return FinalBeliefofAllNodes.get(nodeName);
            }
        }
        return null;
    }

    private static double RetriveProbfromBuffer(String queryNodeName, String bufferName, Hashtable conditionedNodeList)
            throws Exception {
        Enumeration<String> tempEnum = originalPriorofAllNodes.keys();
        while (tempEnum.hasMoreElements()) {
            String nodeName = tempEnum.nextElement();
            if (nodeName.equals(queryNodeName)) {
                String stateName = "";
                if (nodeName.equals(ultimateTargetNode))
                    stateName = ultimateTargetNodeState;
                else if (conditionedNodeList.containsKey(nodeName)) {
                    stateName = Find_State_of_Interest(
                            (Hashtable) conditionedNodeList.get(_net.getNode(nodeName).getShortName()));
                } else
                    stateName = Find_State_of_Interest_for_intermediate_Node(nodeName);

                if (bufferName.equals("prior")) {
                    return (Double) originalPriorofAllNodes.get(nodeName).get(stateName);
                } else {
                    return (Double) FinalBeliefofAllNodes.get(nodeName).get(stateName);
                }
            }
        }
        return 0;
    }
 
    private static HashMap<Hashtable<Node[], Double>, Hashtable<Node[], Hashtable<String, ArrayList<String>>>> ContentSelectionFor_NLG_report(Hashtable conditionedNodeList, String queryNode, String queryNodeState, Graph _BNgraph, String impactChoice) throws Exception {
        Node[][] allSubsetsOfEvidenceNodes = generateRelevantSubsets(MakeNodeList(conditionedNodeList), queryNode, conditionedNodeList, _BNgraph);
        Hashtable<Node[], Double> ImpactValuesofSubsets = new Hashtable<Node[], Double>();
        for (int iter = 0; iter < allSubsetsOfEvidenceNodes.length; iter++) {
            Node[] currentSubset = (Node[]) allSubsetsOfEvidenceNodes[iter];
            if (impactChoice.equals("perc_change")) {
                double PercentageofTargetBeliefChange = FindPercentageofTargetBeliefChange(conditionedNodeList, currentSubset, queryNode, queryNodeState);
                ImpactValuesofSubsets.put(currentSubset, PercentageofTargetBeliefChange);
                _net.clearAllEvidence();
                _net.compile();
            }
        }
        Hashtable<Node[], Hashtable<String, ArrayList<String>>> d_connectedPaths_from_EvidenceSubsetsToTarget = new Hashtable<Node[], Hashtable<String, ArrayList<String>>>();
        Enumeration<Node[]> _subsets = ImpactValuesofSubsets.keys();
        while (_subsets.hasMoreElements()) {
            Node[] currentSubset = _subsets.nextElement();
            Hashtable<String, ArrayList<String>> temp = new Hashtable<String, ArrayList<String>>(Find_D_connected_Paths_from_EvidenceSubsets_to_Target(currentSubset, _net.getNode(queryNode), _BNgraph, conditionedNodeList));
            d_connectedPaths_from_EvidenceSubsetsToTarget.put(currentSubset, temp);
        }
        HashMap<Hashtable<Node[], Double>, Hashtable<Node[], Hashtable<String, ArrayList<String>>>> retContents = new HashMap<>();
        retContents.put(ImpactValuesofSubsets, d_connectedPaths_from_EvidenceSubsetsToTarget);
        return retContents;
    }

    private static String GeneratePreamble(ArrayList<Map.Entry<Node[], Double>> PercentageofTargetBeliefChangeofSubsetstList, Hashtable conditionedNodeList, String queryNode, String queryNodeState, Map<String, String> explainableStates) throws Exception {
        String retString = "";
        double priorBelief = (double) RetriveProbfromBuffer(queryNode, "prior").get(queryNodeState);
        priorBelief = ((double) Math.round((priorBelief) * 1000.0) / 1000.0);
        String priorText = NLGtext_Az.SayPrior(queryNode, queryNodeState, priorBelief);

        double posteriori = RetriveProbfromBuffer(queryNode, "posteriori", conditionedNodeList);
        posteriori = ((double) Math.round((posteriori) * 1000.0) / 1000.0);

        ArrayList<String> _targetNodeInfo = new ArrayList<>();
        _targetNodeInfo.add(queryNode);
        _targetNodeInfo.add(queryNodeState);
        _targetNodeInfo.add(Double.toString(posteriori));

        if (conditionedNodeList.size() > 0) {
            Map.Entry<Node[], Double> tempEntry = PercentageofTargetBeliefChangeofSubsetstList.get(0);            // get the top subset with value of its interest (e.g. percentage of changes) */
            Node[] subsetWithTopchanges = (Node[]) tempEntry.getKey();
            double topValue = (double) tempEntry.getValue();

            if (PercentageofTargetBeliefChangeofSubsetstList.size() == 1) {
                topValue = ((double) Math.round((topValue) * 1000.0) / 1000.0);
                ArrayList<ArrayList<String>> emptyNodeList_for_NO_effect_through_CPT = new ArrayList<>();
                if (topValue > 0) {
                    ArrayList<ArrayList<String>> _nodeInfoList = Find_Evidence_NodeInfoList(subsetWithTopchanges,conditionedNodeList, explainableStates);
                    String direction = "increases";
                    return priorText + "Observing "
                            + NLGtext_Az.SayImply(_nodeInfoList, _targetNodeInfo, direction, "")
                            + NLGtext_Az.SayImply_no_change(emptyNodeList_for_NO_effect_through_CPT, _targetNodeInfo, "")
                            + NLGtext_Az.SayConclusion(_targetNodeInfo, Double.toString(posteriori), Double.toString(priorBelief))
                            + System.getProperty("line.separator").toString();            // put blocked nodelist

                } else if (topValue < 0) {
                    ArrayList<ArrayList<String>> _nodeInfoList = Find_Evidence_NodeInfoList(subsetWithTopchanges,conditionedNodeList, explainableStates);
                    String direction = "decreases";
                    return priorText + "Observing "
                            + NLGtext_Az.SayImply(_nodeInfoList, _targetNodeInfo, direction, "")
                            + NLGtext_Az.SayImply_no_change(emptyNodeList_for_NO_effect_through_CPT, _targetNodeInfo, "")
                            + NLGtext_Az.SayConclusion(_targetNodeInfo, Double.toString(posteriori), Double.toString(priorBelief))
                            + System.getProperty("line.separator").toString(); // put blocked nodelist
                } else
                    return priorText + "But the evidence does not change that probability."
                            + System.getProperty("line.separator").toString();
            }
            priorBelief = ((double) Math.round((priorBelief) * 1000.0) / 1000);
            topValue = ((double) Math.round((topValue) * 1000.0) / 1000.0);


            double valueforTheWholeSet = FindtheVectorforTheWholeSet(PercentageofTargetBeliefChangeofSubsetstList,conditionedNodeList).getKey();            // get the value of the interest for the whole input evidence list
            if (subsetWithTopchanges.length < conditionedNodeList.size()) {
                Node[] remainingSubset = FindRemainingSubsets(subsetWithTopchanges, conditionedNodeList);                // get the remaining subset
                retString = GenerateEnglishwithConflict(priorBelief, subsetWithTopchanges, topValue, queryNode,queryNodeState, remainingSubset, conditionedNodeList, valueforTheWholeSet, explainableStates);
            } else {
                retString = GenerateEnglishwithoutConflict(valueforTheWholeSet, priorBelief, queryNode, queryNodeState,conditionedNodeList, explainableStates);
            }
        } else {
            ArrayList<ArrayList<String>> emptyNodeList_for_NO_effect_through_CPT = new ArrayList<>();
            return priorText
                    + NLGtext_Az.SayImply_no_change(emptyNodeList_for_NO_effect_through_CPT, _targetNodeInfo, Double.toString(posteriori))
                    + NLGtext_Az.SayConclusion(_targetNodeInfo, Double.toString(posteriori), Double.toString(priorBelief));

        }
        return retString;
    }

    private static ArrayList<ArrayList<String>> Find_Evidence_NodeInfoList(Node[] _subset, Hashtable conditionedNodeList, Map<String, String> explainableStates) throws Exception {
        ArrayList<ArrayList<String>> retList = new ArrayList<>();
        Enumeration<String> tempEnum = conditionedNodeList.keys();
        while (tempEnum.hasMoreElements()) {
            String nodeName = tempEnum.nextElement();
            Node tempNode = _net.getNode(nodeName);
            if (Arrays.asList(_subset).contains(tempNode)) {
                Hashtable tempTable = (Hashtable) conditionedNodeList.get(nodeName);
                ArrayList<String> tempList = new ArrayList<>();
                String stateName = explainableStates.get(nodeName);
                tempList.add(nodeName);
                tempList.add(stateName);
                tempList.add(Double.toString((Double) tempTable.get(stateName)));
                retList.add(tempList);
            }
        }
        return retList;
    }

    private static String Find_State_of_Interest(Hashtable tempTable) {
        String retStr = "";
        double maxVal = 0;
        Enumeration<String> tempEnum = tempTable.keys();
        while (tempEnum.hasMoreElements()) {
            String stateName = tempEnum.nextElement();
            double probVal = (Double) tempTable.get(stateName);
            if (probVal > maxVal) {
                maxVal = probVal;
                retStr = stateName;
            }
        }
        return retStr;
    }

    private static String GenerateEnglishwithoutConflict(double valueforTheWholeSet, double priorBelief, String queryNode, String queryNodeState, Hashtable conditionedNodeList, Map<String, String> explainableStates) throws Exception {
        String retStr = "";
        double posteriori = RetriveProbfromBuffer(queryNode, "posterior", conditionedNodeList);
        posteriori = ((double) Math.round((posteriori) * 1000.0) / 1000.0);
        priorBelief = ((double) Math.round((priorBelief) * 1000.0) / 1000.0);

        String PriorText = NLGtext_Az.SayPrior(queryNode, queryNodeState, priorBelief);

        ArrayList<ArrayList<String>> _nodeinfoList = Find_Evidence_NodeInfoList(MakeNodeList(conditionedNodeList), conditionedNodeList, explainableStates); // consider all the evidence (unblocked, obviously) set
        ArrayList<String> _targetNodeInfo = new ArrayList<>();
        _targetNodeInfo.add(queryNode);
        _targetNodeInfo.add(queryNodeState);
        _targetNodeInfo.add(Double.toString(valueforTheWholeSet));

        ArrayList<ArrayList<String>> emptyNodeList_for_NO_effect_through_CPT = new ArrayList<>();

        if (valueforTheWholeSet > 0) {
            String direction = "increases";
            return PriorText + "Observing "
                    + NLGtext_Az.SayImply(_nodeinfoList, _targetNodeInfo, direction, "")
                    + NLGtext_Az.SayImply_no_change(emptyNodeList_for_NO_effect_through_CPT, _targetNodeInfo, "")
                    + NLGtext_Az.SayConclusion(_targetNodeInfo, Double.toString(posteriori), Double.toString(priorBelief))
                    + System.getProperty("line.separator").toString();
        } else {
            String direction = "decreases";
            return PriorText + "Observing "
                    + NLGtext_Az.SayImply(_nodeinfoList, _targetNodeInfo, direction, "")
                    + NLGtext_Az.SayImply_no_change(emptyNodeList_for_NO_effect_through_CPT, _targetNodeInfo, "")
                    + NLGtext_Az.SayConclusion(_targetNodeInfo, Double.toString(posteriori), Double.toString(priorBelief))
                    + System.getProperty("line.separator").toString();
        }
    }

    private static Map.Entry<Double, String> FindtheVectorforTheWholeSet(ArrayList<Entry<Node[], Double>> percentageofTargetBeliefChangeofSubsetstList,Hashtable conditionedNodeList) {
        for (Map.Entry<?, Double> entry : percentageofTargetBeliefChangeofSubsetstList) {
            Node[] tempTable = (Node[]) entry.getKey();
            if (tempTable.length == conditionedNodeList.size()) {
                double val = (double) entry.getValue();
                String dir = "";
                if (val > 0)
                    dir = "increase";
                else if (val < 0)
                    dir = "decrease";
                else
                    dir = "no_change";
                return new java.util.AbstractMap.SimpleEntry<Double, String>(val, dir);
            }
        }
        return null;
    }

    private static Map.Entry<Double, String> FindtheVectorforTheWholeSet(ArrayList<Entry<Node[], Double>> percentageofTargetBeliefChangeofSubsetstList,List<String> wholeNodeList) {
        for (Map.Entry<?, Double> entry : percentageofTargetBeliefChangeofSubsetstList) {
            Node[] tempTable = (Node[]) entry.getKey();
            if (tempTable.length == wholeNodeList.size()) {
                double val = (double) entry.getValue();
                String dir = "";
                if (val > 0)
                    dir = "increase";
                else if (val < 0)
                    dir = "decrease";
                else
                    dir = "no_change";
                return new java.util.AbstractMap.SimpleEntry<Double, String>(val, dir);
            }
        }
        return null;
    }

    private static String GenerateEnglishwithConflict(double priorBelief, Node[] subsetWithTopchanges, double topValue,String queryNode, String queryNodeState, Node[] remainingSubset, Hashtable conditionedNodeList,double valueforTheWholeSet, Map<String, String> explainableStates) throws Exception {
        String PriorText = "";

        priorBelief = ((double) Math.round((priorBelief) * 1000.0) / 1000.0);
        topValue = ((double) Math.round((topValue) * 1000.0) / 1000.0);
        valueforTheWholeSet = ((double) Math.round((valueforTheWholeSet) * 1000.0) / 1000.0);

        double impactChange = valueforTheWholeSet - topValue;
        double posteriori = RetriveProbfromBuffer(queryNode, "posterior", conditionedNodeList);
        posteriori = ((double) Math.round((posteriori) * 1000.0) / 1000.0);

        PriorText = NLGtext_Az.SayPrior(queryNode, queryNodeState, priorBelief);

        ArrayList<ArrayList<String>> _nodeinfoList_1 = Find_Evidence_NodeInfoList(subsetWithTopchanges,
                conditionedNodeList, explainableStates);
        ArrayList<ArrayList<String>> _nodeinfoList_2 = Find_Evidence_NodeInfoList(remainingSubset, conditionedNodeList, explainableStates);
        ArrayList<String> _targetNodeInfo = new ArrayList<>();
        _targetNodeInfo.add(queryNode);
        _targetNodeInfo.add(queryNodeState);
        _targetNodeInfo.add(Double.toString(posteriori));

        if (topValue > 0) {
            String direction = "increases";
            return PriorText
                    + NLGtext_Az.SayContradiction(_nodeinfoList_1, _nodeinfoList_2, _targetNodeInfo, direction, posteriori, impactChange, priorBelief)
                    + System.getProperty("line.separator").toString();

        } else if (topValue < 0) {
            String direction = "decreases";
            return PriorText
                    + NLGtext_Az.SayContradiction(_nodeinfoList_1, _nodeinfoList_2, _targetNodeInfo, direction, posteriori, impactChange, priorBelief)
                    + System.getProperty("line.separator").toString();
        } else
            return "";
    }

    private static Node[] MakeNodeList(ArrayList<String> subsetofNodes) throws Exception {
        Node[] retList = new Node[subsetofNodes.size()];
        for (int i = 0; i < subsetofNodes.size(); i++) {
            String nodeName = subsetofNodes.get(i);
            Node netNode = _net.getNode(nodeName);
            retList[i] = netNode;
        }
        return retList;
    }

    private static Node[] FindRemainingSubsets(Node[] subsetWithTopchanges, Hashtable conditionedNodeList)throws Exception {
        ArrayList<Node> retList = new ArrayList<>();
        Enumeration<String> allNodes = conditionedNodeList.keys();
        int index = 0;
        while (allNodes.hasMoreElements()) {
            Node currentNodeName = (Node) _net.getNode((String) allNodes.nextElement());
            if (!Arrays.asList(subsetWithTopchanges).contains(currentNodeName))
                retList.add(currentNodeName);
            index++;
        }
        return retList.toArray(new Node[0]);
    }

    private static Node[] FindRemainingSubsets(Node[] subsetWithTopchanges, List<String> wholeList)throws Exception {
        ArrayList<Node> retList = new ArrayList<>();
        int index = 0;
        for (String node : wholeList) {
            Node currentNodeName = (Node) _net.getNode(node);
            if (!Arrays.asList(subsetWithTopchanges).contains(currentNodeName))
                retList.add(currentNodeName);
            index++;
        }
        return retList.toArray(new Node[0]);
    }

    private static double FindPercentageofTargetBeliefChange(Hashtable conditionedNodeList, Node[] currentSubset, String queryNode, String queryNodeState) throws Exception {
        double prior = getTargetNodeBelief(queryNode, queryNodeState);        /// OPTION - 2
        set_findings_of_ConditionedNodes_3(conditionedNodeList, currentSubset);
        double posterior = getTargetNodeBelief(queryNode, queryNodeState);
        return (posterior - prior);
    }

    public static ArrayList<Map.Entry<Node[], Double>> sortHashTableValue(Hashtable<?, Double> t, String order) {
        ArrayList<Map.Entry<Node[], Double>> l = new ArrayList(t.entrySet());

        if (order.equals("asc")) {
            Collections.sort(l, new Comparator<Map.Entry<Node[], Double>>() {
                public int compare(Map.Entry<Node[], Double> o1, Map.Entry<Node[], Double> o2) {
                    Double value_1 = (Double) Math.abs(o1.getValue());
                    Double value_2 = (Double) Math.abs(o2.getValue());
                    return my_own_comparator(value_1, value_2, o1.getKey(), o2.getKey(), o1.getValue(), o2.getValue());
                }
                private int my_own_comparator(Double value_1, Double value_2, Node[] key, Node[] key2, Double value, Double value2) {
                    return 0;
                }
            });
        } else {
            Collections.sort(l, new Comparator<Map.Entry<Node[], Double>>() {
                public int compare(Map.Entry<Node[], Double> o1, Map.Entry<Node[], Double> o2) {
                    Double value_1 = (Double) Math.abs(o1.getValue());
                    Double value_2 = (Double) Math.abs(o2.getValue());
                    return my_own_comparator(value_2, value_1, o2.getKey(), o1.getKey(), o2.getValue(), o1.getValue());
                }
                private int my_own_comparator(Double abs_value_2, Double abs_value_1, Node[] set_1, Node[] set_2, Double org_value_1,Double org_value_2) {
                    if (abs_value_2 > abs_value_1)
                        return 1;
                    else if (abs_value_2 < abs_value_1)
                        return -1;
                    else {
                        if (set_2.length > set_1.length)
                            return 1;
                        else if (set_2.length < set_1.length)
                            return -1;
                        else {
                            if (org_value_2 < 0)    // means is a positive value
                                return 1;
                            else
                                return -1;
                        }
                    }
                }
            });
        }
        return l;
    }

    private static Hashtable<String, ArrayList<String>> Find_D_connected_Paths_from_EvidenceSubsets_to_Target(
            Node[] currentSubset, Node targetNode, Graph _BNgraph, Hashtable conditionedNodeList) throws Exception {
        if (currentSubset.length > 0) {
            Hashtable<String, ArrayList<String>> retList = new Hashtable<String, ArrayList<String>>();
            for (int i = 0; i < currentSubset.length; i++) {
                ArrayList<String> _paths = new ArrayList<String>(
                        _BNgraph.findPaths(currentSubset[i].getShortName(), targetNode.getShortName()));
                retList.put(currentSubset[i].getShortName(), AnalysePaths_for_Dconnectivity(_paths, _BNgraph, conditionedNodeList));                // Path analysis: i.e. finding out paths with significant contribution
            }
            return retList; // this list has form like: (NODE, a_list_d_connected_paths_from_A_evidence_to_Target
        }
        return null;
    }

    private static Hashtable Find_UnBlocked_Evidence_Nodes(String targetNodeName, Graph _BNgraph, Hashtable conditionedNodeList) throws Exception {
        if (conditionedNodeList.size() > 0) {
            Hashtable retNodeList = new Hashtable(conditionedNodeList);
            Enumeration<String> allEvidenceNodes = conditionedNodeList.keys();
            while (allEvidenceNodes.hasMoreElements()) {
                String cEvidenceNode = (String) allEvidenceNodes.nextElement();
                ArrayList<String> _paths = new ArrayList<String>(_BNgraph.findPaths(cEvidenceNode, targetNodeName));
                if (is_Blocked_Evidence(_paths, _BNgraph, conditionedNodeList)) {                // Path analysis: i.e. finding out paths with significant contribution
                    blockedEvidenceList.add(cEvidenceNode);
                    retNodeList.remove(cEvidenceNode);
                }
            }
            return retNodeList; // this list has form like: (NODE, a_list_d_connected_paths_from_A_evidence_to_Target
        }
        return null;
    }

    private static boolean is_Blocked_Evidence(ArrayList<String> _paths, Graph _BNgraph, Hashtable conditionedNodeList) throws Exception {
        ArrayList<String> retList = new ArrayList<String>();
        for (int i = 0; i < _paths.size(); i++) {
            String aPath = _paths.get(i);
            String[] aPathNodes = aPath.split(" -> | <- "); // split based on the deliminators : -> or <-

            if (aPathNodes.length == 2)
                return false; // that means first node is the evidence and the second one is the target node, so the evidence node is UNBLOCKED

            int pathIndex = 0;
            boolean isPath_d_connected = true;
            for (int j = 0; j < aPathNodes.length - 2; j++) {
                int tripletLength = aPathNodes[j].length() + 4 + aPathNodes[j + 1].length() + 4 + aPathNodes[j + 2].length(); 												// length of the triplet of such form: "A <arrow> B <arrow> C"
                String triplet = aPath.substring(pathIndex, pathIndex + tripletLength);
                pathIndex += aPathNodes[j].length() + 4;

                if (triplet.equals(aPathNodes[j] + " -> " + aPathNodes[j + 1] + " -> " + aPathNodes[j + 2])
                        || triplet.equals(aPathNodes[j] + " <- " + aPathNodes[j + 1] + " <- " + aPathNodes[j + 2])
                        || triplet.equals(aPathNodes[j] + " <- " + aPathNodes[j + 1] + " -> " + aPathNodes[j + 2])) {                // check if subpath is the form of "A -> B -> C" or "A <- B <- C"

                    if (conditionedNodeList.containsKey(aPathNodes[j + 1])) {                    // check if "aPathNodes[j+1]" (i.e. "B") is observed or NOT // Note, it should be UNOBSERVED to be a part of d-connected path
                        // that means "B" is observed, therefore, this path will be ignored as it would be a blocked path
                        isPath_d_connected = false;
                        break;
                    } else
                        continue;
                } else if (triplet.equals(aPathNodes[j] + " -> " + aPathNodes[j + 1] + " <- " + aPathNodes[j + 2])) {                    // check if "aPathNodes[j+1]" (i.e. "B") is observed or NOT // Note, it or it's descendents should be OBSERVED in order to be a part of d-connected path
                    if (!conditionedNodeList.containsKey(aPathNodes[j + 1]) && !isDescendentObserved(conditionedNodeList, _BNgraph, aPathNodes[j + 1])) {
                        // here the evidence is also BLOCKED, as the central node is not observed, nor any of its descendents are observed
                        isPath_d_connected = false;
                        break;
                    } else
                        continue;
                }
            }
            if (isPath_d_connected)
                return false; 																	// that means, this particular path ensures that, it is free for this evidence node
            else
                continue;
        }
        return true; 																			// that means, there is no path which is free for the evidence node
    }

    private static ArrayList<String> AnalysePaths_for_Dconnectivity(ArrayList<String> _paths, Graph _BNgraph, Hashtable conditionedNodeList) throws Exception {
        ArrayList<String> retList = new ArrayList<String>();
        for (int i = 0; i < _paths.size(); i++) {
            String aPath = _paths.get(i);
            String[] aPathNodes = aPath.split(" -> | <- "); // split based on the deliminators : -> or <-

            int pathIndex = 0;
            boolean isPath_d_connected = true;
            for (int j = 0; j < aPathNodes.length - 2; j++) {
                int tripletLength = aPathNodes[j].length() + 4 + aPathNodes[j + 1].length() + 4 + aPathNodes[j + 2].length(); // length of the triplet of such form: "A <arrow> B <arrow> C"
                String triplet = aPath.substring(pathIndex, pathIndex + tripletLength);
                pathIndex += aPathNodes[j].length() + 4;

                if (triplet.equals(aPathNodes[j] + " -> " + aPathNodes[j + 1] + " -> " + aPathNodes[j + 2])
                        || triplet.equals(aPathNodes[j] + " <- " + aPathNodes[j + 1] + " <- " + aPathNodes[j + 2])
                        || triplet.equals(aPathNodes[j] + " <- " + aPathNodes[j + 1] + " -> " + aPathNodes[j + 2])) {                // check if subpath is the form of "A -> B -> C" or "A <- B <- C"

                    if (conditionedNodeList.containsKey(aPathNodes[j + 1])) {                    // check if "aPathNodes[j+1]" (i.e. "B") is observed or NOT // Note, it should be UNOBSERVED to be a part of d-connected path
                        isPath_d_connected = false;
                        break; 																	// that means "B" is observed, therefore, this path will be ignored as it would be a blocked path
                    }
                } else if (triplet.equals(aPathNodes[j] + " -> " + aPathNodes[j + 1] + " <- " + aPathNodes[j + 2])) {                    // check if "aPathNodes[j+1]" (i.e. "B") is observed or NOT // Note, it or it's descendents should be OBSERVED in order to be a part of d-connected path
                    if (!conditionedNodeList.containsKey(aPathNodes[j + 1]) && !isDescendentObserved(conditionedNodeList, _BNgraph, aPathNodes[j + 1])) {
                        isPath_d_connected = false;
                        break; 																	// COMMON EFEECT case:
                    } else {
                        CommonEffectNodes_and_theirPath.put(aPathNodes[j + 1], aPath); // optional action; may be useful in future
                    }
                }
            }
            if (isPath_d_connected) {                // if the whole path is O.K. (i.e. d-connected) then save it for returning
                retList.add(aPath);
            }
        }
        return retList;
    }

    private static boolean isDescendentObserved(Hashtable conditionedNodeList, Graph _BNgraph, String nodeOfInterest) throws Exception {
        ArrayList<String> descendents = new ArrayList<String>();
        descendents = FindDescendents(_BNgraph, nodeOfInterest);
        for (String currentNode : descendents) {
            if (conditionedNodeList.containsKey(currentNode)) { // here conditionedNodeList indicates all of the nodes within it are already instantiated
                Hashtable currentNodeCPT = (Hashtable) conditionedNodeList.get(currentNode);
                if (isHardEvidence(currentNodeCPT) != null)    // current node is a hard evidence
                    return true;
            }
        }
        return false;
    }

    private static ArrayList<String> FindDescendents(Graph _BNgraph, String nodeOfInterest) {
        ArrayList<String> retList = new ArrayList<String>();
        BT(nodeOfInterest, retList, _BNgraph);
        return retList;
    }

    private static void BT(String currentNode, ArrayList<String> retList, Graph _BNgraph) {
        LinkedList<String> children = _BNgraph.getAllAdajacencyInfo()[_BNgraph.getAllGraphNodes().indexOf(currentNode)];
        if (children.size() == 0) {
            return;
        } else {
            for (String child : children) {
                retList.add(child);
                BT(child, retList, _BNgraph);
            }
            return;
        }
    }

    private static Node[][] generateAllSubsets(Node[] listParent, String queryNode, Hashtable conditionedNodeList, Graph _BNGraph) throws Exception {
        int n = listParent.length;
        ArrayList<ArrayList<Node>> retArr = new ArrayList<>();

        // Run a loop for printing all 2^n subsets one by one
        for (int i = 0; i < (1 << n); i++) {
            ArrayList<Node> temp = new ArrayList<>();
            for (int j = 0; j < n; j++)
                // (1<<j) is a number with jth bit 1 so when we 'and' them with the subset number we get which numbers are present in the subset and which are not
                if ((i & (1 << j)) > 0)
                    temp.add(listParent[j]);
            if (isSubsetRelevant(temp.toArray(new Node[0]), queryNode, conditionedNodeList, _BNGraph) && temp.size() > 0)
                retArr.add(temp);
        }
        return retArr.stream().map(l -> l.stream().toArray(Node[]::new)).toArray(Node[][]::new);
    }

    private static Node[][] generateRelevantSubsets(Node[] listParent, String queryNodeName, Hashtable conditionedNodeList, Graph _BNgraph) throws Exception {
        int n = listParent.length;
        ArrayList<ArrayList<Node>> retArr = new ArrayList<>();

        for (int i = 0; i < (1 << n); i++) {        // Run a loop for printing all 2^n subsets one by one
            ArrayList<Node> temp = new ArrayList<>();
            for (int j = 0; j < n; j++)

                // (1<<j) is a number with jth bit 1 so when we 'and' them with the subset number we get which numbers are present in the subset and which are not
                if ((i & (1 << j)) > 0)
                    temp.add(listParent[j]);

            if (isSubsetRelevant(temp.toArray(new Node[0]), queryNodeName, conditionedNodeList, _BNgraph)&& (temp.size() > 0)) {            // check if this subset is relevant, then add
                retArr.add(temp);
            }
        }
        return retArr.stream().map(l -> l.stream().toArray(Node[]::new)).toArray(Node[][]::new);        // converting an (arraylist of arraylist) to a 2D array
    }

    private static boolean isSubsetRelevant(Node[] currentSubset, String queryNodeName, Hashtable conditionedNodeList, Graph _BNgraph) throws Exception {
        set_findings_of_ConditionedNodes_3(conditionedNodeList, currentSubset); // set findings of the current evidences
        for (int i = 0; i < currentSubset.length; i++) {
            String currentEvidenceNode = currentSubset[i].getShortName();
            List<String> nodeNameList = getNodeNameList(currentSubset);

            ArrayList<String> _paths = new ArrayList<String>(_BNgraph.findPaths(currentEvidenceNode, queryNodeName));
            if (is_Blocked_Evidence(nodeNameList, _paths, _BNgraph, conditionedNodeList)) {            // Path analysis: i.e. finding out paths with significant contribution
                _net.clearAllEvidence();
                _net.compile();
                return false;
            }
        }
        _net.clearAllEvidence();
        _net.compile();
        return true;
    }

    private static List<String> getNodeNameList(Node[] currentSubset) {
        List<String> retList = new ArrayList<>();
        for (Node n : currentSubset) {
            retList.add(n.getShortName());
        }
        return retList;
    }

    private static boolean is_Blocked_Evidence(List<String> currentSubset, ArrayList<String> _paths, Graph _BNgraph, Hashtable conditionedNodeList) {
        for (int i = 0; i < _paths.size(); i++) {
            String aPath = _paths.get(i);
            String[] aPathNodes = aPath.split(" -> | <- "); // split based on the deliminators : -> or <-

            if (aPathNodes.length == 2)
                return false; // that means first node is the evidence and the second one is the target node, so the evidence node is UNBLOCKED

            int pathIndex = 0;
            boolean isPath_d_connected = true;
            for (int j = 0; j < aPathNodes.length - 2; j++) {
                int tripletLength = aPathNodes[j].length() + 4 + aPathNodes[j + 1].length() + 4 + aPathNodes[j + 2].length(); // length of the triplet of such form: "A <arrow> B <arrow> C"
                String triplet = aPath.substring(pathIndex, pathIndex + tripletLength);
                pathIndex += aPathNodes[j].length() + 4;

                if (triplet.equals(aPathNodes[j] + " -> " + aPathNodes[j + 1] + " -> " + aPathNodes[j + 2])
                        || triplet.equals(aPathNodes[j] + " <- " + aPathNodes[j + 1] + " <- " + aPathNodes[j + 2])
                        || triplet.equals(aPathNodes[j] + " <- " + aPathNodes[j + 1] + " -> " + aPathNodes[j + 2])) {                // check if subpath is the form of "A -> B -> C" or "A <- B <- C"

                    if (conditionedNodeList.containsKey(aPathNodes[j + 1])) {                    // check if "aPathNodes[j+1]" (i.e. "B") is observed or NOT // Note, it should be UNOBSERVED to be a part of d-connected path
                        // that means "B" is observed, therefore, this path will be ignored as it would be a blocked path
                        isPath_d_connected = false;
                        break;
                    } else
                        continue;
                } else if (triplet.equals(aPathNodes[j] + " -> " + aPathNodes[j + 1] + " <- " + aPathNodes[j + 2])) {                    // check if "aPathNodes[j+1]" (i.e. "B") is observed or NOT // Note, it or it's descendents should be OBSERVED in order to be a part of d-connected path
                    if (!currentSubset.contains(aPathNodes[j + 1]) && !isDescendentObserved(currentSubset, _BNgraph, aPathNodes[j + 1])) {
                        // here the evidence is also BLOCKED, as the central node is not observed, nor any of its descendents are observed
                        isPath_d_connected = false;
                        break;
                    } else
                        continue;
                }
            }
            if (isPath_d_connected)
                return false; // that means, this particular path ensures that, it is free for this evidence node
            else
                continue;
        }
        return true; // that means, there is no path which is free for the evidence node
    }

    private static boolean isDescendentObserved(List<String> currentSubset,Graph _BNgraph, String nodeOfInterest) {
        ArrayList<String> descendents = new ArrayList<String>();
        descendents = FindDescendents(_BNgraph, nodeOfInterest);
        for (String currentNode : descendents) {
            if (currentSubset.contains(currentNode)) {
                return true;
            }
        }
        return false;
    }

    private static Hashtable getTargetNodeCPT(String NodeName) throws Exception {
        Node _node = _net.getNode(NodeName);
        Hashtable<String, Double> probTable = new Hashtable<>();
        State[] allStates = _node.getStates();
        for (State st : allStates) {
            String stateName = st.getShortName();
            double probVal = _node.getBelief(stateName);
            probVal = ((double) Math.round((probVal) * 1000.0) / 1000.0);
            probTable.put(stateName, probVal);
        }
        return probTable;
    }

    private static void set_findings_of_ConditionedNodes_3(Hashtable conditionedNodeList, Node[] _subset) throws Exception {
        Enumeration _list = conditionedNodeList.keys();
        while (_list.hasMoreElements()) {
            String _activatedNode = (String) _list.nextElement();            /* read the activated Node name */
            Node _node = _net.getNode(_activatedNode);// try to load the Node with the name, if any problem netica Exception will
            int nStates = _node.getNumberStates();
            if (Arrays.asList(_subset).contains(_node)) {
                double[] _likelihoodVector = new double[nStates];                /* set likelihood vector */
                boolean[] _flag = new boolean[nStates];
                Hashtable _tempTable = (Hashtable) conditionedNodeList.get(_activatedNode);
                Enumeration _activeInfo = _tempTable.keys();

                if (nStates < _tempTable.size()) {                /* check the number of instantiated states */
                    Exception exp = new NLGException("Number of instantiated states are greater than it should be");
                    throw exp;
                }
                double tempSum = 0.0;
                while (_activeInfo.hasMoreElements()) {
                    String _knownState = (String) _activeInfo.nextElement();
                    double _knownValue = (double) _tempTable.get(_knownState);
                    tempSum += _knownValue;
                    tempSum = ((double) Math.round((tempSum) * 1000.0) / 1000.0);                    /* set that known value into the corresponding node states */

                    if (tempSum > 1.0) {
                        if ((tempSum - 1.0) <= epsilon)
                            tempSum = 1.0;
                        else {
                            Exception exp = new NLGException("Total probability has become > 1.0 so far !!");
                            throw exp;
                        }
                    }
                    int stateIndex = _node.getState(_knownState).getIndex();
                    _likelihoodVector[stateIndex] = _knownValue;
                    _flag[stateIndex] = true;
                }

                tempSum = ((double) Math.round((tempSum) * 1000.0) / 1000.0);
                if ((nStates == _tempTable.size()) && (tempSum < 1.0)) {
                    Exception exp = new NLGException("Total probability should be 1.0, right?");
                    throw exp;
                }
                /* we may need to make other states equiprobable remaining number of states to be made "Equiprobable " */
                int denom = nStates - _tempTable.size();
                double _totalRemainingPercentageValue_per = (1.0 - tempSum) / ((double) denom);
                for (int iter = 0; iter < nStates; iter++) {
                    if (!_flag[iter])
                        _likelihoodVector[iter] = (float) _totalRemainingPercentageValue_per;
                }

                //finally set the findings as a likelihood of this particular instantiated node
                _node.setEvidenceLikelihood(_likelihoodVector);
            }
        }
        _net.compile();
    }

    private static double getTargetNodeBelief(String queryNodeName, String queryNodeState) throws Exception {
        Node node = _net.getNode(queryNodeName);
        double belief = node.getBelief(queryNodeState);
        return belief;
    }

    private static Node[] MakeNodeList(Hashtable conditionedNodeList) throws Exception {
        ArrayList<Node> retList = new ArrayList<>();
        Enumeration elem = conditionedNodeList.keys();
        while (elem.hasMoreElements()) {
            String _nodeName = (String) elem.nextElement();
            retList.add(_net.getNode(_nodeName));
        }
        return retList.toArray(new Node[0]);
    }

    private static boolean CheckConsistency(Hashtable activeNodeList, String queryNode) throws Exception {
        Enumeration _enum = activeNodeList.keys();
        while (_enum.hasMoreElements()) {
            if (_net.getNode((String) _enum.nextElement()) == null)
                return false;
        }
        return true;
    }

    private static Hashtable<Node[], Double> FindPercentageofTargetBeliefChangeForAllSubsets(Node[][] allSubsetsOfEvidenceNodes, Hashtable U_conditionedNodeList, Hashtable P_conditionedNodeList,String queryNodeName, String queryNodeStateName, double priorVal, ArrayList<String> allChild) throws Exception {
        Hashtable<Node[], Double> PercentageofTargetBeliefChangeofSubsets = new Hashtable<Node[], Double>();
        for (int iter = 0; iter < allSubsetsOfEvidenceNodes.length; iter++) {
            Node[] currentSubset = (Node[]) allSubsetsOfEvidenceNodes[iter];
            set_findings_of_ConditionedNodes_3(U_conditionedNodeList, currentSubset); // setting the CPTs of subsets of
            double posteriorVal = (double) getTargetNodeCPT(queryNodeName).get(queryNodeStateName); // get the posterior
            posteriorVal = ((double) Math.round((posteriorVal) * 1000.0) / 1000.0);
            set_findings_of_ConditionedNodes_3(P_conditionedNodeList, MakeNodeList(allChild)); // resetting the CPTs of
            double change = posteriorVal - priorVal;
            change = ((double) Math.round((change) * 1000.0) / 1000.0);
            PercentageofTargetBeliefChangeofSubsets.put(currentSubset, change);
        }
        return PercentageofTargetBeliefChangeofSubsets;
    }

    private static ArrayList<Entry<Node[], String>> findDirectionofVector( ArrayList<Entry<Node[], Double>> magnitudeofSortedImpactValues) {
        ArrayList<Entry<Node[], String>> retList = new ArrayList<>();
        for (int i = 0; i < magnitudeofSortedImpactValues.size(); i++) {
            Map.Entry<?, Double> tempEntry = magnitudeofSortedImpactValues.get(i);
            Node[] subsetWithTopchanges = (Node[]) tempEntry.getKey();
            double Value = (double) tempEntry.getValue();
            if (Value > 0)
                retList.add(new java.util.AbstractMap.SimpleEntry<Node[], String>(subsetWithTopchanges, "increase"));
            else if (Value < 0)
                retList.add(new java.util.AbstractMap.SimpleEntry<Node[], String>(subsetWithTopchanges, "decrease"));
            else
                retList.add(new java.util.AbstractMap.SimpleEntry<Node[], String>(subsetWithTopchanges, "no_change"));
        }
        return retList;
    }
    
    private static String Find_target_State_Name(String node, Hashtable conditionedNodeList) throws Exception {
        if (node.equals(ultimateTargetNode))
            return ultimateTargetNodeState;
        else if (conditionedNodeList.containsKey(node))
            return Find_State_of_Interest((Hashtable) conditionedNodeList.get(node));
        else
            return Find_State_of_Interest_for_intermediate_Node(node);
    }

    private static String Find_evidence_State_Name(String CurrentNodeName, Hashtable conditionedNodeList)
            throws Exception {
        String retStr = "";
        Enumeration<String> tempEnum = conditionedNodeList.keys();
        while (tempEnum.hasMoreElements()) {
            String nodeName = tempEnum.nextElement();

            if (CurrentNodeName.equals(nodeName)) {
                Hashtable tempTable = (Hashtable) conditionedNodeList.get(nodeName);
                retStr = Find_State_of_Interest(tempTable);
            }
        }
        return retStr;
    }

    private static Hashtable getStateValuesofaNode(String eVnodeName) throws Exception {
        Hashtable retList = new Hashtable<>();
        Node currentNode = _net.getNode(eVnodeName);
        int nStates = currentNode.getNumberStates();
        for (int i = 0; i < nStates; i++) {
            String stateName = currentNode.getState(i).getShortName();
            retList.put(stateName, ""); // the value will be filled later while processing the blob.
        }
        return retList;
    }
    
    //  	the idea of this function is to get the latest CPTs of all the participating nodes (AFTER all evidence nodes are instantiated)
    private static Hashtable Construct_Updated_ConditionedList_for_a_NodeList_2(ArrayList<String> nodeList)throws Exception {
        Hashtable retList = new Hashtable<>();
        for (int i = 0; i < nodeList.size(); i++) {
            String nodeName = nodeList.get(i);
            if (nodeName.startsWith("ubhs92jh_")) {
                Hashtable tempTable = new Hashtable<>();
                tempTable.put("False", 1.0);
                tempTable.put("True", 0.0);
                retList.put(nodeName, tempTable);

            } else {
                Hashtable nodeCPT = (Hashtable) FinalBeliefofAllNodes.get(nodeName);
                retList.put(nodeName, nodeCPT);
            }
        }
        return retList;
    }

    private static Hashtable Construct_Updated_ConditionedList_for_a_NodeList(ArrayList<String> nodeList) throws Exception {
        Hashtable retList = new Hashtable<>();
        for (int i = 0; i < nodeList.size(); i++) {
            String nodeName = nodeList.get(i);
            Node _node = _net.getNode(nodeName);

            Hashtable<String, Double> probTable = new Hashtable<>();
            State[] allStates = _node.getStates();
            for (State st : allStates) {
                String stateName = st.getShortName();
                double probVal = _node.getBelief(stateName);
                probVal = ((double) Math.round((probVal) * 1000.0) / 1000.0);
                probTable.put(stateName, probVal);
            }
            retList.put(nodeName, probTable);
        }
        return retList;
    }

    // 	the idea of this function is to get the CPTs of all the participating nodes (BEFORE all evidence nodes are instantiated)
    private static Hashtable Construct_Prior_ConditionedList_for_a_NodeList(ArrayList<String> nodeList)
            throws Exception {
        Hashtable retList = new Hashtable<>();
        for (int i = 0; i < nodeList.size(); i++) {
            String nodeName = nodeList.get(i);
            if (nodeName.startsWith("ubhs92jh_")) {
                Hashtable tempTable = new Hashtable<>();
                tempTable.put("False", 1.0);
                tempTable.put("True", 0.0);
                retList.put(nodeName, tempTable);
            } else {
                Hashtable nodeCPT = (Hashtable) originalPriorofAllNodes.get(nodeName);
                retList.put(nodeName, nodeCPT);
            }
        }
        return retList;
    }
}
