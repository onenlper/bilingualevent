package model.syntaxTree;

import java.util.ArrayList;
import java.util.HashMap;

import util.Common;

public class GraphNode {

	public ArrayList<GraphNode> nexts;

	public int value;

	public HashMap<GraphNode, String> edgeName;

	public GraphNode backNode;
	
	public boolean visit = false;
	
	public GraphNode(int value) {
		this.value = value;
		this.nexts = new ArrayList<GraphNode>();
		this.edgeName = new HashMap<GraphNode, String>();
	}

	public void addEdge(GraphNode node, String edgeName) {
		if(node==null || edgeName==null) {
			Common.bangErrorPOS("DONOT insert null");
		}
		
		if(!this.edgeName.containsKey(node)) {
			this.nexts.add(node);
			this.edgeName.put(node, edgeName);
		}
	}
	
	public String getEdgeName(GraphNode n2) {
		if(this.edgeName.containsKey(n2)) {
			return this.edgeName.get(n2);
		} else {
			return "null";
		}
	}
}
