package model;

import java.util.ArrayList;

import model.syntaxTree.MyTree;

public class ParseResult {
	
	public String sentence="";
	
	public ArrayList<String> words;
	
	public ArrayList<String> posTags;
	
	public MyTree tree;
	
	public ArrayList<Depend> depends;
	
	public ArrayList<int[]> positions;
	public ArrayList<String> lemmas;
	public DependTree dependTree;
	
	public ParseResult() {
		this.lemmas = new ArrayList<String>();
		this.words = new ArrayList<String>();
		this.posTags = new ArrayList<String>();
		this.depends = new ArrayList<Depend>();
		this.positions = new ArrayList<int[]>();
	}
	
	public String plainSentence = "";
	
	public ParseResult(String sentence, MyTree tree, ArrayList<Depend> depends) {
		this.sentence = sentence;
		this.tree = tree;
		this.depends = depends;
		this.dependTree = new DependTree(depends);
		words = new ArrayList<String>();
		posTags = new ArrayList<String>();
		String tokens[] = sentence.split(" ");
		words.add("ROOT");
		posTags.add("ROOT");
		for(String token:tokens) {
			if(token.isEmpty()) {
				continue;
			}
			int pos = token.lastIndexOf('/');
			String word = token.substring(0,pos);
			String posTag = token.substring(pos+1);
			words.add(word);
			posTags.add(posTag);
		}
		for(int i=1;i<this.words.size();i++) {
			this.plainSentence += this.words.get(i);
		}
	}
}

