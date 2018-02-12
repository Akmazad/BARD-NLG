package Bard.NLG;
 
import java.io.*;
import java.util.*;

import org.omg.CORBA.Environment;

//import norsys.netica.*;
import agenariskserver.*;
//import uk.co.agena.minerva.*;
 

/**
 * @author aazad
 *
 */

public class Graph  {

	private int V;   // No. of vertices
    
    public ArrayList<String> graphNodes= new ArrayList<String>(); // all nodes with their names
    public LinkedList<String>[] adj; //Adjacency Lists
    //private Hashtable<String, List<String>> adjacencyInfo = new Hashtable<String, List<String>>();
    
    private String START = "";
    private String END = "";
    private ArrayList<String> All_paths = new ArrayList<String>();
    
    // Constructor
    Graph(Net _net) throws Exception {
    	
			//NodeList allNodes = new NodeList(_net);
    		Node[] allNodes = _net.getNodes();
			V = allNodes.length;
			adj = new LinkedList[V];
			
			for(int i = 0; i < allNodes.length; i++) {
				// add graph nodes
				String nName = allNodes[i].getShortName();
				graphNodes.add(nName);
				
				// add graph edge
				adj[i] = new LinkedList<String>();
				
				// get and set all parents of the current node
				//NodeList allChildren = new NodeList(_net);
				try {
					Node[] allChildren = allNodes[i].getChildren();

					for(int j = 0; j < allChildren.length; j++) {
						String childNode = allChildren[j].getShortName();
						if(!adj[i].contains(childNode))
							adj[i].add(childNode); // edge format: FROM (current node) -> TO (child node)
					}
				} 
				catch (Exception e){
					e.printStackTrace();
				};
				
			}
    }
 
    public ArrayList<String> getAllGraphNodes(){
    	return this.graphNodes;
    }
    
    public LinkedList<String>[] getAllAdajacencyInfo(){
    	return this.adj;
    }
    
    // Function to add an edge into the graph
    ArrayList<String> findPaths(String from, String to)
    {
        //LinkedList<String> retList = new LinkedList<String>();
        All_paths.clear();
        
        LinkedList<String> visited = new LinkedList<String>();
        
        START = from;
        END = to;
        
        visited.addLast(START);
        breadthFirst(visited);
        return All_paths;
    }

	private void breadthFirst(LinkedList<String> visited) {
		String currentNode = visited.peekLast();
		int currentNodeIndex = graphNodes.indexOf(currentNode);
		//LinkedList<String> adjacentNodes = adj[currentNodeIndex]; // get the adjacency info ( "currentNode -> others" types of info)
		LinkedList<String> adjacentNodes = findAdjacentNodes(currentNode, currentNodeIndex);
		
		for(int i = 0; i < adjacentNodes.size(); i++) {
			String adjNode = adjacentNodes.get(i);
			if(visited.contains(adjNode)) {
				continue;
			}
			if(adjNode.equals(END)) {
				visited.addLast(adjNode);
				printPath(visited);
				visited.removeLast();
			}
		}
		
		for(int i = 0; i < adjacentNodes.size(); i++) {
			String adjNode = adjacentNodes.get(i);
			if(visited.contains(adjNode) || adjNode.equals(END)) {
				continue;
			}
			
			visited.addLast(adjNode);
			breadthFirst(visited);
			visited.removeLast();
		}
	}

	private LinkedList<String> findAdjacentNodes(String currentNodeName, int currentNodeIndex) {
		LinkedList<String> retList = new LinkedList<String>(adj[currentNodeIndex]);
		//retList = adj[currentNodeIndex];
		for(int i = 0; i < V; i++) {
			if(adj[i].contains(currentNodeName)) {
				retList.add(graphNodes.get(i));
			}
		}		
		return retList;
	}

	private void printPath(LinkedList<String> visited) {
		String temp = visited.get(0);
		for(int i = 1; i < visited.size(); i++) {
			String toNode = visited.get(i);
			String fromNode = visited.get(i-1);
			
			if(adj[graphNodes.indexOf(fromNode)].contains(toNode)) // assume the node names in BN is unique
				temp += (" -> " + toNode);
			else {
				toNode = visited.get(i-1);
				fromNode = visited.get(i);
				if(adj[graphNodes.indexOf(fromNode)].contains(toNode)) {
					temp += (" <- " + fromNode);
				}
			}				
		}
		//temp = temp.substring(0, temp.length()-2);
		All_paths.add(temp);
	}

}
