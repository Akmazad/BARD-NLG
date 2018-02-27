package Bard.NLG;
import java.lang.reflect.Array;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Random;
import java.util.Map.Entry;

//import agenariskserver.Node;

public class TextGenerator_Az {

	private Random rand;
	
	private DecimalFormat df = new DecimalFormat("#.#"); // 1-decimal point
	
	public TextGenerator_Az() {
		rand = new Random(1);
	}

	// put all functions here
	public String SayPrior(String nodeName,String stateName,double prob) {
		return SayPriorPrefix() + SayProbNode(nodeName,stateName,prob,"VP") + ". ";
	}

	public String SayPriorPrefix() {
		//Random rand = new Random();
		ArrayList<String> retArr = new ArrayList<>();
		//retArr.add("In general, ");
		retArr.add("In the absence of evidence, ");
		
		return retArr.get(rand.nextInt(retArr.size()));
	}

	public String SayProbNode(String nodeName, String stateName, double prob, String phraseOption) {
		//Random rand = new Random();
		
		if(prob == 1.0) { // this is to wrap SayNode function 
			return SayNode(nodeName, stateName, "NP");
		}
		else {
			if(phraseOption.equals("NP")){
				ArrayList<String> retArr = new ArrayList<>();
				String phrase = "";

				retArr.add(SayNode(nodeName,stateName,phrase) + " with probability " + df.format(prob * 100.0) + "%");
				return retArr.get(rand.nextInt(retArr.size()));
			}
			else if(phraseOption.equals("VP")) {
				ArrayList<String> retArr = new ArrayList<>();
				ArrayList<String> middleText = new ArrayList<>();

				middleText.add("that ");
				//middleText.add("of ");

				boolean adjPhrase = true;
				// this is the default
				//retArr.add("there is " + PutVerbalWord_Az(prob, true, adjPhrase)  + " chance (" + df.format(prob * 100.0) + "%) " + middleText.get(rand.nextInt(middleText.size())) + SayNode(nodeName, stateName, "VP"));
				retArr.add(PutVerbalWord_Az(prob, true, adjPhrase)  + " (" + df.format(prob * 100.0) + "%) " + middleText.get(rand.nextInt(middleText.size())) + SayNode(nodeName, stateName, "VP"));
				retArr.add("the probability of " + SayNode(nodeName, stateName, "NP") + " is " + df.format(prob * 100.0) + "% (" + PutVerbalWord_Az(prob, false, adjPhrase) +  ")");
				
				return retArr.get(rand.nextInt(retArr.size()));
			}
			return "";
		}
	}

	public String SayNode(String nodeName, String stateName, String phrase) {
		if(stateName.equals("") && phrase.equals("")) {
			return nodeName;
		}
		else {
			return (nodeName + "=" + stateName);
		}
	}
	
	public String SayContradict() {;
		ArrayList<String> retArr = new ArrayList<>();
		retArr.add("However, ");
		//retArr.add(" But, ");
		
		return retArr.get(rand.nextInt(retArr.size()));
	}

	public String SayOpposite(String direction) {
		return (direction.equals("increase") ? "decrease":"increase");
	}
	
	public String SayProbNodeList(ArrayList<ArrayList<String>> _nodeInfoList, String phraseOption) {
		String retStr = "";
		if(_nodeInfoList.size() == 1) {
			// first one is the NodeName, second: NodeStateName, third: node Prob.
			retStr += SayProbNode(_nodeInfoList.get(0).get(0), _nodeInfoList.get(0).get(1), Double.parseDouble(_nodeInfoList.get(0).get(2)), phraseOption);
		}
		else {
			int nNodestoTalk = _nodeInfoList.size();
			for(int i = 0; i < (nNodestoTalk - 1); i++) {
				retStr += (SayProbNode(_nodeInfoList.get(i).get(0), _nodeInfoList.get(i).get(1), Double.parseDouble(_nodeInfoList.get(i).get(2)), phraseOption) + ", ");
			}
			retStr = retStr.substring(0,(retStr.length()-2));
			
			retStr += " and " + SayProbNode(_nodeInfoList.get(nNodestoTalk - 1).get(0), _nodeInfoList.get(nNodestoTalk - 1).get(1), Double.parseDouble(_nodeInfoList.get(nNodestoTalk - 1).get(2)), phraseOption);
			
		}
		return retStr;
	}
	
	public String SayNodeList(ArrayList<ArrayList<String>> _nodeInfoList, String phraseOption, String andOR) {
		String retStr = "";
		if(_nodeInfoList.size() == 1) {
			// first one is the NodeName, second: NodeStateName
			retStr += SayNode(_nodeInfoList.get(0).get(0), _nodeInfoList.get(0).get(1), phraseOption);
		}
		else {
			int nNodestoTalk = _nodeInfoList.size();
			for(int i = 0; i < (nNodestoTalk - 1); i++) {
				retStr += (SayNode(_nodeInfoList.get(i).get(0), _nodeInfoList.get(i).get(1), phraseOption) + ", ");
			}
			retStr = retStr.substring(0,(retStr.length()-2));  // getting rid of the last ", "
		
			retStr += ((andOR.equals("and"))? " and ": " or ") + SayNode(_nodeInfoList.get(nNodestoTalk - 1).get(0), _nodeInfoList.get(nNodestoTalk - 1).get(1), phraseOption);
			
		}
		return retStr;
	}
	
	// #################### EXPLAIN-AWAY STATEMENT ###################
	public String Say_Explain_away(ArrayList<ArrayList<String>> _nodeInfoList,
								ArrayList<String> _targetNodeinfo, ArrayList<String> _ceNodeInfo, 
								String direction, String prob) {
		String retStr = "";
		
		// include the targetNode in the "info" list
		ArrayList<ArrayList<String>> tempList = new ArrayList<>(_nodeInfoList);
		tempList.add(_targetNodeinfo);
		
		// first statement
		retStr += SayNodeList(tempList, "", "and"); // empty phraseOption - so for each node, the default: (node=state)
		retStr += SayAlternativeCauses();
		retStr += (SayNode(_ceNodeInfo.get(0), "", "") + ". "); // first index is the CE nodeName
		
		// consequence statement
		retStr += SayConsequence();
		retStr += SayChange(_nodeInfoList,_targetNodeinfo,SayOpposite(direction),direction);
		
		retStr += ((prob.equals("")) ? "": (" to "+ (Double.parseDouble(prob) * 100.0) + "%"));
		
		
		return retStr;
	}
	
	public String Say_Explain_away_Az() {
		String retStr = "";
		
		return retStr;
	}
	
	public String SayChange(ArrayList<ArrayList<String>> _nodeInfoList, ArrayList<String> _targetNodeinfo,
			String OppositeDirection, String direction) {
		String retStr = "";
		
		retStr += SayDirection(OppositeDirection,"NP",0);
		retStr += " in the probability of ";
		retStr += SayNodeList(_nodeInfoList, "", "and");  // empty phraseOption - so for each node, the default: (node=state)
		retStr += " yields ";
		
		retStr += SayDirection(direction,"NP",0);
		retStr += " in the probability of ";
		retStr += (SayNode(_targetNodeinfo.get(0), _targetNodeinfo.get(1), "") + " ");
		
		
		return retStr;
	}

	public String SayDirection(String direction, String phraseOption, Integer number) {
		if(phraseOption.equals("NP")) {
			return (direction.equals("increase")) ? "an increase" : "a decrease";
		}
		else{ // means VP
			String pluralCase = (number == 1) ? "": "s";
			return (direction.equals("increase")) ? "increase"+pluralCase: "decrease"+pluralCase;
		}
	}

	public String SayAlternativeCauses() {
		//Random rand = new Random();
		ArrayList<String> middleText = new ArrayList<>();
		
		middleText.add("causes ");
		middleText.add("explanations ");
		
		return (" are alternative " + middleText.get(rand.nextInt(middleText.size())) + "of ");		
	}
	
	public String SayConsequence() {
		//Random rand = new Random();
		ArrayList<String> retArr = new ArrayList<>();
		retArr.add("Therefore, ");
		retArr.add("Hence, ");
		
		return retArr.get(rand.nextInt(retArr.size()));
	}
	// ##################################################################################
	
	// ##################### CAUSE STATEMENT ############################################
	public String SayCauseRel(ArrayList<ArrayList<String>> _nodeInfoList, 
						ArrayList<String> _targetNodeInfo, String direction, String prob) {
		String retStr = "";
		
		retStr += SayNodeList(_nodeInfoList, "", "and");
		retStr += " causes the probability of ";
		retStr += SayNode(_targetNodeInfo.get(0), _targetNodeInfo.get(1), "");
		retStr += " to ";
		//retStr += SayDirection(direction, "VP", 1);
		retStr += SayDirection(direction, "VP", 0);
		retStr += ((prob.equals("")) ? "": (" to "+ (Double.parseDouble(prob) * 100.0) + "%."));
		return retStr;
	}
	public String SayCanCause(String parent, ArrayList<String> nodeNameListStr) {
		return parent + " can causes " + nodeNameListStr + ". ";
	}
	// ##################################################################################
	
	// ##################### EVIDENCE STATEMENT ############################################
	public String SayEvidenceRel(ArrayList<ArrayList<String>> _nodeInfoList, 
								ArrayList<String> _targetNodeInfo, String direction, String prob) {
		String retStr = "";
		
		retStr += SayNode(_targetNodeInfo.get(0), _targetNodeInfo.get(1), "");
		retStr += " can cause ";
		retStr += (SayNodeList(_nodeInfoList, "", "and") + ". ");
		retStr += SayConsequence();
		retStr += SayChange(_nodeInfoList, _targetNodeInfo, direction, direction);
		retStr += ((prob.equals("")) ? "": ("to "+ (Double.parseDouble(prob) * 100.0) + "%."));
		
		return retStr;
	}
	// ##################################################################################
	
	// ##################### GENERAL IMPLY STATEMENT ############################################
	public String SayImply(ArrayList<ArrayList<String>> _nodeInfoList, 
								ArrayList<String> _targetNodeInfo, String direction, String however) {
		String retStr = "";
		
		retStr += (SayProbNodeList(_nodeInfoList, "NP") + " ");
		//retStr += SayDirection(direction, "VP", _nodeInfoList.size());
		retStr += direction;
		
		if(however.equals("")) {
			retStr += " the probability of ";
			retStr += SayNode(_targetNodeInfo.get(0), _targetNodeInfo.get(1), "NP");
			retStr += ". ";
		}else {
			retStr += " this probability. ";
		}
		
		return retStr;
	}
	// ##################################################################################
	
	// ##################### CONTRADICTION STATEMENT ############################################
	public String SayContradiction(ArrayList<ArrayList<String>> _nodeInfoList_1, ArrayList<ArrayList<String>> _nodeInfoList_2, 
								ArrayList<String> _targetNodeInfo, String direction, double posteriorProb, double impactChange, double priorProb) {
		String retStr = "";
		
		String opDir = SayOpposite(direction,_nodeInfoList_2);
		retStr += ( "Observing " + SayImply(_nodeInfoList_1, _targetNodeInfo, direction, ""));
		//retStr += SayContradict() + "then observing "; 
		retStr += "In light of this, observing "; 
		if(impactChange != 0) {
			retStr += (SayImply(_nodeInfoList_2, _targetNodeInfo, opDir, "however"));
			ArrayList<ArrayList<String>> emptyNodeList_for_NO_effect_through_CPT = new ArrayList<>();
			retStr += (SayImply_no_change(emptyNodeList_for_NO_effect_through_CPT, _targetNodeInfo, ""));
			retStr += SayConclusion(_targetNodeInfo, posteriorProb, priorProb);
		}
		else {
			retStr += (SayImply_no_change(_nodeInfoList_2, _targetNodeInfo, Double.toString(posteriorProb)));
			retStr += SayConclusion(_targetNodeInfo, posteriorProb, priorProb);
		}
		
		return retStr;
	}
	// ##################################################################################

	private String SayOpposite(String direction, ArrayList<ArrayList<String>> _nodeInfoList_2) {
		String retStr = "";
		//boolean plural = (_nodeInfoList_2.size() > 1) ? true:false;
		boolean plural = false;
		
		if(direction.startsWith("increase")) {
			retStr = (plural) ? "decrease":"decreases";
		}
		else {
			retStr = (plural) ? "increase":"increases";
		}
		return retStr;
	}

	public String SayConclusion(ArrayList<String> _targetNodeInfo, double posteriorProb, double priorProb) {
		String retStr = "";
		retStr += "The final probability of ";
		retStr += SayNode(_targetNodeInfo.get(0), _targetNodeInfo.get(1), "NP");
		retStr += " is ";
		retStr += df.format(posteriorProb * 100)  + "% ";
		retStr += "(";
		retStr += (isPriorPosteriorSame(posteriorProb,priorProb)) ? "still ":"";
		boolean adjPhrase = true;
		//retStr += (posteriorProb.equals("")) ? "" : PutVerbalWord_Az(Double.parseDouble(posteriorProb), false, adjPhrase) + " (" + df.format(Double.parseDouble(posteriorProb)*100.0)  + "%).";
		retStr += PutVerbalWord_Az(posteriorProb, false, adjPhrase) + ").";
		
		return retStr;
	}
	
	private boolean isPriorPosteriorSame(double posteriorProb, double priorProb) {
		double priorVal = priorProb * 100;
		double postVal = posteriorProb * 100;
		if(priorVal == 0 && postVal == 0)
			return true;
		else if((priorVal > 0 && priorVal < 15) && (postVal > 0 && postVal < 15 ))
			return true;
		else if((priorVal >= 15 && priorVal < 40) && (postVal >= 15 && postVal < 40 ))
			return true;
		else if((priorVal >= 40 && priorVal < 60) && (postVal >= 40 && postVal < 60 ))
			return true;
		else if((priorVal >= 60 && priorVal < 85) && (postVal >= 60 && postVal < 85 ))
			return true;
		else if((priorVal >= 85 && priorVal < 100) && (postVal >= 85 && postVal < 100 ))
			return true;
		else if((priorVal == 100) && (postVal == 100))
			return true;
		else
			return false;		
	}

	public String PutVerbalWord_Az(double probVal, boolean withArticle, boolean adjPhrase) {
		probVal = probVal * 100;
    	if(adjPhrase) {
		if(probVal == 0) 
			return (withArticle)? "it is impossible":"impossible";
		else if(probVal < 15) 
			return (withArticle)? "there is almost certainly no chance":"almost certainly not";
		else if(probVal < 40) 
			return (withArticle)? "it is probably not the case":"improbable";
		else if(probVal < 60) 
			return (withArticle)? "the chances are about even":"roughly even chance";
		else if(probVal < 85) 
			return (withArticle)? "it is probable":"probable";
		else if(probVal < 100) 
			return (withArticle)? "it is almost certain":"almost certain";
		else
			return (withArticle)? "it is certain":"certain";
    	}else {
    		if(probVal == 0) 
    			return "impossibly";
    		else if(probVal < 15) 
    			return "almost uncertainly";
    		else if(probVal < 40) 
    			return "improbably";
    		else if(probVal < 60) 
    			return "roughly evenly";
    		else if(probVal < 85) 
    			return "probably";
    		else if(probVal < 100) 
    			return "almost certainly";
    		else
    			return "certainly";
    	}
    		
	}

	public String SayImply_no_change(ArrayList<ArrayList<String>> _nodeInfoList, ArrayList<String> _targetNodeInfo,
			String prob) {
		String retStr = "";

		if(BaseModule.blockedEvidenceNodeInfoList.size() > 0) {
			_nodeInfoList.addAll(BaseModule.blockedEvidenceNodeInfoList);
		}
		
		if(_nodeInfoList.size() > 0) {
			retStr += (SayNodeList(_nodeInfoList, "NP", "or") + " ");
			//retStr += SayDirection(direction, "VP", _nodeInfoList.size());

			retStr += (_nodeInfoList.size() > 1) ? "have":"has"; 

			retStr += " no effect on the probability of ";
			//		retStr += (prob.equals("")) ?
			//				(SayNode(_targetNodeInfo.get(0), _targetNodeInfo.get(1), "NP")) :
			//				(SayProbNode(_targetNodeInfo.get(0), _targetNodeInfo.get(1), Double.parseDouble(_targetNodeInfo.get(2)), "NP"));
			//		
			retStr += (SayNode(_targetNodeInfo.get(0), _targetNodeInfo.get(1), "NP") + " in this scenario. ");
			//retStr += ". Therefore, the final value"
			//		retStr += (prob.equals("")) ?
			//				"": "Therefore, the final probability is " + df.format(Double.parseDouble(prob)*100.0)  + "%.";
		}
		return retStr;
	}

	
}
