package model.syntaxTree;

import java.util.ArrayList;

public class MyTree {
	public MyTreeNode root;
	
	public ArrayList<MyTreeNode> leaves;
	
	public MyTree() {
		this.root = null;
		this.leaves = new ArrayList<MyTreeNode>();
		this.leaves.add(null);
	}
}
