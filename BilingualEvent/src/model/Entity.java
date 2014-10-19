package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;

import model.EntityMention.Animacy;
import model.EntityMention.Gender;
import model.EntityMention.Numb;

public class Entity implements Comparable<Entity> {

	protected Set<Numb> numbers;
	protected Set<Gender> genders;
	protected Set<Animacy> animacies;

	public ArrayList<EntityMention> mentions;
	public String type;
	public String subType;
	public int entityIdx;

	public ArrayList<EntityMention> getMentions() {
		return mentions;
	}

	public void addMention(EntityMention em) {
		em.entity = this;
		this.mentions.add(em);
	}

	public void setMentions(ArrayList<EntityMention> mentions) {
		this.mentions = mentions;
	}

	public EntityMention getFirstMention() {
		Collections.sort(mentions);
		return mentions.get(0);
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

	public Entity() {
		mentions = new ArrayList<EntityMention>();
	}

	EntityMention mostReppresent;

	public EntityMention getMostRepresent() {
		Collections.sort(this.mentions);
		this.mostReppresent = this.mentions.get(0);
		for (EntityMention m : mentions) {
			if (m.moreRepresentativeThan(this.mostReppresent)) {
				this.mostReppresent = m;
			}
		}
		return this.mostReppresent;
	}

	public int compareTo(Entity emp2) {
		Collections.sort(mentions);
		Collections.sort(emp2.mentions);
		int diff = mentions.get(0).headStart - emp2.mentions.get(0).headStart;
		if (diff == 0)
			return mentions.get(0).headEnd - emp2.mentions.get(0).headEnd;
		return diff;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Type: ").append(type).append(" SubType: ").append(subType).append("\n");
		for (EntityMention em : mentions) {
			sb.append(em.toString()).append("\n");
		}
		return sb.toString();
	}
}
