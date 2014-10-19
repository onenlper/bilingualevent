package model.stanford;

import java.util.ArrayList;

public class StanfordResult {
	public ArrayList<StanfordSentence> sentences;
	
	public StanfordCoreference coreference;
	
	public StanfordResult() {
		this.sentences = new ArrayList<StanfordSentence>();
		this.coreference = new StanfordCoreference();
	}
	
	public void addSentence(StanfordSentence sentence) {
		this.sentences.add(sentence);
	}
	
	public void addChain(ArrayList<StanfordMention> chain) {
		this.coreference.coreferenceChains.add(chain);
	}
	
	public StanfordSentence getSentence(int i) {
		return this.sentences.get(i);
	}
}