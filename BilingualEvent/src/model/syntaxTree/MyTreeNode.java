package model.syntaxTree;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import util.Common;

public class MyTreeNode {
	public String value;

	public String backEdge;

	public ArrayList<MyTreeNode> children;

	public boolean mark = false;

	public MyTreeNode parent;

	public int leafIdx = 0;

	public boolean isNNP = false;

	public String productionRule() {
		StringBuilder sb = new StringBuilder();
		sb.append(this.value).append("->");
		for (MyTreeNode node : this.children) {
			sb.append(node.value).append("_");
		}
		return sb.toString();
	}

	public MyTreeNode() {
		children = new ArrayList<MyTreeNode>();
	}

	public MyTreeNode nextChild() {
		if (this.childIndex + 1 == this.parent.children.size()) {
			return null;
		}
		return this.parent.children.get(this.childIndex + 1);
	}

	public ArrayList<MyTreeNode> getLaterSisters() {
		ArrayList<MyTreeNode> sisters = new ArrayList<MyTreeNode>();
		for (int i = this.childIndex; i < this.parent.children.size(); i++) {
			sisters.add(this.parent.children.get(i));
		}
		return sisters;
	}

	public ArrayList<MyTreeNode> getOlderSisters() {
		ArrayList<MyTreeNode> sisters = new ArrayList<MyTreeNode>();
		for (int i = 0; i < this.childIndex; i++) {
			sisters.add(this.parent.children.get(i));
		}
		return sisters;
	}

	public int childIndex;

	public MyTreeNode(String value) {
		this.value = value;
		this.children = new ArrayList<MyTreeNode>();
	}

	public void addChild(MyTreeNode node) {
		node.childIndex = this.children.size();
		this.children.add(node);
		node.parent = this;
	}

	/*
	 * get all ancestors of one tree node, 0 element is the root, also include
	 * itself
	 */
	public ArrayList<MyTreeNode> getAncestors() {
		ArrayList<MyTreeNode> ancestors = new ArrayList<MyTreeNode>();
		MyTreeNode tmp = this;
		while (tmp != null) {
			ancestors.add(0, tmp);
			tmp = tmp.parent;
		}
		return ancestors;
	}

	public boolean isLeafNode() {
		return (this.children.size() == 0);
	}

	public boolean isPOSNode() {
		if (this.children.size() == 0) {
			return false;
		}
		if (this.children.get(0).isLeafNode()) {
			return true;
		}
		return false;
	}

	public static void main(String args[]) {
		String treeStr = "(ROOT" + "  (IP" + "    (VP (VV 请)" + "      (IP" + "        (VP (VV 听)" + "          (NP"
				+ "            (DNP" + "              (NP (NN 记者) (NN 宫能惠))" + "              (DEG 的))"
				+ "            (NP (NN 报导))))))" + "    (PU 。)))";
		MyTree tree = Common.constructTree(treeStr);
		// for(int i=1;i<tree.leaves.size();i++) {
		// TreeNode leaf = tree.leaves.get(i);
		// TreeNode parent = leaf;
		// while(parent!=tree.root) {
		// System.out.print(parent.value+" ");
		// parent = parent.parent;
		// }
		// System.out.println(leaf.value);
		// }
		MyTreeNode leaf = tree.leaves.get(4);
		MyTreeNode parent = leaf.parent.parent.parent.parent;
		MyTreeNode child = parent;
		StringBuilder sb = new StringBuilder();
		while (child.children != null && child.children.size() != 0) {
			child = child.children.get(0);
		}
		System.out.println(child.value);
		System.out.println(child.children.size());
		for (int i = child.leafIdx; i <= 4; i++) {
			sb.append(tree.leaves.get(i).value);
		}
		System.out.println(sb.toString());

		System.out.println(tree.root.getPlainText(true));

		MyTreeNode copyNode = tree.root.copy();
		System.out.println(copyNode.getPlainText(true));
	}

	public ArrayList<MyTreeNode> getLeaves() {
		ArrayList<MyTreeNode> leaves = new ArrayList<MyTreeNode>();
		ArrayList<MyTreeNode> frontiers = new ArrayList<MyTreeNode>();
		frontiers.add(this);
		while (frontiers.size() > 0) {
			MyTreeNode tn = frontiers.remove(frontiers.size() - 1);
			if (tn.children.size() == 0) {
				leaves.add(tn);
			}
			for (int i = tn.children.size() - 1; i >= 0; i--) {
				frontiers.add(tn.children.get(i));
			}
		}
		return leaves;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		ArrayList<MyTreeNode> frontiers = new ArrayList<MyTreeNode>();
		frontiers.add(this);
		while (frontiers.size() > 0) {
			MyTreeNode tn = frontiers.remove(frontiers.size() - 1);
			// System.out.println(tn.value + "#");
			// System.out.println(tn.parent ==null);
			if (tn.children.size() == 0 && !tn.parent.value.equalsIgnoreCase("-none-")) {
				sb.append(tn.value).append(" ");
			}
			for (int i = tn.children.size() - 1; i >= 0; i--) {
				frontiers.add(tn.children.get(i));
			}
		}
		return sb.toString();
	}

	public MyTreeNode pruneNone() {
		ArrayList<MyTreeNode> frontie = new ArrayList<MyTreeNode>();
		frontie.add(this);
		while (frontie.size() > 0) {
			MyTreeNode tn = frontie.remove(0);

			ArrayList<MyTreeNode> leaves = tn.getLeaves();
			boolean notNone = false;
			for (MyTreeNode leaf : leaves) {
				if (!leaf.parent.value.equalsIgnoreCase("-none-")) {
					notNone = true;
					break;
				}
			}
			if (!notNone) {
				tn.parent.children.remove(tn);
			}
			frontie.addAll(tn.children);
		}
		return this;
	}

	private void appendTreeNodeValue(MyTreeNode tn, StringBuilder sb, boolean leaf) {
		boolean haveMarkedChild = false;
		for (MyTreeNode child : tn.children) {
			if (child.mark) {
				haveMarkedChild = true;
				break;
			}
		}
		if (!haveMarkedChild) {
			if (tn.mark) {
				if (tn.value.equalsIgnoreCase("-none-")) {
					if (leaf) {
						sb.append(" (NP-SBJ none)");
					} else {
						sb.append(" (-none- )");
					}
				} else if (tn.children.size() > 0) {
					// leaf
					if (leaf && tn.children.size() == 1 && tn.children.get(0).children.size() == 0) {
						sb.append(" (").append(tn.value).append(" ").append(tn.children.get(0).value).append(")");
					} else {
						sb.append(" (").append(tn.value).append(" )");
					}
				} else {
					sb.append(" ").append(tn.value);
				}
			}
		} else {
			if (tn.mark) {
				sb.append(" ").append("(").append(tn.value);
				ArrayList<MyTreeNode> children = tn.children;
				for (MyTreeNode child : children) {
					appendTreeNodeValue(child, sb, leaf);
				}
				sb.append(")");
			}
		}
	}

	public String getPlainText(boolean leaf) {
		StringBuilder sb = new StringBuilder();
		appendTreeNodeValue(this, sb, leaf);
		return sb.toString();
	}

	public MyTreeNode copy() {
		return copyNode(this);
	}

	public MyTreeNode copyNode(MyTreeNode oldNode) {
		MyTreeNode newNode = new MyTreeNode();
		newNode.isNNP = oldNode.isNNP;
		newNode.leafIdx = oldNode.leafIdx;
		newNode.value = oldNode.value;

		for (MyTreeNode child : oldNode.children) {
			MyTreeNode newChild = copyNode(child);
			newChild.parent = newNode;
			newNode.addChild(newChild);
		}
		return newNode;
	}

	/*
	 * set all the nodes in the subtree as mark or unmark
	 */
	public void setAllMark(boolean mark) {
		ArrayList<MyTreeNode> frontie = new ArrayList<MyTreeNode>();
		frontie.add(this);
		while (frontie.size() > 0) {
			MyTreeNode tn = frontie.remove(0);
			tn.mark = mark;
			frontie.addAll(tn.children);
		}
	}

	public int hashCode() {
		ArrayList<MyTreeNode> leaves = this.getLeaves();
		String str = leaves.get(0).leafIdx + "_" + leaves.get(leaves.size() - 1).leafIdx + "_" + this.toString();
		return str.hashCode();
	}

	public boolean equals(Object em2) {
		ArrayList<MyTreeNode> leaves1 = this.getLeaves();
		ArrayList<MyTreeNode> leaves2 = ((MyTreeNode) em2).getLeaves();
		if (this.toString().equals(((MyTreeNode) em2).toString()) && leaves1.get(0).leafIdx == leaves2.get(0).leafIdx
				&& leaves1.size() == leaves2.size()) {
			return true;
		} else {
			return false;
		}
	}

	// depth first search
	public String getMarkTreeString() {
		StringBuilder sb = new StringBuilder();
		if (this.mark) {
			sb.append(" (");
			sb.append(this.value);
			boolean markedChild = false;
			for (MyTreeNode node : this.children) {
				if (node.mark) {
					markedChild = true;
				}
			}
			if (markedChild) {
				for (MyTreeNode node : this.children) {
					sb.append(node.getMarkTreeString());
				}
			}
			sb.append(")");
		}
		return sb.toString();
	}

	public ArrayList<MyTreeNode> getDepthFirstOffsprings() {
		ArrayList<MyTreeNode> offsprings = new ArrayList<MyTreeNode>();
		ArrayList<MyTreeNode> frontie = new ArrayList<MyTreeNode>();
		frontie.add(this);
		while (frontie.size() > 0) {
			MyTreeNode tn = frontie.remove(frontie.size() - 1);
			offsprings.add(tn);
			for (int i = tn.children.size() - 1; i >= 0; i--) {
				frontie.add(tn.children.get(i));
			}
		}
		return offsprings;
	}

	public ArrayList<MyTreeNode> getBroadFirstOffsprings() {
		ArrayList<MyTreeNode> offsprings = new ArrayList<MyTreeNode>();
		ArrayList<MyTreeNode> frontie = new ArrayList<MyTreeNode>();
		frontie.add(this);
		while (frontie.size() > 0) {
			MyTreeNode tn = frontie.remove(0);
			offsprings.add(tn);
			frontie.addAll(tn.children);
		}
		return offsprings;
	}

	// @Override
	// public int compareTo(TreeNode arg0) {
	// ArrayList<TreeNode> leaves1 = this.getLeaves();
	// ArrayList<TreeNode> leaves2 = arg0.getLeaves();
	//		
	// int start1 = leaves1.get(0).leafIdx;
	// int end1 = leaves1.get(leaves1.size()-1).leafIdx;
	//		
	// int start2 = leaves2.get(0).leafIdx;
	// int end2 = leaves2.get(leaves2.size()-1).leafIdx;
	//		
	// if(start1!=start2) {
	// return start1-start2;
	// } else {
	// return end1-end2;
	// }
	// }

	public MyTreeNode getHeadLeaf() {
		if ((this.children.size() == 1 && this.children.get(0).children.size() == 0)) {
			return this.children.get(0);
		} else if (this.children.size() == 0) {
			return this;
		} else {
			MyTreeNode headChild = this.findHeadChild();
			return headChild.getHeadLeaf();
		}
	}

	public static boolean arabic = false;

	public MyTreeNode findHeadChild() {
		if (headRules == null) {
			if (!arabic) {
				loadRules();
			} else {
				loadArabicRules();
				System.out.println("load arabic rules");
			}
		}
		String nodeValue = this.value;
		String[][] travalWays = headRules.get(nodeValue);
		if (travalWays == null) {
			return this.children.get(this.children.size() - 1);
		}
		for (String[] travelWay : travalWays) {
			if (travelWay[0].equals("left")) {
				for (int i = 1; i < travelWay.length; i++) {
					for (int j = 0; j < this.children.size(); j++) {
						if (this.children.get(j).value.equalsIgnoreCase(travelWay[i])) {
							return this.children.get(j);
						}
					}
				}
			} else if (travelWay[0].equalsIgnoreCase("leftdis")) {
				for (int j = 0; j < this.children.size(); j++) {
					for (int i = 1; i < travelWay.length; i++) {
						if (this.children.get(j).value.equalsIgnoreCase(travelWay[i])) {
							return this.children.get(j);
						}
					}
				}
			} else if (travelWay[0].equalsIgnoreCase("right")) {
				for (int i = 1; i < travelWay.length; i++) {
					for (int j = this.children.size() - 1; j >= 0; j--) {
						if (this.children.get(j).value.equalsIgnoreCase(travelWay[i])) {
							return this.children.get(j);
						}
					}
				}
			} else if (travelWay[0].equalsIgnoreCase("rightdis")) {
				for (int j = this.children.size() - 1; j >= 0; j--) {
					for (int i = 1; i < travelWay.length; i++) {
						if (this.children.get(j).value.equalsIgnoreCase(travelWay[i])) {
							return this.children.get(j);
						}
					}
				}
			}
		}
		return this.children.get(this.children.size() - 1);
	}

	static Map<String, String[][]> headRules;

	private static void loadArabicRules() {
		headRules = new HashMap<String, String[][]>();

		headRules.put("NX", new String[][] { { "left", "DT", "DTNN", "DTNNS", "DTNNP", "DTNNPS", "DTJJ",
				"DTNOUN_QUANT", "NOUN_QUANT" } });
		headRules.put("ADJP", new String[][] {
				{ "rightdis", "ADJ", "DTJJ", "ADJ_NUM", "DTADJ_NUM", "JJR", "DTJJR" },
				{ "right", "ADJP", "VN", "NOUN", "NNP", "NOFUNC", "NO_FUNC", "NNPS", "NNS", "DTNN", "DTNNS", "DTNNP",
						"DTNNPS", "DTJJ", "DTNOUN_QUANT", "NOUN_QUANT" }, { "right", "RB", "CD", "DTRB", "DTCD" },
				{ "right", "DT" } }); // sometimes right, sometimes left
										// headed??
		headRules.put("ADVP", new String[][] {
				{ "left", "WRB", "RB", "ADVP", "WHADVP", "DTRB" },
				{ "left", "CD", "RP", "NOUN", "CC", "ADJ", "DTJJ", "ADJ_NUM", "DTADJ_NUM", "IN", "NP", "NNP", "NOFUNC",
						"DTRP", "DTNN", "DTNNP", "DTNNPS", "DTNNS", "DTJJ", "DTNOUN_QUANT", "NOUN_QUANT" } }); // NNP
																												// is
																												// a
																												// gerund
																												// that
																												// they
																												// called
																												// an
																												// unknown
																												// (=NNP,
																												// believe
																												// it
																												// or
																												// not...)
		headRules.put("CONJP", new String[][] { { "right", "IN", "RB", "NOUN", "NNS", "NNP", "NNPS", "DTRB", "DTNN",
				"DTNNS", "DTNNP", "DTNNPS", "DTNOUN_QUANT", "NOUN_QUANT" } });
		headRules.put("FRAG", new String[][] {
				{ "left", "NOUN", "NNPS", "NNP", "NNS", "DTNN", "DTNNS", "DTNNP", "DTNNPS", "DTNOUN_QUANT",
						"NOUN_QUANT" }, { "left", "VBP" } });
		headRules.put("INTJ", new String[][] { { "left", "RP", "UH", "DTRP" } });
		headRules.put("LST", new String[][] { { "left" } });
		headRules.put("NAC", new String[][] { { "left", "NP", "SBAR", "PP", "MWP", "ADJP", "S", "PRT", "UCP" },
				{ "left", "ADVP" } }); // note: maybe CC, RB should be the
										// heads?
		headRules.put("NP", new String[][] {
				{ "left", "NOUN", "DET+NN", "NNS", "NNP", "NNPS", "NP", "PRP", "WHNP", "QP", "WP", "DTNNS", "DTNNPS",
						"DTNNP", "NOFUNC", "NO_FUNC", "DTNOUN_QUANT", "NOUN_QUANT" },
				{ "left", "ADJ", "DTJJ", "JJR", "DTJJR", "ADJ_NUM", "DTADJ_NUM" }, { "right", "CD", "DTCD" },
				{ "left", "PRP$" }, { "right", "DT" } }); // should the JJ rule
															// be left or right?
		headRules.put("PP", new String[][] { { "left", "PREP", "PP", "MWP", "PRT", "X" },
				{ "left", "NNP", "RP", "NOUN" }, { "left", "NP" } }); // NN is
																		// for a
																		// mistaken
																		// "fy",
																		// and
																		// many
																		// wsT
		headRules.put("PRN", new String[][] { { "left", "NP" } }); // don't get
																	// PUNC
		headRules.put("PRT", new String[][] { { "left", "RP", "PRT", "IN", "DTRP" } });
		headRules.put("QP", new String[][] { { "right", "CD", "DTCD", "NOUN", "ADJ", "NNS", "NNP", "NNPS", "DTNN",
				"DTNNS", "DTNNP", "DTNNPS", "DTJJ", "DTNOUN_QUANT", "NOUN_QUANT" } });

		headRules.put("S", new String[][] { { "left", "VP", "MWV", "S" },
				{ "right", "PP", "MWP", "ADVP", "SBAR", "UCP", "ADJP" } }); // really
																			// important
																			// to
																			// put
																			// in
																			// -PRD
																			// sensitivity
																			// here!
		headRules.put("SQ", new String[][] { { "left", "VP", "MWV", "PP", "MWP" } }); // to
																						// be
																						// principled,
																						// we
																						// need
																						// -PRD
																						// sensitivity
																						// here
																						// too.
		headRules.put("SBAR", new String[][] {
				{ "left", "WHNP", "WHADVP", "WRB", "RP", "IN", "SBAR", "CC", "WP", "WHPP", "ADVP", "PRT", "RB", "X",
						"DTRB", "DTRP" },
				{ "left", "NOUN", "NNP", "NNS", "NNPS", "DTNN", "DTNNS", "DTNNP", "DTNNPS", "DTNOUN_QUANT",
						"NOUN_QUANT" }, { "left", "S" } });
		headRules.put("SBARQ", new String[][] {
				{ "left", "WHNP", "WHADVP", "RP", "IN", "SBAR", "CC", "WP", "WHPP", "ADVP", "PRT", "RB", "X" },
				{ "left", "NOUN", "NNP", "NNS", "NNPS", "DTNN", "DTNNS", "DTNNP", "DTNNPS", "DTNOUN_QUANT",
						"NOUN_QUANT" }, { "left", "S" } }); // copied from SBAR
															// rule -- look more
															// closely when
															// there's time
		headRules.put("UCP", new String[][] { { "left" } });
		headRules
				.put("VP", new String[][] {
						{ "left", "VBD", "VBN", "VBP", "VBG", "DTVBG", "VN", "DTVN", "VP", "RB", "X", "VB" },
						{ "left", "IN" },
						{ "left", "NNP", "NOFUNC", "NOUN", "DTNN", "DTNNP", "DTNNPS", "DTNNS", "DTNOUN_QUANT",
								"NOUN_QUANT" } }); // exclude RP because we
													// don't want negation
													// markers as heads -- no
													// useful information?

		headRules
				.put("MWV", new String[][] {
						{ "left", "VBD", "VBN", "VBP", "VBG", "DTVBG", "VN", "DTVN", "VP", "RB", "X", "VB" },
						{ "left", "IN" },
						{ "left", "NNP", "NOFUNC", "NOUN", "DTNN", "DTNNP", "DTNNPS", "DTNNS", "DTNOUN_QUANT",
								"NOUN_QUANT" } }); // exclude RP because we
													// don't want negation
													// markers as heads -- no
													// useful information?
		headRules.put("MWP", new String[][] { { "left", "PREP", "PP", "MWP", "PRT", "X" },
				{ "left", "NNP", "RP", "NOUN" }, { "left", "NP" } }); // NN is
																		// for a
																		// mistaken
																		// "fy",
																		// and
																		// many
																		// wsT

		// also, RB is used as gerunds

		headRules.put("WHADVP", new String[][] { { "left", "WRB", "WP" }, { "right", "CC" }, { "left", "IN" } });
		headRules.put("WHNP", new String[][] { { "right", "WP" } });
		headRules.put("WHPP", new String[][] { { "left", "IN", "RB" } });
		headRules.put("X", new String[][] { { "left" } });

		// Added by Mona 12/7/04 for the newly created DT nonterm cat
		headRules.put("DTNN", new String[][] { { "right" } });
		headRules.put("DTNNS", new String[][] { { "right" } });
		headRules.put("DTNNP", new String[][] { { "right" } });
		headRules.put("DTNNPS", new String[][] { { "right" } });
		headRules.put("DTJJ", new String[][] { { "right" } });
		headRules.put("DTRP", new String[][] { { "right" } });
		headRules.put("DTRB", new String[][] { { "right" } });
		headRules.put("DTCD", new String[][] { { "right" } });
		headRules.put("DTIN", new String[][] { { "right" } });

		// stand-in dependency:
		headRules.put("EDITED", new String[][] { { "left" } });

		// one stray SINV in the training set...garbage head rule here.
		headRules.put("SINV", new String[][] { { "left", "ADJP", "VP" } });
	}

	private static void loadRules() {
		headRules = new HashMap<String, String[][]>();
		headRules
				.put("NP", new String[][] { { "rightdis", "NN", "NNP", "NNPS", "NNS", "NX", "NML", "JJR" },
						{ "left", "NP", "PRP" }, { "rightdis", "$", "ADJP", "FW" }, { "right", "CD" },
						{ "rightdis", "JJ", "JJS", "QP", "DT", "WDT", "NML", "PRN", "RB", "RBR", "ADVP" },
						{ "left", "POS" }, });
		headRules.put("WHNP", new String[][] { { "rightdis", "NN", "NNP", "NNPS", "NNS", "NX", "NML", "JJR", "WP" },
				{ "left", "WHNP", "NP" }, { "rightdis", "$", "ADJP", "PRN", "FW" }, { "right", "CD" },
				{ "rightdis", "JJ", "JJS", "RB", "QP" }, { "left", "WHPP", "WHADJP", "WP$", "WDT" } });
		// WHADJP
		headRules.put("WHADJP", new String[][] { { "left", "ADJP", "JJ", "JJR" }, { "right", "RB" }, });
		headRules.put("WHADVP", new String[][] { { "rightdis", "WRB", "WHADVP", "RB", "JJ" }, });

		headRules.put("QP", new String[][] { { "right", "$", "NNS", "NN", "CD", "JJ", "PDT", "DT", "IN", "RB", "NCD",
				"QP", "JJR", "JJS" } });

		headRules.put("S", new String[][] { { "left", "VP", "S", "FRAG", "SBAR", "ADJP", "UCP", "TO" },
				{ "right", "NP" } });

		headRules.put("SBAR", new String[][] { { "left", "S", "SQ", "SINV", "SBAR", "FRAG", "VP", "WHNP", "WHPP",
				"WHADVP", "WHADJP", "IN", "DT" } });

		headRules.put("SQ", new String[][] { { "left", "VP", "SQ", "ADJP", "VB", "VBZ", "VBD", "VBP", "MD", "AUX",
				"AUXG" } });

		headRules.put("UCP", new String[][] {});

		headRules.put("CONJP", new String[][] { { "right", "VB", "JJ", "RB", "IN", "CC" }, });

		headRules.put("FRAG", new String[][] { { "left", "IN" }, { "right", "RB" }, { "left", "NP" },
				{ "left", "ADJP", "ADVP", "FRAG", "S", "SBAR", "VP" }, });

		headRules.put("PP",
				new String[][] { { "right", "IN", "TO", "VBG", "VBN", "RP", "FW", "JJ" }, { "left", "PP" } });

		headRules.put("PRN", new String[][] { { "left", "VP", "SQ", "S", "SINV", "SBAR", "NP", "ADJP", "PP", "ADVP",
				"INTJ", "WHNP", "NAC", "VBP", "JJ", "NN", "NNP" }, });

		headRules.put("XS", new String[][] { { "right", "IN" } });

		headRules.put("ADVP", new String[][] { { "right", "NP" } });
	}
	
	public ArrayList<MyTreeNode> getLeftSisters() {
		ArrayList<MyTreeNode> sisters = new ArrayList<MyTreeNode>();
		for (int i = 0; i < this.childIndex; i++) {
			sisters.add(this.parent.children.get(i));
		}
		return sisters;
	}
}
