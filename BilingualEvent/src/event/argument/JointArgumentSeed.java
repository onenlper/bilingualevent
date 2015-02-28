package event.argument;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import model.ACEChiDoc;
import model.ACEDoc;
import model.EntityMention;
import model.EventMention;
import model.EventMentionArgument;
import model.Feature;
import model.ParseResult;
import model.SemanticRole;
import model.syntaxTree.MyTreeNode;
import seeds.SeedUtil;
import util.ChineseUtil;
import util.Common;
import util.Util;
import entity.semantic.SemanticTrainMultiSeed;
import event.preProcess.ChineseTriggerIndent;
import event.trigger.TriggerIndent;

public class JointArgumentSeed {

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
		if (mode.equals("test")) {
			// allSVMResult = Util.loadSemanticResult();
			// loadSystemEntityMentions();
			loadSVMResult(Util.part);
			systemEventMentionses = Util.readResult("joint_" + args[1]
					+ "/result" + ".trigger" + Util.part, "chi");
		}

		JointArgumentSeed argumentFeature = new JointArgumentSeed();
		if (mode.equals("train")) {
			argumentFeature.featureSpace = new HashMap<String, Integer>();
		} else {
			argumentFeature.featureSpace = Common
					.readFile2Map("argumentJointFeaSpace_" + args[1]
							+ Util.part);
		}

		ArrayList<String> argumentRoleFeatures = new ArrayList<String>();
		ArrayList<String> argumentLines = new ArrayList<String>();
		ArrayList<String> relateEMs = new ArrayList<String>();

		if (mode.equals("train")) {
			ACEChiDoc doc = SeedUtil.getSeedDoc();
			for (EventMention eventMention : doc.goldEventMentions) {
				argumentRoleFeatures.addAll(argumentFeature
						.buildTrainRoleFeatures(eventMention, doc));
			}
			ArrayList<String> lines = Common.getLines("ACE_Chinese_train6");
			
			for (String line : lines) {
				
				String tks[] = line.trim().split("\\s+");
				if(tks.length==1) {
					continue;
				}
				String file = tks[0];
				boolean all = false;
				HashSet<Integer> eventIDs = new HashSet<Integer>();
				for(int i=1;i<tks.length;i++) {
					if(tks[i].equals("all")) {
						all = true;
						break;
					}
					eventIDs.add(Integer.parseInt(tks[i]));
				}
				
				ACEChiDoc document = new ACEChiDoc(file);
				for (EventMention eventMention : document.goldEventMentions) {
					if(eventIDs.contains(eventMention.getAnchorEnd()) || all) {
						argumentRoleFeatures.addAll(argumentFeature
							.buildTrainRoleFeatures(eventMention, document));
					}
				}
			}

		} else {
			ArrayList<String> files = Common.getLines("ACE_Chinese_test"
					+ Util.part);
			
			for (String file : files) {
				ACEChiDoc document = new ACEChiDoc(file);

				ArrayList<EventMention> eventMentions = new ArrayList<EventMention>();
				if (systemEventMentionses.containsKey(file)) {
					eventMentions.addAll(systemEventMentionses.get(file)
							.values());
				}

				for (EventMention eventMention : eventMentions) {
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
		Common.outputLines(argumentLines, "data/Joint_argumentLines_" + mode
				+ model + Util.part);
		Common.outputLines(argumentRoleFeatures, "data/Joint_argument_" + mode
				+ model + Util.part);
		Common.outputLines(relateEMs, "data/Joint_argumentRelateEM_" + mode
				+ model + Util.part);

		System.out.println("Build argument detection feature " + mode + "...");
		System.out.println("=================");

		if (mode.equals("train")) {
			Common.outputHashMap(argumentFeature.featureSpace,
					"argumentJointFeaSpace_" + args[1] + Util.part);
		}

		System.err.println("ArgCount: " + argCount);

//		System.out.println(roleC + "######");
	}

	public static void loadSVMResult(String part) {
		mentionses = new HashMap<String, ArrayList<EntityMention>>();
		timeExpressions = new HashMap<String, ArrayList<EntityMention>>();
		valueExpressions = new HashMap<String, ArrayList<EntityMention>>();

		String folder = "./";
		ArrayList<String> mentionStrs = Common.getLines(folder + "mention.test"
				+ part);
		System.out.println(mentionStrs.size());
		ArrayList<String> typeResult = Common.getLines(folder
				+ "multiType.result" + part);

		for (int i = 0; i < mentionStrs.size(); i++) {
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

			String type = SemanticTrainMultiSeed.semClasses.get(typeIndex - 1);
			if (type.equalsIgnoreCase("none")) {
				continue;
			}

			em.semClass = type;

			ArrayList<EntityMention> mentions = mentionses.get(fileKey);
			ArrayList<EntityMention> timeExpression = timeExpressions
					.get(fileKey);
			ArrayList<EntityMention> valueExpression = valueExpressions
					.get(fileKey);
			if (mentions == null) {
				mentions = new ArrayList<EntityMention>();
				mentionses.put(fileKey, mentions);
			}
			if (timeExpression == null) {
				timeExpression = new ArrayList<EntityMention>();
				timeExpressions.put(fileKey, timeExpression);
			}
			if (valueExpression == null) {
				valueExpression = new ArrayList<EntityMention>();
				valueExpressions.put(fileKey, valueExpression);
			}
			if (type.equalsIgnoreCase("val")) {
				em.type = "Value";
				valueExpression.add(em);
			} else if (type.equalsIgnoreCase("time")) {
				em.type = "Time";
				timeExpression.add(em);
			} else {
				mentions.add(em);
			}
		}
	}

	// public static HashMap<String, ArrayList<EntityMention>> allSVMResult;
	static HashMap<String, ArrayList<EntityMention>> mentionses;
	static HashMap<String, ArrayList<EntityMention>> timeExpressions;
	static HashMap<String, ArrayList<EntityMention>> valueExpressions;

	static HashMap<String, ArrayList<EntityMention>> nerses;

	public ArrayList<EntityMention> getArgumentCandidates(EventMention em,
			ACEChiDoc document, boolean train) {
		ArrayList<EntityMention> candidates = new ArrayList<EntityMention>();

		ArrayList<EntityMention> mentions = new ArrayList<EntityMention>();

		ArrayList<EntityMention> npMentions = null;
		ArrayList<EntityMention> timeMentions = null;
		ArrayList<EntityMention> valueMentions = null;

		if (!train) {
			npMentions = mentionses.get(document.fileID);
			timeMentions = timeExpressions.get(document.fileID);
			valueMentions = valueExpressions.get(document.fileID);

			for (EntityMention m : npMentions) {
				Util.setMentionType(m, document);
			}
		} else {
			npMentions = document.goldEntityMentions;
			timeMentions = document.goldTimeMentions;
			valueMentions = document.goldValueMentions;
		}

		// if(!train) {
		// for(EntityMention mention : npMentions) {
		// this.assignSystemSemanticType(mention, document);
		// }
		// }

		mentions.addAll(npMentions);
		mentions.addAll(timeMentions);
		mentions.addAll(valueMentions);

		int position[] = ChineseUtil.findParseFilePosition(em.getAnchorStart(),
				em.getAnchorEnd(), document);
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

	static int argCount = 0;

	static int roleC = 0;

	public static List<String> roles = Arrays.asList("Crime", "Victim",
			"Origin", "Adjudicator", "Target", "Org", "Recipient", "Vehicle",
			"Plaintiff", "Attacker", "Place", "Buyer", "Money", "Giver",
			"Beneficiary", "Agent", "Time", "Seller", "Defendant", "Artifact",
			"Prosecutor", "Sentence", "Price", "Position", "Instrument",
			"Destination", "Person", "Entity", "null");

	public ArrayList<String> buildTrainRoleFeatures(EventMention eventMention,
			ACEChiDoc document) {
		ArrayList<EventMentionArgument> arguments = eventMention
				.getEventMentionArguments();

		HashMap<String, EventMentionArgument> goldArgHash = new HashMap<String, EventMentionArgument>();

		for (EventMentionArgument arg : arguments) {
			goldArgHash.put(Integer.toString(arg.getEnd()), arg);
		}

		ArrayList<EntityMention> entityMentions = this.getArgumentCandidates(
				eventMention, document, true);
		sw = true;
		ArrayList<String> features = new ArrayList<String>();
		for (EntityMention mention : entityMentions) {
			String start = Integer.toString(mention.getStart());
			String end = Integer.toString(mention.getEnd());
			String role = "null";
			EventMentionArgument arg = goldArgHash.get(end);
			if (arg != null) {
				role = arg.getRole();
				roleC += 1;
			}
			if(role.startsWith("Time")) {
				role = "Time";
			}
			
			if (!roles.contains(role)) {
				Common.bangErrorPOS(role);
			}

			String feature = this.buildFeature(mention, eventMention, document,
					Integer.toString(roles.indexOf(role) + 1));
			features.add(feature);
		}
		sw = false;
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
			ACEChiDoc document, ArrayList<String> argumentLines) {
		ArrayList<EntityMention> entityMentions = this.getArgumentCandidates(
				eventMention, document, false);
		ArrayList<String> features = new ArrayList<String>();

		argCount += entityMentions.size();
		for (EntityMention entityMention : entityMentions) {
			String feature = this.buildFeature(entityMention, eventMention,
					document, Integer.toString(Util.roles.indexOf("null") + 1));
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

	public ArrayList<Boolean> sameClause = new ArrayList<Boolean>();

	public String buildFeature(EntityMention entityMention,
			EventMention eventMention, ACEChiDoc document, String label) {
		String trigger = eventMention.getAnchor();
		int position[] = ChineseUtil.findParseFilePosition(entityMention.start,
				entityMention.end, document);
		int position2[] = ChineseUtil.findParseFilePosition(
				eventMention.getAnchorStart(), eventMention.getAnchorEnd(),
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
			for (int k = entityAncestors.size() - 1; k >= 0; k--) {
				MyTreeNode node = entityAncestors.get(k);
				if (node.value.equalsIgnoreCase("ip")
						|| node.value.equalsIgnoreCase("root")) {
					entityIP = node;
					break;
				}
			}
			for (int k = eventAncestors.size() - 1; k >= 0; k--) {
				MyTreeNode node = eventAncestors.get(k);
				if (node.value.equalsIgnoreCase("ip")
						|| node.value.equalsIgnoreCase("root")) {
					eventIP = node;
					break;
				}
			}
			if (entityIP == eventIP) {
				sameClause.add(true);
			} else {
				sameClause.add(false);
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
			while (!pr.dependTree.vertexMap.containsKey(entityID)
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

		features.add(trigger);
		features.add(triggerPOS);

		features.add(entityMention.head);
		features.add(entityMention.getType());
		features.add(entityMention.semClass);
		features.add(entityMention.subType);

		String eventSubType = eventMention.subType;

		eventSubType = eventMention.getAnchor();
		// if(eventSubType.equalsIgnoreCase("null")) {
		// eventSubType =
		// pipelineResults.get(document.fileID).get(eventMention.toString()).subType;
		// }

		features.add(eventSubType);
		features.add(eventSubType + "_" + entityMention.head);
		features.add(eventSubType + "_" + entityMention.semClass);
		features.add(eventSubType + "_" + entityMention.subType);

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

		HashMap<EventMention, SemanticRole> semanticRoles = document.semanticRoles;
		boolean arg0 = false;
		boolean arg1 = false;
		boolean tmpArg = false;
		boolean pred = false;
		for (SemanticRole role : semanticRoles.values()) {
			EventMention predicate = role.predict;
			if (!(predicate.getAnchorEnd() < eventMention.getAnchorStart() || predicate
					.getAnchorStart() > eventMention.getAnchorEnd())) {
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
		Util.addZeroPronounFeature(features, document, entityMention,
				eventMention, label, sw);

		// System.out.println(features);
		// Common.pause("");

		return this.convert(features, label);
	}

	public String convert(ArrayList<String> feaStrs, String label) {
		ArrayList<Feature> feas = new ArrayList<Feature>();
		StringBuilder sb = new StringBuilder();
		sb.append(label);
		for (int i = 0; i < feaStrs.size(); i++) {
			if (feaStrs.get(i) == null) {
				continue;
			}
			String feaStr = feaStrs.get(i).toLowerCase();
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
