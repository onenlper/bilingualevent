package util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import model.ACEChiDoc;
import model.ACEDoc;
import model.EventMention;
import model.syntaxTree.MyTreeNode;
import util.CollectCounts.Pair;

public class CollectNegativeContext {

	public static void main(String args[]) {
		ArrayList<String> files = Common.getLines("ACE_Chinese_train0");
		
		HashMap<String, Pair> leftPairs = new HashMap<String, Pair>();
		HashMap<String, Pair> rightPairs = new HashMap<String, Pair>();
		
		for(String file : files) {
			ACEDoc doc = new ACEChiDoc(file);
			ArrayList<EventMention> events = doc.goldEventMentions;
			for(int i=0;i<events.size();i++) {
				EventMention e1 = events.get(i);
				MyTreeNode node1 = doc.getTreeNode(e1.getAnchorStart());
				MyTreeNode clause1 = lowestClause(node1, doc);
				
				HashMap<String, ArrayList<String>> left1 = getLeftWords(clause1, node1);
				HashMap<String, ArrayList<String>> right1 = getRightWords(clause1, node1);
				
				for(int j=i+1;j<events.size();j++) {
					EventMention e2 = events.get(j);
					MyTreeNode node2 = doc.getTreeNode(e2.getAnchorStart());
					MyTreeNode clause2 = lowestClause(node2, doc);
					
					HashMap<String, ArrayList<String>> left2 = getLeftWords(clause2, node2);
					HashMap<String, ArrayList<String>> right2 = getRightWords(clause2, node2);
					
					if(!e1.getSubType().equals(e2.getSubType())) {
						continue;
					}
					
					boolean coref = e2.getEventChain()==e1.getEventChain();
					
					constructPairs(leftPairs, left1, left2, coref);
					
					constructPairs(rightPairs, right1, right2, coref);
				}
				
			}
		}
//		output(leftPairs);
		output(rightPairs);
	}

	private static void output(HashMap<String, Pair> leftPairs) {
		ArrayList<Pair> sortedLeftPair = new ArrayList<Pair>();
		sortedLeftPair.addAll(leftPairs.values());
		Collections.sort(sortedLeftPair);
		Collections.reverse(sortedLeftPair);
		for(Pair p : sortedLeftPair) {
			System.out.println(p.key + ":\t" + p.count + "\t" + p.coref/(p.coref + p.notcoref));
		}
	}

	public static void constructPairs(HashMap<String, Pair> leftPairs,
			HashMap<String, ArrayList<String>> left1,
			HashMap<String, ArrayList<String>> left2, boolean coref) {
		for(String key : left1.keySet()) {
			if(left2.containsKey(key)) {
				for(String s1 : left1.get(key)) {
					for(String s2 : left2.get(key))	{
						String k = "";
						if(s1.compareTo(s2)<0) {
							k = s1 + "#" + s2; 
						} else {
							k = s2 + "#" + s1;
						}
						Pair pair = leftPairs.get(k);
						if(pair==null) {
							pair = new Pair(k);
							leftPairs.put(k, pair);
						}
						pair.increase();
						if(coref) {
							pair.coref += 1;
						} else {
							pair.notcoref += 1;
						}
					}
				}
			}
		}
	}
	
	public static HashMap<String, ArrayList<String>> getLeftWords(MyTreeNode ip, MyTreeNode node) {
		ArrayList<MyTreeNode> strs = new ArrayList<MyTreeNode>();
		ArrayList<MyTreeNode> leaves = ip.getLeaves();
		for(int i=0;i<leaves.size();i++) {
			if(leaves.get(i)==node) {
				break;
			}
			if(leaves.get(i).parent.value.equals("PU")) {
				continue;
			}
			strs.add(leaves.get(i));
		}
		HashMap<String, ArrayList<String>> ret = new HashMap<String, ArrayList<String>>();
		for(MyTreeNode t : strs) {
			ArrayList<String> lst = ret.get(t.parent.value);
			if(lst==null) {
				lst = new ArrayList<String>();
				ret.put(t.parent.value, lst);
			}
			lst.add(t.value);
		}
		return ret;
	}
	
	public static HashMap<String, ArrayList<String>> getRightWords(MyTreeNode ip, MyTreeNode node) {
		ArrayList<MyTreeNode> strs = new ArrayList<MyTreeNode>();
		ArrayList<MyTreeNode> leaves = ip.getLeaves();
		for(int i=leaves.size()-1;i>=0;i--) {
			if(leaves.get(i)==node) {
				break;
			}
			if(leaves.get(i).parent.value.equals("PU")) {
				continue;
			}
			strs.add(leaves.get(i));
		}
		HashMap<String, ArrayList<String>> ret = new HashMap<String, ArrayList<String>>();
		for(MyTreeNode t : strs) {
			ArrayList<String> lst = ret.get(t.parent.value);
			if(lst==null) {
				lst = new ArrayList<String>();
				ret.put(t.parent.value, lst);
			}
			lst.add(t.value);
		}
		return ret;
	}
	
	
	
	public static MyTreeNode lowestClause(MyTreeNode node, ACEDoc doc) {
		ArrayList<MyTreeNode> ancestors = node.getAncestors();
		MyTreeNode clause = ancestors.get(0);
		for(int i=ancestors.size()-1;i>=0;i--) {
			if(ancestors.get(i).value.equals("IP")) {
				clause = ancestors.get(i);
			}
		}
		return clause;
	}
}
