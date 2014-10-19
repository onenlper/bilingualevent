package model.stanford;

import java.util.ArrayList;

import model.stanford.StanfordXMLReader.StanfordDep;
import model.syntaxTree.MyTree;

public class StanfordSentence {
	public int id;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public ArrayList<StanfordToken> getTokens() {
		return tokens;
	}
	
	public StanfordToken getToken(int i) {
		return this.tokens.get(i);
	}

	public void setTokens(ArrayList<StanfordToken> tokens) {
		this.tokens = tokens;
	}

	public String getParse() {
		return parse;
	}

	public void setParse(String parse) {
		this.parse = parse;
	}

	public MyTree getParseTree() {
		return parseTree;
	}

	public void setParseTree(MyTree parseTree) {
		this.parseTree = parseTree;
	}

	public ArrayList<StanfordDep> getBasicDependencies() {
		return basicDependencies;
	}

	public void setBasicDependencies(ArrayList<StanfordDep> basicDependencies) {
		this.basicDependencies = basicDependencies;
	}

	public ArrayList<StanfordDep> getCollapsedDependencies() {
		return collapsedDependencies;
	}

	public void setCollapsedDependencies(ArrayList<StanfordDep> collapsedDependencies) {
		this.collapsedDependencies = collapsedDependencies;
	}

	public ArrayList<StanfordDep> getCollapsedCcprocessedDependencies() {
		return collapsedCcprocessedDependencies;
	}

	public void setCollapsedCcprocessedDependencies(ArrayList<StanfordDep> collapsedCcprocessedDependencies) {
		this.collapsedCcprocessedDependencies = collapsedCcprocessedDependencies;
	}

	public ArrayList<StanfordToken> tokens;
	public String parse = "";
	public MyTree parseTree;
	public ArrayList<StanfordDep> basicDependencies;
	public ArrayList<StanfordDep> collapsedDependencies;
	public ArrayList<StanfordDep> collapsedCcprocessedDependencies;

	public StanfordSentence() {
		this.tokens = new ArrayList<StanfordToken>();
		this.basicDependencies = new ArrayList<StanfordDep>();
		this.collapsedDependencies = new ArrayList<StanfordDep>();
		this.collapsedCcprocessedDependencies = new ArrayList<StanfordDep>();
	}

	public void addToken(StanfordToken token) {
		this.tokens.add(token);
	}
}