package event.argument;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import model.ACEChiDoc;
import model.EntityMention;
import model.EventMention;
import model.EventMentionArgument;
import model.Feature;
import model.ParseResult;
import model.SemanticRole;
import model.syntaxTree.MyTreeNode;
import util.ChineseUtil;
import util.Common;
import util.Util;

public class ArgumentFeature {

	public static HashMap<String, HashMap<String, EventMention>> systemEventMentionses;

	public static String mode;

	public HashMap<String, Integer> featureSpace;

	public static void main(String args[]) {
		if (args.length <3) {
			System.out.println("java ~ [train|test|development] [svm|maxent] [folder]");
		}
		mode = args[0];
		String model = args[1];
		Util.part = args[2];
		if (!args[0].equalsIgnoreCase("train")) {
			if(args.length==4 && args[3].equals("discourse")) {
				systemEventMentionses = Util.readResult("pipe_" + model + "/result" + ".trigger.discourse" + Util.part, "chi");
			} else {
				systemEventMentionses = Util.readResult("pipe_" + model + "/result" + ".trigger" + Util.part, "chi");
			}
		}

		ArgumentFeature argumentFeature = new ArgumentFeature();
		if (mode.equals("train")) {
			argumentFeature.featureSpace = new HashMap<String, Integer>();
		} else {
			argumentFeature.featureSpace = Common.readFile2Map("argumentPipeFeaSpace_" + args[1] + Util.part);
		}

		ArrayList<String> files = Common.getLines("ACE_Chinese_" + args[0] + Util.part);
		ArrayList<String> argumentIndentFeatures = new ArrayList<String>();
		ArrayList<String> argumentRoleFeatures = new ArrayList<String>();

		ArrayList<String> argumentLines = new ArrayList<String>();
		ArrayList<String> relateEMs = new ArrayList<String>();

		for (String file : files) {
			ACEChiDoc document = new ACEChiDoc(file);
			if (args[0].equalsIgnoreCase("train")) {
				for (EventMention eventMention : document.goldEventMentions) {
					ArrayList<String> identFeatures = argumentFeature.buildTrainIndentFeatures(eventMention, document, argumentLines);
					argumentIndentFeatures.addAll(identFeatures);
					argumentRoleFeatures.addAll(argumentFeature.buildTrainRoleFeatures(eventMention, document));
				}
			} else {
				ArrayList<EventMention> eventMentions = new ArrayList<EventMention>();
//				eventMentions.addAll(document.readGoldEventMention());
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
					relateEMs.add(sb.toString());

					ArrayList<String> identFeatures = argumentFeature.buildTestFeatures(eventMention, document, null);
					argumentIndentFeatures.addAll(identFeatures);
					argumentRoleFeatures.addAll(argumentFeature
							.buildTestFeatures(eventMention, document, argumentLines));
				}
			}
		}
//		if (!args[0].equalsIgnoreCase("train")) {
			Common.outputLines(argumentLines, "data/Pipe_argumentLines_" + mode + model + Util.part);
//		}
		Common.outputLines(argumentRoleFeatures, "data/Pipe_argumentRole_" + mode + model + Util.part);
		Common.outputLines(argumentIndentFeatures, "data/Pipe_argumentIndent_" + mode + model + Util.part);
		Common.outputLines(relateEMs, "data/Pipe_argumentRelateEM_" + mode + model + Util.part);

		System.out.println("Build argument detection feature " + mode + "...");
		System.out.println("=================");

		if (mode.equals("train")) {
			Common.outputHashMap(argumentFeature.featureSpace, "argumentPipeFeaSpace_" + args[1] + Util.part);
		}
	}

	public ArrayList<EntityMention> getArgumentCandidates(EventMention em, ACEChiDoc document) {
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

	public ArrayList<String> buildTrainRoleFeatures(EventMention eventMention, ACEChiDoc document) {
		ArrayList<EventMentionArgument> arguments = eventMention.getEventMentionArguments();
		ArrayList<String> features = new ArrayList<String>();
		for (EventMentionArgument argument : arguments) {
			int start = argument.getStart();
			int end = argument.getEnd();
			ArrayList<EntityMention> mentions = new ArrayList<EntityMention>();
			mentions.addAll(document.goldEntityMentions);
			mentions.addAll(document.goldTimeMentions);
			mentions.addAll(document.goldValueMentions);
			EntityMention mention = this.getEntityMention(start, end, mentions);
			String feature = this.buildFeature(mention, eventMention, document, Integer.toString(Util.roles
					.indexOf(argument.getRole()) + 1));
			features.add(feature);
			
			
		}
		return features;
	}

	static boolean sw = false;

	public ArrayList<String> buildTrainIndentFeatures(EventMention eventMention, ACEChiDoc document, ArrayList<String> argumentLines) {
		ArrayList<EntityMention> entityMentions = this.getArgumentCandidates(eventMention, document);
		ArrayList<String> features = new ArrayList<String>();
		sw = true;
		for (EntityMention entityMention : entityMentions) {
			boolean gold = this.isGoldArgument(eventMention.getEventMentionArguments(), entityMention);
			if (gold) {
				String feature = this.buildFeature(entityMention, eventMention, document, "1");
				features.add(feature);
			} else {
				String feature = this.buildFeature(entityMention, eventMention, document, "2");
				features.add(feature);
			}
			
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
		sw = false;
		return features;
	}

	public boolean isGoldArgument(ArrayList<EventMentionArgument> arguments, EntityMention mention) {
		for (EventMentionArgument argument : arguments) {
			if (argument.getStart() == mention.start && argument.getEnd() == mention.end) {
				return true;
			}
		}
		return false;
	}

	public EntityMention getEntityMention(int start, int end, ArrayList<EntityMention> mentions1) {
		for (EntityMention mention : mentions1) {
			if (mention.start == start && mention.end == end) {
				return mention;
			}
		}
		return null;
	}

	public ArrayList<String> buildTestFeatures(EventMention eventMention, ACEChiDoc document,
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

public String buildFeature(EntityMention entityMention, EventMention eventMention, ACEChiDoc document,
			String label) {
		String trigger = eventMention.getAnchor();
		int position[] = ChineseUtil.findParseFilePosition(entityMention.start, entityMention.end, document);
		int position2[] = ChineseUtil.findParseFilePosition(eventMention.getAnchorStart(), eventMention.getAnchorEnd(),
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
//			if(entityIP==eventIP) {
//				sameClause.add(true);
//			} else {
//				sameClause.add(false);
//			}
//
//			if(!Util.roles.get(Integer.parseInt(label)-1).equalsIgnoreCase("null")) {
//				if(entityIP==eventIP) {
//					near++;
//				} else {
//					far++;
//				}
//			}
//			
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
			while (!pr.dependTree.vertexMapTree.containsKey(entityID) && entityID!=0) {
				entityID--;
			}
			if (entityID != eventID) {
				MyTreeNode entityDepNode = pr.dependTree.vertexMapTree.get(entityID);
				MyTreeNode eventDepNode = pr.dependTree.vertexMapTree.get(eventID);
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
		
		ArrayList<SemanticRole> semanticRoles = new ArrayList<SemanticRole>(document.semanticRoles.values());
		boolean arg0 = false;
		boolean arg1 = false;
		boolean tmpArg = false;
		boolean pred = false;
		for (SemanticRole role : semanticRoles) {
			EventMention predicate = role.predict;
			if (!(predicate.getAnchorEnd() < eventMention.getAnchorStart() || predicate.getAnchorStart() > eventMention
					.getAnchorEnd())) {
				pred = true;
				ArrayList<EntityMention> arg0s = role.arg0;
				for (EntityMention arg : arg0s) {
					if (arg.end == entityMention.end) {
						arg0 = true;
					}
				}
				ArrayList<EntityMention> arg1s = role.arg1;
				for (EntityMention arg : arg1s) {
					if (arg.end == entityMention.end) {
						arg1 = true;
					}
				}
				ArrayList<EntityMention> tempArgs = role.tmp;
				for (EntityMention arg : tempArgs) {
					if (arg.end == entityMention.end) {
						tmpArg = true;
					}
				}
			}
		}

		if (pred) {
			features.add("1");
		} else {
			features.add("0");
		}

		if (arg0) {
			features.add("1");
		} else {
			features.add("0");
		}
		if (arg1) {
			features.add("1");
		} else {
			features.add("0");
		}
		if (tmpArg) {
			features.add("1");
		} else {
			features.add("0");
		}
		Util.addZeroPronounFeature(features, document, entityMention, eventMention, label, sw);
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
