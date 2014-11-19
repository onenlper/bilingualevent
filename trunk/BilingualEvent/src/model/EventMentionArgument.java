package model;

import java.util.ArrayList;

public class EventMentionArgument implements Comparable{
	String refID;
	public String role = "None";
	String extent;
	int start;
	int end;
	
	public static boolean positionCompare = false;
	
	public static boolean changeType = false;
	
	public ArrayList<Double> roleConfidences = new ArrayList<Double>();
	
	public ArrayList<Double> fullJointConfidences = new ArrayList<Double>();
	
	public String jointLabel;
	
	public int vote;
	
	public double confidence;
	public double roleConfidence;
	
	public boolean equals(Object em2) {
		if (this.start == ((EventMentionArgument) em2).start
				&& this.end == ((EventMentionArgument) em2).end) {
			return true;
		} else {
			return false;
		}
	}

	public String toString() {
		return this.start + "," + this.end;
	}
	
	public int hashCode() {
		String str = this.start + "," + this.end;
		return str.hashCode();
	}
	
	EventMention eventMention;
	
	public EntityMention getEntityMention() {
		return mention;
	}

	public void setEntityMention(EntityMention entityMention) {
		this.mention = entityMention;
	}

	public EntityMention mention;
	
	public EventMention getEventMention() {
		return eventMention;
	}

	public void setEventMention(EventMention eventMention) {
		this.eventMention = eventMention;
	}

	public EventMentionArgument() {
		this.refID = "";
		this.role = "";
		this.extent = "";
	}
	
	public String getRefID() {
		return refID;
	}
	public void setRefID(String refID) {
		this.refID = refID;
	}
	public String getRole() {
		return role;
	}
	public void setRole(String role) {
		this.role = role;
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
	public void setStart(int start) {
		this.start = start;
	}
	public int getEnd() {
		return end;
	}
	public void setEnd(int end) {
		this.end = end;
	}

	@Override
	public int compareTo(Object o) {
		if(positionCompare) {
			return this.getStart()-((EventMentionArgument)o).getStart();
		} else {
			return (this.vote-((EventMentionArgument)o).vote)*(-1);
		}
	}
}
