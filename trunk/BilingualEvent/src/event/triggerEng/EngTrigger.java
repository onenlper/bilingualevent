package event.triggerEng;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import model.ACEChiDoc;
import model.ACEDoc;
import model.ACEEngDoc;
import model.EntityMention;
import model.EventMention;
import model.EventMentionArgument;
import model.Feature;
import model.ParseResult;
import model.syntaxTree.MyTreeNode;
import util.ChineseUtil;
import util.Common;
import util.Util;

public class EngTrigger {

	public ArrayList<EntityMention> getEntities(ACEDoc document, int start,
			int end) {
		ArrayList<EntityMention> mentions = new ArrayList<EntityMention>();
		ArrayList<EntityMention> goldMentions = document.goldEntityMentions;
		for (EntityMention mention : goldMentions) {
			if (mention.start >= start && mention.end <= end) {
				mentions.add(mention);
			}
		}
		return mentions;
	}
	
	public String buildFeature(EventMention em, ACEDoc document, String label) {
		String trigger = em.getAnchor();
		int position[] = ChineseUtil.findParseFilePosition2(
				em.getAnchorStart(), em.getAnchorEnd(), document);
		ParseResult pr = document.parseReults.get(position[0]);
		int leftIndex = position[1];
		int rightIndex = position[3];

		ArrayList<String> words = pr.words;
		ArrayList<String> posTags = pr.posTags;
		ArrayList<int[]> positions = pr.positions;

		String word = trigger;
		String pos = pr.posTags.get(position[3]);

		MyTreeNode leaf = pr.tree.leaves.get(rightIndex);
		ArrayList<MyTreeNode> ancestors = leaf.getAncestors();
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < ancestors.size() - 1; i++) {
			sb.append(ancestors.get(i).value).append("_");
		}
		String path = sb.toString().replaceAll("\\s+", "");

		sb = new StringBuilder();
		for (int i = ancestors.size() - 2; i >= 0; i--) {
			sb.append(ancestors.get(i).value).append("_");
			if (ancestors.get(i).value.equalsIgnoreCase("IP")) {
				break;
			}
		}
		String path2 = sb.toString();
		String previousW = "";
		String nextW = "";
		String previousPOS = "";
		String nextPOS = "";
		if (leftIndex != 1) {
			previousW = words.get(leftIndex - 1);
			previousPOS = posTags.get(leftIndex - 1);
		}
		if (rightIndex != words.size() - 1) {
			nextW = words.get(rightIndex + 1);
			nextPOS = words.get(rightIndex + 1);
		}

		ArrayList<String> features = new ArrayList<String>();
		for (int i = -2; i <= 2; i++) {
			int k1 = leftIndex + i;
			int k2 = leftIndex + i + 1;

			String pos1 = "null";
			String pos2 = "null";
			String word1 = "null";
			String word2 = "null";
			
			String type1 = "null";
			String type2 = "null";

			if (k1 >= 1 && k1 < pr.words.size()) {
				word1 = pr.words.get(k1);
				pos1 = pr.posTags.get(k1);
				
				type1 = word1;
				if(document.allGoldNPEndMap.containsKey(pr.positions.get(k1)[1])) {
					type1 = document.allGoldNPEndMap.get(pr.positions.get(k1)[1]).entity.type;
				}
			}
			features.add("Uni_" + i + "_" + word1);
			features.add("UniPOS_" + i + "_" + pos1);
//			features.add("UniType_" + i + "_" + type1);

			if (k2 >= 1 && k2 < pr.words.size()) {
				word2 = pr.words.get(k2);
				pos2 = pr.posTags.get(k2);
				
				type2 = word2;
				if(document.allGoldNPEndMap.containsKey(pr.positions.get(k2)[1])) {
					type2 = document.allGoldNPEndMap.get(pr.positions.get(k2)[1]).entity.type;
				}
			}
			features.add("Bi_" + i + "_" + word1 + "#" + word2);
			features.add("BiPOS_" + i + "_" + pos1 + "#" + pos2);
//			features.add("BiType_" + i + "_" + type1 + "#" + type2);
		}
		features.add("lemma_" + pr.lemmas.get(leftIndex));
		features.add("posTag_" + pr.posTags.get(leftIndex));

		for(String synonym : Common.getSynonyms(pr.words.get(leftIndex), pr.posTags.get(leftIndex))) {
			features.add("synonym_" + synonym);	
		}
		features.add("nomlex_" + Common.getNomlex(pr.lemmas.get(leftIndex)));

//		features.add("brown_" + Common.getBrownCluster(pr.words.get(leftIndex)));
//		features.add("porter_" + Common.getPorterStem(pr.words.get(leftIndex)));
//		for (Depend dep : pr.depends) {
//			if (dep.first == leftIndex) {
//				features.add("depWord_" + pr.words.get(dep.second) + pr.words.get(leftIndex));
////				features.add("firstDepType_" + dep.type + "#" + pr.words.get(leftIndex));
//			}
//			if (dep.second == leftIndex) {
//				features.add("govWord_" + pr.words.get(dep.first) + pr.words.get(leftIndex));
////				features.add("secondDepType_" + dep.type + "#" + pr.words.get(leftIndex));
//			}
//		}
//		features.add(trigger);
//		features.add(pos);
//		features.add(previousW + "_" + word);
//		features.add(word + "_" + nextW);
//		features.add(previousPOS + "_" + pos);
//		features.add(pos + "_" + nextPOS);
//		features.add(Integer.toString(leaf.getAncestors().size()));
//		features.add(path);
//		features.add(path2);
//
//		features.add(leaf.parent.parent.value);
//		features.add(leaf.parent.parent.productionRule());
//
		ArrayList<EntityMention> leftNearbyMentions = this.getEntities(
				document, positions.get(1)[0], em.getAnchorStart());

		int shortest = Integer.MAX_VALUE;
		String type1 = "null";
		String extent1 = "null";
		for (EntityMention mention : leftNearbyMentions) {
			MyTreeNode node = this.getNPTreeNode(mention, document.parseReults,
					position[0], position[1], position[3]);
			int distance = this.getSyntaxTreeDistance(leaf, node);
			if (distance < shortest) {
				shortest = distance;
				type1 = mention.entity.getType();
				extent1 = mention.extent.replace(" ", "").replace("\n", "");
			}
		}
		features.add("leftMType_" + type1);
		features.add("leftMExtent_" + extent1);

		ArrayList<EntityMention> rightNearbyMentions = this.getEntities(
				document, em.getAnchorEnd() + 1,
				positions.get(positions.size() - 1)[1]);

		shortest = Integer.MAX_VALUE;
		String type2 = "null";
		String extent2 = "null";
		for (EntityMention mention : rightNearbyMentions) {
			int distance = 0;
			if (mention.end < em.getAnchorStart()) {
				distance = em.getAnchorStart() - mention.end;
			} else {
				distance = mention.start - em.getAnchorEnd();
			}
			if (distance < shortest) {
				shortest = distance;
				type2 = mention.entity.getType();
				extent2 = mention.extent.replace(" ", "").replace("\n", "");
			}
		}
		features.add("rightMType_" + type2);
		features.add("rightMExtent_" + extent2);

		return convert(features, label);
	}

	public String convert(ArrayList<String> feaStrs, String label) {
		ArrayList<Feature> feas = new ArrayList<Feature>();
		StringBuilder sb = new StringBuilder();
		sb.append(label);
		for (int i = 0; i < feaStrs.size(); i++) {
			String feaStr = feaStrs.get(i);
			String extendFeaStr = feaStr.trim();
			int idx = getFeaIdx(extendFeaStr);
			if (idx == -1) {
				continue;
			}
			feas.add(new Feature(idx, 1));
		}
		HashSet<Feature> feaSets = new HashSet<Feature>();
		feaSets.addAll(feas);
		feas.clear();
		feas.addAll(feaSets);
		Collections.sort(feas);

		for (Feature fea : feas) {
			sb.append(" ").append(fea.idx).append(":").append(fea.value);
		}
		return sb.toString().trim();
	}

	public EntityMention getEntityMention(EventMentionArgument argument,
			ACEChiDoc document) {
		ArrayList<EntityMention> mentions = new ArrayList<EntityMention>();
		mentions.addAll(document.goldEntityMentions);
		mentions.addAll(document.goldTimeMentions);
		mentions.addAll(document.goldValueMentions);
		for (EntityMention mention : mentions) {
			if (mention.start == argument.getStart()
					&& mention.end == argument.getEnd()) {
				return mention;
			}
		}
		return null;
	}

	public static void addKey(HashMap<String, Integer> maps, String key) {
		if (maps.containsKey(key)) {
			int k = maps.get(key);
			maps.put(key, k + 1);
		} else {
			maps.put(key, 1);
		}
	}

	public String getMainLabel(ArrayList<EventMention> mentions) {
		HashMap<String, Integer> maps = new HashMap<String, Integer>();
		for (EventMention mention : mentions) {
			String type = mention.getType();
			addKey(maps, type);
		}
		String mainLabel = "null";
		if (maps.size() != 0) {
			int maxVal = -1;
			for (String key : maps.keySet()) {
				if (maps.get(key) > maxVal) {
					mainLabel = key;
					maxVal = maps.get(key);
				}
			}
		}
		return mainLabel;
	}

	public MyTreeNode getNPTreeNode(EntityMention np,
			ArrayList<ParseResult> prs, int npSenIdx, int npWordStartIdx,
			int npWordEndIdx) {
		MyTreeNode NP = null;
		try {
			ArrayList<MyTreeNode> leaves = prs.get(npSenIdx).tree.leaves;
			MyTreeNode leftNp = leaves.get(npWordStartIdx);
			MyTreeNode rightNp = leaves.get(npWordEndIdx);
			// System.out.println(npWordEndIdx +np.getContent());
			ArrayList<MyTreeNode> leftAncestors = leftNp.getAncestors();
			ArrayList<MyTreeNode> rightAncestors = rightNp.getAncestors();
			for (int i = 0; i < leftAncestors.size()
					&& i < rightAncestors.size(); i++) {
				if (leftAncestors.get(i) == rightAncestors.get(i)) {
					NP = leftAncestors.get(i);
				} else {
					break;
				}
			}
		} catch (Exception e) {
			System.out.println("ERROR when finding tree node");
			return null;
		}
		return NP;
	}

	public int getSyntaxTreeDistance(MyTreeNode treeNode1, MyTreeNode treeNode2) {
		ArrayList<MyTreeNode> ancestors1 = treeNode1.getAncestors();
		ArrayList<MyTreeNode> ancestors2 = treeNode2.getAncestors();
		if (ancestors1.get(0) == ancestors2.get(0)) {
			int i = 1;
			for (i = 1; i < ancestors1.size() && i < ancestors2.size(); i++) {
				if (ancestors1.get(i) != ancestors2.get(i)) {
					break;
				}
			}
			return ancestors1.size() - i + ancestors2.size() - i - 2;
		} else {
			return Integer.MAX_VALUE;
		}
	}

	public static List<String> types = Arrays.asList("Life", "Movement",
			"Transaction", "Business", "Conflict", "Contact", "Personnel",
			"Justice", "null");

	public static String mode = "train";

	public static HashMap<String, Integer> featureSpace;

	public static int getFeaIdx(String feature) {
		if (!featureSpace.containsKey(feature)) {
			if (mode.equalsIgnoreCase("train")) {
				featureSpace.put(feature, featureSpace.size() + 1);
				return featureSpace.size();
			} else {
				return -1;
			}
		} else {
			return featureSpace.get(feature);
		}
	}

	public static void main(String args[]) {
		if (args.length < 3) {
			System.out
					.println("java ~ [train|test|development] [svm|maxent] [folder]");
		}
		mode = args[0];
		System.out.println(mode);
		Util.part = args[2];
		if (mode.equalsIgnoreCase("train")) {
			featureSpace = new HashMap<String, Integer>();
		} else {
			featureSpace = Common.readFile2Map("trSpaceEng" + Util.part);
		}
		ArrayList<String> files = Common.getLines("ACE_English_" + mode
				+ Util.part);

		ArrayList<String> triggerSubTypeFeatures = new ArrayList<String>();
		EngTrigger triggerFeature = new EngTrigger();

		ArrayList<String> emInstances = new ArrayList<String>();
		for (String file : files) {
			HashSet<EventMention> goldEMs = new HashSet<EventMention>();
			ACEDoc document = new ACEEngDoc(file);
			ArrayList<EventMention> ems = document.goldEventMentions;
			goldEMs.addAll(ems);
			ArrayList<EventMention> candidateTriggers;

			if (args[0].equals("train")) {
				candidateTriggers = getTrainEMs(document);
			} else {
				candidateTriggers = getTestEMs(document);
			}
			for (EventMention em : candidateTriggers) {
				StringBuilder sb = new StringBuilder();
				sb.append(file).append(" ").append(em.getAnchorStart())
						.append(" ").append(em.getAnchorEnd()).append(" ")
						.append(em.getType()).append(" ").append(em.inferFrom);
				emInstances.add(sb.toString());

				triggerSubTypeFeatures
						.add(triggerFeature.buildFeature(em, document,
								Integer.toString(Util.subTypes
										.indexOf(em.subType) + 1)));
			}
		}
		Common.outputLines(emInstances, "data/engTrName_" + args[0] + Util.part);
		Common.outputLines(triggerSubTypeFeatures, "data/engTrFea_" + args[0]
				+ Util.part);
		System.out.println("Build features for " + args[0] + Util.part);
		System.out.println("==========");
		if (mode.equalsIgnoreCase("train")) {
			Common.outputHashMap(featureSpace, "trSpaceEng" + Util.part);
		}
		StringBuilder sb = new StringBuilder();
		for (String key : posTags.keySet()) {
			System.out.println(key + "#" + posTags.get(key));
			sb.append("\"").append(key).append("\"").append(", ");
		}
		System.out.println(sb.toString().trim());
	}

	static HashMap<String, Integer> posTags = new HashMap<String, Integer>();

	public static ArrayList<EventMention> getTrainEMs(ACEDoc document) {
		HashSet<EventMention> trainEMs = new HashSet<EventMention>();
		HashSet<EventMention> goldEMSet = new HashSet<EventMention>();
		goldEMSet.addAll(document.goldEventMentions);

		// WP#2
		// IN#5
		// JJ#146
		// RB#11
		// DT#19
		// RP#3
		// VBG#375
		// VBD#574
		// PRP$#2
		// NN#1669
		// VB#569
		// VBN#727
		// NNS#454
		// VBP#84
		// CD#14
		// NNP#73
		// PRP#43
		// MD#1
		// WDT#1
		// WRB#1
		// VBZ#50

		HashSet<String> posTags = new HashSet<String>(Arrays.asList("WP", "IN",
				"JJ", "RB", "DT", "RP", "VBG", "VBD", "PRP$", "NN", "VB",
				"VBN", "NNS", "VBP", "CD", "NNP", "PRP", "MD", "WDT", "WRB",
				"VBZ"));

		for (ParseResult pr : document.parseReults) {
			for (int i = 1; i < pr.words.size(); i++) {
				int start = pr.positions.get(i)[0];
				int end = pr.positions.get(i)[1];
				String word = pr.words.get(i).replace("\n", " ")
						.replaceAll("\\s+", " ");
				EventMention em = new EventMention();
				em.setAnchorStart(start);
				em.setAnchorEnd(end);
				em.setAnchor(word);
				em.setType("None");
				em.setSubType("None");
				if (!goldEMSet.contains(em)) {
					trainEMs.add(em);
				}
			}
		}

		trainEMs.addAll(goldEMSet);
		ArrayList<EventMention> ems = new ArrayList<EventMention>();
		ems.addAll(trainEMs);
		return ems;
	}

	public static ArrayList<EventMention> getTestEMs(ACEDoc document) {
		
		HashSet<EventMention> testEMs = new HashSet<EventMention>();
		for (ParseResult pr : document.parseReults) {
			for (int i = 1; i < pr.words.size(); i++) {
				int start = pr.positions.get(i)[0];
				int end = pr.positions.get(i)[1];
				String word = pr.words.get(i).replace("\n", " ")
						.replaceAll("\\s+", " ");
				EventMention em = new EventMention();
				em.setAnchorStart(start);
				em.setAnchorEnd(end);
				em.setAnchor(word);
				em.setType("None");
				em.setSubType("None");
				testEMs.add(em);
			}
		}
		ArrayList<EventMention> ems = new ArrayList<EventMention>();
		ems.addAll(testEMs);
		return ems;
	}
}
