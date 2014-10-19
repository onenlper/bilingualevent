package model.stanford;

import java.util.ArrayList;

public class StanfordMention {
	public int sentenceId;
	public int startId;
	public int endId;
	
	public int entityId;
	
	boolean visited = false;
	
	ArrayList<StanfordToken> sTokens;
	
	public StanfordMention() {
		this.sTokens = new ArrayList<StanfordToken>();
	}
	
	public void addStanfordToken(StanfordToken sToken) {
		this.sTokens.add(sToken);
	}

	public int getSentenceId() {
		return sentenceId;
	}

	public void setSentenceId(int sentenceId) {
		this.sentenceId = sentenceId;
	}

	public int getStartId() {
		return startId;
	}

	public void setStartId(int startId) {
		this.startId = startId;
	}

	public int getEndId() {
		return endId;
	}

	public void setEndId(int endId) {
		this.endId = endId;
	}

	public int getHeadId() {
		return headId;
	}

	public void setHeadId(int headId) {
		this.headId = headId;
	}

	public boolean isRepresentative() {
		return representative;
	}

	public void setRepresentative(boolean representative) {
		this.representative = representative;
	}

	int headId;
	boolean representative;
}