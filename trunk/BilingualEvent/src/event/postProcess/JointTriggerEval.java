package event.postProcess;

import java.util.ArrayList;
import java.util.HashMap;

import model.ACEChiDoc;
import model.EventMention;
import util.Common;
import util.Util;

public class JointTriggerEval {

	public static HashMap<String, HashMap<String, EventMention>> systemEMses;

	//load stanford maxent
	public static void loadMaxEntSystem2(String folder) {
		System.err.println("GE");
		systemEMses = new HashMap<String, HashMap<String, EventMention>>();
		ArrayList<String> lines = Common.getLines("/users/yzcchen/workspace/NAACL2013-B/src/data/Joint_triggers_test_system" + Util.part);
		ArrayList<String> typeLines = Common.getLines("/users/yzcchen/tool/stanford-classifier-2012-11-11/output"
				+ Util.part);
		for (int i = 0; i < lines.size(); i++) {
			String line = lines.get(i);
			String typeLine = typeLines.get(i);

			String tokens[] = typeLine.split("\\s+");
			String subType = tokens[2];
			subType = Util.subTypes.get(Integer.valueOf(subType) - 1);
			tokens = line.split("\\s+");
			String fileID = tokens[0];
			int start = Integer.valueOf(tokens[1]);
			int end = Integer.valueOf(tokens[2]);
			EventMention em = new EventMention();
			em.setAnchorStart(start);
			em.setAnchorEnd(end);
			em.inferFrom = tokens[4];
			em.setSubType(subType);
			em.confidence = 1;
			if (systemEMses.containsKey(fileID)) {
				systemEMses.get(fileID).put(em.toString(), em);
			} else {
				HashMap<String, EventMention> ems = new HashMap<String, EventMention>();
				ems.put(em.toString(), em);
				systemEMses.put(fileID, ems);
			}
		}
	}

	public static void loadMaxEntSystem(String folder) {
		System.err.println("GE");
		systemEMses = new HashMap<String, HashMap<String, EventMention>>();
		ArrayList<String> lines = Common.getLines("data/Joint_triggers_test_system" + Util.part);
		ArrayList<String> typeLines = Common
				.getLines("/users/yzcchen/tool/maxent/bin/JointTriggerOutput_test" + Util.part);
		for (int i = 0; i < lines.size(); i++) {
			String line = lines.get(i);
			String typeLine = typeLines.get(i);

			String tokens[] = typeLine.split("\\s+");
			String subType = "";
			double maxValue = 0;
			for (int k = 0; k < tokens.length / 2; k++) {
				String pre = tokens[k * 2];
				double val = Double.valueOf(tokens[k * 2 + 1]);
				if (val >= maxValue) {
					subType = pre;
					maxValue = val;
				}
			}
			subType = Util.subTypes.get(Integer.valueOf(subType) - 1);
			tokens = line.split("\\s+");
			String fileID = tokens[0];
			int start = Integer.valueOf(tokens[1]);
			int end = Integer.valueOf(tokens[2]);
			EventMention em = new EventMention();
			em.setAnchorStart(start);
			em.setAnchorEnd(end);
			em.inferFrom = tokens[4];
			em.setSubType(subType);
			em.confidence = 1;
			if (systemEMses.containsKey(fileID)) {
				systemEMses.get(fileID).put(em.toString(), em);
			} else {
				HashMap<String, EventMention> ems = new HashMap<String, EventMention>();
				ems.put(em.toString(), em);
				systemEMses.put(fileID, ems);
			}
		}
	}

	static String mode;

	public static void loadSVMSystem(String folder) {
		systemEMses = new HashMap<String, HashMap<String, EventMention>>();
		ArrayList<String> lines = Common.getLines("data/Joint_triggers_" + folder + "_system" + Util.part);
		ArrayList<String> typeLines = Common.getLines("/users/yzcchen/tool/svm_multiclass/JointTriggerOutput_" + folder
				+ Util.part);
		for (int i = 0; i < lines.size(); i++) {
			String line = lines.get(i);
			String typeLine = typeLines.get(i);

			String tokens[] = typeLine.split("\\s+");

			String subType = Util.subTypes.get(Integer.valueOf(tokens[0]) - 1);
			double typeConfidence = 0;
			if (!tokens[Integer.valueOf(tokens[0])].equalsIgnoreCase("nan")) {
				typeConfidence = Double.valueOf(tokens[Integer.valueOf(tokens[0])]);
			}

			ArrayList<Double> confidences = new ArrayList<Double>();
			for (int k = 1; k < tokens.length; k++) {
				if (tokens[k].equalsIgnoreCase("nan")) {
					confidences.add(-1000000.0);
				} else {
					confidences.add(Double.valueOf(tokens[k]));
				}
			}

			tokens = line.split("\\s+");
			String fileID = tokens[0];
			int start = Integer.valueOf(tokens[1]);
			int end = Integer.valueOf(tokens[2]);

			String inferFrom = tokens[4];

			EventMention em = new EventMention();
			em.setAnchorStart(start);
			em.setAnchorEnd(end);
			em.setSubType(subType);
			em.inferFrom = inferFrom;
			em.typeConfidences = confidences;
			em.typeConfidence = typeConfidence;
			if (subType.equals("null")) {
				em.confidence = -1;
			} else {
				em.confidence = 1;
			}
			if (systemEMses.containsKey(fileID)) {
				systemEMses.get(fileID).put(em.toString(), em);
			} else {
				HashMap<String, EventMention> ems = new HashMap<String, EventMention>();
				ems.put(em.toString(), em);
				systemEMses.put(fileID, ems);
			}
		}
	}

	public static void main(String args[]) {
		if (args.length < 3) {
			System.out.println("java ~ [test|development] [svm|maxent] [folder]");
			System.exit(1);
		}
		Util.part = args[2];
		mode = args[0];
		if (args[1].equals("svm")) {
			loadSVMSystem(mode);
		} else if (args[1].equals("maxent")) {
			loadMaxEntSystem(mode);
		} else if (args[1].equals("maxent_std")) {
			loadMaxEntSystem2(mode);
		}
		ArrayList<String> files = Common.getLines("ACE_Chinese_" + mode + Util.part);
		double gold = 0;
		double system = 0;
		double hit = 0;
		double hitType = 0;
		ArrayList<EventMention> recallErrors = new ArrayList<EventMention>();
		ArrayList<EventMention> precisionErrors = new ArrayList<EventMention>();
		for (String file : files) {
			ArrayList<EventMention> golds = (new ACEChiDoc(file)).goldEventMentions;
			if (golds != null) {
				gold += golds.size();
			}
			ACEChiDoc document = new ACEChiDoc(file);
			ArrayList<EventMention> systems = new ArrayList<EventMention>();
			if (systemEMses.containsKey(file)) {
				for (String key : systemEMses.get(file).keySet()) {
					EventMention s = systemEMses.get(file).get(key);
					if (s.subType.equalsIgnoreCase("null")) {
						continue;
					}
					systems.add(s);
				}
				system += systems.size();
				if (golds != null) {
					for (int i = 0; i < golds.size(); i++) {
						EventMention g = golds.get(i);
						for (int j = 0; j < systems.size(); j++) {
							EventMention s = systems.get(j);
							if (g.equals(s)) {
								hit++;
								if (g.subType.equals(s.subType)) {
									hitType++;
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
			if (systems.size() != 0) {
				for (EventMention em : systems) {
					em.document = document;
					em.setAnchor(em.document.content.substring(em.getAnchorStart(), em.getAnchorEnd() + 1).replace(
							"\n", "").replaceAll("\\s+", ""));
					precisionErrors.add(em);
				}
			}
		}
		// System.out.println("Recall Error");
		// for (EventMention em : recallErrors) {
		// System.out.println(em + ":" + em.getAnchor() + " " +
		// em.document.fileID);
		// }
		// System.out.println("Precision Error");
		// for (EventMention em : precisionErrors) {
		// System.out.println(em + ":" + em.getAnchor() + " " +
		// em.document.fileID);
		// }
		System.out.println("Gold: " + gold);
		System.out.println("System: " + system);
		System.out.println("Hit: " + hit);
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

		if (mode.startsWith("dev")) {
			Util.outputResult(systemEMses, "joint_" + args[1] + "_dev/result.trigger" + args[2]);
		} else {
			if (args.length == 4 && args[3].equals("discourse")) {
				Util.outputResult(systemEMses, "joint_" + args[1] + "/result.trigger.discourse" + args[2]);
			} else {
				Util.outputResult(systemEMses, "joint_" + args[1] + "/result.trigger" + args[2]);
				Util.outputResult(systemEMses, "joint_" + args[1] + "/result" + args[2]);
			}

		}
	}
}
