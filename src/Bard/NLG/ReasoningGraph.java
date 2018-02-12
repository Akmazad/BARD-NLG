package Bard.NLG;

import java.io.*;
import java.util.*;

import org.omg.CORBA.Environment;

//import norsys.netica.*;

public class ReasoningGraph {
	private int V;   // No. of vertices
    public ArrayList<String> graphNodes= new ArrayList<String>(); // all nodes with their names
    public LinkedList<String> adj[]; //Adjacency Lists
    //private Hashtable<String, List<String>> adjacencyInfo = new Hashtable<String, List<String>>();
    
    // Constructor
	public ReasoningGraph(ArrayList<String> chainList) {
		
		ArrayList<String> allNodes = ParseNodeNamesFromChainList(chainList);
		
		V = allNodes.size();
		adj = new LinkedList[V];
		for(int i = 0; i < V; i++) {
			graphNodes.add(allNodes.get(i));
			adj[i] = new LinkedList<String>();
		}

		/*adding the edges*/
		for(String path:chainList) {
			//HashSet<String> nodes = new HashSet<String>(Arrays.asList(path.split(" -> | <- ")));
			String[] tempNodes = path.split(" -> | <- ");	

			for(int i = tempNodes.length - 1; i >= 1; i--) {
				int source_index = graphNodes.indexOf(tempNodes[i]);
				//int end_index = graphNodes.indexOf(tempNodes[i-1]);
				if(!adj[source_index].contains(tempNodes[i-1]))
						adj[source_index].add(tempNodes[i-1]);
			}
		}

	}
	private ArrayList<String> ParseNodeNamesFromChainList(ArrayList<String> chainList) {
		ArrayList<String> retList = new ArrayList<String>();
		
			for(String path:chainList) {
				//HashSet<String> nodes = new HashSet<String>(Arrays.asList(path.split(" -> | <- ")));
				String[] tempNodes = path.split(" -> | <- ");	
				for(String n:tempNodes) {
					if(!retList.contains(n)) {
						retList.add(n);
					}
				}
			}
		
		return retList;
	}

	public ArrayList<String> getAllGraphNodes(){
    	return this.graphNodes;
    }
    
	public ArrayList<String> getChilds(String currentNodeName){
		ArrayList<String> retList = new ArrayList<String>();
		
		int source_index = graphNodes.indexOf(currentNodeName);
		for(int i = 0; i < adj[source_index].size(); i++) {
			retList.add(adj[source_index].get(i));
		}
		return retList;
		
	}
	
	public ArrayList<String> getParents(String currentNodeName){
		ArrayList<String> retList = new ArrayList<String>();
		for(int i = 0; i < V; i++) {
			if(adj[i].contains(currentNodeName)) {
				retList.add(graphNodes.get(i));
			}
		}
		return retList;
	}
    public LinkedList<String>[] getAllAdajacencyInfo(){
    	return this.adj;
    }
}
