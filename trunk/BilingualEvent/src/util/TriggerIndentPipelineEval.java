package util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import model.ACEChiDoc;
import model.EventMention;

public class TriggerIndentPipelineEval {

	static HashMap<String, ArrayList<EventMention>> systemEMses;

	public static HashMap<String, HashSet<EventMention>> readRefinedEMs() {
		HashMap<String, HashSet<EventMention>> refinedMentions = new HashMap<String, HashSet<EventMention>>();
		ArrayList<String> refineEMLines = Common.getLines("pipeline/pendingTriggers");
		ArrayList<String> refineEMResultsLines = Common.getLines("/users/yzcchen/tool/maxent/bin/test_refine.txt");

		ArrayList<String> confirmLines = Common.getLines("pipeline/confirmedTriggers");

		for (int i = 0; i < refineEMResultsLines.size(); i++) {
			String resultLine = refineEMResultsLines.get(i);
			String refineEMLine = refineEMLines.get(i);

			String label = getEMLabel(resultLine);
			if (Integer.valueOf(label) == 1) {
				confirmLines.add(refineEMLine);
			}
		}
		for (String line : confirmLines) {
			String tokens[] = line.split("\\s+");
			String fileID = tokens[0];
			int start = Integer.valueOf(tokens[1]);
			int end = Integer.valueOf(tokens[2]);
			EventMention mention = new EventMention();
			mention.setAnchorStart(start);
			mention.setAnchorEnd(end);
			HashSet<EventMention> ems = refinedMentions.get(fileID);
			if (ems == null) {
				ems = new HashSet<EventMention>();
				refinedMentions.put(fileID, ems);
			}
			ems.add(mention);
		}

		return refinedMentions;
	}

	public static String getEMLabel(String line) {
		String tokens[] = line.split("\\s+");
		String maxLabel = "";
		double maxValue = -1;
		for (int i = 0; i < tokens.length / 2; i++) {
			String label = tokens[i * 2];
			double value = Double.valueOf(tokens[i * 2 + 1]);
			if (value > maxValue) {
				maxLabel = label;
				maxValue = value;
			}
		}
		return maxLabel;
	}

	public static void loadSystem() {
		systemEMses = new HashMap<String, ArrayList<EventMention>>();

		HashMap<String, HashSet<EventMention>> refinedMentions = readRefinedEMs();

		ArrayList<String> lines = Common.getLines("data/Chinese_triggerIndent_" + mode + "_system");
		ArrayList<String> types = Common.getLines("/users/yzcchen/tool/maxent/bin/" + mode + "_triggerType.txt");

		ArrayList<String> subTypes = Common.getLines("/users/yzcchen/tool/maxent/bin/" + mode + "_triggerSubType.txt");

		for (int i = 0; i < lines.size(); i++) {
			String line = lines.get(i);
			String typeLine = types.get(i);
			String subTypeLine = subTypes.get(i);

			String tokens[] = typeLine.split("\\s+");
			String type = "";
			double maxValue = 0;
			for (int k = 0; k < tokens.length / 2; k++) {
				String pre = tokens[k * 2];
				double val = Double.valueOf(tokens[k * 2 + 1]);
				if (val >= maxValue) {
					type = pre;
					maxValue = val;
				}
			}

			String subType = "";
			maxValue = -1;
			tokens = subTypeLine.split("\\s+");
			for (int k = 0; k < tokens.length / 2; k++) {
				String pre = tokens[k * 2];
				double val = Double.valueOf(tokens[k * 2 + 1]);
				if (val >= maxValue) {
					subType = pre;
					maxValue = val;
				}
			}

			tokens = line.split("\\s+");
			String fileID = tokens[0];
			int start = Integer.valueOf(tokens[1]);
			int end = Integer.valueOf(tokens[2]);
			EventMention em = new EventMention();
			em.setAnchorStart(start);
			em.setAnchorEnd(end);
			em.setType(type);
			em.setSubType(subType);

			if (refinedMentions.containsKey(fileID) && refinedMentions.get(fileID).contains(em)) {

				if (systemEMses.containsKey(fileID)) {
					systemEMses.get(fileID).add(em);
				} else {
					ArrayList<EventMention> ems = new ArrayList<EventMention>();
					ems.add(em);
					systemEMses.put(fileID, ems);
				}
			}
		}
	}

	static String mode;

	public static void main(String args[]) {
		if (args.length != 1) {
			System.out.println("java ~ [test]");
			System.exit(1);
		}
		mode = args[0];
		loadSystem();
		ArrayList<String> files = Common.getLines("ACE_Chinese_" + mode);
		double gold = 0;
		double system = 0;
		double hit = 0;
		double hitType = 0;
		double hitSubType = 0;
		ArrayList<EventMention> recallErrors = new ArrayList<EventMention>();
		ArrayList<EventMention> precisionErrors = new ArrayList<EventMention>();
		for (String file : files) {
			ArrayList<EventMention> golds = (new ACEChiDoc(file)).goldEventMentions;
			ArrayList<EventMention> systems = systemEMses.get(file);
			if (golds != null) {
				gold += golds.size();
			}
			ACEChiDoc document = new ACEChiDoc(file);
			if (systems != null) {
				system += systems.size();
				if (golds != null) {
					for (int i = 0; i < golds.size(); i++) {
						EventMention g = golds.get(i);
						for (int j = 0; j < systems.size(); j++) {
							EventMention s = systems.get(j);
							if (g.equals(s)) {
								hit++;
								if (g.type.equals(s.type)) {
									hitType++;
								}
								if (g.subType.equals(s.subType)) {
									hitSubType++;
								}
								golds.remove(i);
								i--;
								systems.remove(j);
								break;
							}
						}
					}
				}
			}
			if (golds != null) {
				for (EventMention em : golds) {
					em.document = document;
					em.setAnchor(em.document.content.substring(em.getAnchorStart(), em.getAnchorEnd() + 1).replace(
							"\n", "").replaceAll("\\s+", ""));
					recallErrors.add(em);
				}
			}
			if (systems != null) {
				for (EventMention em : systems) {
					em.document = document;
					em.setAnchor(em.document.content.substring(em.getAnchorStart(), em.getAnchorEnd() + 1).replace(
							"\n", "").replaceAll("\\s+", ""));
					precisionErrors.add(em);
				}
			}
		}
		// System.out.println("Recall Error");
		// for(EventMention em : recallErrors) {
		// System.out.println(em + ":" + em.getAnchor() + " " +
		// em.document.fileID);
		// }
		// System.out.println("Precision Error");
		// for(EventMention em : precisionErrors) {
		// System.out.println(em + ":" + em.getAnchor() + " " +
		// em.document.fileID);
		// }

		System.out.println("====Trigger Identify====");
		double p = hit / system;
		double r = hit / gold;
		double f = 2 * p * r / (p + r);
		System.out.println(mode);
		System.out.println("R: " + r * 100);
		System.out.println("P: " + p * 100);
		System.out.println("F: " + f * 100);

		System.out.println("====Trigger Type====");
		double p2 = hitType / system;
		double r2 = hitType / gold;
		double f2 = 2 * p2 * r2 / (p2 + r2);
		System.out.println(mode);
		System.out.println("R: " + r2 * 100);
		System.out.println("P: " + p2 * 100);
		System.out.println("F: " + f2 * 100);

		System.out.println("====Trigger SubType====");
		double p3 = hitSubType / system;
		double r3 = hitSubType / gold;
		double f3 = 2 * p3 * r2 / (p3 + r2);
		System.out.println(mode);
		System.out.println("R: " + r3 * 100);
		System.out.println("P: " + p3 * 100);
		System.out.println("F: " + f3 * 100);
	}
}
