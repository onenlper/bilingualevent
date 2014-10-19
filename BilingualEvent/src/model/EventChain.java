package model;

import java.util.ArrayList;

public class EventChain {
	String ID;
	
	String type;
	
	String subtype;
	
	String modality;
	
	public String getID() {
		return ID;
	}

	public void setID(String iD) {
		ID = iD;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getSubtype() {
		return subtype;
	}

	public void setSubtype(String subtype) {
		this.subtype = subtype;
	}

	public String getModality() {
		return modality;
	}

	public void setModality(String modality) {
		this.modality = modality;
	}

	public String getPolarity() {
		return polarity;
	}

	public void setPolarity(String polarity) {
		this.polarity = polarity;
	}

	public String getGenericity() {
		return genericity;
	}

	public void setGenericity(String genericity) {
		this.genericity = genericity;
	}

	public String getTense() {
		return tense;
	}

	public void setTense(String tense) {
		this.tense = tense;
	}

	public ArrayList<EventArgument> getArguments() {
		return arguments;
	}

	public void setArguments(ArrayList<EventArgument> arguments) {
		this.arguments = arguments;
	}

	public ArrayList<EventMention> getEventMentions() {
		return eventMentions;
	}

	public void setEventMentions(ArrayList<EventMention> eventMentions) {
		this.eventMentions = eventMentions;
	}
	
	String polarity;
	
	String genericity;
	
	String tense;
	
	ArrayList<EventArgument> arguments;
	
	ArrayList<EventMention> eventMentions;
	
	public int chainID;
	
	public int getChainID() {
		return chainID;
	}

	public void setChainID(int chainID) {
		this.chainID = chainID;
	}

	public EventChain() {
		this.arguments = new ArrayList<EventArgument>();
		this.eventMentions = new ArrayList<EventMention>();
	}
	
	public void addEventMention(EventMention eventMention) {
		eventMention.chainID = this.chainID;
		eventMention.eventChain = this;
		this.eventMentions.add(eventMention);
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for(EventMention em : this.eventMentions) {
			sb.append(em.start).append(",").append(em.end).append(" ");
		}
		return sb.toString();
	}
}
