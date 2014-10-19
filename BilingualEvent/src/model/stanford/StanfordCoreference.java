package model.stanford;

import java.util.ArrayList;

public class StanfordCoreference {
	
	public ArrayList<ArrayList<StanfordMention>> coreferenceChains;
	
	public StanfordCoreference() {
		this.coreferenceChains = new ArrayList<ArrayList<StanfordMention>>();
	}
}
