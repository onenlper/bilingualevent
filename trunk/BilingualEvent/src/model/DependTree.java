package model;

import java.util.ArrayList;
import java.util.HashMap;

import model.syntaxTree.GraphNode;
import model.syntaxTree.MyTreeNode;

// May be a graph, but force it to be a tree
public class DependTree {
	public ArrayList<MyTreeNode> vertexes;
	
	public ArrayList<Depend> edges;

	public HashMap<Integer, MyTreeNode> vertexMapTree;
	
	public HashMap<Integer, GraphNode> vertexMap;
	
	public MyTreeNode root;
	
	public DependTree (ArrayList<Depend> edges) {
		this.vertexes = new ArrayList<MyTreeNode>();
		this.edges = new ArrayList<Depend>();
		this.vertexMapTree = new HashMap<Integer, MyTreeNode>();
		this.edges = edges;
		for(Depend edge : edges) {
			int parent = edge.first;
			int child = edge.second;
			String type = edge.type;
			MyTreeNode pNode = vertexMapTree.get(parent);
			if(pNode==null) {
				pNode = new MyTreeNode();
				pNode.value = Integer.toString(parent);
				vertexMapTree.put(parent, pNode);
				if(parent==0) {
					root = pNode;
				}
			}
			MyTreeNode cNode = vertexMapTree.get(child);
			if(cNode==null) {
				cNode = new MyTreeNode();
				cNode.value = Integer.toString(child);
				vertexMapTree.put(child, cNode);
			}
			if(cNode.parent!=null && parent!=0) {
				continue;
			}
			
			boolean loop = false;
			MyTreeNode pop = pNode.parent;
			while(pop!=null) {
				if(pop.value==cNode.value) {
					loop = true;
					break;
				}
				pop = pop.parent;
			}
			if(!loop) {
				pNode.addChild(cNode);
			}
			cNode.backEdge = type;
		}
		
		this.vertexMap = new HashMap<Integer, GraphNode>();

		// add prep
//		ArrayList<Depend> newDeps = breakPrepDep(edges, s);
//		edges.clear();
//		edges.addAll(newDeps);
		for (Depend edge : edges) {
			int parent = edge.first;
			int child = edge.second;
			String type = edge.type;
//			System.out.println(type);
			GraphNode pNode = vertexMap.get(parent);
			if (pNode == null) {
				pNode = new GraphNode(parent);
				vertexMap.put(parent, pNode);
			}
			GraphNode cNode = vertexMap.get(child);
			if (cNode == null) {
				cNode = new GraphNode(child);
				vertexMap.put(child, cNode);
			}
			pNode.addEdge(cNode, type + "+");
			cNode.addEdge(pNode, type + "-");
		}
	}
}
