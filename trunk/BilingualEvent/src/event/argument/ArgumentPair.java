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

public class ArgumentPair {

	public static HashMap<String, HashMap<String, EventMention>> systemEventMentionses;

	public static String mode;

	public HashMap<String, Integer> featureSpace;

	int near = 0;
	int far = 0;

	public static void main(String args[]) {
		if (args.length != 3) {
			System.out.println("java ~ [train|test|development] [svm|maxent] [folder]");
			System.exit(1);
		}
		mode = args[0];
		String model = args[1];
		Util.part = args[2];
		if (!args[0].equalsIgnoreCase("train")) {
			systemEventMentionses = Util.readResult("joint_" + args[1] + "/result" + ".trigger" + Util.part, "chi");
		}

		ArgumentPair argumentFeature = new ArgumentPair();
		if (mode.equals("train")) {
			argumentFeature.featureSpace = new HashMap<String, Integer>();
			loadTrainForTest(Util.part);
		} else {
			argumentFeature.featureSpace = Common.readFile2Map("argumentJointFeaSpace_" + args[1] + Util.part);
			loadTest(Util.part);
		}

		ArrayList<String> files = Common.getLines("ACE_Chinese_" + args[0] + Util.part);

		ArrayList<String> argumentPairFeatures = new ArrayList<String>();

		ArrayList<String> argumentLines = new ArrayList<String>();

		for (String file : files) {
			ACEChiDoc document = new ACEChiDoc(file);
			if (args[0].equalsIgnoreCase("train")) {
				for (EventMention eventMention : document.goldEventMentions) {
					argumentPairFeatures.addAll(argumentFeature.buildTrainPairFeatures(eventMention, document));
				}
			} else {
				ArrayList<EventMention> eventMentions = new ArrayList<EventMention>();
				// eventMentions.addAll(document.goldEventMentions);
				if (systemEventMentionses.containsKey(file)) {
					eventMentions.addAll(systemEventMentionses.get(file).values());
				}
				if (eventMentions == null) {
					continue;
				}
				for (EventMention eventMention : eventMentions) {
					argumentPairFeatures.addAll(argumentFeature
							.buildTestPairFeatures(eventMention, document, argumentLines));
				}
			}
		}
		if (!args[0].equalsIgnoreCase("train")) {
			Common.outputLines(argumentLines, "data/Joint_argumentPairLines_" + mode + model + Util.part);
		}
		Common.outputLines(argumentPairFeatures, "data/Joint_argumentPair_" + mode + model + Util.part);

		System.out.println("Build argument detection feature " + mode + "...");
		System.out.println("=================");

		if (mode.equals("train")) {
			Common.outputHashMap(argumentFeature.featureSpace, "argumentJointFeaSpace_" + args[1] + Util.part);
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

	public ArrayList<String> buildTrainPairFeatures(EventMention eventMention, ACEChiDoc document) {
		ArrayList<EventMentionArgument> arguments = eventMention.getEventMentionArguments();

		HashMap<String, EventMentionArgument> goldArgHash = new HashMap<String, EventMentionArgument>();

		for (EventMentionArgument arg : arguments) {
			goldArgHash.put(arg.toString(), arg);
		}

		ArrayList<EntityMention> entityMentions = this.getArgumentCandidates(eventMention, document);
		ArrayList<String> features = new ArrayList<String>();
		for (EntityMention mention1 : entityMentions) {
			boolean gold1 = this.isGoldArgument(eventMention.getEventMentionArguments(), mention1);
			for (EntityMention mention2 : entityMentions) {
				boolean gold2 = this.isGoldArgument(eventMention.getEventMentionArguments(), mention2);
				if (gold1 == gold2) {
					continue;
				}
				String label = "";
				if (gold1 && !gold2) {
					label = "1";
				} else if (!gold1 && gold2) {
					label = "2";
				}
				String feature1 = this.buildFeature(mention1, eventMention, document, label, true);
				String feature2 = this.buildFeature(mention1, eventMention, document, label, false);
				features.add(feature1 + feature2);
			}
		}
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

	public ArrayList<String> buildTestPairFeatures(EventMention eventMention, ACEChiDoc document,
			ArrayList<String> argumentLines) {
		ArrayList<EntityMention> entityMentions = this.getArgumentCandidates(eventMention, document);
		ArrayList<String> features = new ArrayList<String>();

		for (EntityMention entityMention1 : entityMentions) {
			for (EntityMention entityMention2 : entityMentions) {
				if (entityMention1.equals(entityMention2)) {
					continue;
				}
				String feature1 = this.buildFeature(entityMention1, eventMention, document, "1", true);
				String feature2 = this.buildFeature(entityMention1, eventMention, document, "1", false);
				features.add(feature1 + feature2);

				if (argumentLines != null) {
					StringBuilder sb = new StringBuilder();
					sb.append(document.fileID).append(" ").append(eventMention.getAnchorStart()).append(" ").append(
							eventMention.getAnchorEnd()).append(" ").append(entityMention1.start).append(" ").append(
							entityMention1.end).append(" ").append(entityMention2.start).append(" ").append(
							entityMention2.end).append(" ");

					argumentLines.add(sb.toString());
				}
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

	public String buildFeature(EntityMention entityMention, EventMention eventMention, ACEChiDoc document,
			String label, boolean first) {
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
			for (int k = entityAncestors.size() - 1; k >= 0; k--) {
				MyTreeNode node = entityAncestors.get(k);
				if (node.value.equalsIgnoreCase("ip") || node.value.equalsIgnoreCase("root")) {
					entityIP = node;
					break;
				}
			}
			for (int k = eventAncestors.size() - 1; k >= 0; k--) {
				MyTreeNode node = eventAncestors.get(k);
				if (node.value.equalsIgnoreCase("ip") || node.value.equalsIgnoreCase("root")) {
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
			while (!pr.dependTree.vertexMapTree.containsKey(entityID) && entityID != 0) {
				entityID--;
			}
			if (entityID != eventID) {
				MyTreeNode entityDepNode = pr.dependTree.vertexMapTree.get(entityID);
				MyTreeNode eventDepNode = pr.dependTree.vertexMapTree.get(eventID);
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
		features.add(eventMention.getType());
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

		boolean zero = Util.isZeroPronoun(eventMention, document);
		// if(zero) {
		// pred = true;
		// arg0 = true;
		// }
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
		// Util.addZeroPronounFeature(features, document, entityMention,
		// eventMention, label, sw);
		// // addDependencyFeature(features, document, eventMention,
		// entityMention, pr);
		return this.convert(features, label, first);
	}

	int secondStart = 800000;

	public String convert(ArrayList<String> feaStrs, String label, boolean first) {
		ArrayList<Feature> feas = new ArrayList<Feature>();
		StringBuilder sb = new StringBuilder();
		if (first) {
			sb.append(label);
		}
		for (int i = 0; i < feaStrs.size(); i++) {
			String feaStr = feaStrs.get(i);
			String extendFeaStr = feaStr.trim() + "_" + i;
			int idx = getFeaIdx(extendFeaStr);
			if (idx == -1) {
				continue;
			}
			if (first) {
				feas.add(new Feature(idx, 1));
			} else {
				feas.add(new Feature(idx + secondStart, 1));
			}
		}
		Collections.sort(feas);
		for (Feature fea : feas) {
			sb.append(" ").append(fea.idx).append(":").append(fea.value);
		}
		return sb.toString().replace("\n", "");
	}

	static HashMap<String, HashMap<String, EventMention>> jointResults;

	public static void loadTest(String folder) {
		jointResults = Util.readResult("joint_svm/result" + folder, "chi");
	}

	public static void loadTrainForTest(String folder) {
		jointResults = new HashMap<String, HashMap<String, EventMention>>();
		for (int i = 1; i <= 10; i++) {
			if (i == Integer.valueOf(folder)) {
				continue;
			}
			HashMap<String, HashMap<String, EventMention>> part = Util.readResult("joint_svm/result"
					+ Integer.toString(i), "chi");
			for (String key : part.keySet()) {
				jointResults.put(key, part.get(key));
			}
		}
	}
}
