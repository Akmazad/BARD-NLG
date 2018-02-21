package Bard.NLG.Generator;

import Bard.NLG.Printer.Printer;

import java.text.DecimalFormat;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import Bard.NLG.TextGenerator_Az;
import Bard.NLG.Generator.StructureAnalyser.Rule;

import static Bard.NLG.Generator.NodeInfo.DIRECTION.DECREASE;
import static Bard.NLG.Generator.NodeInfo.DIRECTION.INCREASE;
import static Bard.NLG.Generator.NodeInfo.DIRECTION.NEUTRAL;


public class TextGenerator {

    public static final boolean DEBUG = true;

    public static final String[] CONNECTORS = {"in addition,", "also,"};

    public static int CONNECTOR_IDX = 0;
    
    public static boolean fake_it_philip = false;

    public String getConnector() {
        String res = CONNECTORS[CONNECTOR_IDX];
        CONNECTOR_IDX++;
        CONNECTOR_IDX = CONNECTOR_IDX % 2;
        return res;
    }

    // --- --- --- Fields

    // Formatting
    private DecimalFormat decimalFormat = new DecimalFormat("#.#");
    Printer printer = new Printer();

    // All the segments
    public List<Segment> segments;

    // The final target we want to explain
    public String ultimateTarget;

    // All the node info
    public Set<NodeInfo> allNodeInfos;


    //
    //public final Map<String, String> mapIdName;


    // --- --- --- Constructor

    public TextGenerator() {
    	
    }
    public TextGenerator(List<Segment> segments, boolean fake_it_Philip) {
        this.segments = segments;
        //this.ultimateTarget = segments.get(segments.size() - 1).segment.target;
        this.ultimateTarget = FindUltimateTarget(segments); 
        // (t1, t2)->t1 ! handle duplicated key with merge function. Should be same name so just choose one.
        //this.mapIdName = segments.stream().flatMap(s -> s.getMapIdName().entrySet().stream()).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (t1, t2) -> t1));
        this.allNodeInfos = segments.stream().flatMap(s -> s.getNodeInfos().stream()).collect(Collectors.toSet());
        this.fake_it_philip = fake_it_Philip;
    }

    public TextGenerator(List<Segment> segments) {
        this.segments = segments;
        //this.ultimateTarget = segments.get(segments.size() - 1).segment.target;
        this.ultimateTarget = FindUltimateTarget(segments); 
        // (t1, t2)->t1 ! handle duplicated key with merge function. Should be same name so just choose one.
        //this.mapIdName = segments.stream().flatMap(s -> s.getMapIdName().entrySet().stream()).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (t1, t2) -> t1));
        this.allNodeInfos = segments.stream().flatMap(s -> s.getNodeInfos().stream()).collect(Collectors.toSet());
    }

    private String FindUltimateTarget(List<Segment> AllSegments) {
		String target = "";
		for(Segment seg: AllSegments){
			NodeInfo tempTargetInfo = seg.getTarget();
			if(tempTargetInfo.isUltimateTarget)
				target = tempTargetInfo.nodeName;
		}
		return target;
	}

	// --- --- --- Public Methods

    public String getText() {
    	
    	if(fake_it_philip) {
    		printer.openTag("div").onTop(t -> t.addAttribute("class", "container")).srcnl();
            printer.addH1("Details").srcnl();
            printPreamble();
            printer.srcnl();

            printer.closeTag("div");
            return printer.realize();
    	}
    	
        printer.openTag("div").onTop(t -> t.addAttribute("class", "container")).srcnl();
        //printer.addH1("Details").srcnl();
        //printPreamble();
        //printer.srcnl();
        
        printReasoning_L0();
        //printReasoning();
        printer.closeTag("div");
        return printer.realize();
    }


    // --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- ---
    // PREAMBLE
    // --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- ---

    // --- --- --- Tools

    // --- --- --- Preamble

    private void printPreamble() {
        //printer.addH2("Preamble");
    	printer.addH2("Bayesian Network Structure");

    	if(segments.size() == 0) {
    		printer.openPar().addTextRaw("NO TEXT").closePar();    		
    	}else {
        // --- --- --- Preamble construction:
        // We have the following relation with respect to the target:
        // CAUSAL: target <- node                              (node causes target)
        // ANTICAUSAL: target -> node                          (target causes node)
        // COMMON EFFECT: target -> effect <- alternate cause  (target causes node)
        // * Anticausal & Common effect are treated together.
        // * When the target of the current segment is causing the target in the next segment, mention it
        //   as an ANTICAUSAL (current target causes next target)
        // * If a "link" has already been mentioned, do not mentioned it again.
        //   We only check causal direction. We use an "already seen" filter:
        HashSet<Edge> causalEdges = new HashSet<Edge>();


        // --- --- --- For each segment
        for (int i = 0; i < segments.size(); ++i) {

            // --- --- --- Gather the information about the edges, filter them.
            Segment currSeg = segments.get(i);
            NodeInfo currTarget = currSeg.getTarget();


            // CAUSAL: Keep edges C -> T not already seen.
            // Then, add the new edges in the set of seen edges.
            List<NodeInfo> causeTarget =
                    currSeg.getCausal().stream()
                            .filter(c -> !causalEdges.contains(new Edge(c, currTarget)))
                            .collect(Collectors.toList());

            causeTarget.forEach(ni -> causalEdges.add(new Edge(ni, currTarget)));


            // ANTICAUSAL: Keep edges T -> C not already seen
            // Then, add the new edges in the set of seen edges.
            List<NodeInfo> causedByTarget =
                    currSeg.getAntiCausal().stream()
                            .filter(ac -> !causalEdges.contains(new Edge(currTarget, ac)))
                            .collect(Collectors.toList());

            // Az
            for(int iter = 0; iter < causedByTarget.size(); iter++) {
            	NodeInfo _node = causedByTarget.get(iter);
            	if(_node.nodeName.startsWith("ubgs92jh"))
            		causedByTarget.remove(_node);
            }
            
            causedByTarget.forEach(ni -> causalEdges.add(new Edge(currTarget, ni)));


            // COMMON EFFECT: Always say alternate causes, i.e. no filter.
            // Still add the links in the set of seen edges
            Map<NodeInfo, Set<NodeInfo>> commonEffect = currSeg.getCommonEffect();

            commonEffect.forEach((effect, altCauses) -> {
                causalEdges.add(new Edge(currTarget, effect));
                altCauses.forEach(c -> causalEdges.add(new Edge(c, effect)));
            });


            // FORWARD LOOKING: look in the next segments for causal edges currentTarget -> nextTarget
            // Pull them back in 'causedByTarget'
            
//            for (int j = i + 1; j < segments.size(); ++j) {
//                Segment nextSegment = segments.get(j);
//                NodeInfo nextTarget = nextSegment.getTarget();
//                Edge t_nt = new Edge(currTarget, nextTarget);
//                if (nextSegment.getCausal().contains(currTarget) && !causalEdges.contains(t_nt)) {
//                    causedByTarget.add(nextTarget);
//                    causalEdges.add(t_nt);
//                }
//            }

            // --- --- --- Text Generation

            // Do connection between parts
            boolean connect = false;

            printer.openPar();

            // CAUSAL: EFFECTS -> TARGET
            if (!causeTarget.isEmpty()) {
                connect = true;
                //
                printer.sentenceForceIN(); // Starting with nodes, so we are in a sentence
                printListNode(causeTarget, this::printNode, "and");
                printer.addVerbModal("cause", Printer.Mode.CAN);
                printNode(currTarget);
                //printer.eos().lineBreak();
                printer.eos();
            }


            // ANTICAUSAL: EFFECTS <- TARGET
            if (!causedByTarget.isEmpty()) {
                // Connect
                if (connect) {
                    printer.addText(getConnector());
                }
                connect = true;
                //
                printer.sentenceForceIN(); // Starting with nodes, so we are in a sentence
                printNode(currTarget);
                printer.addVerbModal("cause", Printer.Mode.CAN);
                printListNode(causedByTarget, this::printNode, "and");
                //printer.addVerb("indicate", causedByTarget);
                //printer.addVerb("indicate", causedByTarget);
                //printer.addText((causedByTarget.size() > 1)? " are effects of ":" is an effect of ");
               
                //printer.eos().lineBreak();
                printer.eos();
            }

            // COMMON EFFECT
            if (!commonEffect.isEmpty()) {
                // For each key, i.e. each common effect
                for (NodeInfo effect : commonEffect.keySet()) {
                    List<NodeInfo> altCauses = new ArrayList<NodeInfo>(commonEffect.get(effect));

                    // Connect
                    if (connect) {
                        printer.addText(getConnector());
                    }
                    connect = true;

                    // Current target can cause effect, ...
                    printer.sentenceForceIN(); // Starting with nodes, so we are in a sentence
                    printNode(currTarget);
                    printer.addVerbModal("cause", Printer.Mode.CAN);
                    printNode(effect);
                    printer.addTextRaw(", and");

                    // ... and [list of altcauses] is/are ...
                    printListNode(altCauses, this::printNode, "and");
                    printer.addVerb("be", altCauses);

                    // alternative(s) causes of effect
                    if (altCauses.size() < 2) {
                        printer.addText("an other cause of");
                    } else {
                        printer.addText("other causes of");
                    }
                    printNode(effect);
                    //printer.eos().lineBreak();
                    printer.eos();
                }
            }
            printer.closePar();
        }


        // --- --- --- After all the segments: create a table
        printer.srcnl()
        .addH2("Initial and Updated Probabilities")
                .openPar()
                .addText("The following table contains the initial probabilities in the absence of " + 
                		"evidence, and the updated probabilities after the evidence has been " + 
                		"considered").eos().srcnl()
                .closePar();

        printer.openTable();
        // Header
//        printer.openTableHead().openTableRow()
//                .tableHeader("Node = State").onLast(Printer.Comp::doRaw)
//                .tableHeader("Probability").onLast(Printer.Comp::doRaw)
//                .closeTableRow().closeTableHead();
        
        printer.openTableHead().openTableRow()
        .tableHeader("Type").onLast(Printer.Comp::doRaw)
        .tableHeader("Variable = State").onLast(Printer.Comp::doRaw)
        .tableHeader("Initial Probability").onLast(Printer.Comp::doRaw)
        .tableHeader("Updated Probability").onLast(Printer.Comp::doRaw)
        .closeTableRow().closeTableHead();

        
        
        // Body
        printer.openTableBody();
        allNodeInfos.stream().sorted(Comparator.comparing(a -> a.nodeName)).sorted(Comparator.comparing(a -> a.nodeName.equals(ultimateTarget) ? 0 : a.isEvidence() ? 1 : 2)).forEach(ni -> {
        	
        	if(!ni.nodeName.startsWith("ubgs92jh")) {
        	
	        	printer.openTableRow();
	
	            String typeLabel = "Ordinary";
	            if (ni.nodeName.equals(ultimateTarget)) {
	            	typeLabel  = "Target";
	            }
	            else if (ni.isEvidence()) {
	            	typeLabel = "Evidence";
	            }
	            printer.tableData(typeLabel).onLast(Printer.Comp::doRaw);
	            
	            printer.openTableData();
	            printNodeState(ni);
	            printer.closeTableData();
	
	            boolean adjPhrase = true;
//	            printer.tableData(PutVerbalWord_Az(ni.prior, false, adjPhrase) + " (" + getPercentFormat(ni.prior) + "%)").onLast(Printer.Comp::doRaw);
//	            printer.tableData(PutVerbalWord_Az(ni.posterior, false, adjPhrase) + " (" + getPercentFormat(ni.posterior) + "%)").onLast(Printer.Comp::doRaw);
	            
	            printer.tableData(getPercentFormat(ni.prior) + "% (" + PutVerbalWord_Az(ni.prior, false, adjPhrase) + ")").onLast(Printer.Comp::doRaw);
	            printer.tableData(getPercentFormat(ni.posterior) + "% (" + PutVerbalWord_Az(ni.posterior, false, adjPhrase) + ")").onLast(Printer.Comp::doRaw);

	
	            printer.closeTableRow();
        	}
        });
        printer.closeTableBody();
        // End of table
        printer.closeTable();
    }

    }

    public String printBNTextwithCausalityOnly(List<Rule> orderedText, Set<NodeInfo> allNodeInfoList) {
    	//printer.addH2("Preamble");
    	printer.addH2("Bayesian Network Structure");

    	if(orderedText.size() == 0) {
    		printer.openPar().addTextRaw("There are no dependencies between the nodes in this BN.").closePar();    		
    	}else {
    		for (int i = 0; i < orderedText.size(); ++i) {
    			// --- --- --- Gather the information about the edges, filter them.
    			String currText = GetTextFromRule(orderedText.get(i));
    			// --- --- --- Text Generation
    			printer.openPar();
    			printer.addText(currText).eos();
    			printer.closePar();
    		}
    	}
    	// --- --- --- After all the segments: create a table
    	printer.srcnl()
    	.addH2("Initial and Updated Probabilities")
    	.openPar()
    	.addText("The following table contains the initial probabilities in the absence of " + 
    			"evidence, and the updated probabilities after the evidence has been " + 
    			"considered").eos().srcnl()
    	.closePar();

    	printer.openTable();
    	printer.openTableHead().openTableRow()
    	.tableHeader("Type").onLast(Printer.Comp::doRaw)
    	.tableHeader("Variable = State").onLast(Printer.Comp::doRaw)
    	.tableHeader("Initial Probability").onLast(Printer.Comp::doRaw)
    	.tableHeader("Updated Probability").onLast(Printer.Comp::doRaw)
    	.closeTableRow().closeTableHead();

    	// Body
    	printer.openTableBody();
    	allNodeInfoList.stream().sorted(Comparator.comparing(a -> a.nodeName)).sorted(Comparator.comparing(a -> a.isUltimateTarget ? 0 : a.isEvidence ? 1 : 2)).forEach(ni -> {
    		printer.openTableRow();

    		String typeLabel = "Ordinary";
    		if (ni.isUltimateTarget) {
    			typeLabel  = "Target";
    		}
    		else if (ni.isEvidence) {
    			typeLabel = "Evidence";
    		}
    		printer.tableData(typeLabel).onLast(Printer.Comp::doRaw);

    		printer.openTableData();
    		printNodeState(ni);
    		printer.closeTableData();

    		boolean adjPhrase = true;
    		printer.tableData(getPercentFormat(ni.prior) + "% (" + PutVerbalWord_Az(ni.prior, false, adjPhrase) + ")").onLast(Printer.Comp::doRaw);
    		printer.tableData(getPercentFormat(ni.posterior) + "% (" + PutVerbalWord_Az(ni.posterior, false, adjPhrase) + ")").onLast(Printer.Comp::doRaw);


    		printer.closeTableRow();
    	});
    	printer.closeTableBody();
    	// End of table
    	printer.closeTable();

    	return printer.realize();
    }


    private String GetTextFromRule(Rule rule) {
		String retStr = "";
		int cSource = rule.sources.size();
		int cTarget = rule.targets.size();
		
		if(cSource == 1) {
			retStr += rule.sources.get(0);
		}else {
			for(int i = 0; i < (cSource - 1); i++) {
				retStr += (rule.sources.get(i) + ", ");
			}
			retStr = retStr.substring(0, retStr.length() - 2);
			retStr += (" and " + rule.sources.get(cSource-1));
		}

		retStr += " can cause ";
		if(cTarget == 1)
			retStr += rule.targets.get(0);
		else {
			for(int i = 0; i < (cTarget - 1); i++) {
				retStr += (rule.targets.get(i) + ", ");
			}
			retStr = retStr.substring(0, retStr.length() - 2);
			retStr += (" and ") + rule.targets.get(cTarget-1);
		}

		return retStr;
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


    // --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- ---
    // SPECIFIC
    // --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- ---

    // --- --- --- Tools

    private int printAnterior(List<NodeInfo> nis, String segTarget) {
        // --- --- --- Gathering info
        // Evidence and non evidence nodes
    	
        List<NodeInfo> EV = nis.stream().filter(n -> (n.isEvidence && !n.nodeName.equals(segTarget))).sorted(Comparator.comparing(a -> a.nodeName)).collect(Collectors.toList());
        List<NodeInfo> nonEV = nis.stream().filter(n -> (!n.isEvidence && !n.nodeName.equals(segTarget))).collect(Collectors.toList());
        // --- Non Evidence: 3 possible directions
        List<NodeInfo> inc = nonEV.stream().filter(n -> n.getDirection() == INCREASE).sorted(Comparator.comparing(a -> a.nodeName)).collect(Collectors.toList());
        List<NodeInfo> dec = nonEV.stream().filter(n -> n.getDirection() == DECREASE).sorted(Comparator.comparing(a -> a.nodeName)).collect(Collectors.toList());
        List<NodeInfo> neu = nonEV.stream().filter(n -> n.getDirection() == NEUTRAL).sorted(Comparator.comparing(a -> a.nodeName)).collect(Collectors.toList());

        boolean connect = false;

        // Observing the evidences ...
        if (!EV.isEmpty()) {
            printer.addText("Observing");
            printListNode(EV, this::printNodeState, "and");
            connect = true;
        }

        // [, and] the increases
        if (!inc.isEmpty()) {
            if (connect) {
                printer.addTextRaw(", and");
                printer.addText("the increase in the probability of");
            }else {
            	printer.addText("The increase in the probability of");
            }
            
            printListNode(inc, this::printNodeState, "and");
            connect = true;
        }

        // [, and] the decreases
        if (!dec.isEmpty()) {
            if (connect) {
                printer.addTextRaw(", and");
                printer.addText("the decrease in the probability of");
            }else {
            	printer.addText("The decrease in the probability of");
            }
            
            printListNode(dec, this::printNodeState, "and");
            connect = true;
        }

        // [, together with] the unchanged probability
        if (!neu.isEmpty()) {
            if (connect) {
                printer.addTextRaw(", together with");
                printer.addText("the unchanged probability of");
            }else {
            	printer.addText("The unchanged probability of");
            }
            
            printListNode(dec, this::printNodeState, "and");
        }

        // Check the number of the verb.
        // By default, consider the mix of EV and nonEV: plural.
        int number = 2;
        if (nonEV.isEmpty()) {
            // Only evidence = only one observing = singular
            number = 1;
        } else if (EV.isEmpty()) {
            // We have "non evidence" nodes, but no evidence. Number = number of "non evidence" nodes.
            number = nonEV.size();
        }

        return number;
    }

    private void doCause(NodeInfo.DIRECTION direction, NodeInfo currentTarget, List<NodeInfo> causes) {
        int number = printAnterior(causes, currentTarget.nodeName);
        printer.addVerb("cause", number);
        switch (direction) {
            case INCREASE:
                printer.addText("an increase");
                break;
            case DECREASE:
                printer.addText("a decrease");
                break;
            case NEUTRAL:
                break;
        }
        printer.addText("in the probability of");
        printNodeState(currentTarget);
        printer.eos();
    }

    private void doAntiCause(NodeInfo.DIRECTION direction, NodeInfo currentTarget, List<NodeInfo> antiCauses) {
        int number = printAnterior(antiCauses, currentTarget.nodeName);
        switch (direction) {
            case INCREASE:
                printer.addVerb("increase", number);
                break;
            case DECREASE:
                printer.addVerb("decrease", number);
                break;
            case NEUTRAL:
                break;
        }
        printer.addText("the probability of");
        printNodeState(currentTarget);
        printer.eos();
    }

    private void doCommonEffect(NodeInfo.DIRECTION direction, NodeInfo currentTarget, Map<NodeInfo, Set<NodeInfo>> commonEffect) {

    }

    private void doBatch(Segment segment, NodeInfo currentTarget, NodeInfo.DIRECTION direction,
                         List<NodeInfo> causes, List<NodeInfo> antiCauses, Map<NodeInfo, Set<NodeInfo>> commonEffect,
                         Set<NodeInfo> seenNodesFilter) {

        // Test if we have to do anything: do not look at alternate causes are they are linked to their effect.
        // This is also why we cannot directly look at the batch.
        if (!causes.isEmpty() || !antiCauses.isEmpty() || !commonEffect.keySet().isEmpty()) {
            // --- --- --- Open a new row, add the label in the first column, open the 2nd column for the text.
            printer.openTableRow().openTableData();
            switch (direction) {
                case INCREASE:
                    printer.addTextRaw("Support");
                    break;
                case DECREASE:
                    printer.addTextRaw("Detract");
                    break;
                case NEUTRAL:
                    break;
            }
            printer.closeTableData().openTableData();





            // --- --- --- Generate the text

            // Do connection between parts
            boolean connect = false;

            // CAUSES
            if (!causes.isEmpty()) {
                // Connect
                connect = true;
                //
                doCause(direction, currentTarget, causes);
            }

            // ANTICAUSES
            if (!antiCauses.isEmpty()) {
                // Connect
                if (connect) {
                    printer.addText(getConnector());
                }
                connect = true;
                //
                doAntiCause(direction, currentTarget, antiCauses);
            }

            // COMMON EFFECT

            // --- Filtering seen nodes - prevent conflicting node influence not handled by current batches
            seenNodesFilter.addAll(causes);
            seenNodesFilter.addAll(antiCauses);

            for (NodeInfo effect : commonEffect.keySet()) {
                // Connect
                if (connect) {
                    printer.addText(getConnector());
                }
                connect = true;

                // --- --- --- Text

                // AntiCause
                doAntiCause(direction, currentTarget, Collections.singletonList(effect));
                // Update the filters
                seenNodesFilter.add(currentTarget);

                // AltCauses depending on the batches, filtering out seen nodes
                List<NodeInfo> alt_Causes = new ArrayList<>(commonEffect.get(effect));
                alt_Causes.removeAll(seenNodesFilter);
                // Update the filter
                seenNodesFilter.addAll(commonEffect.get(effect));

                // Reverse direction and verb selection
                NodeInfo.DIRECTION reverseDirection = NEUTRAL; // default, with illegal value
                String verbFurther = null;
                String verbHowever = null;

                // Reverse direction and verb selection
                switch (direction) {
                    case INCREASE:
                        verbFurther = "increase";
                        verbHowever = "decrease";
                        reverseDirection = DECREASE;
                        break;
                    case DECREASE:
                        verbFurther = "decrease";
                        verbHowever = "increase";
                        reverseDirection = INCREASE;
                        break;
                    case NEUTRAL:
                        // Should not happen !
                        throw new IllegalArgumentException("Can not call doBatch with NEUTRAL direction");
                }

                // Cases
                List<NodeInfo> further = filter(segment, alt_Causes, direction);
                List<NodeInfo> neutral = filter(segment, alt_Causes, NEUTRAL);
                List<NodeInfo> however = filter(segment, alt_Causes, reverseDirection);

                // Do further
                if(!further.isEmpty()){
                    List<NodeInfo> allALT = new ArrayList<>(further);
                    allALT.add(0, currentTarget); // Add the target as alternate causes

                    // ... since [target + alt] are alternative causes of [effect],
                    printer.addText(getConnector()).addText("since");
                    printListNode(allALT, this::printNode, "and");
                    printer.addText("are alternative causes of");
                    printNode(effect);
                    printer.addTextRaw(",");

                    // [observing/the increase/the decrease of alt] further [verb further] the probability of [target]
                    int number = printAnterior(further, currentTarget.nodeName);
                    printer.addText("further");
                    printer.addVerb(verbFurther, number);
                    printer.addText("the probability of");
                    printNodeState(currentTarget);
                    printer.eos();
                }

                // Do neutral
                if(!neutral.isEmpty()){
                    List<NodeInfo> allALT = new ArrayList<>(neutral);
                    allALT.add(0, currentTarget); // Add the target as alternate causes

                    // Even though [target + alt] are alternative causes of [effect],
                    printer.addText("Even though");
                    printListNode(allALT, this::printNode, "and");
                    printer.addText("are alternative causes of");
                    printNode(effect);

                    // in light of [effect], [alt] have no effect on the probability of [target]
                    printer.addText(", in light of");
                    printNodeState(effect);
                    printer.addTextRaw(",");
                    printListNode(neutral, this::printNodeState, "and");
                    printer.addVerb("have", neutral).addText("no effect on the probability of");
                    printNodeState(currentTarget);
                    printer.eos();
                }

                // Do However
                if(!however.isEmpty()){
                    List<NodeInfo> allALT = new ArrayList<>(however);
                    allALT.add(0, currentTarget); // Add the target as alternate causes

                    // However, since [target + alt] are alternative causes of [effect],
                    printer.addText("however, since");
                    printListNode(allALT, this::printNode, "and");
                    printer.addText("are alternative causes of");
                    printNode(effect);
                    printer.addTextRaw(",");

                    // [observing/the increase/the decrease of alt] [verb however] the probability of [target]
                    int number = printAnterior(however, currentTarget.nodeName);
                    printer.addVerb(verbHowever, number);
                    printer.addText("the probability of");
                    printNodeState(currentTarget);
                    printer.eos();
                }

            }

            // --- --- --- Close the 2nd column and the row
            printer.closeTableData().closeTableRow();

        }
    }

    private void doConclusion_L0(NodeInfo currentTarget) {
    	printer.openTableRow().openTableData().addTextRaw("Conclusion").closeTableData();
        printer.openTableData();
        //printer.addText("(" + getPercentFormat(currentTarget.posterior) + "%)").eos();
        
        
        
        printer.addText("The probability of");
        printNode(currentTarget); 
        printer.addText("=");
        printState(currentTarget);
        String verbalWord = PutVerbalWord_Az(currentTarget.posterior, false, true);
        //verbalWord = verbalWord.equals("roughly even")?verbalWord+" chance":verbalWord;	// concate "chances" if it is "roughly even"
        printer.addText("is " + getPercentFormat(currentTarget.posterior) + "% (" + verbalWord + ")").eos();

        printer.closeTableData().closeTableRow();
	}

    private void doConclusion(NodeInfo currentTarget) {
        printer.openTableRow().openTableData().addTextRaw("Conclusion").closeTableData();
        printer.openTableData();
        printNodeVerbalState(currentTarget);
        printer.addText("(" + getPercentFormat(currentTarget.posterior) + "%)").eos();

        /*
        printer.addText("The resultant probability of");
        printNode(currentTarget);
        printer.addText("is " + getPercentFormat(currentTarget.posterior) + "%").eos();
        */

        printer.closeTableData().closeTableRow();
    }


    // --- --- --- Reasoning

    private void printReasoning_L0() {

        printer.addH2("Reasoning");

        // ------------ Get all the evidence nodes in all the segments
        Set<NodeInfo> allEvidenceinBN = new HashSet<NodeInfo>();
        segments.forEach(seg -> allEvidenceinBN.addAll(seg.getNodeInfos().stream().
        		filter(NodeInfo::isEvidence).sorted(Comparator.comparing(a -> a.nodeName)).collect(Collectors.toSet())));
        
        // --- ---- --- Per segment
        for (int i = 0; i < segments.size(); ++i) {

            // The segment
            Segment seg = segments.get(i);

            // Current target:
            NodeInfo currentTarget = seg.getTarget();

            // All Evidence nodes sorted alphabetically.
            List<NodeInfo> evnodes = seg.getNodeInfos().stream()
                    .filter(NodeInfo::isEvidence).sorted(Comparator.comparing(a -> a.nodeName))
                    .collect(Collectors.toList());

            // --- --- --- Printing:
            printer.addH3("Step " + (i + 1));
            printer.openTable().openTableBody();

            // --- --- --- Result variable: target of the current segment
            printer.openTableRow()
                    .openTableData().addTextRaw("Result variable:").closeTableData()
                    .openTableData();
            printNode(currentTarget);
            printer.closeTableData().closeTableRow();

            // --- --- --- List of observed Nodes:
            printer.openTableRow()
            .openTableData().addTextRaw("Observed:").closeTableData()
            .openTableData().sentenceForceIN(); // Ensure we are not starting a sentence inside the following list
            if (!evnodes.isEmpty()) {
                printListNode(evnodes, this::printNodeState, "and");
            }else {
            	printer.addText("(empty)");
            }
            printer.closeTableData().closeTableRow();
            printer.sentenceForceOUT();

            // IMMEDIATE INFLUENCES
            printer.openTableRow()
            .openTableData().addTextRaw("Immediate influences:").closeTableData()
            .openTableData().sentenceForceIN(); // Ensure we are not starting a sentence inside the following list
            int number = printAnterior(new ArrayList(seg.getNodeInfos()),currentTarget.nodeName);
            printer.closeTableData().closeTableRow();
            printer.sentenceForceOUT();

            // OTHER INFLUENCES [NOT MENTIONED YET]
            printer.openTableRow()
            .openTableData().addTextRaw("Other Influences [not mentioned yet]:").closeTableData()
            .openTableData().sentenceForceIN(); // Ensure we are not starting a sentence inside the following list
            allEvidenceinBN.removeAll(evnodes);	// asymmetric set difference: allEvidenceinBN \ evnodes
            if (!allEvidenceinBN.isEmpty()) {
                printListNode(new ArrayList<NodeInfo>(allEvidenceinBN), this::printNodeState, "and");
            }else {
            	printer.addText("(empty)");
            }
            printer.closeTableData().closeTableRow();
            printer.sentenceForceOUT();
            
            // CONCLUSION
            doConclusion_L0(currentTarget);
            // --- --- --- Complete the table
            printer.closeTableBody().closeTable().horizontalRule();
        }

    }

    
	private void printReasoning() {

        printer.addH2("Reasoning");

        // --- ---- --- Per segment
        for (int i = 0; i < segments.size(); ++i) {

            // --- --- --- Gather information:

            // The segment
            Segment seg = segments.get(i);

            // Current target:
            // also check if we start with detract or support
            NodeInfo currentTarget = seg.getTarget();

            // By default, we suppose that the target is decreasing: start with support and conclude with detract
            boolean startWithDetract = false;
            if (currentTarget.getPrior() < currentTarget.getPosterior()) {
                // The value of the target actually increases: start with detract and conclude with support
                startWithDetract = true;
            }

            // All Evidence nodes sorted alphabetically.
            List<NodeInfo> evnodes = seg.getNodeInfos().stream()
                    .filter(NodeInfo::isEvidence).sorted(Comparator.comparing(a -> a.nodeName))
                    .collect(Collectors.toList());


            // --- --- SUPPORT
            // --- CAUSES
            List<NodeInfo> supportCauses = seg.getCausal().stream().filter(seg::isSupporting).collect(Collectors.toList());
            // --- ANTICAUSES (excluding common effects)
            List<NodeInfo> supportAntiCauses = seg.getAntiCausal().stream().filter(seg::isSupporting).collect(Collectors.toList());
            // --- COMMON EFFECTS: the effect is supporting but the alternate cause might be detracting
            Map<NodeInfo, Set<NodeInfo>> supportCommonEffect =
                    seg.getCommonEffect().entrySet().stream()
                            .filter(e -> seg.isSupporting(e.getKey()))
                            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));


            // --- --- DETRACT
            // --- CAUSES:
            List<NodeInfo> detractCauses = seg.getCausal().stream().filter(seg::isDetracting).collect(Collectors.toList());
            // --- ANTICAUSES (excluding common effects)
            List<NodeInfo> detractAntiCauses = seg.getAntiCausal().stream().filter(seg::isDetracting).collect(Collectors.toList());
            // --- COMMON EFFECTS: the effect is supporting but the alternate cause might be detracting
            Map<NodeInfo, Set<NodeInfo>> detractCommonEffect =
                    seg.getCommonEffect().entrySet().stream()
                            .filter(e -> seg.isDetracting(e.getKey()))
                            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));


            // --- --- --- Printing:
            printer.addH3("Step " + (i + 1));
            printer.openTable().openTableBody();

            // --- --- --- Result variable: target of the current segment
            printer.openTableRow()
                    .openTableData().addTextRaw("Result variable").closeTableData()
                    .openTableData();
            printNode(currentTarget);
            printer.closeTableData().closeTableRow();

            // --- --- --- List of evidence:
            if (!evnodes.isEmpty()) {
                printer.openTableRow()
                        .openTableData().addTextRaw("Evidence").closeTableData()
                        .openTableData().sentenceForceIN(); // Ensure we are not starting a sentence inside the following list
                printListNode(evnodes, this::printNodeState, "and");
                printer.closeTableData().closeTableRow();
            }

            printer.sentenceForceOUT();

            Set<NodeInfo> seenNodes = new HashSet<>();

            // --- --- --- No decide if we first do the detract or the support
            if (startWithDetract) {
                // DETRACT - SUPPORT - CONCLUSION

                // DETRACT BATCH:
                doBatch(seg, currentTarget, DECREASE,
                        detractCauses, detractAntiCauses, detractCommonEffect, seenNodes);

                // SUPPORT BATCH:
                doBatch(seg, currentTarget, INCREASE,
                        supportCauses, supportAntiCauses, supportCommonEffect, seenNodes);

                // CONCLUSION
                doConclusion(currentTarget);

            } else {
                // SUPPORT - DETRACT - CONCLUSION

                // SUPPORT BATCH:
                doBatch(seg, currentTarget, INCREASE,
                        supportCauses, supportAntiCauses, supportCommonEffect, seenNodes);

                // DETRACT BATCH:
                doBatch(seg, currentTarget, DECREASE,
                        detractCauses, detractAntiCauses, detractCommonEffect, seenNodes);

                // CONCLUSION
                doConclusion(currentTarget);
            }

            // --- --- --- Complete the table
            printer.closeTableBody().closeTable().horizontalRule();
        }

    }


    // --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- ---
    // HELPERS
    // --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- ---

    // --- Filtering

    private List<NodeInfo> filter(Segment s, List<NodeInfo> l, NodeInfo.DIRECTION direction){
        switch(direction){
            case INCREASE:
                return l.stream().filter(s::isSupporting).collect(Collectors.toList());
            case DECREASE:
                return l.stream().filter(s::isDetracting).collect(Collectors.toList());
            case NEUTRAL:
                return l.stream().filter(s::isNeutral).collect(Collectors.toList());
        }
        // Should not happen
        return l;
    }

    // --- Probabilities

    private String getPercentFormat(double d) {
        return decimalFormat.format(d * 100);
    }


    // --- Node printing

    private void printNodeUpdateAttribute(NodeInfo ni) {
        printer.onTop(c -> c.addAttribute("class", "node"));
        if (ni.isEvidence) {
            printer.onTop(c -> c.addAttribute("class", "nodeEvidence"));
        } else if (ni.nodeName.equals(ultimateTarget)) {
            printer.onTop(c -> c.addAttribute("class", "nodeQuery"));
        }
    }

    private void printNode(NodeInfo ni) {
        printer.openSpan();
        printNodeUpdateAttribute(ni);
        printer.addTextRaw(ni.nodeName).closeSpan();
    }

    private void printNodeState(NodeInfo ni) {
        printer.openSpan();
        printNodeUpdateAttribute(ni);
        printer.addTextRaw(ni.nodeName + "=" + ni.stateName).closeSpan();
    }

    private void printState(NodeInfo ni) {
        printer.openSpan().onTop(c -> c.addAttribute("class", "state"));
        printer.addTextRaw(ni.stateName);
        printer.closeSpan();
    }

    private void printNodeVerbalState(NodeInfo ni) {
        double p = ni.posterior * 100;

        if (p == 0) { // it is impossible for [Node] to be [State] (0%)
            printer.addText("it is impossible for");
            printNode(ni);
            printer.addText("to be");
            printState(ni);
        } else if (p < 15) { // [Node] is almost certainly not [State] (>0-15%)
            printNode(ni);
            printer.sentenceForceIN(); // After node
            printer.addText("is almost certainly not");
            printState(ni);
        } else if (p < 40) { // [Node] is probably not [State] (15-40%)
            printNode(ni);
            printer.sentenceForceIN(); // After node
            printer.addText("is probably not");
            printState(ni);
        } else if (p < 60) { // [Node] is [State] with roughly even chances (40-60%)
            printNode(ni);
            printer.sentenceForceIN(); // After node
            printer.addText("is");
            printState(ni);
            printer.addText("with roughly even chances");
        } else if (p < 85) { // [Node] is probably [State] (60-85%)
            printNode(ni);
            printer.sentenceForceIN(); // After node
            printer.addText("is probably");
            printState(ni);
        } else if (p < 100) { // [Node] is almost certainly [State] (85-<100%)
            printNode(ni);
            printer.sentenceForceIN(); // After node
            printer.addText("is almost certainly");
            printState(ni);
        } else { // [Node] is certainly [State] (100%)
            printNode(ni);
            printer.sentenceForceIN(); // After node
            printer.addText("is certainly");
            printState(ni);
        }
    }

    private void printListNode(List<NodeInfo> nis, Consumer<NodeInfo> printfn, String sep) {
    	nis = nis.stream().sorted(Comparator.comparing(a -> a.nodeName)).collect(Collectors.toList());
    	
        if (nis.size() == 0) {
            /* */
        } else if (nis.size() == 1) { // 1 node
            printfn.accept(nis.get(0));
        } else if (nis.size() == 2) { // 2 nodes
            printfn.accept(nis.get(0));
            printer.addText(sep);
            printfn.accept(nis.get(1));
        } else { // 3 Or more
            // first item idx = 0
            printfn.accept(nis.get(0));
            // middle items idx = [1, size-2]
            for (int i = 1; i < nis.size() - 1; ++i) {
                printer.addTextRaw(", ");
                printfn.accept(nis.get(i));
            }
            // last item idx = size-1
            printer.addText(sep);
            printfn.accept(nis.get(nis.size() - 1));
        }
    }
	public String printBNTextwithCausalityOnly(ArrayList<String> bnText, Set<NodeInfo> allNodeInfoList) {
    	//printer.addH2("Preamble");
    	printer.addH2("Bayesian Network Structure");

    	if(bnText.size() == 0) {
    		printer.openPar().addTextRaw("There are no dependencies between the nodes in this BN.").closePar();    		
    	}else {
    		for (int i = 0; i < bnText.size(); ++i) {
    			// --- --- --- Gather the information about the edges, filter them.
    			String currText = bnText.get(i);
    			// --- --- --- Text Generation
    			printer.openPar();
    			printer.addText(currText).eos();
    			printer.closePar();
    		}
    	}
    	// --- --- --- After all the segments: create a table
    	printer.srcnl()
    	.addH2("Initial and Updated Probabilities")
    	.openPar()
    	.addText("The following table contains the initial probabilities in the absence of " + 
    			"evidence, and the updated probabilities after the evidence has been " + 
    			"considered").eos().srcnl()
    	.closePar();

    	printer.openTable();
    	printer.openTableHead().openTableRow()
    	.tableHeader("Type").onLast(Printer.Comp::doRaw)
    	.tableHeader("Variable = State").onLast(Printer.Comp::doRaw)
    	.tableHeader("Initial Probability").onLast(Printer.Comp::doRaw)
    	.tableHeader("Updated Probability").onLast(Printer.Comp::doRaw)
    	.closeTableRow().closeTableHead();

    	// Body
    	printer.openTableBody();
    	allNodeInfoList.stream().sorted(Comparator.comparing(a -> a.nodeName)).sorted(Comparator.comparing(a -> a.isUltimateTarget ? 0 : a.isEvidence ? 1 : 2)).forEach(ni -> {
    		printer.openTableRow();

    		String typeLabel = "Ordinary";
    		if (ni.isUltimateTarget) {
    			typeLabel  = "Target";
    		}
    		else if (ni.isEvidence) {
    			typeLabel = "Evidence";
    		}
    		printer.tableData(typeLabel).onLast(Printer.Comp::doRaw);

    		printer.openTableData();
    		printNodeState(ni);
    		printer.closeTableData();

    		boolean adjPhrase = true;
    		printer.tableData(getPercentFormat(ni.prior) + "% (" + PutVerbalWord_Az(ni.prior, false, adjPhrase) + ")").onLast(Printer.Comp::doRaw);
    		printer.tableData(getPercentFormat(ni.posterior) + "% (" + PutVerbalWord_Az(ni.posterior, false, adjPhrase) + ")").onLast(Printer.Comp::doRaw);


    		printer.closeTableRow();
    	});
    	printer.closeTableBody();
    	// End of table
    	printer.closeTable();

    	return printer.realize();
	}

}
