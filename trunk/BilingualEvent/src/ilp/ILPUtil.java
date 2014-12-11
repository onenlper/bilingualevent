package ilp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import model.EventMention;
import util.Common;

public class ILPUtil {

	public static HashSet<String> loadNegativeConstrain(ArrayList<EventMention> mentions, String fileID) {
		String baseFolder = "/users/yzcchen/workspace/CoNLL-2012/src/ace/maxent_0/";
		ArrayList<String> allLines2 = Common.getLines(baseFolder + "/all.txt2");

		int k = allLines2.indexOf(fileID);
		String negtivePath = baseFolder + Integer.toString(k) + ".negativeLinks";
		HashSet<String> negativeLines = new HashSet<String>();

		ArrayList<String> lines = new ArrayList<String>();
		lines.addAll(Common.getLines(negtivePath));
		negativeLines.addAll(lines);
		return negativeLines;
	}

	public static HashMap<String, Double> loadCorefSVMProb(ArrayList<EventMention> mentions, String fileID) {
		String baseFolder = "/users/yzcchen/workspace/CoNLL-2012/src/ace/maxent_0/";
		ArrayList<String> allLines2 = Common.getLines(baseFolder + "/all.txt2");

		int k = allLines2.indexOf(fileID);

		ArrayList<String> pairLines = Common.getLines(baseFolder + Integer.toString(k) + ".mpextent");
		ArrayList<String> confLines = Common.getLines(baseFolder + Integer.toString(k) + ".mppred");

		HashMap<String, Double> confidences = new HashMap<String, Double>();
		for (int i = 0; i < pairLines.size(); i++) {
			String pairLine = pairLines.get(i);
			String confLine = confLines.get(i);
			String tokens[] = pairLine.split(",");
			EventMention m1 = new EventMention();
			EventMention m2 = new EventMention();

			m1.setAnchorStart(Integer.valueOf(tokens[0]));
			m1.setAnchorEnd(Integer.valueOf(tokens[1]));

			m2.setAnchorStart(Integer.valueOf(tokens[2]));
			m2.setAnchorEnd(Integer.valueOf(tokens[3]));

			int m1ID = mentions.indexOf(m1);
			int m2ID = mentions.indexOf(m2);
			double confidence = sigmoid(Double.parseDouble(confLine.toString()), -0.42);
			double confidenceNo = sigmoid(-1.0 * Double.parseDouble(confLine.toString()), -0.42);

			double confNorm = confidence/(confidence+confidenceNo);
			
//			System.err.println(confLine.toString() + "#" + confidence);
			confidences.put(m1ID + "_" + m2ID, confNorm);
		}
		System.err.println("C: " + confidences.size());
		return confidences;
	}

	public static HashMap<String, Double> loadCorefProb(ArrayList<EventMention> mentions, String fileID) {
		String baseFolder = "/users/yzcchen/workspace/CoNLL-2012/src/ace/maxent_0/";
		ArrayList<String> allLines2 = Common.getLines(baseFolder + "/all.txt2");

		int k = allLines2.indexOf(fileID);

		ArrayList<String> pairLines = Common.getLines(baseFolder + Integer.toString(k) + ".maxentextent");
		ArrayList<String> confLines = Common.getLines(baseFolder + Integer.toString(k) + ".maxentpred");

		HashMap<String, Double> confidences = new HashMap<String, Double>();
		for (int i = 0; i < pairLines.size(); i++) {
			String pairLine = pairLines.get(i);
			String confLine = confLines.get(i);
			String tokens[] = pairLine.split(",");
			EventMention m1 = new EventMention();
			EventMention m2 = new EventMention();

			m1.setAnchorStart(Integer.valueOf(tokens[0]));
			m1.setAnchorEnd(Integer.valueOf(tokens[1]));

			m2.setAnchorStart(Integer.valueOf(tokens[2]));
			m2.setAnchorEnd(Integer.valueOf(tokens[3]));

			int m1ID = mentions.indexOf(m1);
			int m2ID = mentions.indexOf(m2);
			double confidence = -1;
			for (String token : confLine.split("\\s+")) {
				if (token.startsWith("1:")) {
					confidence = Double.parseDouble(token.substring(2));
					break;
				}
			}
			confidences.put(m1ID + "_" + m2ID, confidence);
		}
		return confidences;
	}

	public static HashMap<String, HashMap<String, EventMention>> systemEMses;

	public static double c1 = 0;
	
	public static double sigmoid(double value, double alpha) {
		double sig = 1.0 / (1.0 + Math.exp(alpha * (value-c1)));
		return sig;
	}

	public static void loadSVMResult() {
		systemEMses = new HashMap<String, HashMap<String, EventMention>>();
		ArrayList<String> lines = Common
				.getLines("/users/yzcchen/workspace/NAACL2013-B/src/data/Joint_triggers_test_system");
		ArrayList<String> typeLines = Common.getLines("/users/yzcchen/tool/svm_multiclass/JointTriggerOutput_test");
		for (int i = 0; i < lines.size(); i++) {
			String line = lines.get(i);
			String typeLine = typeLines.get(i);

			String tokens[] = typeLine.split("\\s+");

			double typeConfidence[] = new double[34];
			if (!tokens[1].equalsIgnoreCase("nan")) {
				double Norm = 0;
				for (int k = 1; k < tokens.length; k++) {
					typeConfidence[k - 1] = sigmoid(Double.parseDouble(tokens[k]), -0.42);
					Norm += typeConfidence[k - 1];
				}
				for(int f=0;f<typeConfidence.length;f++) {
					typeConfidence[f] = typeConfidence[f]/Norm;
//					System.err.println(typeConfidence[f]);
				}
			} else {
				typeConfidence[33] = 1;
				for(int k = 0;k<33;k++) {
					typeConfidence[k] = 0;
				}
			}

			tokens = line.split("\\s+");
			String fileID = tokens[0];
			fileID = fileID.replace("/users/yzcchen/chen3/coling2012/LDC2006T06/data/Chinese",
					"/users/yzcchen/ACL12/data/ACE2005/Chinese")
					+ ".sgm";

			int start = Integer.valueOf(tokens[1]);
			int end = Integer.valueOf(tokens[2]);

			EventMention em = new EventMention();

			em.setAnchorStart(start);
			em.setAnchorEnd(end);
			em.typeConfidences = new ArrayList(Arrays.asList(typeConfidence));

			if (systemEMses.containsKey(fileID)) {
				systemEMses.get(fileID).put(em.toString(), em);
			} else {
				HashMap<String, EventMention> ems = new HashMap<String, EventMention>();
				ems.put(em.toString(), em);
				systemEMses.put(fileID, ems);
			}
		}
	}

	// load stanford maxent
	public static void loadTriggerProb() {
		systemEMses = new HashMap<String, HashMap<String, EventMention>>();
		ArrayList<String> lines = Common
				.getLines("/users/yzcchen/workspace/NAACL2013-B/src/data/Joint_triggers_test_system");
		ArrayList<String> typeLines = Common.getLines("/users/yzcchen/tool/stanford-classifier-2012-11-11/output");
		for (int i = 0; i < lines.size(); i++) {
			String line = lines.get(i);
			String typeLine = typeLines.get(i);

			String tokens[] = typeLine.split("\\s+");
			double typeConfidence[] = new double[34];
			for (int m = 4; m < tokens.length; m++) {
				String t[] = tokens[m].split(":");
				int idx = Integer.parseInt(t[0]);
				double value = Double.parseDouble(t[1]);
				typeConfidence[idx - 1] = value;
			}

			tokens = line.split("\\s+");
			String fileID = tokens[0];
			fileID = fileID.replace("/users/yzcchen/chen3/coling2012/LDC2006T06/data/Chinese",
					"/users/yzcchen/ACL12/data/ACE2005/Chinese")
					+ ".sgm";
			int start = Integer.valueOf(tokens[1]);
			int end = Integer.valueOf(tokens[2]);
			EventMention em = new EventMention();

			em.setAnchorStart(start);
			em.setAnchorEnd(end);

			em.typeConfidences = new ArrayList(Arrays.asList(typeConfidence));

			if (systemEMses.containsKey(fileID)) {
				systemEMses.get(fileID).put(em.toString(), em);
			} else {
				HashMap<String, EventMention> ems = new HashMap<String, EventMention>();
				ems.put(em.toString(), em);
				systemEMses.put(fileID, ems);
			}
		}
	}
}
