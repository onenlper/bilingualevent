package entity.semantic;

import java.util.ArrayList;
import java.util.HashMap;

import model.ACEChiDoc;
import model.EntityMention;
import util.Common;

public class SemanticEvaluateMultiSeed {

	public static class Evaluate {
		public int system = 0;
		public int gold = 0;
		public int hit = 0;
	}
	
	public static HashMap<String, Evaluate> evaluates = new HashMap<String, Evaluate>();
	
	public static HashMap<String, ArrayList<EntityMention>> allSVMResult;
	
	public static HashMap<String, ArrayList<EntityMention>> loadSVMResult(String part) {
		HashMap<String, ArrayList<EntityMention>> allSVMResult = new HashMap<String, ArrayList<EntityMention>>();
		String folder = "./";
		ArrayList<String> mentionStrs = Common.getLines(folder + "mention.test" + part);
		System.out.println(mentionStrs.size());
		ArrayList<String> typeResult = Common.getLines(folder + "multiType.result" + part);
		
		for(int i=0;i<mentionStrs.size();i++) {
			String mentionStr = mentionStrs.get(i);
			String fileKey = mentionStr.split("\\s+")[1];
			String startEndStr = mentionStr.split("\\s+")[0];
			int headStart = Integer.valueOf(startEndStr.split(",")[0]);
			int headEnd = Integer.valueOf(startEndStr.split(",")[1]);
			EntityMention em = new EntityMention();
			em.headStart = headStart;
			em.headEnd = headEnd;
			em.start = headStart;
			em.end = headEnd;
			
			int typeIndex = Integer.valueOf(typeResult.get(i).split("\\s+")[0]);
			
			String type = SemanticTrainMultiSeed.types.get(typeIndex - 1);
			if(type.equalsIgnoreCase("none") || type.equalsIgnoreCase("val") || type.equalsIgnoreCase("time")) {
				continue;
			}
			
			em.semClass = type;
			
			if(allSVMResult.containsKey(fileKey)) {
				allSVMResult.get(fileKey).add(em);
			} else {
				ArrayList<EntityMention> ems = new ArrayList<EntityMention>();
				ems.add(em);
				allSVMResult.put(fileKey, ems);
			}
		}
		return allSVMResult;
	}
	
	public static void main(String args[]) {
		if (args.length != 1) {
			System.err.println("java ~ folder");
			System.exit(1);
		}
		String test[] = new String[1];
		test[0] = args[0];
		allSVMResult = loadSVMResult(args[0]);
		ArrayList<String> files = Common.getLines("ACE_Chinese_test" + args[0]);

		double gold = 0;
		double sys = 0;
		double hit = 0;
		double hitType = 0;
		for (int index = 0; index < files.size(); index++) {
			
			String file = files.get(index);
			ACEChiDoc doc = new ACEChiDoc(file);
			
			ArrayList<EntityMention> goldMentions = doc.goldEntityMentions;
			ArrayList<EntityMention> sysMentions = allSVMResult.get(file);
			
			gold += goldMentions.size();
			sys += sysMentions.size();
			
			for (EntityMention em : goldMentions) {
				for(EntityMention sm : sysMentions) {
					if(em.headEnd==sm.headEnd) {
						hit+= 1;
						if(em.semClass.equalsIgnoreCase(sm.semClass)) {
							hitType += 1;
						}
					}
				}
			}
		}
		System.out.println("Gold Mentions: " + gold + "\t System Mentions:" + sys);
		
		System.out.println("Ident Hit: " + hit);
		double r = hit/gold;
		double p = hit/sys;
		double f = 2*p*r/(p+r);
		System.out.println("R: " + r + "\t P: " + p + "\t F: " + f);
		
		System.out.println("Type Hit: " + hitType);
		r = hitType/gold;
		p = hitType/sys;
		f = 2*p*r/(p+r);
		System.out.println("R: " + r + "\t P: " + p + "\t F: " + f);
	}
	private static void printSemEvalute() {
		double typeSystem = 0;
		double typeGolden = 0;
		double typeHit = 0;

		double subtypeSystem = 0;
		double subtypeGolden = 0;
		double subtypeHit = 0;

		for (String key : evaluates.keySet()) {
			Evaluate eva = evaluates.get(key);
			double system = eva.system;
			double gold = eva.gold;
			double hit = eva.hit;

			String str = getPerformance(key, gold, system, hit);
//			System.out.println(str);

			if (key.charAt(1) == '-') {
				subtypeSystem += system;
				subtypeGolden += gold;
				subtypeHit += hit;
			} else {
				typeSystem += system;
				typeGolden += gold;
				typeHit += hit;
			}
		}
		String str1 = getPerformance("all type: ", typeGolden, typeSystem, typeHit);
		System.out.println(str1);
		String str2 = getPerformance("all subtype: ", subtypeGolden, subtypeSystem, subtypeHit);
		System.out.println(str2);
	}

	private static String getPerformance(String key, double gold, double system, double hit) {
		double precision = hit / system;
		double recall = hit / gold;
		double fscore = 2 * precision * recall / (precision + recall);
		StringBuilder sb = new StringBuilder();
		sb.append(key).append(":").append(" gold: ").append(gold).append(" system: ").append(system).append(" hit: ")
				.append(hit).append("\nprecision: ").append(precision).append(" recall: ").append(recall).append(
						" fscore: ").append(fscore);
		return sb.toString();
	}
	
	
}
