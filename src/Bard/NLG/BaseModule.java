package Bard.NLG;

//import norsys.netica.*;
import java.io.*;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Stack;
import java.util.Collections;
import java.util.Comparator;
import java.lang.Math;
import java.lang.reflect.Array;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.math3.distribution.*;
import org.apache.commons.math3.stat.inference.*;
import org.json.JSONObject;

//import org.json.simple.JSONObject;

import com.google.common.collect.Multimap;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

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
	// private static Map<String, ArrayList<String>> CE_blobs = new Map<>();
	// private static ListMultimap<String, String> CE_blobs_structures =
	// ArrayListMultimap.create();

	private static Hashtable backupConditionedList = new Hashtable<>();

	private static ArrayList<Blob> CE_blobs = new ArrayList<>();
	private static ArrayList<Blob> BFS_blobs = new ArrayList<>();

	private static ArrayList<String> allBNnodes = new ArrayList<>();

	private static ArrayList<String> incEvidenceList = new ArrayList<>();
	private static ArrayList<String> decEvidenceList = new ArrayList<>();

	private static ArrayList<String> blockedEvidenceList = new ArrayList<>();
	public static ArrayList<ArrayList<String>> blockedEvidenceNodeInfoList = new ArrayList<>();
	private static ArrayList<String> weakEvidenceList = new ArrayList<>();

	private static Hashtable<String, Hashtable<String, Double>> originalPriorofAllNodes = new Hashtable<>();
	private static Hashtable<String, Hashtable<String, Double>> FinalBeliefofAllNodes = new Hashtable<>();

	private static String ultimateTargetNode = "";
	private static String ultimateTargetNodeState = "";

	private static String outputResponse_for_A_Target = "";

	private static TextGenerator_Az NLGtext = new TextGenerator_Az();

	private static Map<String, String> semanticStates = new HashMap();
	private static Map<String, String> explainableStates  = new HashMap<>();
	
	private static Net _net;
	
	//private static Graph _BNgraph = null;

	private static int stateIndexofInterest = 0;

	private static double epsilon = 0.001;
	
	


	public BaseModule() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param args
	 */
	public String runNLG(JSONObject config) throws Exception {
		return runNLG(new Net(config.getString("netPath")), config);
	}
	private static ArrayList<ArrayList<String>> CreatblockedNodeInfoList() throws Exception {
		ArrayList<ArrayList<String>> retList = new ArrayList<>();
		for(String bNode: blockedEvidenceList) {
			ArrayList<String> tempList = new ArrayList<>();
			tempList.add(bNode);
			tempList.add(explainableStates.get(bNode));
			tempList.add("0.0");
			
			retList.add(tempList);
		}
			
		return retList;
	}

	public String runNLG(Net net, JSONObject config) throws Exception {
		_net = net;

		Hashtable conditionedNodeList = new Hashtable();
		HashMap hMap = new HashMap<>();

		ArrayList<ArrayList<String>> targetList = new ArrayList<>();

		
		Hashtable backgroundNodeList = new Hashtable();
		String queryNode = "";
		String queryNodeState = "";
		Graph _BNgraph = new Graph(_net);
		
		String verbosity = "";

		/// Go through all nodes defined in config
		for (Object _node : config.getJSONArray("nodes")) {
			JSONObject node = (JSONObject) _node;
			/// Add background nodes to the right list
			if (node.getBoolean("background")) {
				backgroundNodeList.put(node.getString("name"), true);
			}
			/// Add conditioned nodes to their own list
			else {
				HashMap tempMap = (HashMap) node.getJSONObject("conditioned").toMap();
				Hashtable tempTable = new Hashtable<>();
				tempTable.putAll(tempMap);
				conditionedNodeList.put(node.getString("name"), tempTable);
			}
		}
		
				
		// JSONObject jsonTargetNode = (JSONObject) config.getJSONObject("target");
		// queryNode = jsonTargetNode.getString("name");
		// queryNodeState = jsonTargetNode.getString("State");
		//
		for (Object _node : config.getJSONArray("target")) {
			JSONObject node = (JSONObject) _node;
			ArrayList<String> tempList = new ArrayList<>();

			tempList.add(node.getString("name"));
			tempList.add(node.getString("state"));

			targetList.add(tempList);
		}
		verbosity = config.getString("verbosity");

		String FinalOutputString = "";

		boolean _isConfigFileOK = CheckConsistency(conditionedNodeList, queryNode);
		if (!_isConfigFileOK) {
			Exception exp = new NLGException("Invalid Node Detected in the configuration File");
			throw exp;
		}

		DecimalFormat df = new DecimalFormat("#.#"); // 1-decimal point
		df.setRoundingMode(RoundingMode.CEILING);
		
		/* Response for case where NO EVIDENCE info provided */
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
					//probList.add(Double.toString(probVal));
					probList.add(df.format(probVal * 100.0) + "% (" + NLGtext.PutVerbalWord_Az(probVal, false, adjectiveProb) + ")");
				}
				Double probVal2 = _net.getNode(targetList.get(targetList.size() - 1).get(0))
						.getBelief(targetList.get(targetList.size() - 1).get(1));
				probVal2 = ((double) Math.round((probVal2) * 1000.0) / 1000.0);
				//probList.add(Double.toString(probVal2));
				probList.add(df.format(probVal2 * 100.0) + "% (" + NLGtext.PutVerbalWord_Az(probVal2,false, adjectiveProb) + ")");

				if (targetList.size() > 1) {
					FinalOutputString += "and " + targetList.get(targetList.size() - 1).get(0) + "="
							+ targetList.get(targetList.size() - 1).get(1) + " are ";
					for (int i = 0; i < (probList.size() - 1); i++) {
						FinalOutputString += (probList.get(i) + ", ");
					}
					FinalOutputString += "and " + probList.get(probList.size() - 1) + ", respectively."
							+ System.getProperty("line.separator").toString();
				} else {
					// FinalOutputString = FinalOutputString.substring(0, FinalOutputString.length()
					// - 2);
					FinalOutputString += (targetList.get(0).get(0) + "=" + targetList.get(0).get(1) + " is "
							+ probList.get(0) + "." + System.getProperty("line.separator").toString());
				}
			} else
				FinalOutputString = System.getProperty("line.separator").toString()
						+ "Error: NLG can't produce a report on the current request."
						+ System.getProperty("line.separator").toString();
			
			
			/* customizing this for long verbosity*/
			if(verbosity.equals("long")) {
				FinalOutputString = "<h1 class=\"target\">Target(s): " + escapeHtml(getTargetNodeNames(targetList)) + "</h1>\n <div class=\"summary\"> \n <h1>Summary</h1>\n" + escapeHtml(FinalOutputString) + "</div>";
			}
			
			return FinalOutputString;
		}

		/*
		 * the conditionedNodeList needs to backed up since it gets changed over the
		 * course of generating explanations for a single target
		 */

		backupConditionedList = new Hashtable<>(conditionedNodeList);
		allBNnodes = FindAllBNnodes();

		// =======================
		// get ultimate prior and ultimate posteriori for all the nodes
		/* Also, get rid of the blocked nodes */
		_net.clearAllEvidence();
		_net.compile();
		saveOriginalBeleifsofAllNodes("prior");
		set_findings_of_ConditionedNodes_3(conditionedNodeList, MakeNodeList(conditionedNodeList)); // this will
		// help to find
		// and get rid
		// of
		// d-separeted
		// evidences (in
		// the context
		// of querynode)
		// from the list
		saveOriginalBeleifsofAllNodes("posterior");
		
		semanticStates = GetSemanticStatesBN(); // 
		explainableStates = GetExplainableStatesBN(conditionedNodeList,	targetList); // for evidence-target nodes, states will be picked from the JSONobject,
		// otherwise: True/Yes, or the state at the zero index


		_net.clearAllEvidence();
		_net.compile();
		// ========================

		// Work-in-Progress for each target node
		List<String> JsonOutPutList = new ArrayList<String>();
		
		for (int i = 0; i < targetList.size(); i++) {

			outputResponse_for_A_Target = "";
			conditionedNodeList = (Hashtable) backupConditionedList.clone();

			queryNode = targetList.get(i).get(0);
			queryNodeState = targetList.get(i).get(1);

			// Re initialize all the global variables
			re_InitializeGlobalVariable();

			conditionedNodeList = Find_UnBlocked_Evidence_Nodes(queryNode, _BNgraph, conditionedNodeList);
			blockedEvidenceNodeInfoList = CreatblockedNodeInfoList();

			
			/* compile the net */
			_net.clearAllEvidence();
			_net.compile();

			ultimateTargetNode = queryNode;
			ultimateTargetNodeState = queryNodeState;


			// --------------------- Generate Summary [start] ------------------------
			
			HashMap<Hashtable<Node[], Double>, Hashtable<Node[], Hashtable<String, ArrayList<String>>>> _contents = ContentSelectionFor_NLG_report(
					conditionedNodeList, queryNode, queryNodeState, _BNgraph, "perc_change");
			Iterator iter = _contents.entrySet().iterator();
			Hashtable<Node[], Double> impactValues = null;
			Hashtable<Node[], Hashtable<String, ArrayList<String>>> d_connectedPaths = null;
			while (iter.hasNext()) {
				Map.Entry<Hashtable<Node[], Double>, Hashtable<Node[], Hashtable<String, ArrayList<String>>>> _entry = (Map.Entry<Hashtable<Node[], Double>, Hashtable<Node[], Hashtable<String, ArrayList<String>>>>) iter
						.next();
				impactValues = new Hashtable<>(_entry.getKey());
				d_connectedPaths = new Hashtable<>(_entry.getValue()); // this one will eventually be ChainList
			}

			// get the hashtable sorted as an ArrayList
			ArrayList<Map.Entry<Node[], Double>> sortedImpactValuesofSubsets = sortHashTableValue(impactValues, "desc");

			_net.clearAllEvidence();
			_net.compile();
			String firstPreamble = GeneratePreamble(sortedImpactValuesofSubsets, conditionedNodeList, queryNode,
					queryNodeState, explainableStates);
			_net.clearAllEvidence();
			_net.compile();

			if(verbosity.equals("long")) {
				outputResponse_for_A_Target += "<h1 class=\"target\">Target: " + escapeHtml(queryNode) + "</h1>\n";
				outputResponse_for_A_Target += "<div class=\"summary\">\n";
				outputResponse_for_A_Target += "<h1>Summary</h1>\n";

				outputResponse_for_A_Target += escapeHtml(firstPreamble) + "\n</div>";
			}else
				outputResponse_for_A_Target += firstPreamble; 

			if (verbosity.equals("short")) {
				
				//FinalOutputString += "[Query variable:= " + queryNode + "] " + firstPreamble;			// if they want plain text in the short mode;
				FinalOutputString += outputResponse_for_A_Target;								// if they want HTML text in the short mode;
				continue;
			}
			
			// --------------------- Generate Summary [end] ------------------------
			
			//// code for only getting the whole BN structure (preamble of detailed part) with FAKE evidence for all nodes
			Hashtable fake_conditionedNodeList = makeFakeConditionedNodeList(targetList,conditionedNodeList);
			boolean fake_it_Philip = true;

			// add fake node (and edge) to the Agena BN: it helps to allow more paths to be free (i.e. d-connected)
			if(fake_it_Philip) {		// fake detail analysis: to get the whole BN
				Node[] bnNodes = _net.getNodes();
				int j = 0;
				for(Node node: bnNodes) {
					if (node.getParents().length >= 2) {
						String fakeNodeName = "ubgs92jh_"+j;		// ubgs92jh = fake nodeName initial
						Node tempNode = _net.addNode(fakeNodeName, new String[] {"True","False"});
						tempNode.addParent(net.getNode(node.getShortName()));
					}
					j++;
				}


				Analyser a = new Analyser();
				MakeBNforMH(_net, fake_conditionedNodeList, queryNode, a);

				List<RawSegment> fake_orderedSegmentList = a.getRawSegments(); // replace this MH1 function name with Matt's
				List<Segment> fake_orderedSegmentListForNLG = ReConstructSegmentForNLG(conditionedNodeList, fake_orderedSegmentList, semanticStates,
						explainableStates, fake_conditionedNodeList, "perc_change", _BNgraph, fake_it_Philip);
				TextGenerator fake_tg = new TextGenerator(fake_orderedSegmentListForNLG, fake_it_Philip);
				outputResponse_for_A_Target += fake_tg.getText().replace("<br>", ""); 

				bnNodes = _net.getNodes();
				for(Node node: bnNodes) {
					if (node.getShortName().startsWith("ubgs92jh")) {		// ubgs92jh = fake nodeName initial
						node.remove();
					}
					j++;
				}
			}
			
			fake_it_Philip = false;
			fake_conditionedNodeList.clear();
			// real detail analysis
				if(conditionedNodeList.size() > 0) {
					set_findings_of_ConditionedNodes_3(conditionedNodeList, MakeNodeList(conditionedNodeList));
					outputResponse_for_A_Target += System.getProperty("line.separator").toString();
					outputResponse_for_A_Target += System.getProperty("line.separator").toString();
//					outputResponse_for_A_Target += "Now we are going to look at how the evidence affects the probabilities along the paths to the variable " + queryNode 
//							+ ". Please note that all the conclusions are in light of all the available evidence." + System.getProperty("line.separator").toString(); 
					outputResponse_for_A_Target += System.getProperty("line.separator").toString();
					ArrayList<String> chainList = PareseChainList(d_connectedPaths);
					Analyser a = new Analyser();
					MakeBNforMH(_net, conditionedNodeList, queryNode, a);
					List<RawSegment> orderedSegmentList = a.getRawSegments(); // replace this MH1 function name with Matt's
					List<Segment> orderedSegmentListForNLG = ReConstructSegmentForNLG(conditionedNodeList, orderedSegmentList, semanticStates,
							explainableStates, fake_conditionedNodeList, "perc_change", _BNgraph, fake_it_Philip);
					JsonOutPutList.add(Segment.toStringJSON(orderedSegmentListForNLG));
					TextGenerator tg = new TextGenerator(orderedSegmentListForNLG, fake_it_Philip);
					outputResponse_for_A_Target += tg.getText().replace("<br>", "");
				}
			
			FinalOutputString += outputResponse_for_A_Target; // this "outputResponse_for_A_Target" holds (overall +
//			FinalOutputString += System.getProperty("line.separator").toString();
		}

		BufferedWriter bw = null;
////		try {
////			String filePathforJson = "C:\\Users\\aazad\\Google Drive\\BARD-NLG Team [private]\\Matt-Az Interfacing [Gamma1]\\output_JSON_Files_including_3Nations [18th Jan]\\";
////			//filePathforJson += (getFileNameWithoutExtension(new File(NLG.filename)) + "_NLG_Explanation.txt");
////			filePathforJson += (getFileNameWithoutExtension(new File(NLG_Az.filename)) + ".json");
////			File oFile = new File(filePathforJson);
////
////			/* This logic will make sure that the file 
////			 * gets created if it is not present at the
////			 * specified location*/
////			if (!oFile.exists()) {
////				oFile.createNewFile();
////			}
////
////			FileWriter fw = new FileWriter(oFile);
////			bw = new BufferedWriter(fw);
////			for(int i = 0; i < JsonOutPutList.size(); i++)
////				bw.write(JsonOutPutList.get(i));
////		} catch (IOException ioe) {
////			ioe.printStackTrace();
////		}
////		finally
////		{ 
////			try{
////				if(bw!=null)
////					bw.close();
////			}catch(Exception ex){
////				System.out.println("Error in closing the BufferedWriter"+ex);
////			}
////		}
//
		bw = null;
		try {
			String filePathforJson = "C:\\Users\\aazad\\Google Drive\\BARD-NLG Team [private]\\Matt-Az Interfacing [Gamma1]\\output_Explanation_Files [New 13 Level 0 (integrated with Matt's Java Code)]\\";
			filePathforJson += (getFileNameWithoutExtension(new File(NLG_Az.filename)) + "_NLG_Explanation.html");
			File oFile = new File(filePathforJson);

			/* This logic will make sure that the file 
			 * gets created if it is not present at the
			 * specified location*/
			if (!oFile.exists()) {
				oFile.createNewFile();
			}

			FileWriter fw = new FileWriter(oFile);
			bw = new BufferedWriter(fw);
			
			bw.write(FinalOutputString);
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
		finally
		{ 
			try{
				if(bw!=null)
					bw.close();
			}catch(Exception ex){
				System.out.println("Error in closing the BufferedWriter"+ex);
			}
		}
		
		return FinalOutputString;
	}
	
	private String getTargetNodeNames(ArrayList<ArrayList<String>> targetList) {
		String retStr = "";
		for(ArrayList<String> targetInfo: targetList) {
			retStr += targetInfo.get(0) + ", ";
		}
		return retStr.substring(0,retStr.length()-2);
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
		for(Node node:allBNnodes) {
			if(isNodeOK_tobe_Fake_Evidence(node, targetList, conditionedNodeList)) {
				State[] allStates = node.getStates();
				Hashtable tempTable = new Hashtable<>();
				for(int i = 0; i < allStates.length; i++) {
					double val = 0.0;
					if(i == 0)
						val = 1.0;
					tempTable.put(allStates[i].getShortName(), val);
				}
				retTable.put(node.getShortName(),tempTable);
			}
		}
		return retTable;
	}

	private boolean isNodeOK_tobe_Fake_Evidence(Node node, ArrayList<ArrayList<String>>targetList, Hashtable conditionedNodeList) throws Exception {
		
//		for(ArrayList<String> targetnodeInfo: targetList) {
//			if(targetnodeInfo.get(0).equals(node.getShortName()))
//				return false;
//		}
		
		if(ultimateTargetNode.equals(node.getShortName()))
			return false;
		
//		if(conditionedNodeList.containsKey(node.getShortName()))
//			return true;
		
		if((node.getParents().length >= 2) && (node.getChildren().length == 0)) 		// hoping 'node' is CE node
			return true;
//		else if((node.getParents().length >= 2) && (node.getChildren().length >= 1) && conditionedNodeList.containsKey(node.getShortName()))
//			return false;
		else if((node.getParents().length == 1) && (node.getChildren().length == 0)) // hoping 'node' is non-CE but a leaf
			return true;
		else if((node.getParents().length == 0) && (node.getChildren().length == 1))// hoping 'node' is non-CE but a leaf
			return true;
		else			// hoping 'node' is non-CE and non-Terminal one, so not safe to be an evidence - can block a path
			return false;
	}

	private static String getFileNameWithoutExtension(File file) {
        String fileName = "";
 
        try {
            if (file != null && file.exists()) {
                String name = file.getName();
                fileName = name.replaceFirst("[.][^.]+$", "");
            }
        } catch (Exception e) {
            e.printStackTrace();
            fileName = "";
        }
 
        return fileName;
 
    }


	private Map<String, String> GetExplainableStatesBN (Hashtable conditionedNodeList,
			ArrayList<ArrayList<String>> targetList) throws Exception {
		Map<String, String> retMap = new HashMap<String, String>();
		for (String nodeName : allBNnodes) {
			String pfState = "";
		
			if (conditionedNodeList.containsKey(nodeName) && (isHardEvidence((Hashtable) conditionedNodeList.get(nodeName)) != null)) {
				//Hashtable nodeTable = (Hashtable) conditionedNodeList.get(nodeName);
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

	private Map<String, String> GetInferredStatesFromAFullyInstantiatedBN() throws Exception {
		Map<String, String> retMap = new HashMap<String, String>();
		for (String nodeName : allBNnodes) {
			Hashtable nodeTable = RetriveProbfromBuffer(nodeName, "posterior");
			String maxState = Find_State_of_Interest(nodeTable);
			retMap.put(nodeName, maxState);
		}
		return retMap;
	}

	private Map<String, String> GetPreferredStatesFromAFullyInstantiatedBN(Hashtable conditionedNodeList,
			ArrayList<ArrayList<String>> targetList) throws Exception {
		Map<String, String> retMap = new HashMap<String, String>();
		for (String nodeName : allBNnodes) {
			String pfState = "";
			if (conditionedNodeList.containsKey(nodeName) && isHardEvidence((Hashtable) conditionedNodeList.get(nodeName)) != null) {
				Hashtable nodeTable = (Hashtable) conditionedNodeList.get(nodeName);
				pfState = Find_State_of_Interest(nodeTable);
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

	private String MH2(List<Segment> orderedSegmentListForNLG) {
		// TODO Auto-generated method stub
		return null;
	}

	private List<Segment> ReConstructSegmentForNLG(Hashtable conditionedNodeList, List<RawSegment> orderedSegmentList,
			Map<String, String> semanticStates, Map<String, String> explainableStates, Hashtable fake_conditionedNodeList,  String impactChoice, Graph _BNGraph, boolean fake_it_Philip)
			throws Exception {
		List<Segment> retList = new ArrayList<>();

		for (RawSegment rawSeg : orderedSegmentList) {

			// add all the things that need to be added in the NLG segment
			// do the impact analysis of a segment to find SB and DB with respective impact
			// values
			List<String> segOtherNodeList = FindNodesInAsegment(rawSeg);
			
			Map<String, Map<Set<String>, Double>> SB_DB_infos = new HashMap<String, Map<Set<String>,Double>>();
			if(!fake_it_Philip)
			SB_DB_infos = impactAnalysisOfAsegment(rawSeg.target, segOtherNodeList,
					semanticStates, explainableStates, conditionedNodeList, impactChoice, _BNGraph);
			else
			{
//				SB_DB_infos = impactAnalysisOfAsegment(rawSeg.target, segOtherNodeList,
//						semanticStates, explainableStates, fake_conditionedNodeList, impactChoice, _BNGraph);
				
				Set<String> SB = new HashSet<String>();
				Set<String> DB = new HashSet<String>();

				SB.add(segOtherNodeList.get(0));
				for(int it = 1; it < segOtherNodeList.size(); it++)
					DB.add(segOtherNodeList.get(it));
				
				Map<Set<String>, Double> tempMap = new HashMap<>();
				tempMap.put(SB, 999.0);
				SB_DB_infos.put("sb", tempMap);
				
				if(!DB.isEmpty()) {
					tempMap = new HashMap<>();
					tempMap.put(DB, -999.0);
					SB_DB_infos.put("db", tempMap);
				}
				else {
					tempMap = new HashMap<>();
					SB_DB_infos.put("db", tempMap);
				}
					
				
			}
			
			//List<Set<String>> SB_DB_sets = new ArrayList<Set<String>>(SB_DB_infos.keySet()); // make it ordered - as set
																								// isn't ordered
			
			Set<String> SB = new HashSet<>();
			Set<String> DB = new HashSet<>();
			
			List<Set<String>> SB_set_info = new ArrayList<Set<String>>(SB_DB_infos.get("sb").keySet());
			if(!SB_set_info.isEmpty()) {
				SB = SB_set_info.get(0);
			}
			List<Set<String>> DB_set_info = new ArrayList<Set<String>>(SB_DB_infos.get("db").keySet());
			if(!DB_set_info.isEmpty()) {
				DB = DB_set_info.get(0);
			}
			 Double SB_impactVal = (SB.size() > 0) ? SB_DB_infos.get("sb").values().toArray(new Double[0])[0]  : 0.0;
			 Double DB_impactVal = (DB.size() > 0) ? SB_DB_infos.get("db").values().toArray(new Double[0])[0] : 0.0;
			 
			 //Segment nlgSeg = new Segment(rawSeg,SB,DB,SB_impactVal,DB_impactVal);
			 Segment nlgSeg = new Segment(rawSeg,SB,SB_impactVal,DB,DB_impactVal);
			 
			 // add node information for all the nodes in segment
			// for target
			NodeInfo targetNodeInfo = ConstructTargetNodeInfo(rawSeg.target, semanticStates, explainableStates, conditionedNodeList, fake_it_Philip);
			nlgSeg.put(rawSeg.target, targetNodeInfo);

			// for other nodes
			for (String oSegNode : segOtherNodeList) {
				NodeInfo oNinfo = ConstructOtherNodeInfo(oSegNode, semanticStates, explainableStates, conditionedNodeList, fake_conditionedNodeList,fake_it_Philip);
				nlgSeg.put(oSegNode, oNinfo);
			}
			retList.add(nlgSeg);
		}
		return retList;
	}

	private NodeInfo ConstructOtherNodeInfo(String oSegNode, Map<String,String> semanticStates, Map<String,String> explainableStates, Hashtable conditionedNodeList, Hashtable fake_conditionedNodeList, boolean fake_it_Philip) throws Exception {
		// target evidence nodes was specially handled in the "preferredStates" or "inferredStates"
		// Name:
		// ID:
		String node_id = _net.getNode(oSegNode).agenaNode.getConnNodeId();
//		// Preamble: prior State
//		String State = explainableStates.get(oSegNode);
//		// Specifics: general state
//		String specifincs_general_State = semanticStates.get(oSegNode);
//		// Specifics: posterior state
//		String specifics_posterior_State = preamble_prior_State;
		
		if(oSegNode.equals("Glymer(High)")) {
			int stopHere = 0;
		}
		
		String state_Name = explainableStates.get(oSegNode);
		// prior prob
		Double prior_prob = 0.0;
		Double posterior_prob = 0.0;
		if(!oSegNode.startsWith("ubgs92jh")) {
			prior_prob = (Double) RetriveProbfromBuffer(oSegNode, "prior").get(state_Name);
			posterior_prob = (Double) RetriveProbfromBuffer(oSegNode, "posterior").get(state_Name);
		}
		else {
			prior_prob = 0.5;
			posterior_prob = 0.5;
		}
		// posterior prob
		

		boolean isEvidence = (conditionedNodeList.containsKey(oSegNode)) ? true:false;
		boolean isTarget = false;
		
		//NodeInfo nodeInfo = new NodeInfo(node_id, oSegNode, preamble_prior_State, specifincs_general_State, specifics_posterior_State, prior_prob, posterior_prob);
		NodeInfo nodeInfo = new NodeInfo(oSegNode, oSegNode, state_Name, prior_prob, posterior_prob, isEvidence, isTarget); // nodeID = nodeName Now
		return nodeInfo;

	}

	private NodeInfo ConstructTargetNodeInfo(String target , Map<String,String> semanticStates, Map<String,String> explainableStates, Hashtable conditionedNodeList, boolean fake_it_Philip) throws Exception {
		// target node was specially handled in the "preferredStates" or "inferredStates"
		// Name:
		// ID:
		String node_id = _net.getNode(target).agenaNode.getConnNodeId();
		// Preamble: prior State
//		String preamble_prior_State = explainableStates.get(target);
//		// Specifics: general state
//		String specifincs_general_State = semanticStates.get(target);
//		// Specifics: posterior state
//		String specifics_posterior_State = preamble_prior_State;
		
		String state_name = explainableStates.get(target);;
		// prior prob
		Double prior_prob = 0.0;
		Double posterior_prob = 0.0;
		if(!target.startsWith("ubgs92jh")) {
			prior_prob = (Double) RetriveProbfromBuffer(target, "prior").get(state_name);
			// posterior prob
			posterior_prob = (Double) RetriveProbfromBuffer(target, "posterior").get(state_name);
		}else {
			prior_prob= 0.5;
			posterior_prob = 0.5;
		}
		boolean isEvidence = (conditionedNodeList.containsKey(target)) ? true:false;
		boolean isTarget = (ultimateTargetNode.equals(target)) ? true: false;
		
		//NodeInfo nodeInfo = new NodeInfo(node_id, target, preamble_prior_State, specifincs_general_State, specifics_posterior_State, prior_prob, posterior_prob);
		NodeInfo nodeInfo = new NodeInfo(target, target, state_name, prior_prob, posterior_prob, isEvidence,isTarget); // nodeID = nodeName now
		return nodeInfo;
	}

	private Map<String, Map<Set<String>, Double>> impactAnalysisOfAsegment(String targetNode, List<String> segOtherNodeList,
			Map<String, String> semanticStates, Map<String, String> explainableStates, Hashtable conditionedNodeList, String impactChoice, Graph _BNGraph)
			throws Exception {
		Map<String, Map<Set<String>, Double>> retMap = new HashMap<>(); // (key-1: SB; value-1: SB_impact), (key-2: DB; value-2:
															// DB_impact)
		String queryNodeName = targetNode;
		String queryNodeStateName = explainableStates.get(targetNode);

		if(segOtherNodeList.size() == 0)
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
			
			set_findings_of_ConditionedNodes_3(P_conditionedNodeList,
					MakeNodeList((ArrayList<String>) segOtherNodeList)); // resetting the CPTs of
			
			double priorValue = getTargetNodeBelief(queryNodeName, queryNodeStateName);
			priorValue = ((double) Math.round((priorValue) * 1000.0) / 1000.0);
			
			// instantiate impact vector (magnitude + direction)
			ArrayList<Map.Entry<Node[], Double>> magnitudeofSortedImpactValues = new ArrayList<>();
			ArrayList<Map.Entry<Node[], String>> directionofSortedimpactValues = new ArrayList<>();

			if (impactChoice.equals("perc_change")) {
				Hashtable<Node[], Double> PercentageofTargetBeliefChangeofSubsets = FindPercentageofTargetBeliefChangeForAllSubsets(
						allSubsetsOfEvidenceNodes, U_conditionedNodeList, P_conditionedNodeList, queryNodeName,
						queryNodeStateName, priorValue, (ArrayList<String>) segOtherNodeList);

				// get the hashtable sorted as an ArrayList
				magnitudeofSortedImpactValues = sortHashTableValue(PercentageofTargetBeliefChangeofSubsets, "desc");
				directionofSortedimpactValues = findDirectionofVector(magnitudeofSortedImpactValues);
			}

			int topIndex = 0;
			
			Node[] SubsetofTopImpact = (Node[]) magnitudeofSortedImpactValues.get(topIndex).getKey();
			double impactValueofTopSubset = (double) magnitudeofSortedImpactValues.get(topIndex).getValue();
			impactValueofTopSubset = ((double) Math.round((impactValueofTopSubset) * 1000.0) / 1000.0);
			
			String impactDirectionofTopSubset = (String) directionofSortedimpactValues.get(topIndex).getValue();
			

			// get the value of the interest for the whole input evidence list
			Map.Entry<Double, String> impactVectorofTheWholeSet = FindtheVectorforTheWholeSet(
					magnitudeofSortedImpactValues, segOtherNodeList);
			double valueforTheWholeSet = impactVectorofTheWholeSet.getKey();
			//double posteriorValue = valueforTheWholeSet + priorValue;
			double posteriorValue = RetriveProbfromBuffer(queryNodeName, "posterior", conditionedNodeList);
			posteriorValue = ((double) Math.round((posteriorValue) * 1000.0) / 1000.0);
			valueforTheWholeSet = ((double) Math.round((valueforTheWholeSet) * 1000.0) / 1000.0);
			
			Set<String> SB = new HashSet<String>();
			Set<String> DB = new HashSet<String>();

			if (SubsetofTopImpact.length < segOtherNodeList.size()) { 		// conflicting contribution
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
					
				} else if (impactDirectionofTopSubset.equals("decrease")){
					SB = getNodeNames(remainingSubset); 
					Map<Set<String>, Double> tempMap = new HashMap<>();
					tempMap.put(SB, valueforTheWholeSet - impactValueofTopSubset);
					retMap.put("sb", tempMap);
					
					DB = getNodeNames(SubsetofTopImpact); 
					tempMap = new HashMap<>();
					tempMap.put(DB, impactValueofTopSubset);
					retMap.put("db", tempMap);
					
				}else {		// (impactDirection = no_change) ; insert empty set with 0.0 impact value
					Map<Set<String>, Double> tempMap = new HashMap<>(); 
					tempMap.put(SB, 0.0); retMap.put("sb", tempMap);
					
					tempMap = new HashMap<>();
					tempMap.put(DB, 0.0);  retMap.put("db", tempMap);
				}
			}else {														// non-conflicting contribution -- all impact comes from either SB or DB
				if(valueforTheWholeSet > 0) {
					SB = getNodeNames(segOtherNodeList);
					Map<Set<String>, Double> tempMap = new HashMap<>();
					tempMap.put(SB, valueforTheWholeSet);
					retMap.put("sb", tempMap);
					
					tempMap = new HashMap<>();
					tempMap.put(DB, 0.0);	// insert empty set with 0.0 impact value
					retMap.put("db", tempMap);
				}else if(valueforTheWholeSet < 0) {
					Map<Set<String>, Double> tempMap = new HashMap<>();
					tempMap.put(SB, 0.0);	// insert empty set with 0.0 impact value
					retMap.put("sb", tempMap);
					
					
					DB = getNodeNames(segOtherNodeList); 
					tempMap = new HashMap<>();
					tempMap.put(DB, valueforTheWholeSet);
					retMap.put("db", tempMap);
				}else {
					Map<Set<String>, Double> tempMap = new HashMap<>();
					tempMap.put(SB, 0.0); retMap.put("sb", tempMap); 
					
					tempMap = new HashMap<>();
					tempMap.put(DB, 0.0); // insert empty set with 0.0 impact value
					retMap.put("db", tempMap);
				}
				
			}

			// add things into the map
		}
		return retMap;
	}

	private Set<String> getNodeNames(List<String> nodeList) {
		Set<String> retSet = new HashSet<String>();
		for(String n: nodeList) {
			retSet.add(n);
		}
		return retSet;
	}

	private Set<String> getNodeNames(Node[] nodeList) {
		Set<String> retSet = new HashSet<String>();
		for(Node n: nodeList) {
			retSet.add(n.getShortName());
		}
		return retSet;
	}

	private Map<String, Map< Set<String>, Double>> conductImpactAnalysisForSingleEdge(String evidenceNodeName,
			String evidenceStateofInterest, String queryNodeName, String queryNodeStateName) throws Exception {
		Map<String, Map< Set<String>, Double>> retMap = new HashMap<String, Map< Set<String>, Double>>();

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

		if (impactVal > 0) {	// positive impact
			// == put SB
			SB.add(evidenceNodeName);
			Map<Set<String>, Double> tempMap = new HashMap<>();
			tempMap.put(SB, impactVal);
			retMap.put("sb", tempMap);
			
			// == put DB
			//DB.add("");
			tempMap = new HashMap<>();
			tempMap.put(DB, 0.0);
			retMap.put("db", tempMap);
			
		} else if (impactVal < 0) {	// negative impact
			// == put SB 
			//SB.add("");
			Map<Set<String>, Double> tempMap = new HashMap<>();
			tempMap.put(SB, 0.0);
			retMap.put("sb", tempMap);
			
			// == put DB
			DB.add(evidenceNodeName);
			tempMap = new HashMap<>();
			tempMap.put(DB, impactVal);
			retMap.put("db", tempMap);
		} else {					// NO impact (impactVal == 0)
			
			// == put SB
			//SB.add("");
			Map<Set<String>, Double> tempMap = new HashMap<>();
			tempMap.put(SB, 0.0);
			retMap.put("sb", tempMap);
			
			// == put DB
			//DB.add("");
			tempMap = new HashMap<>();
			tempMap.put(DB, 0.0);
			retMap.put("db", tempMap);
		}
		return retMap;
	}

	private List<String> FindNodesInAsegment(RawSegment rawSeg) {
		List<String> retList = new ArrayList<>(); // this list will contain distinct nodes in a segment
		// retList.add(rawSeg.commonEffect.)

		Set<String> all_CE_nodes = rawSeg.commonEffect.keySet();
		for (String ceNode : all_CE_nodes) {
			if (!retList.contains(ceNode)) { // first insert ce nodes
				retList.add(ceNode);
			}

			Set<String> all_CE_depenedants = rawSeg.commonEffect.get(ceNode);
			for (String ceDepandant : all_CE_depenedants) {
				if (!retList.contains(ceDepandant)) { // then insert ce depandant nodes (aka alternate causes)
					retList.add(ceDepandant);
				}
			}
		}

		for (String causalNode : rawSeg.causal) {
			if (!retList.contains(causalNode)) { // then add causal nodes
				retList.add(causalNode);
			}
		}
		for (String anti_causalNode : rawSeg.antiCausal) {
			if (!retList.contains(anti_causalNode)) { // then finally add anti-causal nodes
				retList.add(anti_causalNode);
			}
		}

		return retList;
	}

	private void MakeBNforMH(Net _net2, Hashtable conditionedNodeList, String queryNode, Analyser a) throws Exception {

		// add target
		a.addTarget(queryNode);
		//System.out.println(queryNode);
		//Bard.package$.MODULE$.on();
		
		// add evidence nodes
		ArrayList<String> evidenceList = Collections.list(conditionedNodeList.keys());
		for (String ev : evidenceList) {
			a.addEvidence(ev);
			//System.out.println(ev);
		}

		
		// get all the nodes from the BN and add them in gbuilder
		Node[] allnodes = _net.getNodes();
		for (Node n1 : allnodes) {
			// String sourceNodeID = n1.agenaNode.getConnNodeId();
			String sourceNodeID = n1.getShortName();
			a.addSimple(sourceNodeID); // all the node IDs are inserted
		}
		
		for (Node n1 : allnodes) {
			String sourceNodeID = n1.getShortName();
			Node[] children = n1.getChildren();
			for (Node n2 : children) {
				// String targetNodeID = n2.agenaNode.getConnNodeId();
				String targetNodeID = n2.getShortName();
				a.addEdge(sourceNodeID, targetNodeID); // add the edges
			}
		}
	}

	private ArrayList<String> tempFilterOutPaths(ArrayList<String> chainList) {
		ArrayList<String> retList = new ArrayList<>();
		for (String chain : chainList) {
			String[] nodes = chain.split(" <- | -> ");
			if (!CommonEffectNodes_and_theirPath.containsKey(nodes[0])) {
				retList.add(chain);
			}
		}
		return retList;
	}

	private void re_InitializeGlobalVariable() {
		// global variables
		CommonEffectNodes_and_theirPath = new Hashtable<String, String>();
		// private static Map<String, ArrayList<String>> CE_blobs = new Map<>();
		// CE_blobs_structures = ArrayListMultimap.create();

		CE_blobs = new ArrayList<>();
		BFS_blobs = new ArrayList<>();

		incEvidenceList = new ArrayList<>();
		decEvidenceList = new ArrayList<>();

		blockedEvidenceList = new ArrayList<>();
		blockedEvidenceNodeInfoList = new ArrayList<>();
		weakEvidenceList = new ArrayList<>();

		//semanticStates = new HashMap<>();
		//explainableStates = new HashMap<>();
		
		//originalPriorofAllNodes = new Hashtable<>();
		//FinalBeliefofAllNodes = new Hashtable<>();

		ultimateTargetNode = "";
		ultimateTargetNodeState = "";

		outputResponse_for_A_Target = "";

		NLGtext = new TextGenerator_Az();
		//NLGtext  = null;

	}

	private static void saveOriginalBeleifsofAllNodes(String bufferName) throws Exception {
		// Node[] allNodes = new NodeList(_net);
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

	private static boolean RelativeRisk(Node[] sourceNodeList, String queryNodeName, String queryNodeStateName,
			Hashtable conditionedNodeList) throws Exception {
		// returns true, if RR > 1; false otherwise.

		// OPTION - 1
		/*
		 * NodeList _allEvidenceNodeExceptCurrent = new NodeList(_net);
		 * _allEvidenceNodeExceptCurrent =
		 * get_allEvidenceNodeExceptCurrent(_net,conditionedNodeList,sourceNodeList);
		 */
		/*
		 * //set findings (observation) all all the evidences except the current node:
		 * '_node' set_findings_of_ConditionedNodes_3(_net, conditionedNodeList,
		 * _allEvidenceNodeExceptCurrent);
		 * 
		 * //get the posterior distribution of target Node double denom =
		 * getTargetNodeBelief(_net, queryNodeName, queryNodeStateName);
		 * 
		 * // now set the current evidence node '_node', which will complete the
		 * evidence list set_findings_of_ConditionedNodes_3(_net, conditionedNodeList,
		 * sourceNodeList);
		 * 
		 * //get the posterior distribution of target Node double nom =
		 * getTargetNodeBelief(_net, queryNodeName, queryNodeStateName);
		 */

		// OPTION - 2
		double denom = getTargetNodeBelief(queryNodeName, queryNodeStateName);
		// now set the current evidence node '_node', which will complete the evidence
		// list
		set_findings_of_ConditionedNodes_3(conditionedNodeList, sourceNodeList);
		// get the posterior distribution of target Node
		double nom = getTargetNodeBelief(queryNodeName, queryNodeStateName);

		if ((nom / denom) > 1)
			return true;
		else
			return false;

	}

	private static ArrayList<String> tempFunction(ArrayList<String> chainList) {
		// chainList.set(1,"");
		// chainList.set(2,"");
		// chainList.set(6,"");
		// chainList.set(7,"");
		// chainList.set(9,"");
		// chainList.set(17,"");
		// chainList.set(18,"");
		// chainList.set(20,"");
		// chainList.set(15,"");
		// chainList.set(16,"");

		chainList.set(9, "");
		chainList.set(10, "");
		chainList.set(12, "");
		chainList.set(13, "");
		chainList.set(14, "");
		chainList.set(16, "");
		chainList.set(18, "");
		chainList.set(19, "");
		chainList.set(25, "");
		chainList.set(26, "");

		chainList.removeAll(Collections.singleton(""));
		return chainList;
	}

	private static ArrayList<String> ContentOrderingFor_NLG_report(ReasoningGraph _reasoningGraph,
			ArrayList<String> chainList, Hashtable conditionedNodeList, Hashtable backgroundNodeList, Graph _BNgraph,
			String queryNode, Graph _BNGraph) throws Exception {
		ArrayList<String> retList = new ArrayList<String>();
		// Re-model the reasoning graph to address special patterns (e.g. common-effect,
		// diamond shape)
		// WILL DO IN FUTURE

		// set flags
		boolean[] flag = SetflagsforNodes(_reasoningGraph, conditionedNodeList, queryNode);

		// Mark PreKNOTs
		ArrayList<String> preKNOTs = MarkPreKNOTS(_reasoningGraph, chainList, queryNode);

		// Populate OrderQueue
		ArrayList<String[]> OrderQ = PopulateOrderQueue(_reasoningGraph, preKNOTs, queryNode);

		// Read the OrderQueue

		ArrayList<String> explanation = ReadOrderQueue(OrderQ, _reasoningGraph, flag, conditionedNodeList,
				backgroundNodeList, _BNGraph);
		// System.out.println(explanation);
		return retList;
	}

	private static ArrayList<String> ReadOrderQueue(ArrayList<String[]> orderQ, ReasoningGraph _reasoningGraph,
			boolean[] flag, Hashtable conditionedNodeList, Hashtable backgroundNodeList, Graph _BNGraph)
			throws Exception {
		ArrayList<String> retList = new ArrayList<String>();

		int index = orderQ.size() - 1;
		while (index > 0) {
			ArrayList<String[]> L = PopQ(orderQ, index);
			index -= L.size();

			String chunk = "";
			for (int i = 0; i < L.size(); i++) {
				int cNodeIndex = _reasoningGraph.graphNodes.indexOf(L.get(i)[0]);
				if (!flag[cNodeIndex]) {

					// get children of the cNodeIndex
					LinkedList<String> children = _reasoningGraph.getAllAdajacencyInfo()[cNodeIndex];

					if (children.size() >= 2) {
						ArrayList<String[]> tempL = new ArrayList<>();
						for (String _child : children) {
							if (!flag[_reasoningGraph.graphNodes.indexOf(_child)]) {
								String subChain = ReturnSubChain(_child, _reasoningGraph, flag);
								if (subChain.split(" - ").length > 1) { // here we have DFS chain possibility
									Process_A_SubChain_for_Text(subChain, conditionedNodeList, backgroundNodeList,
											_BNGraph, flag, _reasoningGraph);
									// chunk += ("(" + subChain + "). ");

								}
								tempL.add(new String[] { _child, L.get(i)[0] });
								flag[_reasoningGraph.graphNodes.indexOf(_child)] = true;
							}
						}
						if (tempL.size() > 0)
							chunk += ReturnChunkTail(tempL);
					} else {
						String subChain = ReturnSubChain(L.get(i)[0], _reasoningGraph, flag);
						if (subChain.split(" - ").length > 1) { // here too we have DFS chain possibility
							outputResponse_for_A_Target += System.getProperty("line.separator").toString();
							Process_A_SubChain_for_Text(subChain, conditionedNodeList, backgroundNodeList, _BNGraph,
									flag, _reasoningGraph);
							// chunk += ("(" + subChain + "). ");
						}
					}
					flag[cNodeIndex] = true; // flag the Node that, it's subchain has been process (if any)
				}

			}

			if (!spokenBefore(L, flag, _reasoningGraph)) {
				// here we used have a BFS sort of order, could reveal a CE_BLOB or a simple BFS
				// blob
				if (L.size() > 1)
					Check_and_Process_CE_or_BFS_blob(L, _BNGraph, flag, _reasoningGraph, conditionedNodeList);
				else if (L.size() == 1) {
					if (!was_In_Any_CEblob_that_was_talked_before(L, flag, _reasoningGraph)) {
						Check_and_Process_CE_or_BFS_blob(L, _BNGraph, flag, _reasoningGraph, conditionedNodeList);
					}
				}
			}

			if (!chunk.equals(""))
				retList.add(chunk);
		}
		return retList;
	}

	private static boolean was_In_Any_CEblob_that_was_talked_before(ArrayList<String[]> L, boolean[] flag,
			ReasoningGraph _reasoningGraph) {
		for (Blob b : CE_blobs) {
			String _parentNode = L.get(0)[1];
			String _child = L.get(0)[0];

			ArrayList<String> targetNodeList = b.queryNodeList;
			if (targetNodeList.contains(_parentNode) && flag[_reasoningGraph.graphNodes.indexOf(_child)])
				return true;
		}
		return false;
	}

	private static void Process_A_SubChain_for_Text(String subChain, Hashtable conditionedNodeList,
			Hashtable backgroundNodeList, Graph _BNGraph, boolean[] flag, ReasoningGraph _reasoningGraph)
			throws Exception {

		try {
			String nodes[] = subChain.split(" - ");
			for (int i = 0; i < (nodes.length - 1); i++) {
				Blob b = Check_a_single_Edge_if_CEblob(nodes[i], nodes[i + 1]);
				if (b == null) { // this ([i] - [i+1]) is a simple edge, not a part of a CE_blob
					// if(true) {
					// String evidenceStateofInterest =
					// _net.getNode(nodes[i]).getState(stateIndexofInterest).getShortName();
					String evidenceStateofInterest = "";
					if (conditionedNodeList.containsKey(nodes[i]))
						evidenceStateofInterest = Find_State_of_Interest(
								(Hashtable) conditionedNodeList.get((String) _net.getNode(nodes[i]).getShortName()));
					else
						evidenceStateofInterest = Find_State_of_Interest_for_intermediate_Node(nodes[i]);

					String queryNodeName = nodes[i + 1];

					// String queryNodeState =
					// (queryNodeName.equals(ultimateTargetNode))?ultimateTargetNodeState:_net.getNode(queryNodeName).getState(stateIndexofInterest).getShortName();
					String queryNodeState = Find_target_State_Name(queryNodeName, conditionedNodeList);

					Explain_a_Single_Edge(nodes[i], evidenceStateofInterest, queryNodeName, queryNodeState, _BNGraph);

				} else {
					// handle the CE_blob 'b' that involve this particular single pair
					// node[i]-node[i+1]
					// Say_with_CE(b, conditionedNodeList);
					Produce_Text_for_Explain_Away(b, _BNGraph, flag, _reasoningGraph, conditionedNodeList);

				}
			}
		} catch (Exception e) {
			e.printStackTrace();
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

	private static void Explain_a_Single_Edge(String evidenceNodeName, String evidenceStateofInterest,
			String queryNodeName, String queryNodeStateName, Graph _BNGraph) throws Exception {
		ArrayList<String> allChilds = new ArrayList<>();
		allChilds.add(evidenceNodeName);
		Hashtable U_conditionedNodeList = Construct_Updated_ConditionedList_for_a_NodeList(allChilds);
		Hashtable P_conditionedNodeList = Construct_Prior_ConditionedList_for_a_NodeList(allChilds);

		double priorValue = (double) RetriveProbfromBuffer(queryNodeName, "prior").get(queryNodeStateName); // this
		// is
		// another
		// choice

		// set_findings_of_ConditionedNodes_3(P_conditionedNodeList,
		// MakeNodeList(allChilds)); // resetting the CPTs of all the children of
		// current BFS blob
		// double priorValue = (double)
		// getTargetNodeCPT(queryNodeName).get(queryNodeStateName);

		// priorValue = ((double) Math.round((priorValue) * 10.0) / 10.0);

		// set_findings_of_ConditionedNodes_3(U_conditionedNodeList,
		// MakeNodeList(allChilds));
		// double posteriorValue = (double)
		// getTargetNodeCPT(queryNodeName).get(queryNodeStateName);
		double posteriorValue = (double) RetriveProbfromBuffer(queryNodeName, "posteriori").get(queryNodeStateName);

		String directionOfChange = FindDirectionofChange(priorValue, posteriorValue);
		String causality = FindCausalityofanEdge(evidenceNodeName, queryNodeName, _BNGraph);

		ArrayList<String> tempList = new ArrayList<>();
		double EvideceProb_so_Far = (double) ((Hashtable) U_conditionedNodeList.get(evidenceNodeName))
				.get(evidenceStateofInterest);

		tempList.add(evidenceNodeName);
		tempList.add(evidenceStateofInterest);
		tempList.add(Double.toString(EvideceProb_so_Far)); // this "1.0" is not actually correct prob for intermediate
		// node, but we set it for the sake of only mentioning the
		// (node=state) format
		ArrayList<ArrayList<String>> _nodeinfoList = new ArrayList<>();
		_nodeinfoList.add(tempList);
		ArrayList<String> _targetNodeInfo = new ArrayList<>();
		_targetNodeInfo.add(queryNodeName);
		_targetNodeInfo.add(queryNodeStateName);
		_targetNodeInfo.add(Double.toString(posteriorValue));

		if (causality.equals("Causal")) {
			outputResponse_for_A_Target += NLGtext.SayCauseRel(_nodeinfoList, _targetNodeInfo, directionOfChange,
					Double.toString(posteriorValue)) + System.getProperty("line.separator").toString();
		} else {
			outputResponse_for_A_Target += NLGtext.SayEvidenceRel(_nodeinfoList, _targetNodeInfo, directionOfChange,
					Double.toString(posteriorValue)) + System.getProperty("line.separator").toString();
		}

		outputResponse_for_A_Target += System.getProperty("line.separator").toString();
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

	private static String FindCausalityofanEdge(String node1, String node2, Graph _BNGraph) {
		int index = _BNGraph.getAllGraphNodes().indexOf(node1);
		LinkedList<String> adj[] = _BNGraph.getAllAdajacencyInfo();
		if (adj[index].contains(node2))
			return "Causal";
		else
			return "Anti_Causal";
	}

	private static String FindDirectionofChange(double priorValue, double posteriorValue) {
		String direction = "";

		priorValue = ((double) Math.round((priorValue) * 1000.0) / 1000.0);
		posteriorValue = ((double) Math.round((posteriorValue) * 1000.0) / 1000.0);

		double value = (posteriorValue - priorValue);
		if (value > 0)
			direction = "increase";
		else if (value < 0)
			direction = "decrease";
		else
			direction = "no_change";
		return direction;
	}

	private static boolean isNodeInOriginalEvidenceList(String nodeName, Hashtable conditionedNodeList)
			throws Exception {
		Node[] nodeList = MakeNodeList(conditionedNodeList);

		for (int i = 0; i < nodeList.length; i++) {
			Node tempNode = _net.getNode(nodeName);
			if (Arrays.asList(nodeList).contains(tempNode)) {
				return true;
			}
		}

		return false;
	}

	private static Blob Check_a_single_Edge_if_CEblob(String node1, String node2) throws Exception {

		for (Blob b : CE_blobs) {
			Node[] evidenceList = MakeNodeList(b.evidenceSet);
			Node tempNode = _net.getNode(node1);
			if (Arrays.asList(evidenceList).contains(tempNode) && b.CE_nodeName.equals(node2)) { // this is possible
				// since a CE blob
				// demand node[i] ->
				// node[i+1] edge
				return b;
			}
		}

		return null;
	}

	private static boolean spokenBefore(ArrayList<String[]> L, boolean[] flag, ReasoningGraph _reasoningGraph) {
		for (int i = 0; i < L.size(); i++) {
			if (flag[_reasoningGraph.graphNodes.indexOf(L.get(i)[0])]
					&& flag[_reasoningGraph.graphNodes.indexOf(L.get(i)[1])])
				return true;
		}
		return false;
	}

	private static String ReturnChunkTail(ArrayList<String[]> L) {
		String chunkTail = "";
		for (int i = 0; i < L.size(); i++) {
			chunkTail += (L.get(i)[0] + ", ");
		}
		chunkTail = chunkTail.substring(0, chunkTail.length() - 2);
		if (chunkTail.split(", ").length >= 2)
			chunkTail += " together cause increase/decrease the Pr(" + L.get(0)[1] + ")\n";
		else
			chunkTail += " cause increase/decrease the Pr(" + L.get(0)[1] + ")\n";

		return chunkTail;
	}

	private static void Check_and_Process_CE_or_BFS_blob(ArrayList<String[]> L, Graph _BNGraph, boolean[] flag,
			ReasoningGraph _reasoningGraph, Hashtable conditionedNodeList) throws Exception {

		// check if the center node is a CE_node or not
		String potential_CE_node = L.get(0)[1];
		Blob b = Check_for_CEblob(potential_CE_node);
		if (b != null) {
			// that means b contains a CE_blob

			// ###################################### detect and talk about the descendant
			// edge (blob) first
			ArrayList<ArrayList<String>> _nodeinfoList = new ArrayList<>();
			ArrayList<String> outList_1 = Collections.list(b.evidenceSet.keys());
			ArrayList<String> outList_2 = b.queryNodeList;

			Hashtable<String, String> tempEvidenceList = new Hashtable<>();
			ArrayList<String> tempTargetList = new ArrayList<>();
			LinkedList<String> adj[] = _BNGraph.getAllAdajacencyInfo();

			for (int i = 0; i < L.size(); i++) {
				String tempNode = L.get(i)[0];
				if (!outList_1.contains(tempNode) && !outList_2.contains(tempNode)) { // that means the evidence list in
					// the CE_blob doesn't contain
					// the descendant
					int sourceIndex = _BNGraph.getAllGraphNodes().indexOf(potential_CE_node);
					if (adj[sourceIndex].contains(tempNode)) {
						tempEvidenceList.put(tempNode, "anti_causal");
					} else
						tempEvidenceList.put(tempNode, "causal");
				}
			}
			if (tempEvidenceList.size() == 1) {
				// say a causal link, from evidence to target
				String evidenceNodeName = Collections.list(tempEvidenceList.keys()).get(0);
				// String evidenceStateofInterest =
				// _net.getNode(evidenceNodeName).getState(stateIndexofInterest).getShortName();
				String evidenceStateofInterest = "";
				if (conditionedNodeList.containsKey(evidenceNodeName))
					evidenceStateofInterest = Find_State_of_Interest((Hashtable) conditionedNodeList
							.get((String) _net.getNode(evidenceNodeName).getShortName()));
				else
					evidenceStateofInterest = Find_State_of_Interest_for_intermediate_Node(evidenceNodeName);

				String queryNodeName = potential_CE_node;
				// String queryNodeStateName =
				// (queryNodeName.equals(ultimateTargetNode))?ultimateTargetNodeState:_net.getNode(queryNodeName).getState(stateIndexofInterest).getShortName();
				String queryNodeStateName = Find_target_State_Name(queryNodeName, conditionedNodeList);

				Explain_a_Single_Edge(evidenceNodeName, evidenceStateofInterest, queryNodeName, queryNodeStateName,
						_BNGraph);
			} else if (tempEvidenceList.size() > 1) {
				tempTargetList.add(potential_CE_node);
				Blob temp_blob = Get_a_BFS_blob(potential_CE_node, tempEvidenceList, tempTargetList,
						conditionedNodeList, null);
				Explain_a_BFS_Blob(temp_blob, "perc_change", _BNGraph, _reasoningGraph, flag, conditionedNodeList);
				outputResponse_for_A_Target += (System.getProperty("line.separator").toString()
						+ System.getProperty("line.separator").toString());
			}
			// ArrayList<String> _targetNodeInfo = new ArrayList<>();
			// _targetNodeInfo.add(potential_CE_node);
			// _targetNodeInfo.add(_net.getNode(potential_CE_node).getState(stateIndexofInterest).getShortName());
			// _targetNodeInfo.add("");
			//
			// talk about the remaining CE_blob
			Produce_Text_for_Explain_Away(b, _BNGraph, flag, _reasoningGraph, conditionedNodeList);

		} else {
			b = Check_for_BFS_blob(potential_CE_node);
			if (b != null) {
				Explain_a_BFS_Blob(b, "perc_change", _BNGraph, _reasoningGraph, flag, conditionedNodeList);
				outputResponse_for_A_Target += (System.getProperty("line.separator").toString()
						+ System.getProperty("line.separator").toString());
			} else {
				// then this must be a single edge, so speak about that edge with corresponding
				// causality
				String evidenceNodeName = L.get(0)[0];
				// String evidenceStateName =
				// _net.getNode(evidenceNodeName).getState(stateIndexofInterest).getShortName();
				String evidenceStateofInterest = "";
				if (conditionedNodeList.containsKey(evidenceNodeName))
					evidenceStateofInterest = Find_State_of_Interest((Hashtable) conditionedNodeList
							.get((String) _net.getNode(evidenceNodeName).getShortName()));
				else
					evidenceStateofInterest = Find_State_of_Interest_for_intermediate_Node(evidenceNodeName);

				String targetNodeName = L.get(0)[1];
				// String targetStateName =
				// _net.getNode(targetNodeName).getState(stateIndexofInterest).getShortName();
				String targetStateName = Find_target_State_Name(targetNodeName, conditionedNodeList);
				Explain_a_Single_Edge(evidenceNodeName, evidenceStateofInterest, targetNodeName, targetStateName,
						_BNGraph);
			}
		}
	}

	private static void Produce_Text_for_Explain_Away(Blob b, Graph _BNGraph, boolean[] flag,
			ReasoningGraph _reasoningGraph, Hashtable conditionedNodeList) throws Exception {

		Hashtable<String, String> evidenceChangeHistory = GetEvidenceChangeHistory(b, conditionedNodeList);
		ArrayList<String> evidenceList = Collections.list(b.evidenceSet.keys());
		ArrayList<ArrayList<String>> combinedParentListInfo = Combine_List_and_Create_nodeListInfo(evidenceList,
				b.queryNodeList, b.queryNodeStateList, conditionedNodeList);

		String CE_StateofInterest = "";
		if (conditionedNodeList.containsKey(b.CE_nodeName))
			CE_StateofInterest = Find_State_of_Interest(
					(Hashtable) conditionedNodeList.get((String) _net.getNode(b.CE_nodeName).getShortName()));
		else
			CE_StateofInterest = Find_State_of_Interest_for_intermediate_Node(b.CE_nodeName);

		outputResponse_for_A_Target += (NLGtext.SayNodeList(combinedParentListInfo, "NP", "and")
				+ NLGtext.SayAlternativeCauses() + NLGtext.SayNode(b.CE_nodeName, CE_StateofInterest, "NP") + ",");

		for (int i = 0; i < b.queryNodeList.size(); i++) {
			String T_i = b.queryNodeList.get(i);
			String T_i_state = b.queryNodeStateList.get(i);

			ArrayList<ArrayList<String>> T_i_increase = new ArrayList<>();
			ArrayList<ArrayList<String>> T_i_decrease = new ArrayList<>();

			// double posteriori = (double) getTargetNodeCPT(T_i).get(T_i_state);
			double posteriori = (double) RetriveProbfromBuffer(T_i, "posteriori").get(T_i_state);
			String probToMention = (is_Target_also_a_BFS_blob_target(T_i)
					&& !flag[_reasoningGraph.graphNodes.indexOf(T_i)]) ? "" : (" " + Double.toString(posteriori) + " ");

			for (int j = 0; j < evidenceList.size(); j++) {
				String P_j = evidenceList.get(j);
				ArrayList<String> tempEv = new ArrayList<>();
				tempEv.add(P_j);
				Hashtable P_P_j = Construct_Prior_ConditionedList_for_a_NodeList(tempEv);
				Hashtable U_P_j = Construct_Updated_ConditionedList_for_a_NodeList(tempEv);

				set_findings_of_ConditionedNodes_3(P_P_j, MakeNodeList(tempEv));
				double priorValue = (double) getTargetNodeCPT(T_i).get(T_i_state);

				set_findings_of_ConditionedNodes_3(U_P_j, MakeNodeList(tempEv));
				// double posteriori = (double) getTargetNodeCPT(T_i).get(T_i_state);

				String StateofInterest = "";
				if (conditionedNodeList.containsKey(P_j))
					StateofInterest = Find_State_of_Interest(
							(Hashtable) conditionedNodeList.get((String) _net.getNode(P_j).getShortName()));
				else
					StateofInterest = Find_State_of_Interest_for_intermediate_Node(P_j);

				String direction = FindDirectionofChange(priorValue, posteriori);
				if (direction.equals("increase")) {
					ArrayList<String> _evidenceInfoList = new ArrayList<>();

					_evidenceInfoList.add(P_j);
					_evidenceInfoList.add(StateofInterest);
					_evidenceInfoList.add(evidenceChangeHistory.get(P_j));

					T_i_increase.add(_evidenceInfoList);
				} else if (direction.equals("decrease")) {
					ArrayList<String> _evidenceInfoList = new ArrayList<>();
					_evidenceInfoList.add(P_j);
					_evidenceInfoList.add(StateofInterest);
					_evidenceInfoList.add(evidenceChangeHistory.get(P_j));
					T_i_decrease.add(_evidenceInfoList);
				}
			}
			// return sub chain and talk about that: eg. VA->TB
			if (T_i_decrease.size() > 0) {
				int nElem = T_i_decrease.size();
				String tempText = "";

				for (int iter = 0; iter < (nElem - 1); iter++) {
					ArrayList<String> nodeInfo = T_i_decrease.get(iter);
					tempText += (nodeInfo.get(2) + NLGtext.SayNode(nodeInfo.get(0), nodeInfo.get(1), "NP") + ", ");
				}
				if (nElem > 1)
					tempText += "and ";
				tempText += (T_i_decrease.get(nElem - 1).get(2)
						+ NLGtext.SayNode(T_i_decrease.get(nElem - 1).get(0), T_i_decrease.get(nElem - 1).get(1), "NP")
						+ " decrease the probability of " + NLGtext.SayNode(T_i, T_i_state, "NP")
						+ ((T_i_increase.size() == 0) ? probToMention : "") + ". ");
				outputResponse_for_A_Target += tempText;
			}
			if (T_i_increase.size() > 0) {
				outputResponse_for_A_Target += "But, ";

				int nElem = T_i_increase.size();
				String tempText = "";

				for (int iter = 0; iter < (nElem - 1); iter++) {
					ArrayList<String> nodeInfo = T_i_increase.get(iter);
					tempText += (nodeInfo.get(2) + NLGtext.SayNode(nodeInfo.get(0), nodeInfo.get(1), "NP") + ", ");
				}
				if (nElem > 1)
					tempText += "and ";
				tempText += (T_i_increase.get(nElem - 1).get(2)
						+ NLGtext.SayNode(T_i_increase.get(nElem - 1).get(0), T_i_increase.get(nElem - 1).get(1), "NP")
						+ " increase the probability of " + NLGtext.SayNode(T_i, T_i_state, "NP") + probToMention
						+ ". ");
				outputResponse_for_A_Target += tempText;
			}
		}
		outputResponse_for_A_Target += System.getProperty("line.separator").toString();
	}

	private static boolean is_Target_also_a_BFS_blob_target(String t_i) {
		for (Blob b : BFS_blobs) {
			if (b.queryNodeList.contains(t_i))
				return true;
		}
		return false;
	}

	private static ArrayList<ArrayList<String>> Combine_List_and_Create_nodeListInfo(ArrayList<String> evidenceList,
			ArrayList<String> queryNodeList, ArrayList<String> queryNodeStateList, Hashtable conditionedNodeList)
			throws Exception {
		ArrayList<ArrayList<String>> retList = new ArrayList<>();

		for (int i = 0; i < evidenceList.size(); i++) {
			ArrayList<String> tempList = new ArrayList<>();
			String evidenNodeName = evidenceList.get(i);
			tempList.add(evidenNodeName);

			String evidence_StateofInterest = "";
			if (conditionedNodeList.containsKey(evidenNodeName))
				evidence_StateofInterest = Find_State_of_Interest(
						(Hashtable) conditionedNodeList.get((String) _net.getNode(evidenNodeName).getShortName()));
			else
				evidence_StateofInterest = Find_State_of_Interest_for_intermediate_Node(evidenNodeName);

			// tempList.add(_net.getNode(evidenNodeName).getState(stateIndexofInterest).getShortName());
			tempList.add(evidence_StateofInterest);
			retList.add(tempList);
		}

		for (int i = 0; i < queryNodeList.size(); i++) {
			ArrayList<String> tempList = new ArrayList<>();
			String TargetNodeName = queryNodeList.get(i);
			String TargetNodeStateName = queryNodeStateList.get(i);
			tempList.add(TargetNodeName);
			tempList.add(TargetNodeStateName);
			retList.add(tempList);
		}
		return retList;
	}

	private static Hashtable<String, String> GetEvidenceChangeHistory(Blob b, Hashtable conditionedNodeList)
			throws Exception {
		Hashtable<String, String> retList = new Hashtable<>();
//		ArrayList<String> evideneList = Collections.list(b.evidenceSet.keys());
//
//		for (int i = 0; i < evideneList.size(); i++) {
//			String evidenceNodeName = evideneList.get(i);
//
//			ArrayList<String> tempList = new ArrayList<>();
//			tempList.add(evidenceNodeName);
//
//			ArrayList<ArrayList<String>> tempEvidenceInfoList = Find_Evidence_NodeInfoList(MakeNodeList(tempList),
//					backupConditionedList);
//			if (tempEvidenceInfoList.size() == 0) {
//
//				// String evidenceStateofInterest =
//				// _net.getNode(evidenceNodeName).getState(stateIndexofInterest).getShortName();
//				String evidenceStateofInterest = "";
//				if (conditionedNodeList.containsKey(evidenceNodeName))
//					evidenceStateofInterest = Find_State_of_Interest((Hashtable) conditionedNodeList
//							.get((String) _net.getNode(evidenceNodeName).getShortName()));
//				else
//					evidenceStateofInterest = Find_State_of_Interest_for_intermediate_Node(evidenceNodeName);
//
//				double priorValue = RetriveProbfromBuffer(evidenceNodeName, "prior", conditionedNodeList);
//				double posteriorValue = RetriveProbfromBuffer(evidenceNodeName, "posterior", conditionedNodeList);
//				String directionofChange = FindDirectionofChange(priorValue, posteriorValue);
//				if (directionofChange.equals("increase")) {
//					retList.put(evidenceNodeName, " an increase in the probability of ");
//				} else if (directionofChange.equals("decrease")) {
//					retList.put(evidenceNodeName, " a decrease in the probability of ");
//				} else
//					retList.put(evidenceNodeName, " an increase in the probability of ");
//			} else {
//				retList.put(evidenceNodeName, "a " + tempEvidenceInfoList.get(0).get(2) + " probability of ");
//			}
//		}
		return retList;
	}

	private static Blob Check_for_BFS_blob(String potential_CE_node) throws Exception {
		for (Blob b : BFS_blobs) {
			String targetNodeName = b.queryNodeList.get(0); // in BFS blob, there is only 1 target node in the target
			// nodelist
			if (targetNodeName.equals(potential_CE_node))
				return b;
		}
		return null;
	}

	private static Blob Check_for_CEblob(String potential_CE_node) throws Exception {
		for (Blob b : CE_blobs) {
			if (b.CE_nodeName.equals(potential_CE_node))
				return b;
		}
		return null;
	}

	private static String ReturnSubChain(String nodeOfInterest, ReasoningGraph _reasoningGraph, boolean[] flag) {
		String retString = "";

		Stack<String> retList = new Stack<String>();
		BT_new(nodeOfInterest, retList, _reasoningGraph, flag);

		while (!retList.isEmpty()) {
			retString += (retList.pop() + " - ");
		}

		retString += nodeOfInterest;
		return retString;
	}

	private static void BT_new(String currentNode, Stack<String> retList, ReasoningGraph _reasoningGraph,
			boolean[] flag) {
		// LinkedList<String> children =
		// _BNgraph.adj[_BNgraph.graphNodes.indexOf(currentNode)];
		LinkedList<String> children = _reasoningGraph.getAllAdajacencyInfo()[_reasoningGraph.getAllGraphNodes()
				.indexOf(currentNode)];
		if (children.size() == 0) {
			return;
		} else {
			for (String child : children) {
				if (!flag[_reasoningGraph.graphNodes.indexOf(child)]) {
					retList.push(child);
					BT_new(child, retList, _reasoningGraph, flag);
				}
			}
			return;
		}
	}

	private static ArrayList<String[]> PopQ(ArrayList<String[]> orderQ, int index) {
		ArrayList<String[]> retList = new ArrayList<>();

		String parentNode = orderQ.get(index)[1];
		for (int i = index; i >= 0; i--) {
			if (orderQ.get(i)[1].equals(parentNode)) {
				retList.add(new String[] { orderQ.get(i)[0], orderQ.get(i)[1] });
			} else
				break;
		}
		return retList;
	}

	/*
	 * This function Reads the Reasoning Graph Top-to-Bottom (i.e.
	 * Query-to-Evidence) to create the order queue
	 */
	private static ArrayList<String[]> PopulateOrderQueue(ReasoningGraph _reasoningGraph, ArrayList<String> preKNOTs,
			String queryNode) {
		ArrayList<String[]> Q = new ArrayList<String[]>();

		ArrayList<String> CNlist = new ArrayList<String>();
		String CN = queryNode;
		CNlist.add(CN);
		ArrayList<String> _cnChilds = _reasoningGraph.getChilds(CN);
		Q.add(new String[] { CN, "" }); // here the first element in the string array is the c
		int index = 0;

		while (index < Q.size()) {
			if ((index > 0) && (CNlist.contains(CN))) {// this checking is for the nodes that are already expanded.
				// Note, (index > 0) preventing Root node (queryNode) to enter
				// here because it is already included in CNlist
				index++;
				if (index < Q.size()) {
					CN = Q.get(index)[0];
				}
				continue;
			} else { // it is for other nodes
				_cnChilds = _reasoningGraph.getChilds(CN);
				for (String c : _cnChilds) {
					if (preKNOTs.contains(c))
						break;
					else
						Q.add(new String[] { c, CN });
				}

				if (index != 0)
					CNlist.add(CN);
				index++;
				if (index < Q.size()) {
					CN = Q.get(index)[0];
				}
			}
		}
		return Q;
	}

	private static ArrayList<String> MarkPreKNOTS(ReasoningGraph _reasoningGraph, ArrayList<String> chainList,
			String queryNode) {
		ArrayList<String> preKnotList = new ArrayList<String>();
		if (chainList.size() == 1)
			return preKnotList; // Return empty list: Rationale - if there is only a single chain then there is
		// no preKnots
		else {
			for (String chain : chainList) {
				String[] nodes = chain.split(" <- | -> ");

				if (nodes.length == 2) { // if the chain is just an edge then there is no preKnots, skip that chain
					continue;
				}

				for (int i = 0; i < nodes.length - 2; i++) {
					String nextNode = nodes[i + 1];
					if (_reasoningGraph.getChilds(nextNode).size() >= 2)
						break;

					String nextofnextNode = nodes[i + 2];
					if (_reasoningGraph.getChilds(nextofnextNode).size() >= 2) {
						if (!preKnotList.contains(nodes[i]) && !isInADiamondShape(nodes[i], chainList, chain)) {
							preKnotList.add(nodes[i]);
						}
						break;
					} else {
						if (!preKnotList.contains(nodes[i]) && !isInADiamondShape(nodes[i], chainList, chain)) {
							preKnotList.add(nodes[i]);
						}
					}
				}
			}
			return preKnotList;
		}
	}

	private static boolean isInADiamondShape(String _nodeName, ArrayList<String> chainList, String exceptThisChain) {
		boolean found = false;
		for (String chain : chainList) {
			if (!chain.equals(exceptThisChain)) {
				String[] nodes = chain.split(" <- | -> ");
				if (nodes[0].equals(_nodeName))
					return true;
			}
		}
		return false;
	}

	private static boolean[] SetflagsforNodes(ReasoningGraph _reasoningGraph, Hashtable conditionedNodeList,
			String queryNode) {
		int nReasoningNodes = _reasoningGraph.graphNodes.size();
		boolean[] retList = new boolean[nReasoningNodes];
		// for all the nodes set the flag = false
		for (int i = 0; i < nReasoningNodes; i++) {
			retList[i] = false;
		}
		//// set flag true for the query node
		// retList[_reasoningGraph.graphNodes.indexOf(queryNode)] = true;
		return retList;
	}

	private static ArrayList<String> PareseChainList(
			Hashtable<Node[], Hashtable<String, ArrayList<String>>> d_connectedPaths) {
		ArrayList<String> retList = new ArrayList();
		Enumeration iter = d_connectedPaths.keys();
		while (iter.hasMoreElements()) {
			Hashtable<String, ArrayList<String>> pathSets = d_connectedPaths.get(iter.nextElement());
			Enumeration<String> evidences = pathSets.keys();
			while (evidences.hasMoreElements()) {
				String evidenceName = evidences.nextElement();
				ArrayList<String> paths = pathSets.get(evidenceName);
				for (String path : paths) {
					if (!retList.contains(path)) {
						retList.add(path);
					}
				}
			}

		}
		return retList;
	}

	private static HashMap<Hashtable<Node[], Double>, Hashtable<Node[], Hashtable<String, ArrayList<String>>>> ContentSelectionFor_NLG_report(
			Hashtable conditionedNodeList, String queryNode, String queryNodeState, Graph _BNgraph, String impactChoice)
			throws Exception {

		// CONFLICT ANALYSIS:
		// This function finds which evidence nodes increases or decreases the
		// probability of target node (populates the "incEvidenceList" and
		// "decEvidenceList")
		// ConflictAnalysis(conditionedNodeList,queryNode,queryNodeState);

		// This is the brute-force (multi-way) approach (i.e. check all subsets of
		// evidence (power sets))
		// generate subsets of parents to create a
		// NodeList list_of_all_subsets = new NodeList(_net);
		Node[][] allSubsetsOfEvidenceNodes = generateRelevantSubsets(MakeNodeList(conditionedNodeList), queryNode,
				conditionedNodeList, _BNgraph);

		// Now, for each subsets of evidences, find impact upon the queryNode as a
		// result of setting findings of a particular subset of evidences
		Hashtable<Node[], Double> ImpactValuesofSubsets = new Hashtable<Node[], Double>();
		// Hashtable<Node[], Double> RelativeRiskofSubsets = new Hashtable<Node[],
		// Double>();

		for (int iter = 0; iter < allSubsetsOfEvidenceNodes.length; iter++) {
			Node[] currentSubset = (Node[]) allSubsetsOfEvidenceNodes[iter];

			// ##################################### IMPACT analysis
			// ##############################################

			// find impact of that subset of EVIDENCES on the TARGET node: either Normalized
			// Mutual Information, or K-L divergence
			if (impactChoice.equals("KL")) {
				// KL approach
				Node[] _allEvidenceNodeExceptCurrent = get_allEvidenceNodeExceptCurrent(conditionedNodeList,
						currentSubset);
				double KL_divergence_value = findImpact_using_KL_divergece(currentSubset, _allEvidenceNodeExceptCurrent,
						queryNode, conditionedNodeList);

				// apply significance test here.

				ImpactValuesofSubsets.put(currentSubset, KL_divergence_value);
				_net.clearAllEvidence();
				_net.compile();
			}

			// PercentageofTargetBeliefChange analysis
			else if (impactChoice.equals("perc_change")) {
				double PercentageofTargetBeliefChange = FindPercentageofTargetBeliefChange(conditionedNodeList,
						currentSubset, queryNode, queryNodeState);

				// apply significance test here.

				ImpactValuesofSubsets.put(currentSubset, PercentageofTargetBeliefChange);
				_net.clearAllEvidence();
				_net.compile();
			}
		}

		// and also find the d-connected paths for those subsets as well
		Hashtable<Node[], Hashtable<String, ArrayList<String>>> d_connectedPaths_from_EvidenceSubsetsToTarget = new Hashtable<Node[], Hashtable<String, ArrayList<String>>>();

		// for each of the (significant) subsets: find the intermediate nodes (for the
		// explanation), and its utility value (considering both impact and explanation
		// length)
		// Hashtable<Node[], Node[]> IntermediateNodesofaSubsets = new Hashtable<Node[],
		// Node[]>();

		Enumeration<Node[]> _subsets = ImpactValuesofSubsets.keys();
		while (_subsets.hasMoreElements()) {
			Node[] currentSubset = _subsets.nextElement();

			// find d_connected paths from the current subset of evidences to TARGET
			Hashtable<String, ArrayList<String>> temp = new Hashtable<String, ArrayList<String>>(
					Find_D_connected_Paths_from_EvidenceSubsets_to_Target(currentSubset, _net.getNode(queryNode),
							_BNgraph, conditionedNodeList));
			d_connectedPaths_from_EvidenceSubsetsToTarget.put(currentSubset, temp);

			// find distinct intermediate nodes in those paths for the current subset
			// NodeList intermediateNodesofCurrentSubset = new NodeList(_net);
			// Node[] intermediateNodesofCurrentSubset =
			// FindIntermediateNodesofCurrentSubset(temp);
			// IntermediateNodesofaSubsets.put(currentSubset,
			// intermediateNodesofCurrentSubset);
		}

		// create and return contents now
		// Entry<Hashtable<NodeList, Double>, Hashtable<NodeList,
		// Hashtable<String,ArrayList<String>>>> retContents =
		HashMap<Hashtable<Node[], Double>, Hashtable<Node[], Hashtable<String, ArrayList<String>>>> retContents = new HashMap<>();
		retContents.put(ImpactValuesofSubsets, d_connectedPaths_from_EvidenceSubsetsToTarget);
		return retContents;
	}

	private static String GeneratePreamble(
			ArrayList<Map.Entry<Node[], Double>> PercentageofTargetBeliefChangeofSubsetstList,
			Hashtable conditionedNodeList, String queryNode, String queryNodeState, Map<String,String> explainableStates) throws Exception {
		String retString = "";

		/* get the prior (so far) of the queryNode */
		//double priorBelief = (double) _net.getNode(queryNode).getBelief(queryNodeState);
		double priorBelief = (double) RetriveProbfromBuffer(queryNode, "prior").get(queryNodeState);
		priorBelief = ((double) Math.round((priorBelief) * 1000.0) / 1000.0);
		String priorText = NLGtext.SayPrior(queryNode, queryNodeState, priorBelief);

		double posteriori = RetriveProbfromBuffer(queryNode, "posteriori", conditionedNodeList);
		posteriori = ((double) Math.round((posteriori) * 1000.0) / 1000.0);
		
		ArrayList<String> _targetNodeInfo = new ArrayList<>();
		_targetNodeInfo.add(queryNode);
		_targetNodeInfo.add(queryNodeState);
		_targetNodeInfo.add(Double.toString(posteriori));

		if(conditionedNodeList.size() > 0) {
			/* get the top subset with value of its interest (e.g. percentage of changes) */
			Map.Entry<Node[], Double> tempEntry = PercentageofTargetBeliefChangeofSubsetstList.get(0);
			Node[] subsetWithTopchanges = (Node[]) tempEntry.getKey();
			double topValue = (double) tempEntry.getValue();

			boolean plural = (subsetWithTopchanges.length > 1) ? true:false;

			if (PercentageofTargetBeliefChangeofSubsetstList.size() == 1) {
				topValue = ((double) Math.round((topValue) * 1000.0) / 1000.0);

				ArrayList<ArrayList<String>> emptyNodeList_for_NO_effect_through_CPT = new ArrayList<>();
				
				if (topValue > 0) {
					ArrayList<ArrayList<String>> _nodeInfoList = Find_Evidence_NodeInfoList(subsetWithTopchanges,
							conditionedNodeList, explainableStates);
					
					//String direction = (plural) ? "increase":"increases";
					String direction = "increases";

					return priorText + "Observing "
					+ NLGtext.SayImply(_nodeInfoList, _targetNodeInfo, direction, "")
					+ NLGtext.SayImply_no_change(emptyNodeList_for_NO_effect_through_CPT, _targetNodeInfo, "")
					+ NLGtext.SayConclusion(_targetNodeInfo, Double.toString(posteriori), Double.toString(priorBelief))
					+ System.getProperty("line.separator").toString();			// put blocked nodelist

				} else if (topValue < 0) {
					ArrayList<ArrayList<String>> _nodeInfoList = Find_Evidence_NodeInfoList(subsetWithTopchanges,
							conditionedNodeList, explainableStates);
					
					//String direction = (plural) ? "decrease":"decreases";
					String direction = "decreases";

					return priorText + "Observing "
					+ NLGtext.SayImply(_nodeInfoList, _targetNodeInfo, direction, "")
					+ NLGtext.SayImply_no_change(emptyNodeList_for_NO_effect_through_CPT, _targetNodeInfo, "")
					+ NLGtext.SayConclusion(_targetNodeInfo, Double.toString(posteriori), Double.toString(priorBelief))
					+ System.getProperty("line.separator").toString(); // put blocked nodelist
				} else
					return priorText + "But the evidence does not change that probability."
					+ System.getProperty("line.separator").toString();

			}

			priorBelief = ((double) Math.round((priorBelief) * 1000.0) / 1000);
			topValue = ((double) Math.round((topValue) * 1000.0) / 1000.0);

			// get the value of the interest for the whole input evidence list
			double valueforTheWholeSet = FindtheVectorforTheWholeSet(PercentageofTargetBeliefChangeofSubsetstList,
					conditionedNodeList).getKey();

			if (subsetWithTopchanges.length < conditionedNodeList.size()) {
				// get the remaining subset
				Node[] remainingSubset = FindRemainingSubsets(subsetWithTopchanges, conditionedNodeList);
				retString = GenerateEnglishwithConflict(priorBelief, subsetWithTopchanges, topValue, queryNode,
						queryNodeState, remainingSubset, conditionedNodeList, valueforTheWholeSet, explainableStates);
			} else {
				// this is a temporary solution. OVERA-LOADED FUNCTION CALL
				retString = GenerateEnglishwithoutConflict(valueforTheWholeSet, priorBelief, queryNode, queryNodeState,
						conditionedNodeList, explainableStates);

				// ==================== THIS WHOLE PART SHOULD BE RE-CONSTRUCTED BASED ON SOME
				// SUITABLE IDEA TO FIND SUBSETS WITH "LARGER-SMALLER CONTRIBUTION"
				// // this means all the nodes are working in the same direction of changes
				// // Now, there are two possibilities:
				// // 1) either all of them have same impact (say, together .....), or
				// // 2) Some may have large change (to the query prob.) but some may have small
				// (say, [A,B,C] increase/decreases the prob..., and [X,Y] further
				// increase/decreases the prob...)
				//
				// // here, the second best value would be the subset of nodes that cause large
				// changes (to the query prob.), and that would have size less than the whole
				// set
				// tempEntry = PercentageofTargetBeliefChangeofSubsetstList.get(1);
				// Node[] subsetwithLargerValue = (Node[])tempEntry.getKey();
				// double LargerValue = (double) tempEntry.getValue();
				//
				// Node[] remainingSubset =
				// FindRemainingSubsets(subsetwithLargerValue,conditionedNodeList);
				// double valueofRemainingSubset =
				// getValueWithASubset(remainingSubset,PercentageofTargetBeliefChangeofSubsetstList);
				//
				// retString =
				// GenerateEnglishwithoutConflict(subsetwithLargerValue,LargerValue,remainingSubset,valueofRemainingSubset,valueforTheWholeSet,priorBelief,queryNode,queryNodeState,conditionedNodeList);

			}
		}else {
			// in this case, we have all evidence as blocked
			// say prior
			// say blockednodelist, SayImply_no_change
			
			ArrayList<ArrayList<String>> emptyNodeList_for_NO_effect_through_CPT = new ArrayList<>();
			return priorText 
					+ NLGtext.SayImply_no_change(emptyNodeList_for_NO_effect_through_CPT, _targetNodeInfo, Double.toString(posteriori)) 
					+ NLGtext.SayConclusion(_targetNodeInfo, Double.toString(posteriori), Double.toString(priorBelief));
			
		}
		return retString;
	}

	private static ArrayList<ArrayList<String>> Find_Evidence_NodeInfoList(Node[] _subset,
			Hashtable conditionedNodeList, Map<String,String> explainableStates) throws Exception {
		ArrayList<ArrayList<String>> retList = new ArrayList<>();

		Enumeration<String> tempEnum = conditionedNodeList.keys();
		while (tempEnum.hasMoreElements()) {
			String nodeName = tempEnum.nextElement();

			Node tempNode = _net.getNode(nodeName);
			if (Arrays.asList(_subset).contains(tempNode)) {

				Hashtable tempTable = (Hashtable) conditionedNodeList.get(nodeName);
				ArrayList<String> tempList = new ArrayList<>();
				// String stateName =
				// (String)Collections.list(tempTable.keys()).get(stateIndexofInterest);
				//String stateName = Find_State_of_Interest(tempTable);
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

	private static String GenerateEnglishwithoutConflict(double valueforTheWholeSet, double priorBelief,
			String queryNode, String queryNodeState, Hashtable conditionedNodeList, Map<String,String> explainableStates) throws Exception {
		String retStr = "";

		// double posteriori = valueforTheWholeSet + priorBelief; // since, the (change
		// (i.e. topValue) = posteriori -
		// priori)

		double posteriori = RetriveProbfromBuffer(queryNode, "posterior", conditionedNodeList);

		posteriori = ((double) Math.round((posteriori) * 1000.0) / 1000.0);
		priorBelief = ((double) Math.round((priorBelief) * 1000.0) / 1000.0);

		String PriorText = NLGtext.SayPrior(queryNode, queryNodeState, priorBelief);

		ArrayList<ArrayList<String>> _nodeinfoList = Find_Evidence_NodeInfoList(MakeNodeList(conditionedNodeList),
				conditionedNodeList, explainableStates); // consider all the evidence (unblocked, obviously) set
		ArrayList<String> _targetNodeInfo = new ArrayList<>();
		_targetNodeInfo.add(queryNode);
		_targetNodeInfo.add(queryNodeState);
		_targetNodeInfo.add(Double.toString(valueforTheWholeSet));

		ArrayList<ArrayList<String>> emptyNodeList_for_NO_effect_through_CPT = new ArrayList<>();
		
		boolean plural = (_nodeinfoList.size() > 1) ? true:false;
		
		if (valueforTheWholeSet > 0) {
			//String direction = (plural) ? "increase":"increases";
			String direction = "increases";
			return PriorText + "Observing "
					+ NLGtext.SayImply(_nodeinfoList, _targetNodeInfo, direction, "")
					+ NLGtext.SayImply_no_change(emptyNodeList_for_NO_effect_through_CPT, _targetNodeInfo, "")
					+ NLGtext.SayConclusion(_targetNodeInfo, Double.toString(posteriori), Double.toString(priorBelief))
					+ System.getProperty("line.separator").toString();
		} else {
			//String direction = (plural) ? "decrease":"decreases";
			String direction = "decreases";
			return PriorText + "Observing "
					+ NLGtext.SayImply(_nodeinfoList, _targetNodeInfo, direction, "")
					+ NLGtext.SayImply_no_change(emptyNodeList_for_NO_effect_through_CPT, _targetNodeInfo, "")
					+ NLGtext.SayConclusion(_targetNodeInfo, Double.toString(posteriori), Double.toString(priorBelief))
					+ System.getProperty("line.separator").toString();
		}

	}

	private static String GenerateEnglishwithoutConflict(Node[] subsetwithLargerValue, double largerValue,
			Node[] remainingSubset, double valueofRemainingSubset, double valueforTheWholeSet, double priorBelief,
			String queryNode, String queryNodeState, Hashtable conditionedNodeList) throws Exception {
		String retString = "";
		String PriorText = "SayProbability(SayPrefix(priorFlag=true, conclusionFlag=false), SayNode(" + queryNode + ","
				+ queryNodeState + "), SayProbabilityValue(" + priorBelief + "))."
				+ System.getProperty("line.separator").toString();

		double ratio = (double) Math.round((largerValue / valueofRemainingSubset) * 100.0) / 100.0;
		if (ratio < 2.0) {
			// this indicates the contribution of two subsets (larger and remaining) are not
			// so different: ITS A HEURISTIC
			if (valueforTheWholeSet > 0)
				// retString = PriorText + MergeList(subsetwithLargerValue,remainingSubset) + "
				// together increase the Pr(" + queryNode + "=" + queryNodeState + ") to " +
				// valueforTheWholeSet + System.getProperty("line.separator").toString();
				retString = PriorText + "SayNodeList("
						+ FindEvidenceList(MergeList(subsetwithLargerValue, remainingSubset), conditionedNodeList)
						+ ") TOGETHER SayDirectionOfChange(increase) the probability of SayNode(" + queryNode + ","
						+ queryNodeState + ") SayMagnitudeOfChange(" + priorBelief + "," + valueforTheWholeSet + ")."
						+ System.getProperty("line.separator").toString();
			else if (valueforTheWholeSet < 0)
				retString = PriorText + "SayNodeList("
						+ FindEvidenceList(MergeList(subsetwithLargerValue, remainingSubset), conditionedNodeList)
						+ ") TOGETHER SayDirectionOfChange(decrease) the probability of SayNode(" + queryNode + ","
						+ queryNodeState + ") SayMagnitudeOfChange(" + priorBelief + "," + valueforTheWholeSet + ")."
						+ System.getProperty("line.separator").toString();
			else
				retString = PriorText + "SayNodeList("
						+ FindEvidenceList(MergeList(subsetwithLargerValue, remainingSubset), conditionedNodeList)
						+ ") TOGETHER make no changes in that probability "
						+ System.getProperty("line.separator").toString();
		} else {
			// this means, larger subset's contribution is too high (at least two-times, a
			// Heuristic) compared to the remaining subset
			String UpdateText = "";

			if (largerValue > 0) {
				// UpdateText = subsetwithLargerValue + " increase(s) the Pr(" + queryNode + "="
				// + queryNodeState + ") to " + largerValue + "," +
				// System.getProperty("line.separator").toString();
				UpdateText = "SayNodeList(" + FindEvidenceList(subsetwithLargerValue, conditionedNodeList)
						+ ") SayDirectionOfChange(increase) the probability of SayNode(" + queryNode + ","
						+ queryNodeState + ") SayMagnitudeOfChange(" + priorBelief + "," + largerValue + ")."
						+ System.getProperty("line.separator").toString();
				// UpdateText += remainingSubset + " further increase(s) that to " +
				// valueforTheWholeSet + "," + System.getProperty("line.separator").toString();
				UpdateText += "SayNodeList(" + FindEvidenceList(remainingSubset, conditionedNodeList)
						+ ") FURTHER SayDirectionOfChange(increase) the probability of SayNode(" + queryNode + ","
						+ queryNodeState + ") SayMagnitudeOfChange(" + largerValue + "," + valueforTheWholeSet + ")."
						+ System.getProperty("line.separator").toString();
			} else if (largerValue < 0) {
				// secondpart = subsetwithLargerValue + " decrease(s) the Pr(" + queryNode + "="
				// + queryNodeState + ") to " + largerValue + "," +
				// System.getProperty("line.separator").toString();
				UpdateText = "SayNodeList(" + FindEvidenceList(subsetwithLargerValue, conditionedNodeList)
						+ ") SayDirectionOfChange(decrease) the probability of SayNode(" + queryNode + ","
						+ queryNodeState + ") SayMagnitudeOfChange(" + priorBelief + "," + largerValue + ")."
						+ System.getProperty("line.separator").toString();
				//
				// thirdpart = remainingSubset + " further decrease(s) that to " +
				// valueforTheWholeSet + "," + System.getProperty("line.separator").toString();
				UpdateText += "SayNodeList(" + FindEvidenceList(remainingSubset, conditionedNodeList)
						+ ") FURTHER SayDirectionOfChange(decrease) the probability of SayNode(" + queryNode + ","
						+ queryNodeState + ") SayMagnitudeOfChange(" + largerValue + "," + valueforTheWholeSet + ")."
						+ System.getProperty("line.separator").toString();

			}

			retString = PriorText + UpdateText;
		}
		return retString;
	}

	private static Node[] MergeList(Node[] subsetwithLargerValue, Node[] remainingSubset) throws Exception {

		int totalSize = subsetwithLargerValue.length + remainingSubset.length;
		Node[] retList = new Node[totalSize];

		for (int i = 0; i < subsetwithLargerValue.length; i++) {
			Node n = remainingSubset[i];
			retList[i] = n;
		}

		for (int i = subsetwithLargerValue.length; i < totalSize; i++) {
			Node n = remainingSubset[i - subsetwithLargerValue.length];
			retList[i] = n;
		}
		return retList;

	}

	private static double getValueWithASubset(Node[] aSubset,
			ArrayList<Entry<Node[], Double>> List_of_Values_of_Subsets) {
		for (Map.Entry<?, Double> entry : List_of_Values_of_Subsets) {
			Node[] currentNodeList = (Node[]) entry.getKey();

			Set<Object> set1 = new HashSet<>(Arrays.asList(aSubset));
			Set<Object> set2 = new HashSet<>(Arrays.asList(currentNodeList));
			if (set1.equals(set2)) {
				return (double) entry.getValue();
			}
		}
		return (Double) null;
	}

	private static String getDirectionWithASubset(Node[] aSubset,
			ArrayList<Entry<Node[], String>> List_of_Values_of_Subsets) {
		for (Map.Entry<?, String> entry : List_of_Values_of_Subsets) {
			Node[] currentNodeList = (Node[]) entry.getKey();

			Set<Object> set1 = new HashSet<>(Arrays.asList(aSubset));
			Set<Object> set2 = new HashSet<>(Arrays.asList(currentNodeList));
			if (set1.equals(set2)) {
				return (String) entry.getValue();
			}
		}
		return (String) null;
	}

	private static Map.Entry<Double, String> FindtheVectorforTheWholeSet(
			ArrayList<Entry<Node[], Double>> percentageofTargetBeliefChangeofSubsetstList,
			Hashtable conditionedNodeList) {

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
	private static Map.Entry<Double, String> FindtheVectorforTheWholeSet(
			ArrayList<Entry<Node[], Double>> percentageofTargetBeliefChangeofSubsetstList,
			List<String> wholeNodeList) {

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
	private static String GenerateEnglishwithConflict(double priorBelief, Node[] subsetWithTopchanges, double topValue,
			String queryNode, String queryNodeState, Node[] remainingSubset, Hashtable conditionedNodeList,
			double valueforTheWholeSet, Map<String,String> explainableStates) throws Exception {
		String PriorText = "";
		String UpdateText = "";

		String Conclusionpart = "";

		// double posteriori = topValue + priorBelief; // since, the (change (i.e.
		// topValue) = posteriori - priori)

		priorBelief = ((double) Math.round((priorBelief) * 1000.0) / 1000.0);
		topValue = ((double) Math.round((topValue) * 1000.0) / 1000.0);
		valueforTheWholeSet = ((double) Math.round((valueforTheWholeSet) * 1000.0) / 1000.0);

		double impactChange = valueforTheWholeSet - topValue;
		
		// converting valueforTheWholeSet (it is actually a change value) into the
		// posteriori
		// double posteriori = valueforTheWholeSet + priorBelief; // since, the (change
		// (i.e. topValue) = posteriori -
		// priori)
		double posteriori = RetriveProbfromBuffer(queryNode, "posterior", conditionedNodeList);

		posteriori = ((double) Math.round((posteriori) * 1000.0) / 1000.0);
		// valueforTheWholeSet = valueforTheWholeSet * 100;

		PriorText = NLGtext.SayPrior(queryNode, queryNodeState, priorBelief);

		ArrayList<ArrayList<String>> _nodeinfoList_1 = Find_Evidence_NodeInfoList(subsetWithTopchanges,
				conditionedNodeList, explainableStates);
		ArrayList<ArrayList<String>> _nodeinfoList_2 = Find_Evidence_NodeInfoList(remainingSubset, conditionedNodeList, explainableStates);
		ArrayList<String> _targetNodeInfo = new ArrayList<>();
		_targetNodeInfo.add(queryNode);
		_targetNodeInfo.add(queryNodeState);
		_targetNodeInfo.add(Double.toString(posteriori));

		boolean plural = (_nodeinfoList_1.size() > 1) ? true:false;
		
		if (topValue > 0) {
			// this means top subset caused increase of the target node, and the
			// remainingSubset caused the decrease to it
			//String direction = (plural) ? "increase":"increases";
			String direction = "increases";
			return PriorText
					+ NLGtext.SayContradiction(_nodeinfoList_1, _nodeinfoList_2, _targetNodeInfo, direction, posteriori, impactChange, priorBelief)
					+ System.getProperty("line.separator").toString();

		} else if (topValue < 0) {
			//String direction = (plural) ? "decrease":"decreases";
			String direction = "decreases";
			return PriorText
					+ NLGtext.SayContradiction(_nodeinfoList_1, _nodeinfoList_2, _targetNodeInfo, direction, posteriori, impactChange, priorBelief)
					+ System.getProperty("line.separator").toString();

		}
		else
			return "";
	}

	private static String FindEvidenceList(Node[] subsetWithTopchanges, Hashtable conditionedNodeList)
			throws Exception {
		String retString = "";

		Enumeration<String> tempEnum = conditionedNodeList.keys();
		while (tempEnum.hasMoreElements()) {
			String nodeName = tempEnum.nextElement();

			Node tempNode = _net.getNode(nodeName);
			if (Arrays.asList(subsetWithTopchanges).contains(tempNode)) {
				retString += "SayNode(" + nodeName + ":=" + conditionedNodeList.get(nodeName) + "), ";
			}

		}
		retString = retString.substring(0, retString.length() - 2);

		return retString;
	}

	private static Node[] MakeNodeList(ArrayList<String> subsetofNodes) throws Exception {

		Node[] retList = new Node[subsetofNodes.size()];
		// for(String nodeName:subsetofNodes) {
		for (int i = 0; i < subsetofNodes.size(); i++) {
			String nodeName = subsetofNodes.get(i);
			Node netNode = _net.getNode(nodeName);
			retList[i] = netNode;
		}
		return retList;
	}

	private static Node[] FindRemainingSubsets(Node[] subsetWithTopchanges, Hashtable conditionedNodeList)
			throws Exception {

		// Node[] retList = new Node[conditionedNodeList.size() -
		// subsetWithTopchanges.length];
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

	private static Node[] FindRemainingSubsets(Node[] subsetWithTopchanges, List<String> wholeList)
			throws Exception {
		ArrayList<Node> retList = new ArrayList<>();
		int index = 0;
		for (String node:wholeList) {
			Node currentNodeName = (Node) _net.getNode(node);
			if (!Arrays.asList(subsetWithTopchanges).contains(currentNodeName))
				retList.add(currentNodeName);
			index++;
		}
		return retList.toArray(new Node[0]);
	}
	private static double FindPercentageofTargetBeliefChange(Hashtable conditionedNodeList, Node[] currentSubset,
			String queryNode, String queryNodeState) throws Exception {
		/// OPTION - 2
		double prior = getTargetNodeBelief(queryNode, queryNodeState);
		// now set the current evidence node '_node', which will complete the evidence
		// list
		set_findings_of_ConditionedNodes_3(conditionedNodeList, currentSubset);
		// get the posterior distribution of target Node
		double posterior = getTargetNodeBelief(queryNode, queryNodeState);
		
		// return ((posterior-prior)/prior) * 100;
		// return (posterior-prior)/prior;
		return (posterior - prior);
	}

	public static ArrayList<Map.Entry<Node[], Double>> sortHashTableValue(Hashtable<?, Double> t, String order) {

		// Transfer as List and sort it
		ArrayList<Map.Entry<Node[], Double>> l = new ArrayList(t.entrySet());

		if (order.equals("asc")) {
			Collections.sort(l, new Comparator<Map.Entry<Node[], Double>>() {
				public int compare(Map.Entry<Node[], Double> o1, Map.Entry<Node[], Double> o2) {
					Double value_1 = (Double) Math.abs(o1.getValue());
					Double value_2 = (Double) Math.abs(o2.getValue());
					//return value_1.compareTo(value_2);
					return my_own_comparator(value_1,value_2,o1.getKey(),o2.getKey(),o1.getValue(),o2.getValue());
					// return o1.getValue().compareTo(o2.getValue());
				}

				private int my_own_comparator(Double value_1, Double value_2, Node[] key, Node[] key2, Double value,
						Double value2) {
					// TODO Auto-generated method stub
					return 0;
				}
			});
		} else {
			Collections.sort(l, new Comparator<Map.Entry<Node[], Double>>() {
				public int compare(Map.Entry<Node[], Double> o1, Map.Entry<Node[], Double> o2) {
					Double value_1 = (Double) Math.abs(o1.getValue());
					Double value_2 = (Double) Math.abs(o2.getValue());
					//return value_2.compareTo(value_1);
					return my_own_comparator(value_2,value_1,o2.getKey(),o1.getKey(),o2.getValue(),o1.getValue());
					// return o2.getValue().compareTo(o1.getValue());
				}

				private int my_own_comparator(Double abs_value_2, Double abs_value_1, Node[] set_1, Node[] set_2, Double org_value_1,
						Double org_value_2) {
					if(abs_value_2 > abs_value_1)
						return 1;
					else if (abs_value_2 < abs_value_1)
						return -1;
					else
					{
						if(set_2.length > set_1.length)
							return 1;
						else if (set_2.length < set_1.length)
							return -1;
						else {
							if(org_value_2 < 0)	// means is a positive value
								return 1;
							else
								return -1;
						}
							
					}
				}
			});
		}

		// System.out.println(l);
		return l;
	}

	private static void ConflictAnalysis(Hashtable conditionedNodeList, String queryNode, String queryNodeState) {
		// try {
		// _net.clearAllEvidence();
		//
		// Enumeration elem = conditionedNodeList.keys();
		// while(elem.hasMoreElements()){
		// Node currentEvidenceNode = _net.getNode((String)elem.nextElement());
		// //NodeList tempList = new NodeList(_net);
		// tempList.add(currentEvidenceNode);
		// if(RelativeRisk(tempList, queryNode, queryNodeState, conditionedNodeList,
		// _net)) {
		// incEvidenceList.add(currentEvidenceNode.getName());
		// }
		// else {
		// decEvidenceList.add(currentEvidenceNode.getName());
		// }
		// _net.retractFindings();
		// }
		// }
		// catch (NeticaException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }
	}

	private static Node[] FindIntermediateNodesofCurrentSubset(
			Hashtable<String, ArrayList<String>> d_connectedPaths_from_aSubsetToTarget) throws Exception {

		ArrayList<Node> retList = new ArrayList<Node>();
		// NodeList retList = new NodeList(_net);
		Enumeration<String> evidences = d_connectedPaths_from_aSubsetToTarget.keys();
		while (evidences.hasMoreElements()) {
			ArrayList<String> paths = d_connectedPaths_from_aSubsetToTarget.get(evidences.nextElement());
			for (String path : paths) {
				// HashSet<String> nodes = new HashSet<String>(Arrays.asList(path.split(" -> |
				// <- ")));
				String[] tempNodes = path.split(" -> | <- ");
				for (String n : tempNodes) {
					Node tempNode = _net.getNode(n);
					if (!retList.contains(tempNode)) {
						retList.add(tempNode);
					}
				}
			}
		}
		return retList.toArray(new Node[0]);

	}

	private static Hashtable<String, ArrayList<String>> Find_D_connected_Paths_from_EvidenceSubsets_to_Target(
			Node[] currentSubset, Node targetNode, Graph _BNgraph, Hashtable conditionedNodeList) throws Exception {
		if (currentSubset.length > 0) {

			Hashtable<String, ArrayList<String>> retList = new Hashtable<String, ArrayList<String>>();
			for (int i = 0; i < currentSubset.length; i++) {
				ArrayList<String> _paths = new ArrayList<String>(
						_BNgraph.findPaths(currentSubset[i].getShortName(), targetNode.getShortName()));
				// Path analysis: i.e. finding out paths with significant contribution
				retList.put(currentSubset[i].getShortName(),
						AnalysePaths_for_Dconnectivity(_paths, _BNgraph, conditionedNodeList));
			}

			return retList; // this list has form like: (NODE,
			// a_list_d_connected_paths_from_A_evidence_to_Target

		}
		return null;
	}

	private static Hashtable Find_UnBlocked_Evidence_Nodes(String targetNodeName, Graph _BNgraph,
			Hashtable conditionedNodeList) throws Exception {
		if (conditionedNodeList.size() > 0) {

			Hashtable retNodeList = new Hashtable(conditionedNodeList);

			Enumeration<String> allEvidenceNodes = conditionedNodeList.keys();
			while (allEvidenceNodes.hasMoreElements()) {
				String cEvidenceNode = (String) allEvidenceNodes.nextElement();
				ArrayList<String> _paths = new ArrayList<String>(_BNgraph.findPaths(cEvidenceNode, targetNodeName));
				// Path analysis: i.e. finding out paths with significant contribution
				if (is_Blocked_Evidence(_paths, _BNgraph, conditionedNodeList)) {
					blockedEvidenceList.add(cEvidenceNode);
					retNodeList.remove(cEvidenceNode);
				}
			}

			return retNodeList; // this list has form like: (NODE,
			// a_list_d_connected_paths_from_A_evidence_to_Target

		}
		return null;
	}

	private static boolean is_Blocked_Evidence(ArrayList<String> _paths, Graph _BNgraph,
			Hashtable conditionedNodeList) throws Exception {

		ArrayList<String> retList = new ArrayList<String>();
		// optional list
		// Hashtable<String, String> CommonEffectNodes_and_theirPath = new
		// Hashtable<String, String>();

		for (int i = 0; i < _paths.size(); i++) {
			String aPath = _paths.get(i);
			String[] aPathNodes = aPath.split(" -> | <- "); // split based on the deliminators : -> or <-

			if (aPathNodes.length == 2)
				return false; // that means first node is the evidence and the second one is the target node,
			// so the evidence node is UNBLOCKED

			int pathIndex = 0;
			boolean isPath_d_connected = true;
			for (int j = 0; j < aPathNodes.length - 2; j++) {
				int tripletLength = aPathNodes[j].length() + 4 + aPathNodes[j + 1].length() + 4
						+ aPathNodes[j + 2].length(); // length of the triplet of such form: "A <arrow> B <arrow> C"
				String triplet = aPath.substring(pathIndex, pathIndex + tripletLength);
				pathIndex += aPathNodes[j].length() + 4;

				// check if subpath is the form of "A -> B -> C" or "A <- B <- C"
				if (triplet.equals(aPathNodes[j] + " -> " + aPathNodes[j + 1] + " -> " + aPathNodes[j + 2])
						|| triplet.equals(aPathNodes[j] + " <- " + aPathNodes[j + 1] + " <- " + aPathNodes[j + 2])
						|| triplet.equals(aPathNodes[j] + " <- " + aPathNodes[j + 1] + " -> " + aPathNodes[j + 2])) {

					// check if "aPathNodes[j+1]" (i.e. "B") is observed or NOT // Note, it should
					// be UNOBSERVED to be a part of d-connected path
					if (conditionedNodeList.containsKey(aPathNodes[j + 1])) {
						// that means "B" is observed, therefore, this path will be ignored as it would
						// be a blocked path
						isPath_d_connected = false;
						break;
					} else
						continue;
				} else if (triplet.equals(aPathNodes[j] + " -> " + aPathNodes[j + 1] + " <- " + aPathNodes[j + 2])) {
					// check if "aPathNodes[j+1]" (i.e. "B") is observed or NOT // Note, it or it's
					// descendents should be OBSERVED in order to be a part of d-connected path
					if (!conditionedNodeList.containsKey(aPathNodes[j + 1])
							&& !isDescendentObserved(conditionedNodeList, _BNgraph, aPathNodes[j + 1])) {
						// here the evidence is also BLOCKED, as the central node is not observed, nor
						// any of its descendents are observed
						isPath_d_connected = false;
						break;
					} else
						continue;
				}
			}
			if (isPath_d_connected)
				return false; // that means, this particular path ensures that, it is free for this evidence
			// node
			else
				continue;
		}
		return true; // that means, there is no path which is free for the evidence node
	}

	private static ArrayList<String> AnalysePaths_for_Dconnectivity(ArrayList<String> _paths, Graph _BNgraph,
			Hashtable conditionedNodeList) throws Exception {

		ArrayList<String> retList = new ArrayList<String>();
		// optional list
		// Hashtable<String, String> CommonEffectNodes_and_theirPath = new
		// Hashtable<String, String>();

		for (int i = 0; i < _paths.size(); i++) {
			String aPath = _paths.get(i);
			String[] aPathNodes = aPath.split(" -> | <- "); // split based on the deliminators : -> or <-

			int pathIndex = 0;
			boolean isPath_d_connected = true;
			for (int j = 0; j < aPathNodes.length - 2; j++) {
				int tripletLength = aPathNodes[j].length() + 4 + aPathNodes[j + 1].length() + 4
						+ aPathNodes[j + 2].length(); // length of the triplet of such form: "A <arrow> B <arrow> C"
				String triplet = aPath.substring(pathIndex, pathIndex + tripletLength);
				pathIndex += aPathNodes[j].length() + 4;

				// check if subpath is the form of "A -> B -> C" or "A <- B <- C"
				if (triplet.equals(aPathNodes[j] + " -> " + aPathNodes[j + 1] + " -> " + aPathNodes[j + 2])
						|| triplet.equals(aPathNodes[j] + " <- " + aPathNodes[j + 1] + " <- " + aPathNodes[j + 2])
						|| triplet.equals(aPathNodes[j] + " <- " + aPathNodes[j + 1] + " -> " + aPathNodes[j + 2])) {

					// check if "aPathNodes[j+1]" (i.e. "B") is observed or NOT // Note, it should
					// be UNOBSERVED to be a part of d-connected path
					if (conditionedNodeList.containsKey(aPathNodes[j + 1])) {
						isPath_d_connected = false;
						break; // that means "B" is observed, therefore, this path will be ignored as it would
						// be a blocked path
					}
				} else if (triplet.equals(aPathNodes[j] + " -> " + aPathNodes[j + 1] + " <- " + aPathNodes[j + 2])) {
					// check if "aPathNodes[j+1]" (i.e. "B") is observed or NOT // Note, it or it's
					// descendents should be OBSERVED in order to be a part of d-connected path
					if (!conditionedNodeList.containsKey(aPathNodes[j + 1])
							&& !isDescendentObserved(conditionedNodeList, _BNgraph, aPathNodes[j + 1])) {
						isPath_d_connected = false;
						break; // COMMON EFEECT case:
					} else {
						CommonEffectNodes_and_theirPath.put(aPathNodes[j + 1], aPath); // optional action; may be useful
						// in future

						// CE_blobs.put(aPathNodes[j+1], aPathNodes[j] + " -> " + aPathNodes[j+1]);
						// CE_blobs.put(aPathNodes[j+1], aPathNodes[j+1] + " <- " + aPathNodes[j+2]);
						// CE_blobs_structures.put(aPathNodes[j+1], aPathNodes[j] + " -> " +
						// aPathNodes[j+1] + " <- " + aPathNodes[j+2]);

					}
				}
			}
			if (isPath_d_connected) {
				// if the whole path is O.K. (i.e. d-connected) then save it for returning
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
				if(isHardEvidence(currentNodeCPT) != null)	// current node is a hard evidence
					return true;
			}
		}
		return false;
	}

	private static boolean isCurrentNodeInstantiated(String currentNode, Hashtable conditionedNodeList) throws Exception {
//		//Hashtable priorTable = RetriveProbfromBuffer(currentNode, "prior");
//		Hashtable evidenceTable = (Hashtable) conditionedNodeList.get(currentNode);
//		Enumeration tempEnum = evidenceTable.keys();
//		
//		while(tempEnum.hasMoreElements()) {
//			String stateName = (String) tempEnum.nextElement();
//			Double evProb = (Double) evidenceTable.get(stateName);
//			evProb = ((double) Math.round((evProb) * 1000.0) / 1000.0);
//			
//			// get posterior
//			Double posteriorProb = _net.getNode(currentNode).getBelief(stateName);
//			posteriorProb = ((double) Math.round((posteriorProb) * 1000.0) / 1000.0);
//			
//			if(evProb != posteriorProb) // if a state has current probability which not is equal to its evidence Probability then it indicates that that node is not currently instantiated in the BN
//				return false;
//		}
		return true;
	}

	private static ArrayList<String> FindDescendents(Graph _BNgraph, String nodeOfInterest) {
		ArrayList<String> retList = new ArrayList<String>();
		BT(nodeOfInterest, retList, _BNgraph);
		return retList;
	}

	private static void BT(String currentNode, ArrayList<String> retList, Graph _BNgraph) {
		// LinkedList<String> children =
		// _BNgraph.adj[_BNgraph.graphNodes.indexOf(currentNode)];
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

	private static Node[] get_allEvidenceNodeExceptCurrent(Hashtable conditionedNodeList, Node[] evidenceList)
			throws Exception {

		ArrayList<Node> retList = new ArrayList<>();
		// NodeList retList = new NodeList(_net);

		Enumeration _list = conditionedNodeList.keys();
		while (_list.hasMoreElements()) {
			Node temp_node = _net.getNode((String) _list.nextElement());
			if (!Arrays.asList(evidenceList).contains(temp_node)) {
				retList.add(temp_node);
			}
		}
		return retList.toArray(new Node[0]);
	}

	// private static Hashtable<Node[], Double> FindNormalizedMutualInformation(Net
	// _net, String queryNodeName, Node[] evidenceList) {
	// TODO Auto-generated method stub
	// try {
	// Hashtable<Node[], Double> ImpactValuesofaSubset = new Hashtable<Node[],
	// Double>();
	//
	// Node queryNode = _net.getNode(queryNodeName);
	// Sensitivity svMutualInfo = new Sensitivity(queryNode, _net.getNodes(),
	// Sensitivity.ENTROPY_SENSV);
	// double max_MI = svMutualInfo.getMutualInfo(queryNode);
	//
	// for(int i = 0; i < evidenceList.size(); i++) {
	// Node currentEvidenceNode = evidenceList.getNode(i);
	// double norm_current_MI = svMutualInfo.getMutualInfo(currentEvidenceNode) /
	// max_MI;
	// //ImpactValuesofaSubset.put(key, value)
	// }
	// } catch (NeticaException e) {
	// // TODO Auto-generated catch block
	// e.printStackTrace();
	// }
	// return null;
	// }

	private static Node[][] generateAllSubsets(Node[] listParent, String queryNode, Hashtable conditionedNodeList, Graph _BNGraph) throws Exception {
		int n = listParent.length;
		ArrayList<ArrayList<Node>> retArr = new ArrayList<>();

		// Run a loop for printing all 2^n
		// subsets one by one
		for (int i = 0; i < (1 << n); i++) {
			ArrayList<Node> temp = new ArrayList<>();
			// NodeList temp = new NodeList(_net);
			// Print current subset
			for (int j = 0; j < n; j++)

				// (1<<j) is a number with jth bit 1
				// so when we 'and' them with the
				// subset number we get which numbers
				// are present in the subset and which
				// are not
				if ((i & (1 << j)) > 0)
					// System.out.print(set[j] + " ");
					temp.add(listParent[j]);

			if (isSubsetRelevant(temp.toArray(new Node[0]), queryNode, conditionedNodeList, _BNGraph)
					&& temp.size() > 0)
				retArr.add(temp);

		}
		// return retArr.toArray(new Node[0][0]);
		// converting an (arraylist of arraylist) to a 2D array
		return retArr.stream().map(l -> l.stream().toArray(Node[]::new)).toArray(Node[][]::new);
	}

	private static Node[][] generateRelevantSubsets(Node[] listParent, String queryNodeName,
			Hashtable conditionedNodeList, Graph _BNgraph) throws Exception {
		int n = listParent.length;
		ArrayList<ArrayList<Node>> retArr = new ArrayList<>();

		// Run a loop for printing all 2^n
		// subsets one by one
		for (int i = 0; i < (1 << n); i++) {
			ArrayList<Node> temp = new ArrayList<>();
			// NodeList temp = new NodeList(_net);
			// Print current subset
			for (int j = 0; j < n; j++)

				// (1<<j) is a number with jth bit 1
				// so when we 'and' them with the
				// subset number we get which numbers
				// are present in the subset and which
				// are not
				if ((i & (1 << j)) > 0)
					// System.out.print(set[j] + " ");
					temp.add(listParent[j]);

			// System.out.println("}");

			// check if this subset is relevant, then add
			if (isSubsetRelevant(temp.toArray(new Node[0]), queryNodeName, conditionedNodeList, _BNgraph)
					&& (temp.size() > 0)) {
				retArr.add(temp);
			}
		}
		// return retArr.toArray(new Node[0][0]);
		// converting an (arraylist of arraylist) to a 2D array
		return retArr.stream().map(l -> l.stream().toArray(Node[]::new)).toArray(Node[][]::new);
	}

	private static boolean isSubsetRelevant(Node[] currentSubset, String queryNodeName, Hashtable conditionedNodeList,
			Graph _BNgraph) throws Exception {

		set_findings_of_ConditionedNodes_3(conditionedNodeList, currentSubset); // set findings of the current evidences

		for (int i = 0; i < currentSubset.length; i++) {
			String currentEvidenceNode = currentSubset[i].getShortName();
			List<String> nodeNameList = getNodeNameList(currentSubset);
			
			ArrayList<String> _paths = new ArrayList<String>(_BNgraph.findPaths(currentEvidenceNode, queryNodeName));
			// Path analysis: i.e. finding out paths with significant contribution
			if (is_Blocked_Evidence(nodeNameList, _paths, _BNgraph, conditionedNodeList)) {
				_net.clearAllEvidence();
				_net.compile();
				return false;
			}

			// //NodeList D_connectedNodesofCurrentEvidenceNode = new NodeList(_net);
			// Node[] D_connectedNodesofCurrentEvidenceNode =
			// currentEvidenceNode.getDConnectedNodes(); // get d-connected nodes of current
			// evidence node
			//
			// if(!Arrays.asList(D_connectedNodesofCurrentEvidenceNode).contains((Node)_net.getNode(queryNodeName))){
			// _net.clearAllEvidence();
			// return false;
			// }
		}

		_net.clearAllEvidence();
		_net.compile();

		return true;
	}

	private static List<String> getNodeNameList(Node[] currentSubset) {
		List<String> retList = new ArrayList<>();
		for(Node n: currentSubset) {
			retList.add(n.getShortName());
		}
		return retList;
	}

	private static boolean is_Blocked_Evidence(List<String> currentSubset, ArrayList<String> _paths, Graph _BNgraph,
			Hashtable conditionedNodeList) {
		ArrayList<String> retList = new ArrayList<String>();
		// optional list
		// Hashtable<String, String> CommonEffectNodes_and_theirPath = new
		// Hashtable<String, String>();

		for (int i = 0; i < _paths.size(); i++) {
			String aPath = _paths.get(i);
			String[] aPathNodes = aPath.split(" -> | <- "); // split based on the deliminators : -> or <-

			if (aPathNodes.length == 2)
				return false; // that means first node is the evidence and the second one is the target node,
			// so the evidence node is UNBLOCKED

			int pathIndex = 0;
			boolean isPath_d_connected = true;
			for (int j = 0; j < aPathNodes.length - 2; j++) {
				int tripletLength = aPathNodes[j].length() + 4 + aPathNodes[j + 1].length() + 4
						+ aPathNodes[j + 2].length(); // length of the triplet of such form: "A <arrow> B <arrow> C"
				String triplet = aPath.substring(pathIndex, pathIndex + tripletLength);
				pathIndex += aPathNodes[j].length() + 4;

				// check if subpath is the form of "A -> B -> C" or "A <- B <- C"
				if (triplet.equals(aPathNodes[j] + " -> " + aPathNodes[j + 1] + " -> " + aPathNodes[j + 2])
						|| triplet.equals(aPathNodes[j] + " <- " + aPathNodes[j + 1] + " <- " + aPathNodes[j + 2])
						|| triplet.equals(aPathNodes[j] + " <- " + aPathNodes[j + 1] + " -> " + aPathNodes[j + 2])) {

					// check if "aPathNodes[j+1]" (i.e. "B") is observed or NOT // Note, it should
					// be UNOBSERVED to be a part of d-connected path
					if (conditionedNodeList.containsKey(aPathNodes[j + 1])) {
						// that means "B" is observed, therefore, this path will be ignored as it would
						// be a blocked path
						isPath_d_connected = false;
						break;
					} else
						continue;
				} else if (triplet.equals(aPathNodes[j] + " -> " + aPathNodes[j + 1] + " <- " + aPathNodes[j + 2])) {
					// check if "aPathNodes[j+1]" (i.e. "B") is observed or NOT // Note, it or it's
					// descendents should be OBSERVED in order to be a part of d-connected path
					if (!currentSubset.contains(aPathNodes[j + 1])
							&& !isDescendentObserved(currentSubset, _BNgraph, aPathNodes[j + 1])) {
						// here the evidence is also BLOCKED, as the central node is not observed, nor
						// any of its descendents are observed
						isPath_d_connected = false;
						break;
					} else
						continue;
				}
			}
			if (isPath_d_connected)
				return false; // that means, this particular path ensures that, it is free for this evidence
			// node
			else
				continue;
		}
		return true; // that means, there is no path which is free for the evidence node
	}

	private static boolean isDescendentObserved(List<String> currentSubset,
			Graph _BNgraph, String nodeOfInterest) {
		ArrayList<String> descendents = new ArrayList<String>();
		descendents = FindDescendents(_BNgraph, nodeOfInterest);

		for (String currentNode : descendents) {
			if (currentSubset.contains(currentNode)) { 
				return true;
			}
		}
		return false;
	}

	private static double findImpact_using_KL_divergece(Node[] currentSubset, Node[] _allEvidenceNodeExceptCurrent,
			String queryNode, Hashtable conditionedNodeList) throws Exception {

		Node targetNode = _net.getNode(queryNode);

		// set findings (observation) all all the evidences except the current node:
		// '_node'
		set_findings_of_ConditionedNodes_3(conditionedNodeList, _allEvidenceNodeExceptCurrent);

		// get the posterior distribution of target Node
		double[] P_all = getTargetNodeBeliefS(targetNode);

		// now set the current evidence node '_node', which will complete the evidence
		// list
		set_findings_of_ConditionedNodes_3(conditionedNodeList, currentSubset);

		// get the posterior distribution of target Node
		double[] Q_all = getTargetNodeBeliefS(targetNode);

		// compute the KL-divergence value between two distribution P and Q
		return KL_divergence(Q_all, P_all);
	}

	private static double KL_divergence(double[] from, double[] to) {
		// TODO Auto-generated method stub
		double log2 = Math.log(2.0);
		double epsilon = 1e-10;
		double result = 0.0;

		for (int i = 0; i < from.length; i++) {

			double from_i = (double) from[i];
			double to_i = (double) to[i];

			if (from_i < epsilon) {
				continue;
			}

			double logFract = Math.log(from_i / to_i);

			if (logFract == Double.POSITIVE_INFINITY) {
				//System.out.println("computation of kldivergence returning +inf: from_i=" + from_i + ", to_i=" + to_i);
				System.out.flush();
				return Double.POSITIVE_INFINITY; // can't recover
			}

			result += from_i * (logFract / log2); // express it in log base 2; this is done like this since Java Math
			// package has no function of 2-based log function, they only have
			// 10-based and e-based log function
		}

		return result;
	}

	private static double[] getTargetNodeBeliefS(Node targetNode) throws Exception {
		return targetNode.getBeliefs();
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

	private static Node[] get_allEvidenceNodeExceptCurrent(Hashtable conditionedNodeList, Node _node) throws Exception {

		ArrayList<Node> retList = new ArrayList<>();

		Enumeration _list = conditionedNodeList.keys();
		while (_list.hasMoreElements()) {
			Node temp_node = _net.getNode((String) _list.nextElement());
			if (!temp_node.getShortName().equals(_node.getShortName())) {
				retList.add(temp_node);
			}
		}
		return retList.toArray(new Node[0]);

	}

	private static void print_pairwise_impact_of_all_nodes() throws Exception {
		//
		// // get all the nodelist
		// NodeList _allNodes = new NodeList(_net);
		// _allNodes = _net.getNodes();
		// String line = "\t";
		// for(int i = 0; i < _allNodes.size(); i++){
		// Node _node = _allNodes.getNode(i);
		// line += _node.getName() + "\t";
		// }
		// System.out.println(line);
		//
		// for(int i = 0; i < _allNodes.size(); i++){
		// Node _node_i = _allNodes.getNode(i);
		// line = _node_i.getName() + "\t";
		//
		// for(int j = 0; j < _allNodes.size(); j++){
		// Node _node_j = _allNodes.getNode(j);
		// double impact_i_j = find_impact_of_one_node_upon_other(_net, _node_i,
		// _node_j);
		// line += impact_i_j + "\t";
		// }
		//
		// System.out.println(line);
		// }
		//
	}

	private static double find_impact_of_one_node_upon_other(Node varyingNode, Node queryNode) {
		// // MI (mutual information) based
		// try {
		// Sensitivity svMutualInfo = new Sensitivity(queryNode, _net.getNodes(),
		// Sensitivity.ENTROPY_SENSV);
		// double _MI = svMutualInfo.getMutualInfo(varyingNode);
		// svMutualInfo.finalize();
		// return _MI;
		// // KL-divergence based
		// } catch (NeticaException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }
		return (Double) null;
	}

	private static void print_belief_of_target_nodes_forAllSubsets_of_Evidence_Nodes(Hashtable conditionedNodeList,
			Node[] allSubsetsOfEvidenceNodes, String queryNode, String queryNodeState) {
		//
		// for(int iter = 0; iter < allSubsetsOfEvidenceNodes.size(); iter++) {
		// System.out.print("for the subset of evidence nodes (" +
		// allSubsetsOfEvidenceNodes.get(iter).toString() + "), ");
		// set_findings_of_ConditionedNodes_3(_net, conditionedNodeList,
		// (NodeList)allSubsetsOfEvidenceNodes.get(iter));
		// System.out.println("the Target node belief (" + queryNode + " = " +
		// queryNodeState + ") becomes: " + getTargetNodeBelief(_net, queryNode,
		// queryNodeState));
		// }
		// System.out.println("===========================================================================================");
	}

	private static void set_findings_of_ConditionedNodes_3(Hashtable conditionedNodeList, Node[] _subset)
			throws Exception {
		Enumeration _list = conditionedNodeList.keys();

		while (_list.hasMoreElements()) {
			/* read the activated Node name */
			String _activatedNode = (String) _list.nextElement();

			/*
			 * try to load the Node with the name, if any problem netica Exception will
			 * raise
			 */
			Node _node = _net.getNode(_activatedNode);
			int nStates = _node.getNumberStates();

			if (Arrays.asList(_subset).contains(_node)) {

				/* set likelihood vector */
				double[] _likelihoodVector = new double[nStates];
				boolean[] _flag = new boolean[nStates];

				// HashMap tempMap = (HashMap) conditionedNodeList.get(_activatedNode);
				// Hashtable _tempTable = new Hashtable<>();
				// _tempTable.putAll(tempMap);
				Hashtable _tempTable = (Hashtable) conditionedNodeList.get(_activatedNode);

				Enumeration _activeInfo = _tempTable.keys();

				/* check the number of instantiated states */
				if (nStates < _tempTable.size()) {
					Exception exp = new NLGException("Number of instantiated states are greater than it should be");
					throw exp;
				}

				double tempSum = 0.0;
				while (_activeInfo.hasMoreElements()) {
					String _knownState = (String) _activeInfo.nextElement();
					// double _knownValue = Double.parseDouble((String)
					// _tempTable.get(_knownState));
					double _knownValue = (double) _tempTable.get(_knownState);
					tempSum += _knownValue;

					/* set that known value into the corresponding node states */
					tempSum = ((double) Math.round((tempSum) * 1000.0) / 1000.0);

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
				/* we may need to make other states equiprobable */
				/* remaining number of states to be made "Equiprobable " */
				int denom = nStates - _tempTable.size();
				double _totalRemainingPercentageValue_per = (1.0 - tempSum) / ((double) denom);

				for (int iter = 0; iter < nStates; iter++) {
					if (!_flag[iter])
						_likelihoodVector[iter] = (float) _totalRemainingPercentageValue_per;
				}

				/*
				 * finally set the findings as a likelihood of this particular instantiated node
				 */
				_node.setEvidenceLikelihood(_likelihoodVector);

			}
		}
		_net.compile();
	}

	private static Node[][] generateSubsets(Node[] listParent) throws Exception {
		int n = listParent.length;
		ArrayList<ArrayList<Node>> retArr = new ArrayList<>();
		// Run a loop for printing all 2^n
		// subsets one by one
		for (int i = 0; i < (1 << n); i++) {
			ArrayList<Node> temp = new ArrayList();
			// Print current subset
			for (int j = 0; j < n; j++)

				// (1<<j) is a number with jth bit 1
				// so when we 'and' them with the
				// subset number we get which numbers
				// are present in the subset and which
				// are not
				if ((i & (1 << j)) > 0)
					// System.out.print(set[j] + " ");
					temp.add(listParent[j]);

			// System.out.println("}");
			retArr.add(temp);
		}
		return retArr.toArray(new Node[0][0]);
	}

	private static double getTargetNodeBelief(String queryNodeName, String queryNodeState) throws Exception {
		Node node = _net.getNode(queryNodeName);
		// System.out.println(node.getBeliefs());
		double belief = node.getBelief(queryNodeState);
		// float[] _beliefs = node.getBeliefs();
		// System.out.println("the query node belief on" + queryNodeState + " is: " +
		// belief);
		return belief;

	}

	private static void printTargetNodeBelief(String queryNodeName, String queryNodeState) throws Exception {

		Node node = _net.getNode(queryNodeName);
		double belief = node.getBelief(queryNodeState);
		//System.out.println("the query node belief on" + queryNodeState + " is: " + belief);

	}

	// private static Node[] get_ancestors_of_Query_node(Net _net, String
	// queryNodeName, String queryNodeState) throws Exception{
	//
	// //NodeList nodeList = new NodeList(_net);
	// ArrayList<Node> nodeList = new ArrayList<>();
	// Node queryNode = _net.getNode(queryNodeName);
	//
	// queryNode.getRelatedNodes(nodeList,"ancestors");
	// boolean sucess = nodeList.remove(queryNode);
	// //printNodeList(nodeList);
	// return nodeList.toArray(new Node[0]);
	//
	//
	// }

	private static Hashtable Find_d_connectedNodes(Hashtable conditionedNodeList, String queryNode,
			String queryNodeState) throws Exception {

		Hashtable retNodeList = new Hashtable(conditionedNodeList);
		// retNodeList = conditionedNodeList;

		Enumeration elem = conditionedNodeList.keys();
		while (elem.hasMoreElements()) {
			String _currentNodename = (String) elem.nextElement();
			Node currentEvidenceNode = _net.getNode(_currentNodename);
			Node[] D_connectedNodesofCurrentEvidenceNode = currentEvidenceNode.getDConnectedNodes(); // get d-connected
			// nodes of
			// current
			// evidenc node

			if (!Arrays.asList(D_connectedNodesofCurrentEvidenceNode).contains((Node) _net.getNode(queryNode))) {
				blockedEvidenceList.add((String) _currentNodename);
				retNodeList.remove((String) _currentNodename);
			}
		}
		return retNodeList;
	}

	// private static void printNodeList(NodeList nodeList) {
	// for(int iter = 0; iter < nodeList.size(); iter++)
	// {
	// try {
	// Node _node = nodeList.getNode(iter);
	// System.out.println(_node.getName());
	// } catch (ArrayIndexOutOfBoundsException e) {
	// e.printStackTrace();
	// } catch (NeticaException e) {
	// e.printStackTrace();
	// }
	// }
	// }

	private static Node[] MakeNodeList(Hashtable conditionedNodeList) throws Exception {

		// NodeList retList = new NodeList(_net);
		ArrayList<Node> retList = new ArrayList<>();
		Enumeration elem = conditionedNodeList.keys();
		while (elem.hasMoreElements()) {
			String _nodeName = (String) elem.nextElement();
			retList.add(_net.getNode(_nodeName));
		}
		return retList.toArray(new Node[0]);
	}

	private static boolean CheckConsistency(Hashtable activeNodeList, String queryNode) throws Exception {
		/* Checking node names */
		Enumeration _enum = activeNodeList.keys();
		while (_enum.hasMoreElements()) {
			/* try to find the node in the network */
			if (_net.getNode((String) _enum.nextElement()) == null)
				return false;
		}
		return true;
	}

	private static void Explain_a_BFS_Blob(Blob b, String impactChoice, Graph _BNgraph, ReasoningGraph _reasoningGraph,
			boolean[] flag, Hashtable conditionedNodeList) throws Exception {
//		/* CONSTRUCT UPDATED CONDITIONED LIST FOR THE CHILDREN OF BFS Blob */
//
//		b = Re_Construct_a_BFS_blob(b, _BNgraph, _reasoningGraph, flag);
//
//		if (b.evidenceSet.size() == 1) {
//			String evidenceNodeName = (String) Collections.list(b.evidenceSet.keys()).get(0);
//			// String evidenceStateofInterest =
//			// _net.getNode(evidenceNodeName).getState(stateIndexofInterest).getShortName();
//			String evidenceStateofInterest = "";
//			if (conditionedNodeList.containsKey(b.CE_nodeName))
//				evidenceStateofInterest = Find_State_of_Interest(
//						(Hashtable) conditionedNodeList.get((String) _net.getNode(evidenceNodeName).getShortName()));
//			else
//				evidenceStateofInterest = Find_State_of_Interest_for_intermediate_Node(evidenceNodeName);
//
//			String queryNodeName = b.queryNodeList.get(0);
//			// String queryNodeStateName =
//			// (queryNodeName.equals(ultimateTargetNode))?ultimateTargetNodeState:_net.getNode(queryNodeName).getState(stateIndexofInterest).getShortName();
//			String queryNodeStateName = Find_target_State_Name(queryNodeName, conditionedNodeList);
//
//			Explain_a_Single_Edge(evidenceNodeName, evidenceStateofInterest, queryNodeName, queryNodeStateName,
//					_BNgraph);
//		} else if (b.evidenceSet.size() >= 2) {
//			String queryNodeName = b.queryNodeList.get(0);
//			String queryNodeStateName = b.queryNodeStateList.get(0);
//			double priorValue = RetriveProbfromBuffer(b.queryNodeList.get(0), "prior", conditionedNodeList); // this is
//			// another
//			// choice
//
//			ArrayList<String> allChilds = Collections.list(b.evidenceSet.keys());
//			Hashtable U_conditionedNodeList = Construct_Updated_ConditionedList_for_a_NodeList(allChilds);
//			Hashtable P_conditionedNodeList = Construct_Prior_ConditionedList_for_a_NodeList(allChilds);
//
//			set_findings_of_ConditionedNodes_3(P_conditionedNodeList, MakeNodeList(allChilds)); // resetting the CPTs of
//			// all the children of
//			// current BFS blob
//			// double priorValue = (double)
//			// getTargetNodeCPT(queryNodeName).get(queryNodeStateName);
//
//			// ################ 1. find all subsets of the evidence nodes and filter out
//			// irrelevant subsets ##############
//			// This is the brute-force (multi-way) approach (i.e. check all subsets of
//			// evidence (power sets))
//
//			ArrayList<ArrayList<Node>> list_of_all_subsets = new ArrayList<>();
//
//			Node[][] allSubsetsOfEvidenceNodes = generateAllSubsets(MakeNodeList(b.evidenceSet));
//
//			// instantiate impact vector (magnitude + direction)
//			ArrayList<Map.Entry<Node[], Double>> magnitudeofSortedImpactValues = new ArrayList<>();
//			ArrayList<Map.Entry<Node[], String>> directionofSortedimpactValues = new ArrayList<>();
//
//			if (impactChoice.equals("perc_change")) {
//				// ################ 2. find the impact vectors (magnitude,direction) upon the
//				// target node (based on choice, - could be KL, % of changes in posteriori, MI,
//				// or others ###########################
//				// Hashtable<Node[], Double> PercentageofTargetBeliefChangeofSubsets =
//				// FindPercentageofTargetBeliefChangeForAllRelevantSubsets(allSubsetsOfEvidenceNodes,b.evidenceSet,b.queryNodeList.get(0),b.queryNodeStateList.get(0));
//				Hashtable<Node[], Double> PercentageofTargetBeliefChangeofSubsets = FindPercentageofTargetBeliefChangeForAllSubsets(
//						allSubsetsOfEvidenceNodes, U_conditionedNodeList, P_conditionedNodeList, queryNodeName,
//						queryNodeStateName, priorValue, allChilds);
//
//				// ################ 3. analyze the impact vectors - to detect impact style -
//				// withConflict [some cause increase, some cause decrease], OR withoutConflict
//				// [1) all increase, but some contributes large portion, some little, or 2) all
//				// contribute uniformly)
//
//				// get the hashtable sorted as an ArrayList
//				magnitudeofSortedImpactValues = sortHashTableValue(PercentageofTargetBeliefChangeofSubsets, "desc");
//				directionofSortedimpactValues = findDirectionofVector(magnitudeofSortedImpactValues);
//			}
//
//			// ################ 4. if withConflict, generateExplanationwithConflict ( prior,
//			// update ([sorted nodelist] increase/decrease ,[sorted nodelist]
//			// conflict-decrease/increase), conclusion)
//
//			/* get the top subset with value of its interest (e.g. percentage of changes) */
//			Node[] SubsetofTopImpact = (Node[]) magnitudeofSortedImpactValues.get(0).getKey();
//			double impactValueofTopSubset = (double) magnitudeofSortedImpactValues.get(0).getValue();
//			String impactDirectionofTopSubset = (String) directionofSortedimpactValues.get(0).getValue();
//
//			// get the value of the interest for the whole input evidence list
//			Map.Entry<Double, String> impactVectorofTheWholeSet = FindtheVectorforTheWholeSet(
//					magnitudeofSortedImpactValues, b.evidenceSet);
//			double valueforTheWholeSet = impactVectorofTheWholeSet.getKey();
//			// double posteriorValue = valueforTheWholeSet + priorValue;
//			double posteriorValue = RetriveProbfromBuffer(queryNodeName, "posterior", conditionedNodeList);
//			posteriorValue = ((double) Math.round((posteriorValue) * 1000.0) / 1000.0);
//			valueforTheWholeSet = ((double) Math.round((valueforTheWholeSet) * 1000.0) / 1000.0);
//
//			ArrayList<String> _targetNodeInfo = new ArrayList<>();
//			_targetNodeInfo.add(queryNodeName);
//			_targetNodeInfo.add(queryNodeStateName);
//			_targetNodeInfo.add(Double.toString(posteriorValue));
//
//			if (SubsetofTopImpact.length < b.evidenceSet.size()) {
//				Node[] remainingSubset = FindRemainingSubsets(SubsetofTopImpact, b.evidenceSet);
//
//				ArrayList<ArrayList<String>> TopSubsetNodeinfoList = GetInfoForASubset(SubsetofTopImpact,
//						U_conditionedNodeList);
//				ArrayList<ArrayList<String>> remainingNodeinfoList = GetInfoForASubset(remainingSubset,
//						U_conditionedNodeList);
//
//				if (impactDirectionofTopSubset.equals("increase")) {
//
//					outputResponse_for_A_Target += NLGtext.SayContradiction(remainingNodeinfoList,
//							TopSubsetNodeinfoList, _targetNodeInfo, "decrease", posteriorValue);
//					// Say_without_CE("increase", b, SubsetofTopImpact,conditionedNodeList,
//					// impactChoice);
//					// Say_without_CE("decrease", b, remainingSubset, conditionedNodeList,
//					// impactChoice);
//				} else {
//					outputResponse_for_A_Target += NLGtext.SayContradiction(remainingNodeinfoList,
//							TopSubsetNodeinfoList, _targetNodeInfo, "increase", posteriorValue);
//					// Say_without_CE("decrease", b, SubsetofTopImpact, conditionedNodeList,
//					// impactChoice);
//					// Say_without_CE("increase", b, remainingSubset, conditionedNodeList,
//					// impactChoice);
//				}
//			} else {
//				// ################ 5. else
//				// if larger-smaller contribution fashion [????? - to do], then
//				// generateExplanationwithoutConflict ( prior, update ([sorted nodelist]
//				// increase/decrease ,[sorted nodelist] increase/deccrease) [further],
//				// conclusion)
//
//				// else (means all the evidences are contributing uniformly) then
//				// generateExplanationwithoutConflict ( prior, update ([sorted nodelist]
//				// increase/decrease)
//
//				ArrayList<ArrayList<String>> _nodeinfoList = GetInfoForASubset(MakeNodeList(b.evidenceSet),
//						U_conditionedNodeList); // consider all the evidence (unblocked, obviously) set
//				// ArrayList<String> _targetNodeInfo = new ArrayList<>();
//				// _targetNodeInfo.add(queryNode); _targetNodeInfo.add(queryNodeState);
//				// _targetNodeInfo.add(Double.toString(valueforTheWholeSet));
//				if (valueforTheWholeSet > 0) {
//					String direction = "increase";
//					// outputResponse_for_A_Target += NLGtext.SayImply(_nodeinfoList,
//					// _targetNodeInfo, direction,
//					// Double.toString(valueforTheWholeSet));
//					outputResponse_for_A_Target += NLGtext.SayImply(_nodeinfoList, _targetNodeInfo, direction,
//							Double.toString(posteriorValue));
//
//				} else {
//					String direction = "decrease";
//					// outputResponse_for_A_Target += NLGtext.SayImply(_nodeinfoList,
//					// _targetNodeInfo, direction,
//					// Double.toString(valueforTheWholeSet));
//					outputResponse_for_A_Target += NLGtext.SayImply(_nodeinfoList, _targetNodeInfo, direction,
//							Double.toString(posteriorValue));
//				}
//			}
//			set_findings_of_ConditionedNodes_3(U_conditionedNodeList, MakeNodeList(allChilds)); // setting back the CPTs
//			// of all the children
//			// of current BFS blob
//		}
	}

	private static Blob Re_Construct_a_BFS_blob(Blob b, Graph _BNgraph, ReasoningGraph _reasoningGraph,
			boolean[] flag) {
		Hashtable tempEvidenceList = new Hashtable<>();

		ArrayList<String> allEvNodes = Collections.list(b.evidenceSet.keys());
		for (String evNode : allEvNodes) {
			if (!isBFS_EvidenceNode_Overlap_with_CE_Blob_Info(evNode, b.queryNodeList.get(0), _reasoningGraph, flag)) {
				tempEvidenceList.put(evNode, b.evidenceSet.get(evNode));
			}
		}

		b.evidenceSet = new Hashtable<>(tempEvidenceList);
		return b;
	}

	private static boolean isBFS_EvidenceNode_Overlap_with_CE_Blob_Info(String BFS_evNode, String BFS_targetNode,
			ReasoningGraph _reasoningGraph, boolean[] flag) {
		for (Blob ceBLob : CE_blobs) {
			if ((flag[_reasoningGraph.graphNodes.indexOf(ceBLob.CE_nodeName)])
					&& (ceBLob.CE_nodeName.equals(BFS_evNode) || ceBLob.evidenceSet.containsKey(BFS_evNode))) {
				return true;
			}
		}
		return false;
	}

	private static ArrayList<ArrayList<String>> GetInfoForASubset(Node[] subsetofTopImpact,
			Hashtable U_conditionedNodeList) throws Exception {
		ArrayList<ArrayList<String>> retList = new ArrayList<>();

		for (Node node : subsetofTopImpact) {
			String nodeName = node.getShortName();
			ArrayList<String> nodeInfo = new ArrayList<>();
			nodeInfo.add(nodeName);

			// if(backupConditionedList.containsKey(nodeName)) {
			// nodeInfo.add(getEvidenceStateName(nodeName));
			// nodeInfo.add("1.0"); // because, evidence is with certain probability
			// }
			// else {
			// String nodeStateName =node.getState(stateIndexofInterest).getShortName();
			// nodeInfo.add(nodeStateName);
			// nodeInfo.add((String)((Hashtable)U_conditionedNodeList.get(nodeName)).get(nodeStateName));
			// }

			// String nodeStateName =node.getState(stateIndexofInterest).getShortName();
			String nodeStateName = "";
			if (U_conditionedNodeList.containsKey(nodeName))
				nodeStateName = Find_State_of_Interest(
						(Hashtable) U_conditionedNodeList.get((String) _net.getNode(nodeName).getShortName()));
			else
				nodeStateName = Find_State_of_Interest_for_intermediate_Node(nodeName);

			nodeInfo.add(nodeStateName);
			nodeInfo.add(
					Double.toString(((Double) ((Hashtable) U_conditionedNodeList.get(nodeName)).get(nodeStateName))));

			retList.add(nodeInfo);
		}

		return retList;
	}

	private static String getEvidenceStateName(String _nodeName) {
		String retStr = "";

		Hashtable tempTable = (Hashtable) backupConditionedList.get(_nodeName);
		Enumeration tempEnum = tempTable.keys();
		while (tempEnum.hasMoreElements()) {
			String stateName = (String) tempEnum.nextElement();
			String probVal = (String) tempTable.get(stateName);
			if (Double.parseDouble(probVal) == 100.0)
				return stateName;
		}

		return retStr;
	}

	private static Hashtable<Node[], Double> FindPercentageofTargetBeliefChangeForAllSubsets(
			Node[][] allSubsetsOfEvidenceNodes, Hashtable U_conditionedNodeList, Hashtable P_conditionedNodeList,
			String queryNodeName, String queryNodeStateName, double priorVal, ArrayList<String> allChild)
			throws Exception {

		Hashtable<Node[], Double> PercentageofTargetBeliefChangeofSubsets = new Hashtable<Node[], Double>();

		for (int iter = 0; iter < allSubsetsOfEvidenceNodes.length; iter++) {
			Node[] currentSubset = (Node[]) allSubsetsOfEvidenceNodes[iter];

			set_findings_of_ConditionedNodes_3(U_conditionedNodeList, currentSubset); // setting the CPTs of subsets of
			// children to updated belief
			// vector (updated CPT)
			double posteriorVal = (double) getTargetNodeCPT(queryNodeName).get(queryNodeStateName); // get the posterior
			// after only
			// setting the
			// subset of
			// children
			posteriorVal = ((double) Math.round((posteriorVal) * 1000.0) / 1000.0);

			set_findings_of_ConditionedNodes_3(P_conditionedNodeList, MakeNodeList(allChild)); // resetting the CPTs of
			// all children to prior
			// (original prior)
			// states
			double change = posteriorVal - priorVal;
			change = ((double) Math.round((change) * 1000.0) / 1000.0);

			PercentageofTargetBeliefChangeofSubsets.put(currentSubset, change);
		}
		return PercentageofTargetBeliefChangeofSubsets;
	}

	private static void Say_without_CE(String direction, Blob b, Node[] nodeSet, Hashtable conditionedNodeList,
			String impactChoice) throws Exception {

		// save prior value of the queryNode
		double priorBeleif = b.prior.get(0);

		/* measure the impact of causal nodes */
		Node[] causalNodes = Find_Causal_AntiCausalNodes(b, "causal");
		double causalImpact = 0;
		if (impactChoice.equals("perc_change")) {
			causalImpact = FindPercentageofTargetBeliefChange(conditionedNodeList, causalNodes, b.queryNodeList.get(0),
					b.queryNodeStateList.get(0)); // beware, the "causalNodes" are parameterized in the Net
		}
		// Update the prior of the queryNode now
		// b.prior = getTargetNodeBelief(_net, b.queryNode, b.queryNodeState);

		// Hence, de-parameterize the "causalNodes" set
		removeFindingsofCurrentNodeList(causalNodes);

		/* measure the impact of anti_causal nodes */
		Node[] anti_causalNodes = Find_Causal_AntiCausalNodes(b, "anti_causal");
		double anit_causalImpact = 0;
		if (impactChoice.equals("perc_change")) {
			anit_causalImpact = FindPercentageofTargetBeliefChange(conditionedNodeList, anti_causalNodes,
					b.queryNodeList.get(0), b.queryNodeStateList.get(0));
		}
		// for the same reason, de-parameterize the "anit_causalNodes" set
		removeFindingsofCurrentNodeList(anti_causalNodes);

		if (direction.equals("increase")) {
			if (causalImpact >= anit_causalImpact) {
				if (causalNodes.length > 0) {

					// saveOriginalPriorsofAllNodes();
					set_findings_of_ConditionedNodes_3(conditionedNodeList, causalNodes); // causal sets should be
					// talked first
					// saveUpdateBeleifsofAllNodes();

					double posterior = getTargetNodeBelief(b.queryNodeList.get(0), b.queryNodeStateList.get(0));
					Say_Causal_anitCausal("Causal", direction, causalNodes, conditionedNodeList, priorBeleif,
							posterior);
					priorBeleif = posterior;
				}

				if (anti_causalNodes.length > 0) {

					// saveOriginalPriorsofAllNodes();
					set_findings_of_ConditionedNodes_3(conditionedNodeList, anti_causalNodes); // anti_causal sets
					// should be talked next
					// saveUpdateBeleifsofAllNodes();

					double posterior = getTargetNodeBelief(b.queryNodeList.get(0), b.queryNodeStateList.get(0));
					Say_Causal_anitCausal("anti_Causal", direction, anti_causalNodes, conditionedNodeList, priorBeleif,
							posterior);
				}
			} else {
				if (anti_causalNodes.length > 0) {
					// saveOriginalPriorsofAllNodes();
					set_findings_of_ConditionedNodes_3(conditionedNodeList, anti_causalNodes); // anti_causal sets
					// should be talked
					// first
					// saveUpdateBeleifsofAllNodes();

					double posterior = getTargetNodeBelief(b.queryNodeList.get(0), b.queryNodeStateList.get(0));
					Say_Causal_anitCausal("anti_Causal", direction, anti_causalNodes, conditionedNodeList, priorBeleif,
							posterior);
					priorBeleif = posterior;
				}

				if (causalNodes.length > 0) {
					// saveOriginalPriorsofAllNodes();
					set_findings_of_ConditionedNodes_3(conditionedNodeList, causalNodes); // causal sets should be
					// talked next
					// saveUpdateBeleifsofAllNodes();

					double posterior = getTargetNodeBelief(b.queryNodeList.get(0), b.queryNodeStateList.get(0));
					Say_Causal_anitCausal("Causal", direction, causalNodes, conditionedNodeList, priorBeleif,
							posterior);
				}
			}
		} else {
			if (causalImpact <= anit_causalImpact) {
				if (causalNodes.length > 0) {
					// saveOriginalPriorsofAllNodes();
					set_findings_of_ConditionedNodes_3(conditionedNodeList, causalNodes); // causal sets should be
					// talked first
					// saveUpdateBeleifsofAllNodes();

					double posterior = getTargetNodeBelief(b.queryNodeList.get(0), b.queryNodeStateList.get(0));
					Say_Causal_anitCausal("Causal", direction, causalNodes, conditionedNodeList, priorBeleif,
							posterior);
					priorBeleif = posterior;
				}

				if (anti_causalNodes.length > 0) {
					// saveOriginalPriorsofAllNodes();
					set_findings_of_ConditionedNodes_3(conditionedNodeList, anti_causalNodes); // anti_causal sets
					// should be talked next
					// saveUpdateBeleifsofAllNodes();

					double posterior = getTargetNodeBelief(b.queryNodeList.get(0), b.queryNodeStateList.get(0));
					Say_Causal_anitCausal("anti_Causal", direction, anti_causalNodes, conditionedNodeList, priorBeleif,
							posterior);
				}
			} else {
				if (anti_causalNodes.length > 0) {
					// saveOriginalPriorsofAllNodes();
					set_findings_of_ConditionedNodes_3(conditionedNodeList, anti_causalNodes); // anti_causal sets
					// should be talked
					// first
					// saveUpdateBeleifsofAllNodes();

					double posterior = getTargetNodeBelief(b.queryNodeList.get(0), b.queryNodeStateList.get(0));
					Say_Causal_anitCausal("anti_Causal", direction, anti_causalNodes, conditionedNodeList, priorBeleif,
							posterior);
					priorBeleif = posterior;
				}

				if (causalNodes.length > 0) {
					// saveOriginalPriorsofAllNodes();
					set_findings_of_ConditionedNodes_3(conditionedNodeList, causalNodes); // causal sets should be
					// talked next
					// saveUpdateBeleifsofAllNodes();

					double posterior = getTargetNodeBelief(b.queryNodeList.get(0), b.queryNodeStateList.get(0));
					Say_Causal_anitCausal("Causal", direction, causalNodes, conditionedNodeList, priorBeleif,
							posterior);
				}
			}
		}

	}

	private static void Say_Causal_anitCausal(String causality, String direction, Node[] nodeSet,
			Hashtable conditionedNodeList, double priorBeleif, double posterior) throws Exception {
		String output = causality + ":: SayNodeList(";

		Enumeration elem = conditionedNodeList.keys();
		while (elem.hasMoreElements()) {
			String _conditionedNode = (String) elem.nextElement();
			//
			for (int i = 0; i < nodeSet.length; i++) {
				String tempNodeName;

				tempNodeName = nodeSet[i].getShortName();
				if (tempNodeName.equals(_conditionedNode)) {
					output += ("SayNode(" + _conditionedNode + ":" + conditionedNodeList.get(_conditionedNode) + "), "); // if
					// this
					// approach
					// for
					// printing
					// the
					// value
					// of
					// a
					// hashtable
					// doesn't
					// work,
					// then
					// think
					// about
					// other
					// approach
				}
			}
		}

		output += ("); SayImpactDirection(" + direction + "); SayImpactMagnitude(" + priorBeleif + ", " + posterior
				+ ");");
		// System.out.println(output);
	}

	private static Node[] Find_Causal_AntiCausalNodes(Blob b, String causality) throws Exception {

		ArrayList<Node> retList = new ArrayList<>();
		String targetNode = b.queryNodeList.get(0);

		if (causality.equals("anti_causal")) {
			// ANTI_CAUSAL: edge directions are from the target to the evidence nodes
			int index = b.subNetNodes.indexOf(targetNode);
			LinkedList<String> templist = b.subNetAdj[index];
			for (int i = 0; i < templist.size(); i++) {
				retList.add(_net.getNode(templist.get(i)));
			}
		} else {
			// CAUSAL: edge directions are from the evidence to the target
			for (int i = 0; i < b.subNetAdj.length; i++) {
				String evidenceNode = b.subNetNodes.get(i);
				if (b.subNetAdj[i].contains(targetNode)) {
					retList.add(_net.getNode(evidenceNode));
				}
			}
		}
		return retList.toArray(new Node[0]);

	}

	private static void Say_with_CE(Blob b, Hashtable conditionedNodeList) throws Exception {
		// find priorlist (Note, this are first-level prior(s) of target(s) before )
		ArrayList<Double> priorList_1 = new ArrayList<>();
		for (int i = 0; i < b.queryNodeList.size(); i++) {
			priorList_1.add(RetriveProbfromBuffer(b.queryNodeList.get(i), "prior", conditionedNodeList));
		}

		String ce_node = b.CE_nodeName;
		ArrayList<String> ce_node_info = new ArrayList<>();
		ce_node_info.add(ce_node);

		if (isNodeInOriginalEvidenceList(ce_node, conditionedNodeList)) {
			ArrayList<String> tempArr = new ArrayList<>();
			tempArr.add(ce_node);

			// saveOriginalPriorsofAllNodes();
			set_findings_of_ConditionedNodes_3(conditionedNodeList, MakeNodeList(tempArr));
			// saveUpdateBeleifsofAllNodes();

			ce_node_info.add(getObservedStateName(conditionedNodeList, ce_node));
			ce_node_info.add("100.0");
		} else {

			String ce_node_StateOfInterest = _net.getNode(ce_node).getState(0).getShortName();
			ce_node_info.add(ce_node_StateOfInterest);
			ce_node_info.add(Double.toString(getTargetNodeBelief(ce_node, ce_node_StateOfInterest)));

		}

		// call Explain-away func for text generation

	}

	private static String getObservedStateName(Hashtable conditionedNodeList, String ce_node) {
		String retString = "";
		Enumeration<String> tempEnum = conditionedNodeList.keys();
		while (tempEnum.hasMoreElements()) {
			String nodeName = (String) tempEnum.nextElement();
			if (nodeName.equals(ce_node)) {
				Hashtable tempTable = (Hashtable) conditionedNodeList.get(nodeName);

				Enumeration<String> tempEnum2 = tempTable.keys();
				retString = tempEnum2.nextElement();
				break;
			}
		}
		return retString;
	}

	private static String Generate_Explanations(Hashtable conditionedNodeList, String queryNode, String queryNodeState,
			String impactChoice, Graph _BNgraph) throws Exception {
		String retString = "";

		// get the prior (so far) of the queryNode
		double priorBelief = (double) _net.getNode(queryNode).getBelief(queryNodeState);

		// ################ 1. find all subsets of the evidence nodes and filter out
		// irrelevant subsets ##############
		// This is the brute-force (multi-way) approach (i.e. check all subsets of
		// evidence (power sets))

		ArrayList<ArrayList<Node>> list_of_all_subsets = new ArrayList<>();
		Node[][] allSubsetsOfEvidenceNodes = generateRelevantSubsets(MakeNodeList(conditionedNodeList), queryNode,
				conditionedNodeList, _BNgraph);

		// instantiate impact vector (magnitude + direction)
		ArrayList<Map.Entry<Node[], Double>> magnitudeofSortedImpactValues = new ArrayList<>();
		ArrayList<Map.Entry<Node[], String>> directionofSortedimpactValues = new ArrayList<>();

		if (impactChoice.equals("perc_change")) {
			// ################ 2. find the impact vectors (magnitude,direction) upon the
			// target node (based on choice, - could be KL, % of changes in posteriori, MI,
			// or others ###########################
			Hashtable<Node[], Double> PercentageofTargetBeliefChangeofSubsets = FindPercentageofTargetBeliefChangeForAllRelevantSubsets(
					allSubsetsOfEvidenceNodes, conditionedNodeList, queryNode, queryNodeState);

			// ################ 3. analyze the impact vectors - to detect impact style -
			// withConflict [some cause increase, some cause decrease], OR withoutConflict
			// [1) all increase, but some contributes large portion, some little, or 2) all
			// contribute uniformly)

			// get the hashtable sorted as an ArrayList
			magnitudeofSortedImpactValues = sortHashTableValue(PercentageofTargetBeliefChangeofSubsets, "desc");
			directionofSortedimpactValues = findDirectionofVector(magnitudeofSortedImpactValues);
		}
		// ################ 4. if withConflict, generateExplanationwithConflict ( prior,
		// update ([sorted nodelist] increase/decrease ,[sorted nodelist]
		// conflict-decrease/increase), conclusion)

		/* get the top subset with value of its interest (e.g. percentage of changes) */
		int topIndex = 0;
		double impactValueofTopSubset_0 = (double) magnitudeofSortedImpactValues.get(0).getValue();
		double impactValueofTopSubset_1 = (double) magnitudeofSortedImpactValues.get(1).getValue();
		if(Math.abs(impactValueofTopSubset_0) == Math.abs(impactValueofTopSubset_1)) {
			if(impactValueofTopSubset_0 < 0)	// means negative, then pick the second one which is posit
				topIndex = 1;
		}
		
		Node[] SubsetofTopImpact = (Node[]) magnitudeofSortedImpactValues.get(topIndex).getKey();
		double impactValueofTopSubset = (double) magnitudeofSortedImpactValues.get(topIndex).getValue();
		String impactDirectionofTopSubset = (String) directionofSortedimpactValues.get(topIndex).getValue();

		// get the value of the interest for the whole input evidence list
		Map.Entry<Double, String> impactVectorofTheWholeSet = FindtheVectorforTheWholeSet(magnitudeofSortedImpactValues,
				conditionedNodeList);

		if (SubsetofTopImpact.length < conditionedNodeList.size()) {
			Node[] remainingSubset = FindRemainingSubsets(SubsetofTopImpact, conditionedNodeList);
			retString = GenerateEnglishwithConflict(queryNode, queryNodeState, priorBelief, SubsetofTopImpact,
					impactValueofTopSubset, impactDirectionofTopSubset, remainingSubset, conditionedNodeList,
					impactVectorofTheWholeSet);
		}
		// ################ 5. else
		// if larger-smaller contribution fashion, then
		// generateExplanationwithoutConflict ( prior, update ([sorted nodelist]
		// increase/decrease ,[sorted nodelist] increase/deccrease) [further],
		// conclusion)

		// else (means all the evidences are contributing uniformly) then
		// generateExplanationwithoutConflict ( prior, update ([sorted nodelist]
		// increase/decrease)

		return retString;
	}

	private static String GenerateEnglishwithConflict(String queryNode, String queryNodeState, double priorBelief,
			Node[] subsetofTopImpact, double impactValueofTopSubset, String impactDirectionofTopSubset,
			Node[] remainingSubset, Hashtable conditionedNodeList, Entry<Double, String> impactVectorofTheWholeSet) {
		String retString = "";
		String priorText = "";
		String UpdateText = "";
		String conclusionText = "";

		priorText = "SayPrefix(priorFlag,conclusionFlag)" + "	SayNode(" + queryNode + ", " + queryNodeState + ")	"
				+ "SayProbabilityValue(" + ((double) (Math.round(priorBelief) * 100000) / 100000) + ") + \n";

		// UpdateText

		return null;
	}

	private static ArrayList<Entry<Node[], String>> findDirectionofVector(
			ArrayList<Entry<Node[], Double>> magnitudeofSortedImpactValues) {

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

	private static Hashtable<Node[], Double> FindPercentageofTargetBeliefChangeForAllRelevantSubsets(
			Node[][] allSubsetsOfEvidenceNodes, Hashtable conditionedNodeList, String queryNode, String queryNodeState)
			throws Exception {

		Hashtable<Node[], Double> PercentageofTargetBeliefChangeofSubsets = new Hashtable<Node[], Double>();

		for (int iter = 0; iter < allSubsetsOfEvidenceNodes.length; iter++) {
			Node[] currentSubset = (Node[]) allSubsetsOfEvidenceNodes[iter];

			// ##################################### PercentageofTargetBeliefChange analysis
			// ######################
			_net.clearAllEvidence();
			_net.compile();

			double PercentageofTargetBeliefChange = FindPercentageofTargetBeliefChange(conditionedNodeList,
					currentSubset, queryNode, queryNodeState);
			PercentageofTargetBeliefChangeofSubsets.put(currentSubset, PercentageofTargetBeliefChange);

			_net.clearAllEvidence();
			_net.compile();
		}

		return PercentageofTargetBeliefChangeofSubsets;

	}

	// private static Stack<Entry<Node[], String>> orderNodes(Node[] L,
	// ArrayList<Map.Entry<Node[], Double>> magnitudeofSortedImpactValues,
	// ArrayList<Map.Entry<Node[], String>> directionofSortedimpactValues) throws
	// Exception{
	//
	// Stack<Entry<Node[], String>> retList = new Stack();
	//
	// while(magnitudeofSortedImpactValues.size() > 0) {
	// double valueOfTotalImpact = getValueWithASubset(L,
	// magnitudeofSortedImpactValues);
	// String directionofTotalImpact = getDirectionWithASubset(L,
	// directionofSortedimpactValues);
	//
	// //String dir = (directionofTotalImpact.equals("increase")) ? "asc": "desc";
	// Entry<Node[], Double> _entry =
	// sortHashTableValue(FindIndividualContribution(L,magnitudeofSortedImpactValues,directionofSortedimpactValues),
	// "asc").get(0);
	//
	// retList.push(new
	// java.util.AbstractMap.SimpleEntry<Node[],String>((Node[])_entry.getKey(),directionofTotalImpact+":"+valueOfTotalImpact));
	//
	// // remove everything from L, magList, and dirlist that contain anything
	// related to elements of _entry
	// L = RemoveFromList((Node[])_entry.getKey(),L);
	// magnitudeofSortedImpactValues =
	// RemoveFromList_double((Node[])_entry.getKey(),magnitudeofSortedImpactValues);
	// directionofSortedimpactValues =
	// RemoveFromList_string((Node[])_entry.getKey(),directionofSortedimpactValues);
	// }
	//
	// return retList;
	// }
	private static Hashtable<Node[], Double> FindIndividualContribution(Node[] L,
			ArrayList<Entry<Node[], Double>> magnitudeofSortedImpactValues,
			ArrayList<Entry<Node[], String>> directionofSortedimpactValues) {

		Hashtable<Node[], Double> retList = new Hashtable<>();

		return retList;
	}

	private static Node[] RemoveFromList(Node[] _nodeList, Node[] L) {
		List<Node> _nodeList_temp = Arrays.asList(_nodeList);
		List<Node> L_temp = Arrays.asList(L);

		for (int i = 0; i < _nodeList.length; i++) {
			L_temp.remove(_nodeList_temp.get(i));
		}
		return L_temp.toArray(new Node[0]);
	}

	private static ArrayList<Entry<Node[], Double>> RemoveFromList_double(Node[] _nodeList,
			ArrayList<Entry<Node[], Double>> L) {
		for (int i = 0; i < _nodeList.length; i++) {
			Entry<?, Double> _entry = null;
			for (int j = 0; j < L.size(); j++) {
				_entry = L.get(j);
				Node[] _currentNodeList = (Node[]) _entry.getKey();
				if (Arrays.asList(_currentNodeList).contains(_nodeList[i]))
					break;
			}
			if (_entry != null)
				L.remove(_entry);
		}
		return L;
	}

	private static ArrayList<Entry<Node[], String>> RemoveFromList_string(Node[] _nodeList,
			ArrayList<Entry<Node[], String>> L) {
		for (int i = 0; i < _nodeList.length; i++) {
			Entry<?, String> _entry = null;
			for (int j = 0; j < L.size(); j++) {
				_entry = L.get(j);
				Node[] _currentNodeList = (Node[]) _entry.getKey();
				if (Arrays.asList(_currentNodeList).contains(_nodeList[i]))
					break;
			}
			if (_entry != null)
				L.remove(_entry);
		}
		return L;
	}

	private static void removeFindingsofCurrentNodeList(Node[] _nodeList) throws Exception {

		for (int i = 0; i < _nodeList.length; i++) {
			Node _node = (Node) _nodeList[i];
			_node.clearEvidence();
		}
		_net.compile();
	}

	private static void Find_CE_BFS_Blobs(Graph _BNGraph, ReasoningGraph reasoningGraph, Hashtable conditionedNodeList,
			ArrayList<String> uninformedNodeList, String queryNode, String queryNodeState) throws Exception {
		ArrayList<Blob> retList = new ArrayList<>();
		ArrayList<String> allGraphNodes = reasoningGraph.getAllGraphNodes();
		LinkedList<String> adj[] = _BNGraph.getAllAdajacencyInfo();

		// implement here
		// find CE_blobs
		for (int i = 0; i < allGraphNodes.size(); i++) {
			// is it a CE node?
			String currentNode = allGraphNodes.get(i);
			if (CommonEffectNodes_and_theirPath.containsKey(currentNode)) {
				Hashtable<String, String> tempEvidenceList = new Hashtable<>();
				Hashtable<String, String> tempTargetList = new Hashtable<>();

				// find the target nodes (along with their causality) for the blob
				ArrayList<String> targetNodes = reasoningGraph.getParents(currentNode); // all the parents of the
				// current node in the reasoning
				// graphs will eventually be the
				// target nodes for that CE_blob
				for (String node : targetNodes) {
					int sourceIndex = _BNGraph.getAllGraphNodes().indexOf(currentNode);
					if (adj[sourceIndex].contains(node)) {
						// tempTargetList.put(node, "causal");
					} else {
						tempTargetList.put(node, "anti_causal");
					}
				}

				if (tempTargetList.size() > 0) {
					// find the evidence nodes (along with their causality) for the blob
					ArrayList<String> evidenceNodes = reasoningGraph.getChilds(currentNode); // all the children of the
					// current node in the
					// reasoning graphs will
					// eventually be the
					// evidence nodes for
					// that CE_blob
					for (String node : evidenceNodes) {
						int sourceIndex = _BNGraph.getAllGraphNodes().indexOf(currentNode);
						if (adj[sourceIndex].contains(node)) {
							// tempEvidenceList.put(node, "anti_causal");
						} else
							tempEvidenceList.put(node, "causal");
					}

					Blob b = Get_a_CE_Blob(currentNode, tempEvidenceList, tempTargetList, conditionedNodeList,
							uninformedNodeList);
					CE_blobs.add(b);
				} else {
					// then this is going to be a BFS-blob probably.
					ArrayList<String> evidenceNodes = reasoningGraph.getChilds(currentNode); // all the children of the
					// current node in the
					// reasoning graphs will
					// eventually be the
					// evidence nodes for
					// that CE_blob
					for (String node : evidenceNodes) {
						int sourceIndex = _BNGraph.getAllGraphNodes().indexOf(currentNode);
						if (adj[sourceIndex].contains(node)) {
							tempEvidenceList.put(node, "anti_causal");
						} else
							tempEvidenceList.put(node, "causal");
					}
					ArrayList<String> tempList = new ArrayList<>();
					tempList.add(currentNode);
					Blob bfsBLob = Get_a_BFS_blob(currentNode, tempEvidenceList, tempList, conditionedNodeList,
							uninformedNodeList);
					BFS_blobs.add(bfsBLob);
				}
			} else {
				// find BFS_blobs

				// get the number of children of currentNode in the "Reasoning graph" (not the
				// BNGraph), if > 2, then it should be a BFS blob
				ArrayList<String> evidenceNodes = reasoningGraph.getChilds(currentNode); // all the children of the
				// current node in the
				// reasoning graphs will
				// eventually be the
				// evidence nodes for that
				// CE_blob
				if (evidenceNodes.size() >= 2) {
					Hashtable<String, String> tempEvidenceList = new Hashtable<>();
					ArrayList<String> tempTargetList = new ArrayList<>();

					for (String node : evidenceNodes) {
						int sourceIndex = _BNGraph.getAllGraphNodes().indexOf(currentNode);
						if (adj[sourceIndex].contains(node)) {
							tempEvidenceList.put(node, "anti_causal");
						} else
							tempEvidenceList.put(node, "causal");
					}

					tempTargetList.add(currentNode);
					Blob b = Get_a_BFS_blob(currentNode, tempEvidenceList, tempTargetList, conditionedNodeList,
							uninformedNodeList);
					BFS_blobs.add(b);
				}
			}
		}
	}

	private static Blob Get_a_BFS_blob(String currentNode, Hashtable<String, String> tempEvidenceList,
			ArrayList<String> tempTargetList, Hashtable conditionedNodeList, ArrayList<String> uninformedNodeList)
			throws Exception {
		Blob b = null;

		// actually, the currentNode = targetNode in the BFS blob
		// set the nodeList
		ArrayList<String> nodeList = new ArrayList<>();

		nodeList.add(currentNode);
		Enumeration<String> tempEnum = tempEvidenceList.keys();
		while (tempEnum.hasMoreElements()) {
			String node = tempEnum.nextElement();
			nodeList.add(node);

		}

		// set the edgeList
		LinkedList<String> adj[] = new LinkedList[nodeList.size()];
		for (int i = 0; i < nodeList.size(); i++)
			adj[i] = new LinkedList<>();

		tempEnum = tempEvidenceList.keys();
		while (tempEnum.hasMoreElements()) {
			String node = tempEnum.nextElement();
			String causality = tempEvidenceList.get(node);

			if (causality.equals("causal")) {
				int startIndex = nodeList.indexOf(node);
				adj[startIndex].add(currentNode);
			} else {
				int startIndex = nodeList.indexOf(currentNode);
				adj[startIndex].add(node);
			}
		}

		ArrayList<String> targetStateList = new ArrayList<>();
		String taretNodeStateName = Find_target_State_Name(currentNode, conditionedNodeList);

		targetStateList.add(taretNodeStateName);

		b = new Blob(nodeList, adj, tempEvidenceList, uninformedNodeList, tempTargetList, targetStateList, null,
				currentNode);
		return b;
	}

	private static Blob Get_a_CE_Blob(String currentNode, Hashtable<String, String> tempEvidenceList,
			Hashtable<String, String> tempTargetList, Hashtable conditionedNodeList,
			ArrayList<String> uninformedNodeList) throws Exception {
		Blob b = null;

		// set the nodeList
		ArrayList<String> nodeList = new ArrayList<>();

		nodeList.add(currentNode);
		Enumeration<String> tempEnum = tempEvidenceList.keys();
		while (tempEnum.hasMoreElements()) {
			String node = tempEnum.nextElement();
			nodeList.add(node);

		}
		tempEnum = tempTargetList.keys();
		while (tempEnum.hasMoreElements()) {
			String node = tempEnum.nextElement();
			nodeList.add(node);
		}

		// set the edgeList
		LinkedList<String> adj[] = new LinkedList[nodeList.size()];
		for (int i = 0; i < nodeList.size(); i++)
			adj[i] = new LinkedList<>();

		tempEnum = tempEvidenceList.keys();
		while (tempEnum.hasMoreElements()) {
			String node = tempEnum.nextElement();
			String causality = tempEvidenceList.get(node);

			if (causality.equals("causal")) {
				int startIndex = nodeList.indexOf(node);
				adj[startIndex].add(currentNode);
			} else {
				int startIndex = nodeList.indexOf(currentNode);
				adj[startIndex].add(node);
			}
		}
		tempEnum = tempTargetList.keys();
		ArrayList<String> targetList = new ArrayList<>(); // also save those target Names in a list
		ArrayList<String> targetStateList = new ArrayList<>(); // also save those target Names in a list

		while (tempEnum.hasMoreElements()) {
			String node = tempEnum.nextElement();
			String causality = tempTargetList.get(node);
			targetList.add(node);

			String targetStateName = Find_target_State_Name(node, conditionedNodeList);
			targetStateList.add(targetStateName); // by default, we will be considering the first state of the target
			// node as the "STATE OF THE INTEREST"

			if (causality.equals("anti_causal")) {
				int startIndex = nodeList.indexOf(node);
				adj[startIndex].add(currentNode);
			} else {
				int startIndex = nodeList.indexOf(currentNode);
				adj[startIndex].add(node);
			}
		}

		// // set the evidenceList
		// tempEnum = tempEvidenceList.keys();
		// Hashtable EvidenceList = new Hashtable<>();
		// while(tempEnum.hasMoreElements()) {
		// String EVnodeName = tempEnum.nextElement();
		// // now collect all its statenames
		// Hashtable stateValues = getStateValuesofaNode(_net, EVnodeName);
		// EvidenceList.put(EVnodeName, stateValues);
		// }

		// create the blob now and return
		// b = new Blob(nodeList, adj, EvidenceList, uninformedNodeList, targetList,
		// targetStateList, 0);
		b = new Blob(nodeList, adj, tempEvidenceList, uninformedNodeList, targetList, targetStateList, null,
				currentNode);
		return b;
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

	
	// 	the idea of this function is to get the latest CPTs of all the participating nodes (AFTER all evidence nodes are instantiated)  	
	private static Hashtable Construct_Updated_ConditionedList_for_a_NodeList_2(ArrayList<String> nodeList)
			throws Exception {
		Hashtable retList = new Hashtable<>();

		for (int i = 0; i < nodeList.size(); i++) {
			String nodeName = nodeList.get(i);
			
			if(nodeName.startsWith("ubhs92jh_")) {
				Hashtable tempTable = new Hashtable<>();
				tempTable.put("False", 1.0);
				tempTable.put("True", 0.0);
				retList.put(nodeName, tempTable);
				
			}else {
				Hashtable nodeCPT = (Hashtable) FinalBeliefofAllNodes.get(nodeName);
				retList.put(nodeName, nodeCPT);
			}
		}

		return retList;
	}

	private static Hashtable Construct_Updated_ConditionedList_for_a_NodeList(ArrayList<String> nodeList)
			throws Exception {
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
			
			if(nodeName.startsWith("ubhs92jh_")) {
				Hashtable tempTable = new Hashtable<>();
				tempTable.put("False", 1.0);
				tempTable.put("True", 0.0);
				retList.put(nodeName, tempTable);

			}else {
				Hashtable nodeCPT = (Hashtable) originalPriorofAllNodes.get(nodeName);
				retList.put(nodeName, nodeCPT);
			}
		}
		return retList;
	}
}
