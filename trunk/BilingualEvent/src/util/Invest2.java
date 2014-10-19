package util;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import model.ACEChiDoc;
import model.EntityMention;
import model.EventMention;
import model.EventMentionArgument;

public class Invest2 {
	public static void main(String args[]) {
		if (args.length != 2) {
			System.out.println("java ~ [pipe|joint] [svm|maxent]");
			System.exit(1);
		}
		String arch = args[0];
		String model = args[1];
		ArrayList<String> files = Common.getLines("ACE_Chinese_all");

		HashMap<String, HashMap<String, EventMention>> pipelineResults = new HashMap<String, HashMap<String, EventMention>>();
		for (int i = 1; i <= 10; i++) {
			HashMap<String, HashMap<String, EventMention>> part = Util.readResult(arch + "_" + model + "/result"
					+ Integer.toString(i), "chi");
			for (String key : part.keySet()) {
				pipelineResults.put(key, part.get(key));
			}
		}

		for (String file : files) {
			int a = file.lastIndexOf(File.separator);
			System.out.println(file.substring(a + 1));
			System.out.println(file);
			System.out.println("=====================");
			ACEChiDoc document = new ACEChiDoc(file);
			ArrayList<EntityMention> goldEntityMentions = document.goldEntityMentions;

			ArrayList<EventMention> eventMentions = new ArrayList<EventMention>();
			if (pipelineResults.containsKey(file)) {
				eventMentions.addAll(pipelineResults.get(file).values());
			}

			for (EventMention eventMention : eventMentions) {
				eventMention.setAnchor(document.content.substring(eventMention.getAnchorStart(),
						eventMention.getAnchorEnd() + 1).replace(" ", "").replace("\n", ""));

				for (EventMentionArgument argument : eventMention.getEventMentionArguments()) {
					argument.setExtent(document.content.substring(argument.getStart(), argument.getEnd() + 1).replace(
							" ", "").replace("\n", ""));
				}
			}
			Collections.sort(eventMentions);
			for (EventMention eventMention : eventMentions) {
				String trigger = eventMention.getAnchor().replace(" ", "").replace("\n", "");
				if((model.equalsIgnoreCase("maxent")&&eventMention.confidence<0.5)||(model.startsWith("svm")&&eventMention.confidence<0)) { 
					continue;
				}
				System.out.println(eventMention.getAnchor() + ":" + eventMention.type + "#" + eventMention.subType
						+ "$" + eventMention.confidence);
				for (EventMentionArgument argument : eventMention.getEventMentionArguments()) {
					int entityID = getEntityID(argument, goldEntityMentions);
					if(!argument.getRole().equals("null"))
					System.out.println(argument.getExtent() + "@" + argument.getRole() + "###" + entityID);
				}
				System.out.println("-----------------");
			}
			System.out.println("======================");
		}
	}

	public static int getEntityID(EventMentionArgument argument, ArrayList<EntityMention> mentions) {
		for (EntityMention mention : mentions) {
			if (argument.getStart() == mention.start && argument.getEnd() == mention.end) {
				return mention.entityIndex;
			}
		}
		return -1;
	}

	static HashMap<String, HashMap<String, EventMention>> systemEMses;

}
