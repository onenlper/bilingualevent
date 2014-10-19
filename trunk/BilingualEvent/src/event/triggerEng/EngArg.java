package event.triggerEng;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

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

public class EngArg {

	public static HashMap<String, HashMap<String, EventMention>> systemEventMentionses;

	public static String mode;

	public HashMap<String, Integer> featureSpace;

	int near = 0;
	int far = 0;
	
	public static void main(String args[]) {
		if (args.length <3) {
			System.out.println("java ~ [train|test|development] [svm|maxent] [folder]");
		}
		mode = args[0];
		String model = args[1];
		Util.part = args[2];
		if (!args[0].equalsIgnoreCase("train")) {
			systemEventMentionses = Util.readResult("data/engTrResult" + Util.part, "eng");
		}

		EngArg argumentFeature = new EngArg();
		if (mode.equals("train")) {
			argumentFeature.featureSpace = new HashMap<String, Integer>();
		} else {
			argumentFeature.featureSpace = Common.readFile2Map("engArgSpace" + Util.part);
		}

		ArrayList<String> files = Common.getLines("ACE_English_" + args[0] + Util.part);

		ArrayList<String> argumentRoleFeatures = new ArrayList<String>();

		ArrayList<String> argumentLines = new ArrayList<String>();
		ArrayList<String> relateEMs = new ArrayList<String>();

		for (String file : files) {
			ACEDoc document = new ACEEngDoc(file);
			if (args[0].equalsIgnoreCase("train")) {
				for (EventMention eventMention : document.goldEventMentions) {
					argumentRoleFeatures.addAll(argumentFeature.buildTrainRoleFeatures(eventMention, document));
				}
			} else {
				ArrayList<EventMention> eventMentions = new ArrayList<EventMention>();
//				eventMentions.addAll(document.goldEventMentions);
				if (systemEventMentionses.containsKey(file)) {
					eventMentions.addAll(systemEventMentionses.get(file).values());
				}
				if (eventMentions == null) {
					continue;
				}
				for (EventMention eventMention : eventMentions) {
					StringBuilder sb = new StringBuilder();
					sb.append(document.fileID).append(" ").append(eventMention.getAnchorStart()).append(" ").append(
							eventMention.getAnchorEnd()).append(" ").append(eventMention.confidence).append(" ")
							.append(eventMention.type).append(" ").append(eventMention.typeConfidence).append(" ")
							.append(eventMention.subType).append(" ").append(eventMention.subTypeConfidence).append(" ").append(eventMention.inferFrom);
					for(double confidence : eventMention.typeConfidences) {
						sb.append(" ").append(confidence);
					}
					relateEMs.add(sb.toString());
					argumentRoleFeatures.addAll(argumentFeature
							.buildTestFeatures(eventMention, document, argumentLines));
				}
			}
		}
		Common.outputLines(argumentLines, "data/engArgName_" + mode + Util.part);
		Common.outputLines(argumentRoleFeatures, "data/engArgFea_" + mode + Util.part);
		Common.outputLines(relateEMs, "data/engTrofArgName_" + mode + Util.part);
		
		System.out.println("Build argument detection feature " + mode + "...");
		System.out.println("=================");

		if (mode.equals("train")) {
			Common.outputHashMap(argumentFeature.featureSpace, "engArgSpace" + Util.part);
		}
	}

	public ArrayList<EntityMention> getArgumentCandidates(EventMention em, ACEDoc document) {
		ArrayList<EntityMention> candidates = new ArrayList<EntityMention>();

		ArrayList<EntityMention> mentions = new ArrayList<EntityMention>();

		ArrayList<EntityMention> npMentions = document.goldEntityMentions;
		ArrayList<EntityMention> timeMentions = document.goldTimeMentions;
		ArrayList<EntityMention> valueMentions = document.goldValueMentions;

		mentions.addAll(npMentions);
		mentions.addAll(timeMentions);
		mentions.addAll(valueMentions);

		int position[] = ChineseUtil.findParseFilePosition(em.getAnchorStart(), em.getAnchorEnd(), document);
		ParseResult pr = document.parseReults.get(position[0]);

		int leftBound = pr.positions.get(1)[0];
		int rightBound = pr.positions.get(pr.positions.size() - 1)[1];

		for (EntityMention mention : mentions) {
			if (mention.start >= leftBound && mention.end <= rightBound) {
				candidates.add(mention);
			}
		}
		return candidates;
	}
	static boolean sw = false;
	public ArrayList<String> buildTrainRoleFeatures(EventMention eventMention, ACEDoc document) {
		ArrayList<EventMentionArgument> arguments = eventMention.getEventMentionArguments();

		HashMap<String, EventMentionArgument> goldArgHash = new HashMap<String, EventMentionArgument>();

		for (EventMentionArgument arg : arguments) {
			goldArgHash.put(arg.toString(), arg);
		}

		ArrayList<EntityMention> entityMentions = this.getArgumentCandidates(eventMention, document);
		sw = true;
		ArrayList<String> features = new ArrayList<String>();
		for (EntityMention mention : entityMentions) {
			String start = Integer.toString(mention.getStart());
			String end = Integer.toString(mention.getEnd());
			String role = "null";
			EventMentionArgument arg = goldArgHash.get(start + "," + end);
			if (arg != null) {
				role = arg.getRole();
			}
			String feature = this.buildFeature(mention, eventMention, document, Integer.toString(Util.roles
					.indexOf(role) + 1));
			features.add(feature);
		}
		sw = false;
		return features;
	}

	public EntityMention getEntityMention(int start, int end, ArrayList<EntityMention> mentions1) {
		for (EntityMention mention : mentions1) {
			if (mention.start == start && mention.end == end) {
				return mention;
			}
		}
		return null;
	}

	public ArrayList<String> buildTestFeatures(EventMention eventMention, ACEDoc document,
			ArrayList<String> argumentLines) {
		ArrayList<EntityMention> entityMentions = this.getArgumentCandidates(eventMention, document);
		ArrayList<String> features = new ArrayList<String>();

		for (EntityMention entityMention : entityMentions) {
			String feature = this.buildFeature(entityMention, eventMention, document, Integer.toString(Util.roles
					.indexOf("null") + 1));
			features.add(feature);

			if (argumentLines != null) {
				StringBuilder sb = new StringBuilder();
				sb.append(document.fileID).append(" ").append(eventMention.getAnchorStart()).append(" ").append(
						eventMention.getAnchorEnd()).append(" ").append(eventMention.confidence).append(" ").append(
						eventMention.type).append(" ").append(eventMention.typeConfidence).append(" ").append(
						eventMention.subType).append(" ").append(eventMention.subTypeConfidence).append(" ").append(
						entityMention.start).append(" ").append(entityMention.end);

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

	public String buildFeature(EntityMention entityMention, EventMention eventMention, ACEDoc document,
			String label) {
		String trigger = eventMention.getAnchor();
		int position[] = ChineseUtil.findParseFilePosition2(entityMention.start, entityMention.end, document);
		int position2[] = ChineseUtil.findParseFilePosition2(eventMention.getAnchorStart(), eventMention.getAnchorEnd(),
				document);

		int leftIndex2 = position2[1];
		int rightIndex2 = position2[3];

		ParseResult pr = document.parseReults.get(position[0]);
		ParseResult pr2 = document.parseReults.get(position2[0]);
		int index = position[0];
		int leftIndex = position[1];
		int rightIndex = position[3];
		ArrayList<String> words = pr.words;
		ArrayList<String> posTags = pr.posTags;
		ArrayList<int[]> positions = pr.positions;

		ArrayList<String> words2 = pr2.words;
		ArrayList<String> posTags2 = pr2.posTags;

		String triggerPOS = posTags2.get(position2[3]);

		String path = "-";
		String depPath = "-";
		int distance = -1;
		MyTreeNode entityNode = pr.tree.leaves.get(position[3]);
		MyTreeNode eventNode = pr2.tree.leaves.get(position2[3]);
		
		int entityID = position[3];
		int eventID = position2[3];
		
		if (position[0] == position2[0]) {
			ArrayList<MyTreeNode> entityAncestors = entityNode.getAncestors();
			ArrayList<MyTreeNode> eventAncestors = eventNode.getAncestors();
			
			MyTreeNode entityIP = null;
			MyTreeNode eventIP = null;
			for(int k=entityAncestors.size()-1;k>=0;k--) {
				MyTreeNode node = entityAncestors.get(k);
				if(node.value.equalsIgnoreCase("ip")||node.value.equalsIgnoreCase("root")) {
					entityIP = node;
					break;
				}
			}
			for(int k=eventAncestors.size()-1;k>=0;k--) {
				MyTreeNode node = eventAncestors.get(k);
				if(node.value.equalsIgnoreCase("ip")||node.value.equalsIgnoreCase("root")) {
					eventIP = node;
					break;
				}
			}

			if(!Util.roles.get(Integer.parseInt(label)-1).equalsIgnoreCase("null")) {
				if(entityIP==eventIP) {
					near++;
				} else {
					far++;
				}
			}
			
			int common = 0;
			MyTreeNode commonNode = null;
			for (int i = 0; i < entityAncestors.size() && i < eventAncestors.size(); i++) {
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
			while (!pr.dependTree.vertexMap.containsKey(entityID) && entityID!=0) {
				entityID--;
			}
			if (entityID != eventID) {
				MyTreeNode entityDepNode = pr.dependTree.vertexMap.get(entityID);
				MyTreeNode eventDepNode = pr.dependTree.vertexMap.get(eventID);
				if(entityDepNode!=null && eventDepNode!=null) {
					entityAncestors = entityDepNode.getAncestors();
					eventAncestors = eventDepNode.getAncestors();
					common = 0;
					commonNode = null;
					for (int i = 0; i < entityAncestors.size() && i < eventAncestors.size(); i++) {
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

		features.add(trigger);
		features.add(triggerPOS);
		
		features.add(eventMention.getSubType());
		features.add(entityMention.head);
		features.add(entityMention.getType());
		features.add(entityMention.entity.type);
		features.add(entityMention.entity.subType);
		features.add(eventMention.subType + "_" + entityMention.head);
		features.add(eventMention.subType + "_" + entityMention.entity.type);
		features.add(eventMention.subType + "_" + entityMention.entity.subType);

		features.add(leftWord2);
		features.add(rightWord2);
		features.add(leftWord2 + "_" + leftPOS2);
		features.add(rightWord2 + "-" + rightPOS2);

		features.add(leftWord);
		features.add(rightWord);
		features.add(leftWord + "_" + leftPOS);
		features.add(rightWord + "-" + rightPOS);

		features.add(eventNode.parent.parent.value);
		features.add(eventNode.parent.parent.productionRule());

		if (entityMention.end < eventMention.start) {
			features.add("left");
		} else {
			features.add("right");
		}
		
		features.add(path);
		features.add(Integer.toString(distance));
		features.add(depPath);
		
		return this.convert(features, label);
	}

	public String convert(ArrayList<String> feaStrs, String label) {
		ArrayList<Feature> feas = new ArrayList<Feature>();
		StringBuilder sb = new StringBuilder();
		sb.append(label);
		for (int i = 0; i < feaStrs.size(); i++) {
			String feaStr = feaStrs.get(i);
			String extendFeaStr = feaStr.trim() + "_" + i;
			int idx = getFeaIdx(extendFeaStr);
			if (idx == -1) {
				continue;
			}
			feas.add(new Feature(idx, 1));
		}
		Collections.sort(feas);
		for (Feature fea : feas) {
			sb.append(" ").append(fea.idx).append(":").append(fea.value);
		}
		return sb.toString().trim().replace("\n", "");
	}

}
