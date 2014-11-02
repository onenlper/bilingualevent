package event.triggerEng;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

import model.ACEDoc;
import model.ACEEngDoc;
import model.EntityMention;
import model.EventMention;
import model.EventMentionArgument;
import model.Feature;
import model.ParseResult;
import model.syntaxTree.GraphNode;
import model.syntaxTree.MyTreeNode;
import util.ChineseUtil;
import util.Common;
import util.Util;

public class EngArg {

	public static HashMap<String, HashMap<String, EventMention>> systemEventMentionses;

	public static String mode;

	public HashMap<String, Integer> featureSpace;

	public static void main(String args[]) {
		if (args.length < 3) {
			System.out
					.println("java ~ [train|test|development] [svm|maxent] [folder]");
		}
		mode = args[0];
		String model = args[1];
		Util.part = args[2];
		if (!args[0].equalsIgnoreCase("train")) {
			systemEventMentionses = Util.readResult("data/engTrResult"
					+ Util.part, "eng");
		}

		EngArg argumentFeature = new EngArg();
		if (mode.equals("train")) {
			argumentFeature.featureSpace = new HashMap<String, Integer>();
		} else {
			argumentFeature.featureSpace = Common.readFile2Map("engArgSpace"
					+ Util.part);
		}

		ArrayList<String> files = Common.getLines("ACE_English_" + args[0]
				+ Util.part);

		ArrayList<String> argumentRoleFeatures = new ArrayList<String>();

		ArrayList<String> argumentLines = new ArrayList<String>();
		ArrayList<String> relateEMs = new ArrayList<String>();

		for (String file : files) {
			ACEDoc document = new ACEEngDoc(file);
			if (args[0].equalsIgnoreCase("train")) {
				for (EventMention eventMention : document.goldEventMentions) {
					argumentRoleFeatures.addAll(argumentFeature
							.buildTrainRoleFeatures(eventMention, document));
				}
			} else {
				ArrayList<EventMention> eventMentions = new ArrayList<EventMention>();
				// eventMentions.addAll(document.goldEventMentions);
				if (systemEventMentionses.containsKey(file)) {
					eventMentions.addAll(systemEventMentionses.get(file)
							.values());
				}
				if (eventMentions == null) {
					continue;
				}
				for (EventMention eventMention : eventMentions) {
					if (eventMention.subType.equals("None")) {
						continue;
					}
					StringBuilder sb = new StringBuilder();
					sb.append(document.fileID).append(" ")
							.append(eventMention.getAnchorStart()).append(" ")
							.append(eventMention.getAnchorEnd()).append(" ")
							.append(eventMention.confidence).append(" ")
							.append(eventMention.type).append(" ")
							.append(eventMention.typeConfidence).append(" ")
							.append(eventMention.subType).append(" ")
							.append(eventMention.subTypeConfidence).append(" ")
							.append(eventMention.inferFrom);
					for (double confidence : eventMention.typeConfidences) {
						sb.append(" ").append(confidence);
					}
					relateEMs.add(sb.toString());
					argumentRoleFeatures.addAll(argumentFeature
							.buildTestFeatures(eventMention, document,
									argumentLines));
				}
			}
		}
		Common.outputLines(argumentLines, "data/engArgName_" + mode + Util.part);
		Common.outputLines(argumentRoleFeatures, "data/engArgFea_" + mode
				+ Util.part);
		Common.outputLines(relateEMs, "data/engTrofArgName_" + mode + Util.part);

		System.out.println("Build argument detection feature " + mode + "...");
		System.out.println("=================");

		if (mode.equals("train")) {
			Common.outputHashMap(argumentFeature.featureSpace, "engArgSpace"
					+ Util.part);
		}
		
//		int k = 0;
//		double allPer = 0;
//		for(String key : labelDistri.keySet()) {
//			if(key.equals("null")) {
//				continue;
//			}
//			double percent = labelDistri.get(key)*100./8720.0;
//			System.out.println(key + ":" + labelDistri.get(key) + " \t " + percent);
//			allPer += percent;
//			System.out.println(allPer);
//			k += labelDistri.get(key);
//		}
//
//		System.out.println("All Trs: " + allTrs);
//		System.out.println("All Args: " + allArgs);
//		System.out.println("All TrueArgs: " + allTrueArgs);
//		System.out.println("K: " + k);
	}

	public ArrayList<EntityMention> getArgumentCandidates(EventMention em,
			ACEDoc document) {
		ArrayList<EntityMention> candidates = new ArrayList<EntityMention>();

		ArrayList<EntityMention> mentions = new ArrayList<EntityMention>();

		ArrayList<EntityMention> npMentions = document.goldEntityMentions;
		ArrayList<EntityMention> timeMentions = document.goldTimeMentions;
		ArrayList<EntityMention> valueMentions = document.goldValueMentions;

		mentions.addAll(npMentions);
		mentions.addAll(timeMentions);
		mentions.addAll(valueMentions);

		ParseResult pr = document.parseReults.get(document.positionMap.get(em
				.getAnchorStart())[0]);

		int leftBound = pr.positions.get(1)[0];
		int rightBound = pr.positions.get(pr.positions.size() - 1)[1];

		for (EntityMention mention : mentions) {
			if (mention.start >= leftBound && mention.end <= rightBound) {
				candidates.add(mention);
			}
		}
		return candidates;
	}

	public ArrayList<String> buildTrainRoleFeatures(EventMention eventMention,
			ACEDoc document) {
		ArrayList<EventMentionArgument> arguments = eventMention
				.getEventMentionArguments();
		
		HashMap<String, EventMentionArgument> goldArgHash = new HashMap<String, EventMentionArgument>();

		for (EventMentionArgument arg : arguments) {
			goldArgHash.put(arg.toString(), arg);
		}

		ArrayList<EntityMention> entityMentions = this.getArgumentCandidates(
				eventMention, document);
		ArrayList<String> features = new ArrayList<String>();
		for (EntityMention mention : entityMentions) {
			String start = Integer.toString(mention.getStart());
			String end = Integer.toString(mention.getEnd());
			String role = "null";
			EventMentionArgument arg = goldArgHash.get(start + "," + end);
			if (arg != null) {
				role = arg.getRole();
			}
			
			String feature = this.buildFeature(mention, eventMention, document,
					Integer.toString(Util.roles.indexOf(role) + 1),
					entityMentions);
			features.add(feature);
		}
		return features;
	}

	public EntityMention getEntityMention(int start, int end,
			ArrayList<EntityMention> mentions1) {
		for (EntityMention mention : mentions1) {
			if (mention.start == start && mention.end == end) {
				return mention;
			}
		}
		return null;
	}

	public ArrayList<String> buildTestFeatures(EventMention eventMention,
			ACEDoc document, ArrayList<String> argumentLines) {
		ArrayList<EntityMention> entityMentions = this.getArgumentCandidates(
				eventMention, document);
		ArrayList<String> features = new ArrayList<String>();

		for (EntityMention entityMention : entityMentions) {
			String feature = this.buildFeature(entityMention, eventMention,
					document, Integer.toString(Util.roles.indexOf("null") + 1),
					entityMentions);
			features.add(feature);

			if (argumentLines != null) {
				StringBuilder sb = new StringBuilder();
				sb.append(document.fileID).append(" ")
						.append(eventMention.getAnchorStart()).append(" ")
						.append(eventMention.getAnchorEnd()).append(" ")
						.append(eventMention.confidence).append(" ")
						.append(eventMention.type).append(" ")
						.append(eventMention.typeConfidence).append(" ")
						.append(eventMention.subType).append(" ")
						.append(eventMention.subTypeConfidence).append(" ")
						.append(entityMention.start).append(" ")
						.append(entityMention.end);

				argumentLines.add(sb.toString());
			}
		}
		return features;
	}

	public int getFeaIdx(String feature) {
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

	public String buildFeature2(EntityMention arg, EventMention tr,
			ACEDoc document, String label, ArrayList<EntityMention> allArgs) {
		ArrayList<String> features = new ArrayList<String>();
		ParseResult pr = document.parseReults.get(document.positionMap
				.get(arg.start)[0]);

		int arg_start = document.positionMap.get(arg.start)[1];
		int arg_end = document.positionMap.get(arg.end)[1];
		int arg_head = document.positionMap.get(arg.headEnd)[1];

		String head = arg.head.split("\\s+")[arg.head.split("\\s+").length-1];
		if(!head.contains(pr.words.get(arg_head)) && !pr.words.get(arg_head).contains(head)) {
			System.out.println(arg.head + "$$$" + pr.words.get(arg_head));
//			Common.pause("");
		}
		
		int tr_start = document.positionMap.get(tr.getAnchorStart())[1];
		int tr_end = document.positionMap.get(tr.getAnchorEnd())[1];
		int tr_head = document.positionMap.get(tr.getAnchorStart())[1];

		ArrayList<String> aroundTr = new ArrayList<String>();
		for (int i = tr_head - 2; i <= tr_head + 2; i++) {
			if (i >= 1 && i < pr.words.size()) {
				aroundTr.add(pr.lemmas.get(i) + "#" + (i - tr_head));
			}
		}
//		features.addAll(Common.get1234Ngram(aroundTr, "aroundTr_"));

		ArrayList<String> aroundArg = new ArrayList<String>();
		for (int i = arg_head - 2; i <= arg_head + 2; i++) {
			if (i >= 1 && i < pr.words.size()) {
				aroundArg.add(pr.lemmas.get(i) + "#" + (i - arg_head));
			}
		}
		features.addAll(Common.get1234Ngram(aroundArg, "aroundArg_"));

		features.add("anchor_" + tr.getAnchor());
		features.add("anchorType_" + tr.getType());

		features.add("entityType_" + arg.entity.getType() + "#"
				+ tr.getType());
		features.add("entitySubType_" + arg.entity.getSubType() + "#"
				+ tr.getType());

		for (EntityMention m : arg.entity.mentions) {
			features.add("head_" + m.head);
		}

		boolean overlap = false;
		if (arg_end < tr_start) {
			boolean punc = false;
			for (int i = arg_end + 1; i < tr_start; i++) {
				if (pr.words.get(i).equals(",")) {
					punc = true;
				}
			}
			if (punc) {
				features.add("sep_left" + punc);
			} else {
				features.add("sep_" + "left");
			}
		} else if (arg_start > tr_end) {
			boolean punc = false;
			for (int i = tr_end + 1; i < arg_start; i++) {
				if (pr.words.get(i).equals(",")) {
					punc = true;
				}
			}
			if (punc) {
				features.add("sep_right" + punc);
			} else {
				features.add("sep_" + "right");
			}
		} else {
			overlap = true;
			features.add("sep_" + "overlap");
		}

		HashMap<String, Integer> minSubTypeDis = new HashMap<String, Integer>();
		HashMap<String, Integer> subtypeMs = new HashMap<String, Integer>();

		HashMap<String, Integer> minTypeDis = new HashMap<String, Integer>();
		HashMap<String, Integer> typeMs = new HashMap<String, Integer>();
		int minDis = Integer.MAX_VALUE;
		for (EntityMention m : allArgs) {
			String subType = m.entity.subType;
			int arg_head2 = document.positionMap.get(m.headStart)[1];
			int diss = Math.abs(arg_head2 - tr_head);
			minDis = Math.min(minDis, diss);
			Integer minD = minSubTypeDis.get(subType);
			if (minD != null) {
				minSubTypeDis.put(subType, Math.min(minD.intValue(), diss));
			} else {
				minSubTypeDis.put(subType, diss);
			}
			if (subtypeMs.containsKey(subType)) {
				subtypeMs.put(subType, subtypeMs.get(subType) + 1);
			} else {
				subtypeMs.put(subType, 1);
			}

			String type = m.entity.type;
			minD = minTypeDis.get(type);
			if (minD != null) {
				minTypeDis.put(type, Math.min(minD.intValue(), diss));
			} else {
				minTypeDis.put(type, diss);
			}
			if (typeMs.containsKey(type)) {
				typeMs.put(type, typeMs.get(type) + 1);
			} else {
				typeMs.put(type, 1);
			}
		}
		int diss = Math.abs(arg_head - tr_head);
		if (diss == minSubTypeDis.get(arg.entity.subType)) {
			features.add("closeSubType_" + "yes" + arg.entity.subType);
		} else {
			features.add("closeSubType_" + "no" + arg.entity.subType);
		}
		if (subtypeMs.get(arg.entity.subType) == 1) {
			features.add("onlySubType_" + "yes" + arg.entity.subType);
		} else {
			features.add("onlySubType_" + "no" + arg.entity.subType);
		}

		if (diss == minTypeDis.get(arg.entity.type)) {
			features.add("closeType_" + "yes" + arg.entity.type);
		} else {
			features.add("closeType_" + "no" + arg.entity.type);
		}
		if (typeMs.get(arg.entity.type) == 1) {
			features.add("closeType_" + "yes" + arg.entity.type);
		} else {
			features.add("closeType_" + "no" + arg.entity.type);
		}

		// if(minDis==diss) {
		// features.add("closest_" + "yes" + arg.entity.subType);
		// } else {
		// features.add("closest_" + "no" + arg.entity.subType);
		// }

		if (!overlap) {
			features.add("lexicalDiss_" + Integer.toString(arg_head - tr_head));
		} else {
			features.add("lexicalDiss_0");
		}

		GraphNode trDep = pr.dependTree.vertexMap.get(tr_head + 1);
		GraphNode argDep = pr.dependTree.vertexMap.get(arg_head + 1);

		ArrayList<GraphNode> path = Util.findPath(trDep, argDep);
		features.addAll(Util.getPathFeature("edge_", path, pr));

		// unigram along path
		StringBuilder sb = new StringBuilder();
		StringBuilder sb22 = new StringBuilder();

//		for (int i = 1; i < path.size() - 1; i++) {
//			GraphNode node = path.get(i);
//			String tk = pr.words.get(node.value);
//			String edge = node.getEdgeName(path.get(i + 1));
//			sb22.append(tk).append("#").append(edge).append("#");
//			features.add("between_" + tk);
//			if (i < path.size() - 1) {
//				sb.append(node.getEdgeName(path.get(i + 1))).append(" ");
//				features.add("uniDep#" + edge);
//			}
//		}
//
//		if (path.size() != 0) {
////			sb22.append(pr.words.get((path.get(path.size() - 1).value)));
//		} else {
//			sb22.append("null");
//		}
//
//		
//		
//		// System.out.println(sb22.toString());
//		features.add("dePath22#" + sb22.toString());
//		features.add("dePath#"+ sb.toString());
//		features.add("dePathLength_"+ path.size());
//
//		MyTreeNode trNode = pr.tree.leaves.get(tr_head);
//		MyTreeNode argNode = pr.tree.leaves.get(arg_head);
//		
//		ArrayList<MyTreeNode> trAns = trNode.getAncestors();
//		ArrayList<MyTreeNode> arAns = argNode.getAncestors();
//		MyTreeNode common = null;
//		for(int i=0;i<trAns.size()&&i<arAns.size();i++) {
//			if(trAns.get(i)==arAns.get(i)) {
//				common = trAns.get(i);
//			} else {
//				break;
//			}
//		}
//		features.add("common_" + common.value);
		
		return this.convert(features, label);
	}

	public String buildFeature(EntityMention arg,
			EventMention tr, ACEDoc document, String label, ArrayList<EntityMention> allArgs) {
		int argPosition[] = ChineseUtil.findParseFilePosition2(arg.headStart, arg.headEnd, document);
		int trPosition[] = ChineseUtil.findParseFilePosition2(tr.getAnchorStart(), tr.getAnchorEnd(), document);

		int leftIndex2 = trPosition[1];
		int rightIndex2 = trPosition[3];

		ParseResult pr = document.parseReults.get(argPosition[0]);
		ParseResult pr2 = document.parseReults.get(trPosition[0]);
		
		int leftIndex = argPosition[1];
		int rightIndex = argPosition[3];
		ArrayList<String> words = pr.lemmas;
		ArrayList<String> posTags = pr.posTags;

		ArrayList<String> words2 = pr2.lemmas;
		ArrayList<String> posTags2 = pr2.posTags;

		String path = "-";
		String depPath = "-";
		int distance = -1;
		MyTreeNode entityNode = pr.tree.leaves.get(argPosition[3]);
		MyTreeNode eventNode = pr2.tree.leaves.get(trPosition[3]);

		int entityID = argPosition[3];
		int eventID = trPosition[3];

		if (argPosition[0] == trPosition[0]) {
			ArrayList<MyTreeNode> entityAncestors = entityNode.getAncestors();
			ArrayList<MyTreeNode> eventAncestors = eventNode.getAncestors();

			MyTreeNode entityIP = null;
			MyTreeNode eventIP = null;
			for (int k = entityAncestors.size() - 1; k >= 0; k--) {
				MyTreeNode node = entityAncestors.get(k);
				if (node.value.equalsIgnoreCase("s")
						|| node.value.equalsIgnoreCase("root")) {
					entityIP = node;
					break;
				}
			}
			for (int k = eventAncestors.size() - 1; k >= 0; k--) {
				MyTreeNode node = eventAncestors.get(k);
				if (node.value.equalsIgnoreCase("s")
						|| node.value.equalsIgnoreCase("root")) {
					eventIP = node;
					break;
				}
			}


			int common = 0;
			MyTreeNode commonNode = null;
			for (int i = 0; i < entityAncestors.size()
					&& i < eventAncestors.size(); i++) {
				if (entityAncestors.get(i) == eventAncestors.get(i)) {
					common = i;
					commonNode = eventAncestors.get(i);
				} else {
					break;
				}
			}
			StringBuilder sb = new StringBuilder();
			MyTreeNode parent = entityNode;
			while (parent != commonNode) {
				parent = parent.parent;
				sb.append(parent.value).append("_");
				distance++;
			}
			for (int k = common + 1; k < eventAncestors.size() - 1; k++) {
				sb.append(eventAncestors.get(k).value).append("_");
				distance++;
			}
			path = sb.toString();
			while (!pr.dependTree.vertexMapTree.containsKey(entityID)
					&& entityID != 0) {
				entityID--;
			}
			if (entityID != eventID) {
				MyTreeNode entityDepNode = pr.dependTree.vertexMapTree
						.get(entityID);
				MyTreeNode eventDepNode = pr.dependTree.vertexMapTree
						.get(eventID);
				if (entityDepNode != null && eventDepNode != null) {
					entityAncestors = entityDepNode.getAncestors();
					eventAncestors = eventDepNode.getAncestors();
					common = 0;
					commonNode = null;
					for (int i = 0; i < entityAncestors.size()
							&& i < eventAncestors.size(); i++) {
						if (entityAncestors.get(i) == eventAncestors.get(i)) {
							common = i;
							commonNode = eventAncestors.get(i);
						} else {
							break;
						}
					}
					sb = new StringBuilder();
					parent = entityDepNode;
					while (parent != commonNode) {
						sb.append(parent.backEdge).append("#");
						parent = parent.parent;
					}
					for (int k = common + 1; k < eventAncestors.size(); k++) {
						sb.append(eventAncestors.get(k).backEdge).append("#");
					}
					depPath = sb.toString();
				}
			} else {
				depPath = "-";
			}
		}

		String leftWord = "-";
		String leftPOS = "*";
		String rightWord = "+";
		String rightPOS = "^";
		if (leftIndex > 1) {
			leftWord = words.get(leftIndex - 1);
			leftPOS = posTags.get(leftIndex - 1);
		}
		if (rightIndex < words.size() - 1) {
			rightWord = words.get(rightIndex + 1);
			rightPOS = posTags.get(rightIndex + 1);
		}

		String leftWord2 = "-";
		String leftPOS2 = "*";
		String rightWord2 = "+";
		String rightPOS2 = "^";
		if (leftIndex2 > 1) {
			leftWord2 = words2.get(leftIndex2 - 1);
			leftPOS2 = posTags2.get(leftIndex2 - 1);
		}
		if (rightIndex2 < words2.size() - 1) {
			rightWord2 = words2.get(rightIndex2 + 1);
			rightPOS2 = posTags2.get(rightIndex2 + 1);
		}

		ArrayList<String> features = new ArrayList<String>();

//		 features.add("Tr_POS" + triggerPOS);
		
		 for(EntityMention m : arg.entity.mentions) {
			 features.add("Head_" + m.head);
		 }
		
		 features.add("TrSubType_" + tr.getSubType());
		
		 features.add("ArgType_" + arg.getType());
		 features.add("ArgSemType_" + arg.entity.type);
		 features.add("ArgSemSubType" + arg.entity.subType);
		 features.add("ArgType2_" + tr.subType + "_" + arg.head);
		 features.add("ArgSemType2_" + tr.subType + "_" + arg.entity.type);
		 features.add("ArgSubtype_" + tr.subType + "_" + arg.entity.subType);

		 features.add("l2_" + leftWord2);
		 features.add("r2_" + rightWord2);
		 features.add("l2Pos_" + leftWord2 + "_" + leftPOS2);
		 features.add("r2Pos_" + rightWord2 + "-" + rightPOS2);
		
		 features.add("l1_" + leftWord);
		 features.add("r1_" + rightWord);
		 features.add("l1Pos_" + leftWord + "_" + leftPOS);
		 features.add("r1Pos_" + rightWord + "-" + rightPOS);
		
		 features.add("category_" + eventNode.parent.parent.value);
		 features.add("production_rule" + eventNode.parent.parent.productionRule());

		if (arg.end < tr.getAnchorStart()) {
			features.add("left");
		} else if(arg.start>tr.getAnchorEnd()){
			features.add("right");
		} else {
			features.add("overlap");
		}

		features.add("path" + path);
		features.add("distance2_"+ Integer.toString(rightIndex2 - rightIndex));
		features.add("distance_" + Integer.toString(distance));
		features.add("depPath" + depPath);
		
		
		
		int arg_head = document.positionMap.get(arg.headEnd)[1];

		String head = arg.head.split("\\s+")[arg.head.split("\\s+").length-1];
		if(!head.contains(pr.words.get(arg_head)) && !pr.words.get(arg_head).contains(head)) {
			System.out.println(arg.head + "$$$" + pr.words.get(arg_head));
//			Common.pause("");
		}
		
		int tr_start = document.positionMap.get(tr.getAnchorStart())[1];
		int tr_end = document.positionMap.get(tr.getAnchorEnd())[1];
		int tr_head = document.positionMap.get(tr.getAnchorStart())[1];
		
		
		HashMap<String, Integer> minSubTypeDis = new HashMap<String, Integer>();
		HashMap<String, Integer> subtypeMs = new HashMap<String, Integer>();

		HashMap<String, Integer> minTypeDis = new HashMap<String, Integer>();
		HashMap<String, Integer> typeMs = new HashMap<String, Integer>();
		int minDis = Integer.MAX_VALUE;
		for (EntityMention m : allArgs) {
			String subType = m.entity.subType;
			int arg_head2 = document.positionMap.get(m.headStart)[1];
			int diss = Math.abs(arg_head2 - tr_head);
			minDis = Math.min(minDis, diss);
			Integer minD = minSubTypeDis.get(subType);
			if (minD != null) {
				minSubTypeDis.put(subType, Math.min(minD.intValue(), diss));
			} else {
				minSubTypeDis.put(subType, diss);
			}
			if (subtypeMs.containsKey(subType)) {
				subtypeMs.put(subType, subtypeMs.get(subType) + 1);
			} else {
				subtypeMs.put(subType, 1);
			}

			String type = m.entity.type;
			minD = minTypeDis.get(type);
			if (minD != null) {
				minTypeDis.put(type, Math.min(minD.intValue(), diss));
			} else {
				minTypeDis.put(type, diss);
			}
			if (typeMs.containsKey(type)) {
				typeMs.put(type, typeMs.get(type) + 1);
			} else {
				typeMs.put(type, 1);
			}
		}
//		int diss = Math.abs(arg_head - tr_head);
//		if (diss == minSubTypeDis.get(arg.entity.subType)) {
//			features.add("closeSubType_" + "yes");
//		} else {
//			features.add("closeSubType_" + "no");
//		}
//		if (subtypeMs.get(arg.entity.subType) == 1) {
//			features.add("onlySubType_" + "yes");
//		} else {
//			features.add("onlySubType_" + "no");
//		}

//		if (diss == minTypeDis.get(arg.entity.type)) {
//			features.add("closeType_" + "yes" + arg.entity.type);
//		} else {
//			features.add("closeType_" + "no" + arg.entity.type);
//		}
//		if (typeMs.get(arg.entity.type) == 1) {
//			features.add("closeType_" + "yes" + arg.entity.type);
//		} else {
//			features.add("closeType_" + "no" + arg.entity.type);
//		}
//		
		
		
		return this.convert(features, label);
	}

	public String convert(ArrayList<String> feaStrs, String label) {
		ArrayList<Feature> feas = new ArrayList<Feature>();
		StringBuilder sb = new StringBuilder();
		sb.append(label);

		HashSet<Integer> set = new HashSet<Integer>();

		for (int i = 0; i < feaStrs.size(); i++) {
			String feaStr = feaStrs.get(i);
			String extendFeaStr = feaStr.trim();
			int idx = getFeaIdx(extendFeaStr);
			if (idx == -1) {
				continue;
			}
			if (!set.contains(idx)) {
				feas.add(new Feature(idx, 1));
				set.add(idx);
			}
		}
		Collections.sort(feas);
		for (Feature fea : feas) {
			sb.append(" ").append(fea.idx).append(":").append(fea.value);
		}
		return sb.toString().trim().replace("\n", "");
	}

}
