package model;

import java.util.ArrayList;
import java.util.HashMap;

import model.EntityMention.Numb;

public class EventMention implements Comparable<EventMention> {

	public HashMap<String, String> bvs = new HashMap<String, String>();
	
	public Numb number;
	
	public String CD;
	
	public boolean noun;
	
	private boolean isFake;
	
	public int sentenceID = 0;
	
	public int indexInSentence = 0;
	
	public int sequenceID = -1;
	
	public ParseResult pr;
	
	public HashMap<String, ArrayList<EventMentionArgument>> argHash = new HashMap<String, ArrayList<EventMentionArgument>>();
	
	public ArrayList<Double> typeConfidences = new ArrayList<Double>();
	
	public ArrayList<Double> subTypeConfidences = new ArrayList<Double>();
	
	public int isZeroPronoun = 0;

	public ArrayList<EntityMention> zeroSubjects = null;

	public HashMap<String, Integer> typeHash = new HashMap<String, Integer>();

	public String goldSubtype;
	
	public HashMap<String, ArrayList<EntityMention>> srlArgs = new HashMap<String, ArrayList<EntityMention>>();
	
	public ArrayList<String> modifyList = new ArrayList<String>();
	
	public String toName() {
		return this.getAnchorStart() + "," + this.getAnchorEnd();
	}
	
	public void setFake() {
		this.isFake = true;
		this.polarity = "Fake";
		this.tense = "Fake";
		this.genericity = "Fake";
		this.modality = "Fake";
		this.setSubType("Fake");
		this.polarityConf = new HashMap<String, Double>();
		this.genericityConf = new HashMap<String, Double>();
		this.modalityConf = new HashMap<String, Double>();
		this.tenseConf = new HashMap<String, Double>();
		
		this.polarityConf.put("Fake", 1.0);
		this.genericityConf.put("Fake", 1.0);
		this.modalityConf.put("Fake", 1.0);
		this.tenseConf.put("Fake", 1.0);
	}
	
	public boolean isFake() {
		return this.isFake;
	}
	
	public void increaseType(String type) {
		if (typeHash.containsKey(type)) {
			int count = typeHash.get(type);
			typeHash.put(type, count + 1);
		} else {
			typeHash.put(type, 1);
		}
	}

	public HashMap<String, EventMentionArgument> argumentHash = new HashMap<String, EventMentionArgument>();
	
	public void addArgument(EventMentionArgument argument) {
		this.eventMentionArguments.add(argument);
		this.argumentHash.put(argument.toString(), argument);
	}
	
	public String assignTypeFromTypeHash() {
		int most = -1;
		String type = "null";
		for (String key : typeHash.keySet()) {
			int count = typeHash.get(key);
			if (count > most && !key.equalsIgnoreCase("null")) {
				most = count;
				type = key;
			}
		}
		this.subType = type;
		if(this.subType.equalsIgnoreCase("null") || this.subType.equalsIgnoreCase("none")) {
			this.confidence = -10000;
		} else {
			this.confidence = 1;
		}
//		System.out.println(this.subType);
		for(EventMentionArgument argument : this.eventMentionArguments) {
			if(!argument.jointLabel.startsWith(type + "_")) {
				argument.changeType = true;
//				if(argument.jointLabel.startsWith("null_Na")) {
//					System.out.println(argument.jointLabel);
//				}
			}
		}
		
		return type;
	}

	public String ID;
	public String extent;
	public int start;
	public int end;
	public String type = "null";

	public String inferFrom = "-";

	public String subType = "null";

	public boolean svm = false;
	public boolean maxent = false;

	public String polarity = "-";
	
	public HashMap<String, Double> polarityConf = new HashMap<String, Double>();
	
	public String modality = "-";
	public HashMap<String, Double> modalityConf  = new HashMap<String, Double>();
	
	public String genericity = "-";
	public HashMap<String, Double> genericityConf = new HashMap<String, Double>();
	
	public String tense = "-";
	public HashMap<String, Double> tenseConf = new HashMap<String, Double>();
	
	public String fileID;

	public ACEDoc document;

	public double confidence;

	public double subTypeConfidence;

	public double typeConfidence;

	public String pattern;

	public String label;

	public String posTag;

	public String getID() {
		return ID;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getSubType() {
		return subType;
	}

	public void setSubType(String subType) {
		this.subType = subType;
	}

	public void setID(String iD) {
		ID = iD;
	}

	public String getExtent() {
		return extent;
	}

	public void setExtent(String extent) {
		this.extent = extent;
	}

	public int getStart() {
		return start;
	}

	public void setStart(int extentStart) {
		this.start = extentStart;
	}

	public int getEnd() {
		return end;
	}

	public void setEnd(int extentEnd) {
		this.end = extentEnd;
	}

	public String getLdcScope() {
		return ldcScope;
	}

	public void setLdcScope(String ldcScope) {
		this.ldcScope = ldcScope;
	}

	public int getLdcScopeStart() {
		return ldcScopeStart;
	}

	public void setLdcScopeStart(int ldcScopeStart) {
		this.ldcScopeStart = ldcScopeStart;
	}

	public int getLdcScopeEnd() {
		return ldcScopeEnd;
	}

	public void setLdcScopeEnd(int ldcScopeEnd) {
		this.ldcScopeEnd = ldcScopeEnd;
	}

	public String getAnchor() {
		return anchor;
	}

	public void setAnchor(String anchor) {
		this.anchor = anchor.replace("\n", "").replace(" ", "");
	}

	public int getAnchorStart() {
		return anchorStart;
	}

	public void setAnchorStart(int anchorStart) {
		this.anchorStart = anchorStart;
	}

	public int getAnchorEnd() {
		return anchorEnd;
	}

	public void setAnchorEnd(int anchorEnd) {
		this.anchorEnd = anchorEnd;
	}

	public ArrayList<EventMentionArgument> getEventMentionArguments() {
		return eventMentionArguments;
	}

	public void setEventMentionArguments(ArrayList<EventMentionArgument> eventMentionArguments) {
		this.eventMentionArguments = eventMentionArguments;
	}

	String ldcScope;
	int ldcScopeStart;
	int ldcScopeEnd;
	String anchor = "";
	int anchorStart;
	int anchorEnd;
	public ArrayList<EventMentionArgument> eventMentionArguments;

	public EventChain eventChain;

	public int chainID;

	public int getChainID() {
		return chainID;
	}

	public void setEventChainID(int eventChainID) {
		this.chainID = eventChainID;
	}

	public EventChain getEventChain() {
		return eventChain;
	}

	public void setEventChain(EventChain eventChain) {
		this.eventChain = eventChain;
	}

	public EventMention() {
		this.eventMentionArguments = new ArrayList<EventMentionArgument>();
//		this.setAnchor("");
//		this.setExtent("");
		this.setID("");
		this.setLdcScope("");
		for(int i=0;i<34;i++) {
			typeConfidences.add(0.0);
		}
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(this.anchorStart).append(",").append(this.anchorEnd);
		// .append(":").append(this.getExtent()).append("(")
		// .append(this.getAnchor()).append(")")
		return sb.toString();
	}

	public EventMention antecedent;

	@Override
	public int compareTo(EventMention arg0) {
		int diff = this.getAnchorStart() - ((EventMention) arg0).getAnchorStart();
		if (diff == 0) {
			return this.getAnchorEnd() - ((EventMention) arg0).getAnchorEnd();
		} else {
			return diff;
		}
	}

	public boolean equals(Object em2) {
		if (this.getAnchorStart() == ((EventMention) em2).getAnchorStart()
				&& this.getAnchorEnd() == ((EventMention) em2).getAnchorEnd()) {
			return true;
		} else {
			return false;
		}
	}

	public int hashCode() {
		String str = this.getAnchorStart() + "," + this.getAnchorEnd();
		return str.hashCode();
	}
}
