package event.postProcess;

import java.util.ArrayList;
import java.util.HashMap;

import model.ACEDoc;
import model.ACEEngDoc;
import model.EventMention;
import util.Common;

public class AttriEvaluate {
	
	static String part = "";
	
	public static void main(String args[]) throws Exception {
		if(args.length!=3) {
			System.out.println("java ~ mode attribute part");
			System.exit(1);
		}
		String mode = args[0];
		String attribute = args[1];
		part = args[2];
		systemEMses = loadSystem(mode, attribute);
		eva(args, mode, attribute);
		System.out.println("Baseline: ");
		
		for(String key : systemEMses.keySet()) {
			ArrayList<EventMention> ems = systemEMses.get(key);
			for(EventMention em : ems) {
				if(attribute.equals("polarity")) {
					em.polarity = "Positive";
				} else if(attribute.equals("modality")) {
					em.modality = "Asserted";
				} else if(attribute.equals("genericity")) {
					em.genericity = "Specific";
				} else if(attribute.equals("tense")) {
					em.tense = "Past";
				}
			}
		}
		eva(args, mode, attribute);
		System.out.println("========================================================");
	}

	private static void eva(String[] args, String mode, String attribute)
			throws IllegalAccessException, NoSuchFieldException {
		ArrayList<String> files = Common.getLines("ACE_English_" + args[0] + args[2]);
		HashMap<String, double[]> stats = new HashMap<String, double[]>();
		for (String file : files) {
			ACEDoc document = new ACEEngDoc(file);
			ArrayList<EventMention> goldEMs = document.goldEventMentions;
			ArrayList<EventMention> systemEMs = getSystemEMs(file);
			
			for (EventMention em : goldEMs) {
				String attri = (String) em.getClass().getField(attribute).get(em);
				if (stats.containsKey(attri)) {
					stats.get(attri)[0] += 1;
				} else {
					double[] stat = new double[3];
					stat[0] = 1;
					stats.put(attri, stat);
				}
			}

			for (EventMention em : systemEMs) {
				String attri = (String) em.getClass().getField(attribute).get(em);
				if (stats.containsKey(attri)) {
					stats.get(attri)[1] += 1;
				} else {
					double[] stat = new double[3];
					stat[1] = 1;
					stats.put(attri, stat);
				}

				for (EventMention goldEM : goldEMs) {
					if (em.equals(goldEM)) {
						String goldAttri = (String) goldEM.getClass().getField(attribute).get(goldEM);
						if (attri.equals(goldAttri)) {
							stats.get(attri)[2] += 1;
						}
					}
				}
			}
		}
		for (String attri : stats.keySet()) {
			double stat[] = stats.get(attri);
			double recall = stat[2] / stat[0];
			double precision = stat[2] / stat[1];
			double fscore = 2 * recall * precision / (recall + precision);
			System.out.println("=======");
			System.out.println(attri);
			System.out.println(stat[0]);
			System.out.println(stat[1]);
			System.out.println(stat[2]);
			System.out.println("R:\t" + recall);
			System.out.println("P:\t" + precision);
			System.out.println("F:\t" + fscore);
		}
	}

	public static ArrayList<EventMention> getSystemEMs(String filename) {
		if (systemEMses.containsKey(filename)) {
			return systemEMses.get(filename);
		} else {
			return new ArrayList<EventMention>();
		}
	}

	static HashMap<String, ArrayList<EventMention>> systemEMses = new HashMap<String, ArrayList<EventMention>>();

	public static HashMap<String, ArrayList<EventMention>> loadSystem(String mode, String attribute) throws IllegalArgumentException, SecurityException, IllegalAccessException, NoSuchFieldException {
		ArrayList<String> emLines = Common.getLines("data/English_" + attribute + "_" + mode + "_em" + part);
		ArrayList<String> predictLines = Common.getLines("/users/yzcchen/tool/maxent/bin/" + mode + "_" + attribute + ".txt" + part);
		for (int i = 0; i < emLines.size(); i++) {
			String predictLine = predictLines.get(i);
			String emLine = emLines.get(i);

			String tokens[] = emLine.split("\\s+");
			String file = tokens[0];
			int start = Integer.valueOf(tokens[1]);
			int end = Integer.valueOf(tokens[2]);
			EventMention em = new EventMention();
			em.setAnchorStart(start);
			em.setAnchorEnd(end);

			tokens = predictLine.split("\\s+");
			String label = "";
			double maxVal = -1;
			for (int k = 0; k < tokens.length / 2; k++) {
				String l = tokens[k * 2];
				double val = Double.valueOf(tokens[k*2+1]);
				if (val > maxVal) {
					label = l;
					maxVal = val;
				}
			}
			em.getClass().getField(attribute).set(em, label);
			if (systemEMses.containsKey(file)) {
				systemEMses.get(file).add(em);
			} else {
				ArrayList<EventMention> ems = new ArrayList<EventMention>();
				ems.add(em);
				systemEMses.put(file, ems);
			}

		}
		return systemEMses;
	}
	
	public static HashMap<String, HashMap<String, HashMap<String, Double>>> loadSystemAttriWithConf(String attribute, String part){
		HashMap<String, HashMap<String, HashMap<String, Double>>> systemEMsMap = new HashMap<String, HashMap<String, HashMap<String, Double>>>();
		
		ArrayList<String> emLines = Common.getLines("data/English_" + attribute + "_test_em" + part);
		ArrayList<String> predictLines = Common.getLines("/users/yzcchen/tool/maxent/bin/test_" + attribute + ".txt" + part);
		for (int i = 0; i < emLines.size(); i++) {
			String predictLine = predictLines.get(i);
			String emLine = emLines.get(i);

			String tokens[] = emLine.split("\\s+");
			String file = tokens[0];
			int start = Integer.valueOf(tokens[1]);
			int end = Integer.valueOf(tokens[2]);
			
			String key = start + "," + end;

			tokens = predictLine.split("\\s+");

			HashMap<String, Double> conf = new HashMap<String, Double>();
			
			for (int k = 0; k < tokens.length / 2; k++) {
				String l = tokens[k * 2];
				double val = Double.valueOf(tokens[k*2+1]);
				conf.put(l, val);
			}
			if (systemEMsMap.containsKey(file)) {
				systemEMsMap.get(file).put(key, conf);
			} else {
				HashMap<String, HashMap<String, Double>> ems = new HashMap<String, HashMap<String, Double>>();
				ems.put(key, conf);
				systemEMsMap.put(file, ems);
			}
		}
		return systemEMsMap;
	}
	
	public static HashMap<String, HashMap<String, String>> loadSystemAttri(String attribute, String part){
		HashMap<String, HashMap<String, String>> systemEMsMap = new HashMap<String, HashMap<String, String>>();
		
		ArrayList<String> emLines = Common.getLines("data/English_" + attribute + "_test_em" + part);
		ArrayList<String> predictLines = Common.getLines("/users/yzcchen/tool/maxent/bin/test_" + attribute + ".txt" + part);
		for (int i = 0; i < emLines.size(); i++) {
			String predictLine = predictLines.get(i);
			String emLine = emLines.get(i);

			String tokens[] = emLine.split("\\s+");
			String file = tokens[0];
			int start = Integer.valueOf(tokens[1]);
			int end = Integer.valueOf(tokens[2]);
			
			String key = start + "," + end;

			tokens = predictLine.split("\\s+");
			String label = "";
			double maxVal = -1;
			for (int k = 0; k < tokens.length / 2; k++) {
				String l = tokens[k * 2];
				double val = Double.valueOf(tokens[k*2+1]);
				if (val > maxVal) {
					label = l;
					maxVal = val;
				}
			}
			if (systemEMsMap.containsKey(file)) {
				systemEMsMap.get(file).put(key, label);
			} else {
				HashMap<String, String> ems = new HashMap<String, String>();
				ems.put(key, label);
				systemEMsMap.put(file, ems);
			}
		}
		return systemEMsMap;
	}
}
