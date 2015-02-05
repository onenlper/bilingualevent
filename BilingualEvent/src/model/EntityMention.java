package model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import model.syntaxTree.MyTreeNode;

public class EntityMention implements Comparable<EntityMention> {
	public int sequenceID = -1;
	
	public int indexInSentence;
	public enum Gender {
		MALE, FEMALE, NEUTRAL, UNKNOWN
	}

	public enum Numb {
		SINGULAR, PLURAL, UNKNOWN
	}

	public enum Animacy {
		ANIMATE, INANIMATE, UNKNOWN
	}

	public enum Person {
		I, YOU, HE, SHE, WE, THEY, IT, UNKNOWN, YOUS
	}
	
	public enum MentionType {
		Pronominal, Nominal, Proper
	};
	
	public MentionType mentionType;

	public List<String> getWordSet() {
		return Arrays.asList(this.original.split("\\s+"));
	}
	
	public String lemma = "";
	public String buckWalter = "";
	public String original = "";
	public String source = "";
	public String buckUnWalter = "";
	
	public String semClass;
	
	public String toName() {
		return this.start + "," + this.end;
	}
	
	public int hashCode() {
		String str = this.getS() + "," + this.getE();
		return str.hashCode();
	}

	public MyTreeNode maxTreeNode;
	public MyTreeNode minTreeNode;
	public boolean singleton = false;
	public int PRONOUN_TYPE;
	public Entity entity;

	public boolean isNT;

	public int anaphoric = 0;

	public HashSet<EntityMention> roleSet = new HashSet<EntityMention>();

	public boolean equals(Object em2) {
		if (this.getS() == ((EntityMention) em2).getS() && this.getE() == ((EntityMention) em2).getE()) {
			return true;
		} else {
			return false;
		}
	}

	public boolean moreRepresentativeThan(EntityMention m) {
		if (m == null) {
			return true;
		}
		if (mentionType != m.mentionType) {
			if ((mentionType == MentionType.Proper && m.mentionType != MentionType.Proper)
					|| (mentionType == MentionType.Nominal && m.mentionType == MentionType.Pronominal)) {
				return true;
			} else {
				return false;
			}
		} else {
			if (this.headStart - this.start > m.headStart - m.start) {
				return true;
			} else if (this.headStart < m.headStart) {
				return true;
			} else {
				return false;
			}
		}
	}

	public boolean generic = false;

	public double typeConfidence = Double.NEGATIVE_INFINITY;

	public double subTypeConfidence = Double.NEGATIVE_INFINITY;

	public int start;
	public int end;

	public String extent = "";

	public MyTreeNode treeNode;

	public int sentenceID;
	public int startLeaf;
	public int endLeaf;

	public int headStart;
	public int headEnd;
	public String head = "";

	public boolean isNNP = false;
	public boolean isSub = false;
	public boolean isPronoun = false;
	public ArrayList<String> modifyList = new ArrayList<String>();// record all
	// the
	// modifiers
	public boolean isProperNoun;

	public Numb number;
	public Gender gender;
	public Animacy animacy;
	public Person person;

	public String ner = "OTHER";
	// public String semClass="OTHER";
	// public String subType = "O-OTHER";

	public int index;
	public EntityMention antecedent;
	public int entityIndex;

	public int position[];

	public String getContent() {
		return this.head;
	}

	public int getS() {
		return this.start;
	}

	public int getE() {
		return this.end;
	}

	public boolean flag = false;

	public String type;

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

	public String getExtent() {
		return extent;
	}

	public void setExtent(String extent) {
		this.extent = extent;
	}

	public int getHeadStart() {
		return headStart;
	}

	public void setHeadStart(int headStart) {
		this.headStart = headStart;
	}

	public int getHeadEnd() {
		return headEnd;
	}

	public void setHeadEnd(int headEnd) {
		this.headEnd = headEnd;
	}

	public String getHead() {
		return head;
	}

	public void setHead(String head) {
		this.head = head;
	}

	public int getIndex() {
		return index;
	}

	public void setIndex(int index) {
		this.index = index;
	}

	public EntityMention getAntecedent() {
		return antecedent;
	}

	public void setAntecedent(EntityMention antecedent) {
		this.antecedent = antecedent;
	}

	public int getEntityIndex() {
		return entityIndex;
	}

	public void setEntityIndex(int entityIndex) {
		this.entityIndex = entityIndex;
	}

	public boolean isFlag() {
		return flag;
	}

	public void setFlag(boolean flag) {
		this.flag = flag;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public EntityMention() {

	}

	public String refID = "";
	
	boolean embed = true;
	
	public String subType;

	public EntityMention(int start, int end) {
		this.start = start;
		this.end = end;
	}

	// (14, 15) (20, -1) (10, 20)
	public int compareTo(EntityMention emp2) {
		int diff = this.getS() - emp2.getS();
		if (diff == 0)
			return emp2.getE() - this.getE();
		else
			return diff;
		// if(this.getE()!=-1 && emp2.getE()!=-1) {
		// int diff = this.getE() - emp2.getE();
		// if(diff==0) {
		// return this.getS() - emp2.getS();
		// } else
		// return diff;
		// } else if(this.getE()==-1 && emp2.headEnd!=-1){
		// int diff = this.getS() - emp2.getE();
		// if(diff==0) {
		// return -1;
		// } else
		// return diff;
		// } else if(this.headEnd!=-1 && emp2.headEnd==-1){
		// int diff = this.getE() - emp2.getS();
		// if(diff==0) {
		// return 1;
		// } else
		// return diff;
		// } else {
		// return this.getS()-emp2.getS();
		// }
	}

	public String toString() {
		// sb.append("start: ").append(this.start).append(" end: ").append(this.end).append(" ").append(this.source);
		// sb.append("headstart: ").append(this.headStart).append(" headend: ").append(this.headEnd).append(
		// " ").append(this.head);
		String str = this.original + " (" + this.animacy + " " + this.number + " " + this.gender
				+ " " + this.ner + " " + this.headStart + ") [" + this.sentenceID + ":" + this.position[1] + "," + this.position[2] + " "
				+ this.start + " " + this.end+"]";
		return str;
	}
}