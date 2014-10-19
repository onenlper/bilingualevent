package util;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import model.ACEChiDoc;
import model.EntityMention;
import model.EventChain;
import model.EventMention;
import model.EventMentionArgument;

public class Invest3 {

	static ACEChiDoc document;

	static HashMap<String, Integer> typeStats = new HashMap<String, Integer>();
	static HashMap<String, HashMap<String, Integer>> pairStats = new HashMap<String, HashMap<String, Integer>>();

	public static void addMap(String pair, HashMap<String, Integer> stats) {
		Integer i = stats.get(pair);
		if (i == null) {
			stats.put(pair, 1);
		} else {
			stats.put(pair, i.intValue() + 1);
		}
	}

	public static void main(String args[]) {
		ArrayList<String> files = Common.getLines("ACE_Chinese_all");
		// HashMap<String, HashSet<String>> knownTrigger =
		// Common.readFile2Map6("chinese_trigger_known");
		int zeroArg = 0;
		int triggerNumber = 0;
		
		int chains = 0;
		int events = 0;
		
		for (String file : files) {
			// System.out.println(G++);
			int a = file.lastIndexOf(File.separator);
			// System.out.println(file.substring(a + 1));
			System.out.println(file);
			document = new ACEChiDoc(file);
			
			HashSet<String> set = new HashSet<String>();
			ArrayList<EventChain> goldEventChains = document.goldEventChains;
			
			chains += goldEventChains.size();
			
			for (EventChain chain : goldEventChains) {
				ArrayList<EventMention> eventMentions = chain.getEventMentions();
				
				events += eventMentions.size();
				// if (eventMentions.size() <= 1) {
				// continue;
				// }
				// System.out.println("======================");
				for (EventMention e : eventMentions) {
					if (e.getEventMentionArguments().size() == 0) {
						zeroArg++;
						// System.out.println("X" + eventMention.getAnchor() +
						// ":" +
						// eventMention.type + "#" + eventMention.subType);
					}
					triggerNumber++;
					set.add(e.subType);
				}
			}

		}
		System.out.println("chains:" + chains);
		System.out.println("events:" + events);
		System.out.println("File docs: " + files.size());
		System.out.println(triggerNumber);
	}

	private static StringBuilder printIt(EventMention eventMention) {
		StringBuilder sb = new StringBuilder();
		sb.append("Example: ").append(eventMention.getLdcScope()).append("\n");
		sb.append("Trigger: ").append(eventMention.getAnchor()).append("\n");
		sb.append("Type: ").append(eventMention.getSubType() + "#" + eventMention.getType()).append("\n");
		sb.append("Polarity: ").append(eventMention.polarity).append("\n");
		sb.append("Modality: ").append(eventMention.modality).append("\n");
		sb.append("Genericity: ").append(eventMention.genericity).append("\n");
		sb.append("Tense: ").append(eventMention.tense).append("\n");
		for (EventMentionArgument argument : eventMention.getEventMentionArguments()) {
			String refID = argument.getRefID();
			int eid = -1;
			if (document.id2EntityMap.containsKey(refID)) {
				eid = document.id2EntityMap.get(refID).entityIdx;
			}
			sb.append("Argument (").append(argument.getRole()).append(" " + eid).append("): ").append(
					argument.getExtent()).append("\n");
		}
		// sb.append("=============================").append("\n");
		return sb;
	}

	public static int getEntityID(EventMentionArgument argument, ArrayList<EntityMention> mentions) {
		for (EntityMention mention : mentions) {
			if (argument.getStart() == mention.start && argument.getEnd() == mention.end) {
				return mention.entityIndex;
			}
		}
		return -1;
	}

}
