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

public class JointArgument {

	public static HashMap<String, HashMap<String, EventMention>> systemEventMentionses;

	public static String mode;

	public HashMap<String, Integer> featureSpace;

	int near = 0;
	int far = 0;

	public static void main(String args[]) {
		if (args.length < 3) {
			System.out.println("java ~ [train|test|development] [svm|maxent] [folder]");
		}
		mode = args[0];
		String model = args[1];
		Util.part = args[2];
		if (mode.equals("test")) {
			allSVMResult = Util.loadSemanticResult();
			loadSystemEntityMentions();
			loadTest(Util.part);
		}
		if (!args[0].equalsIgnoreCase("train")) {
			if (args.length == 4 && args[3].equals("discourse")) {
				systemEventMentionses = Util.readResult("joint_" + args[1] + "/result" + ".trigger.discourse"
						+ Util.part, "chi");
			} else {
				systemEventMentionses = Util.readResult("joint_" + args[1] + "/result" + ".trigger" + Util.part, "chi");
			}
		}

		JointArgument argumentFeature = new JointArgument();
		if (mode.equals("train")) {
			argumentFeature.featureSpace = new HashMap<String, Integer>();
		} else {
			argumentFeature.featureSpace = Common.readFile2Map("argumentJointFeaSpace_" + args[1] + Util.part);
		}

		ArrayList<String> files = Common.getLines("ACE_Chinese_" + args[0] + Util.part);

		ArrayList<String> argumentRoleFeatures = new ArrayList<String>();

		ArrayList<String> argumentLines = new ArrayList<String>();
		ArrayList<String> relateEMs = new ArrayList<String>();

		int number = 0;
		
		for (String file : files) {
			ACEChiDoc document = new ACEChiDoc(file);
			if (args[0].equalsIgnoreCase("train")) {
				for (EventMention eventMention : document.goldEventMentions) {
					argumentRoleFeatures.addAll(argumentFeature.buildTrainRoleFeatures(eventMention, document));
				}
			} else {
				ArrayList<EventMention> eventMentions = new ArrayList<EventMention>();
//				 eventMentions.addAll(document.goldEventMentions);
				if (systemEventMentionses.containsKey(file)) {
					eventMentions.addAll(systemEventMentionses.get(file).values());
				}
				if (eventMentions == null) {
					continue;
				}
				number += eventMentions.size();
				for (EventMention eventMention : eventMentions) {
					StringBuilder sb = new StringBuilder();
					sb.append(document.fileID).append(" ").append(eventMention.getAnchorStart()).append(" ").append(
							eventMention.getAnchorEnd()).append(" ").append(eventMention.confidence).append(" ")
							.append(eventMention.type).append(" ").append(eventMention.typeConfidence).append(" ")
							.append(eventMention.subType).append(" ").append(eventMention.subTypeConfidence)
							.append(" ").append(eventMention.inferFrom);
					for (double confidence : eventMention.typeConfidences) {
						sb.append(" ").append(confidence);
					}
					relateEMs.add(sb.toString());

					argumentRoleFeatures.addAll(argumentFeature
							.buildTestFeatures(eventMention, document, argumentLines));
				}
			}
		}
		System.out.println(number + "###");
		ArrayList<String> argumentLinesFar = new ArrayList<String>();
		ArrayList<String> argumentLinesNear = new ArrayList<String>();
		if (!args[0].equalsIgnoreCase("train")) {
			Common.outputLines(argumentLines, "data/Joint_argumentLines_" + mode + model + Util.part);
			for (int i = 0; i < argumentLines.size(); i++) {
				boolean sameIP = argumentFeature.sameClause.get(i);
				if (sameIP) {
					argumentLinesNear.add(argumentLines.get(i));
				} else {
					argumentLinesFar.add(argumentLines.get(i));
				}
			}
			// Common.outputLines(argumentLinesNear,
			// "data/Joint_argumentLines_near" + mode + model + Util.part);
			// Common.outputLines(argumentLinesFar,
			// "data/Joint_argumentLines_far" + mode + model + Util.part);
		}
		Common.outputLines(argumentRoleFeatures, "data/Joint_argument_" + mode + model + Util.part);

		ArrayList<String> argumentRoleFeaturesFar = new ArrayList<String>();
		ArrayList<String> argumentRoleFeaturesNear = new ArrayList<String>();
		for (int i = 0; i < argumentRoleFeatures.size(); i++) {
			boolean sameIP = argumentFeature.sameClause.get(i);
			if (sameIP) {
				argumentRoleFeaturesNear.add(argumentRoleFeatures.get(i));
			} else {
				argumentRoleFeaturesFar.add(argumentRoleFeatures.get(i));
			}
		}
		System.out.println(argumentRoleFeaturesNear.size());
		System.out.println(argumentRoleFeaturesFar.size());
		Common.outputLines(argumentRoleFeaturesNear, "data/Joint_argument_near" + mode + model + Util.part);
		Common.outputLines(argumentRoleFeaturesFar, "data/Joint_argument_far" + mode + model + Util.part);

		Common.outputLines(relateEMs, "data/Joint_argumentRelateEM_" + mode + model + Util.part);

		System.out.println("Build argument detection feature " + mode + "...");
		System.out.println("=================");

		if (mode.equals("train")) {
			Common.outputHashMap(argumentFeature.featureSpace, "argumentJointFeaSpace_" + args[1] + Util.part);
		}
		
		System.err.println("ArgCount: " + argCount);
	}

	public static void loadSystemEntityMentions() {
		String mentionCRFFile = "/users/yzcchen/tool/CRF/CRF++-0.54/yy" + Util.part;
		mentionses = Util.getMentionsFromCRFFile(Common.getLines("ACE_Chinese_test" + Util.part),
				mentionCRFFile);
		String timeCRFFile = "/users/yzcchen/tool/CRF/CRF++-0.54/yy_time" + Util.part;
		timeExpressions = Util.getMentionsFromCRFFile(Common.getLines("ACE_Chinese_test" + Util.part),
				timeCRFFile);
		String valueCRFFile = "/users/yzcchen/tool/CRF/CRF++-0.54/yy_value" + Util.part;
		valueExpressions = Util.getMentionsFromCRFFile(Common.getLines("ACE_Chinese_test" + Util.part),
				valueCRFFile);

		String nerFile = "/users/yzcchen/tool/CRF/CRF++-0.54/ACE/Ner/" + Util.part + ".result";
		nerses = Util.getSemanticsFromCRFFile(Common.getLines("ACE_Chinese_test" + Util.part), nerFile);

		for (String key : timeExpressions.keySet()) {
			for (EntityMention time : timeExpressions.get(key)) {
				time.semClass = "time";
				time.subType = "time";
				time.type = "time";
			}
		}

		for (String key : valueExpressions.keySet()) {
			for (EntityMention value : valueExpressions.get(key)) {
				value.semClass = "value";
				value.subType = "value";
				value.type = "value";
			}
		}

		for (String key : mentionses.keySet()) {
			for (EntityMention mention : mentionses.get(key)) {
				if (Common.isPronoun(mention.head)) {
					mention.type = "PRO";
				} else {
					mention.type = "NOM";
				}
				String key2 = key.replace("/users/yzcchen/chen3/coling2012/LDC2006T06/data/", "/users/yzcchen/ACL12/data/ACE2005/")
						+ ".sgm";
//				System.out.println(key + "@@");
//				System.out.println(allSVMResult.keySet().iterator().next() + "##");
				for (EntityMention sem : allSVMResult.get(key2)) {
					if (sem.start == mention.start && sem.end == mention.end) {
						mention.semClass = sem.semClass;
						mention.subType = sem.subType.substring(2);
						break;
					}
				}
				for (EntityMention ne : nerses.get(key)) {
					if (ne.start == mention.start && ne.end == mention.end) {
						mention.type = "NAM";
						break;
					}
				}
			} 
		}
	}

	public static HashMap<String, ArrayList<EntityMention>> allSVMResult;
	static HashMap<String, ArrayList<EntityMention>> mentionses;
	static HashMap<String, ArrayList<EntityMention>> timeExpressions;
	static HashMap<String, ArrayList<EntityMention>> valueExpressions;

	static HashMap<String, ArrayList<EntityMention>> nerses;

	public void assignSystemSemanticType(EntityMention mention, ACEChiDoc document) {
		//TODO
		ArrayList<EntityMention> systemSemantics = allSVMResult.get(document.fileID);
		boolean find = false;
		for(EntityMention system : systemSemantics) {
			if(system.start==mention.headStart && system.end==mention.headEnd) {
				mention.semClass = system.semClass;
				mention.subType = system.subType;
				find = true;
				break;
			}
		}
		if(!find) {
			System.err.println("GEE");
			System.exit(1);
		}
	}
	
	public ArrayList<EntityMention> getArgumentCandidates(EventMention em, ACEChiDoc document, boolean train) {
		ArrayList<EntityMention> candidates = new ArrayList<EntityMention>();

		ArrayList<EntityMention> mentions = new ArrayList<EntityMention>();

		ArrayList<EntityMention> npMentions = null;
		ArrayList<EntityMention> timeMentions = null;
		ArrayList<EntityMention> valueMentions = null;

		if (!train) {
			npMentions = mentionses.get(document.fileID);
			timeMentions = timeExpressions.get(document.fileID);
			valueMentions = valueExpressions.get(document.fileID);
		} else {
			npMentions = document.goldEntityMentions;
			timeMentions = document.goldTimeMentions;
			valueMentions = document.goldValueMentions;
		}
			
//		if(!train) {
//			for(EntityMention mention : npMentions) {
//				this.assignSystemSemanticType(mention, document);
//			}
//		}

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
	
	static int argCount = 0;

	public ArrayList<String> buildTrainRoleFeatures(EventMention eventMention, ACEChiDoc document) {
		ArrayList<EventMentionArgument> arguments = eventMention.getEventMentionArguments();

		HashMap<String, EventMentionArgument> goldArgHash = new HashMap<String, EventMentionArgument>();

		for (EventMentionArgument arg : arguments) {
			goldArgHash.put(arg.toString(), arg);
		}

		ArrayList<EntityMention> entityMentions = this.getArgumentCandidates(eventMention, document, true);
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

	public ArrayList<String> buildTestFeatures(EventMention eventMention, ACEChiDoc document,
			ArrayList<String> argumentLines) {
		ArrayList<EntityMention> entityMentions = this.getArgumentCandidates(eventMention, document, false);
		ArrayList<String> features = new ArrayList<String>();

		argCount += entityMentions.size();
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

	public ArrayList<Boolean> sameClause = new ArrayList<Boolean>();

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

			if (!Util.roles.get(Integer.parseInt(label) - 1).equalsIgnoreCase("null")) {
				if (entityIP == eventIP) {
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
			while (!pr.dependTree.vertexMap.containsKey(entityID) && entityID != 0) {
				entityID--;
			}
			if (entityID != eventID) {
				MyTreeNode entityDepNode = pr.dependTree.vertexMapTree.get(entityID);
				MyTreeNode eventDepNode = pr.dependTree.vertexMapTree.get(eventID);
				if (entityDepNode != null && eventDepNode != null) {
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
		features.add(entityMention.semClass);
		features.add(entityMention.subType);
		
		String eventSubType = eventMention.subType;
		if(eventSubType.equalsIgnoreCase("null")) {
			eventSubType = pipelineResults.get(document.fileID).get(eventMention.toString()).subType;
		}
		
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
		
//		System.out.println(features);
//		Common.pause("");
		
		return this.convert(features, label);
	}

	public String convert(ArrayList<String> feaStrs, String label) {
		ArrayList<Feature> feas = new ArrayList<Feature>();
		StringBuilder sb = new StringBuilder();
		sb.append(label);
		for (int i = 0; i < feaStrs.size(); i++) {
			if(feaStrs.get(i)==null) {
				continue;
			}
			String feaStr = feaStrs.get(i).toLowerCase();
			String extendFeaStr = feaStr.trim() + "_" + i;
			int idx = getFeaIdx(extendFeaStr);
			if (idx == -1) {
//				System.err.println(extendFeaStr);
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
	static HashMap<String, HashMap<String, EventMention>> pipelineResults;
	public static void loadTest(String folder) {
		pipelineResults = Util.readResult("pipe_svm_3extend/result" + folder, "chi");
	}
	
}

