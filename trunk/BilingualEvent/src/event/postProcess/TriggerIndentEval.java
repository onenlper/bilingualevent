package event.postProcess;

import java.util.ArrayList;
import java.util.HashMap;

import model.ACEChiDoc;
import model.EventMention;
import util.Common;
import util.Util;

public class TriggerIndentEval {

	static HashMap<String, HashMap<String, EventMention>> systemEMses;

	public static void loadSVMSystem() {
		systemEMses = new HashMap<String, HashMap<String, EventMention>>();
		String suffix = "";
		if(mode.startsWith("dev")) {
			suffix = "_dev";
		}
		ArrayList<String> lines = Common.getLines("data/Pipe_triggerLines_" + mode + Util.part);
		ArrayList<String> predicts = Common.getLines("/users/yzcchen/tool/svm_multiclass/coling2012/Pipe_triggerIndent" + suffix
				+ Util.part);
//		ArrayList<String> types = Common.getLines("/users/yzcchen/tool/svm_multiclass/coling2012/Pipe_triggerType" + suffix
//				+ Util.part);
		ArrayList<String> subTypes = Common.getLines("/users/yzcchen/tool/svm_multiclass/coling2012/Pipe_triggerSubType" + suffix
				+ Util.part);
		for (int i = 0; i < lines.size(); i++) {
			String line = lines.get(i);
//			String typeLine = types.get(i);
			String subTypeLine = subTypes.get(i);

//			String tokens[] = typeLine.split("\\s+");
//			String type = Util.types.get(Integer.valueOf(tokens[0]) - 1);
//			
//			double typeConfidence =0;
//			if(!tokens[Integer.valueOf(tokens[0])].equalsIgnoreCase("nan")) {
//				typeConfidence = Double.valueOf(tokens[Integer.valueOf(tokens[0])]);
//			} 
			
			String predictLine = predicts.get(i);
			String tokens[] = predictLine.split("\\s+");

			int predict = Integer.valueOf(tokens[0]);
			double confidence = -100000;
			if(!tokens[1].equalsIgnoreCase("nan")) {
				confidence = Double.valueOf(tokens[1]);
			} 

			tokens = subTypeLine.split("\\s+");
			String subType = Util.subTypes.get(Integer.valueOf(tokens[0]) - 1);
			double subTypeConfidence =0;
			if(!tokens[Integer.valueOf(tokens[0])].equalsIgnoreCase("nan")) {
				subTypeConfidence = Double.valueOf(tokens[Integer.valueOf(tokens[0])]);
			} 
			
			tokens = line.split("\\s+");
			String fileID = tokens[0];
			int start = Integer.valueOf(tokens[1]);
			int end = Integer.valueOf(tokens[2]);
			String inferFrom = tokens[4];
			EventMention em = new EventMention();
			em.setAnchorStart(start);
			em.setAnchorEnd(end);
//			em.setType(type);
			em.setSubType(subType);
//			System.out.println(subType);
			em.confidence = confidence;
//			em.typeConfidence = typeConfidence;
			em.subTypeConfidence = subTypeConfidence;
			em.svm = true;
			em.inferFrom = inferFrom;
			if (systemEMses.containsKey(fileID)) {
				systemEMses.get(fileID).put(em.toString(), em);
			} else {
				HashMap<String, EventMention> ems = new HashMap<String, EventMention>();
				ems.put(em.toString(), em);
				systemEMses.put(fileID, ems);
			}
		}
	}

	public static void loadMaxEntSystem() {
		systemEMses = new HashMap<String, HashMap<String, EventMention>>();
		ArrayList<String> lines = Common.getLines("data/Pipe_triggerLines_" + mode + Util.part);
		String suffix = "";
		if(mode.startsWith("dev")) {
			suffix = "_dev";
		}
		ArrayList<String> predicts = Common.getLines("/users/yzcchen/tool/maxent/bin/coling2012/Pipe_triggerIndent" + suffix
				+ Util.part);
//		ArrayList<String> types = Common.getLines("/users/yzcchen/tool/maxent/bin/coling2012/Pipe_triggerType" + suffix
//				+ Util.part);
		ArrayList<String> subTypes = Common.getLines("/users/yzcchen/tool/maxent/bin/coling2012/Pipe_triggerSubType" + suffix
				+ Util.part);
		for (int i = 0; i < lines.size(); i++) {
			String line = lines.get(i);
//			String typeLine = types.get(i);
			String subTypeLine = subTypes.get(i);

//			String tokens[] = typeLine.split("\\s+");
			String tokens[];
			String type = "";
			double maxValue = 0;
//			for (int k = 0; k < tokens.length / 2; k++) {
//				String pre = tokens[k * 2];
//				double val = Double.valueOf(tokens[k * 2 + 1]);
//				if (val >= maxValue) {
//					type = pre;
//					maxValue = val;
//				}
//			}
//			type = Util.types.get(Integer.parseInt(type) - 1);

			maxValue = -1;
			String predict = "";
			String predictLine = predicts.get(i);
			tokens = predictLine.split("\\s+");
			double confidence = 0;
			for (int k = 0; k < tokens.length / 2; k++) {
				String pre = tokens[k * 2];
				double val = Double.valueOf(tokens[k * 2 + 1]);
				if (val >= maxValue) {
					predict = pre;
					maxValue = val;
				}
				if (Integer.valueOf(predict) == 1) {
					confidence = val;
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
			subType = Util.subTypes.get(Integer.valueOf(subType) - 1);

			tokens = line.split("\\s+");
			String fileID = tokens[0];
			int start = Integer.valueOf(tokens[1]);
			int end = Integer.valueOf(tokens[2]);
			EventMention em = new EventMention();
			em.setAnchorStart(start);
			em.setAnchorEnd(end);
//			em.setType(type);
			em.setSubType(subType);
			em.confidence = confidence;
			em.maxent = true;
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

	public static void main(String args[]) {
		if (args.length < 3) {
			System.out.println("java ~ [test|development] [svm|maxent] [folder]");
			System.exit(1);
		}
		mode = args[0];
		Util.part = args[2];
		if (args[1].equalsIgnoreCase("maxent")) {
			loadMaxEntSystem();
		} else {
			loadSVMSystem();
		}
		ArrayList<String> files = Common.getLines("ACE_Chinese_" + mode + Util.part);
		double gold = 0;
		double system = 0;
		double hit = 0;
//		double hitType = 0;
		double hitSubType = 0;
		ArrayList<EventMention> recallErrors = new ArrayList<EventMention>();
		ArrayList<EventMention> precisionErrors = new ArrayList<EventMention>();
		for (String file : files) {
			ArrayList<EventMention> golds = (new ACEChiDoc(file)).goldEventMentions;

			ArrayList<EventMention> systems = new ArrayList<EventMention>();
			if (systemEMses.containsKey(file)) {
				for (EventMention mention : systemEMses.get(file).values()) {
					if ((mention.confidence > 0.5 && mention.maxent) || (mention.confidence > 0 && mention.svm)) {
						systems.add(mention);
					}
				}
			}

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
//								if (g.type.equals(s.type)) {
//									hitType++;
//								}
								if (g.subType.equals(s.subType)) {
									hitSubType++;
								}
//								System.out.println(g.subType + "#" + s.subType);
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
		System.out.println("s: " + system);
		System.out.println("g: " + gold);
		System.out.println("H: " + hit);
		double p = hit / system;
		double r = hit / gold;
		double f = 2 * p * r / (p + r);
		System.out.println(mode);
		System.out.println("R: " + r * 100);
		System.out.println("P: " + p * 100);
		System.out.println("F: " + f * 100);

//		System.out.println("====Trigger Type====");
//		double p2 = hitType / system;
//		double r2 = hitType / gold;
//		double f2 = 2 * p2 * r2 / (p2 + r2);
//		System.out.println(mode);
//		System.out.println("R: " + r2 * 100);
//		System.out.println("P: " + p2 * 100);
//		System.out.println("F: " + f2 * 100);

		System.out.println("====Trigger SubType====");
		double p3 = hitSubType / system;
		double r3 = hitSubType / gold;
		double f3 = 2 * p3 * r3 / (p3 + r3);
		System.out.println(mode);
		System.out.println("R: " + r3 * 100);
		System.out.println("P: " + p3 * 100);
		System.out.println("F: " + f3 * 100);

		if(args[0].startsWith("dev")) {
			Util.outputResult(systemEMses, "pipe_" + args[1] + "_dev/result.trigger" + Util.part);
		} else {
			if(args.length==4 && args[3].equals("discourse")) {
				Util.outputResult(systemEMses, "pipe_" + args[1] + "/result.trigger.discourse" + Util.part);
			} else {
				Util.outputResult(systemEMses, "pipe_" + args[1] + "/result.trigger" + Util.part);
			}
		}
	}
}
