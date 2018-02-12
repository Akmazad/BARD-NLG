package Bard.NLG;
import java.util.Hashtable;
import java.util.ArrayList;
import java.util.LinkedList;

public class Blob {
//Graph subNet;
ArrayList<String> subNetNodes = new ArrayList<>();
LinkedList<String>[] subNetAdj;

Hashtable evidenceSet;
ArrayList<String> uninformedPriorSet = new ArrayList<>();
ArrayList<String> queryNodeList = new ArrayList<>();
ArrayList<String> queryNodeStateList = new ArrayList<>();

String CE_nodeName = "";

ArrayList<Double> prior;

	public Blob(ArrayList<String> _subNetNodes, ArrayList<String> BNGraphNodes, LinkedList<String>[] BNAdj, 
			Hashtable _evidenceSet, ArrayList<String> _uninformedPriorSet, ArrayList<String> _queryNode, 
			ArrayList<String> _queryNodeState, ArrayList<Double> _prior, String _ceNodeName) {
		subNetNodes = _subNetNodes;
		
		/* populate subnet */
		subNetAdj = new LinkedList[_subNetNodes.size()];
		for(int i = 0; i < _subNetNodes.size(); i++) {
			int index = BNGraphNodes.indexOf(_subNetNodes.get(i));
			
			subNetAdj[i] = new LinkedList<>();
			LinkedList<String> tempList = BNAdj[index];
			for(int j = 0; j < tempList.size(); j++) {
				subNetAdj[i].add(tempList.get(j));
			}
		}
		
		evidenceSet = new Hashtable<>(_evidenceSet);
		uninformedPriorSet = _uninformedPriorSet;
		queryNodeList = _queryNode;
		queryNodeStateList = _queryNodeState;
		CE_nodeName = _ceNodeName;
		prior = _prior;
	}

	public Blob(ArrayList<String> _subNetNodes, LinkedList<String>[] adj, Hashtable _evidenceSet, 
			ArrayList<String> _uninformedPriorSet, ArrayList<String> _queryNode, ArrayList<String> _queryNodeState, 
			ArrayList<Double> _prior, String _ceNodeName) {
		subNetNodes = _subNetNodes;
		
		/* populate subnet */
		subNetAdj = new LinkedList[_subNetNodes.size()];
		subNetAdj = adj;
		
		evidenceSet = new Hashtable<>(_evidenceSet);
		uninformedPriorSet = _uninformedPriorSet;
		queryNodeList = _queryNode;
		queryNodeStateList = _queryNodeState;
		CE_nodeName = _ceNodeName;
		prior = _prior;
	}
}
