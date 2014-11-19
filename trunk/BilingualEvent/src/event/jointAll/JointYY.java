package event.jointAll;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;

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
import event.preProcess.ChineseTriggerIndent;
import event.trigger.TriggerIndent;

public class JointYY {
	
	public static HashMap<String, String[]> loadSemanticDic() {
		HashMap<String, String[]> semanticDic = new HashMap<String, String[]>();
		ArrayList<String> lines = Common.getLines("dict/TongyiciCiLin_8.txt");
		for (String line : lines) {
			String tokens[] = line.split("\\s+");
			String word = tokens[0];
			String semantic[] = new String[tokens.length - 2];
			for (int i = 0; i < semantic.length; i++) {
				semantic[i] = tokens[2 + i];
			}
			semanticDic.put(word, semantic);
		}
		return semanticDic;
	}
	
	public static HashMap<String, String[]> semanticDic = loadSemanticDic();

	public static HashSet<String> proBankPredicts = Common.readFile2Set("predicts");

	public static String mode;

	public HashMap<String, Integer> featureSpace;

	int near = 0;
	int far = 0;
	
	public static void main(String args[]) {
		if (args.length != 3) {
			System.out.println("java ~ [train|test|development] [svm|maxent] [folder]");
		}
		mode = args[0];
		String model = args[1];
		Util.part = args[2];

		JointYY argumentFeature = new JointYY();
		if (mode.equals("train")) {
			argumentFeature.featureSpace = new HashMap<String, Integer>();
//			loadTrainForTest(Util.part);
		} else {
			argumentFeature.featureSpace = Common.readFile2Map("FullJointFeaSpace_" + args[1] + Util.part);
//			loadTest(Util.part);
		}

		ArrayList<String> files = Common.getLines("ACE_Chinese_" + args[0] + Util.part);
		ArrayList<String> jointFeatures = new ArrayList<String>();

		ArrayList<String> argumentLines = new ArrayList<String>();
		ArrayList<EventMention> candidateTriggers;

		for (String file : files) {
//			System.out.println(file);
			ACEChiDoc document = new ACEChiDoc(file);
			if (args[0].equals("train")) {
				candidateTriggers = getTrainEMs(document);
			} else {
				candidateTriggers = getTestEMs(document);
			}
			if (args[0].equalsIgnoreCase("train")) {
				for (EventMention eventMention : candidateTriggers) {
					jointFeatures.addAll(argumentFeature.buildTrainRoleFeatures(eventMention, document));
				}
			} else {
				for (EventMention eventMention : candidateTriggers) {
					jointFeatures.addAll(argumentFeature
							.buildTestFeatures(eventMention, document, argumentLines));
				}
			}
		}
		if (!args[0].equalsIgnoreCase("train")) {
			Common.outputLines(argumentLines, "data/Joint_Lines_" + mode + model + Util.part);
		}
		Common.outputLines(jointFeatures, "data/Joint_features_" + mode + model + Util.part);
		
		System.out.println("Build argument detection feature " + mode + "...");
		System.out.println("=================");

		if (mode.equals("train")) {
			Common.outputHashMap(argumentFeature.featureSpace, "FullJointFeaSpace_" + args[1] + Util.part);
		}
	}

	public static ArrayList<EventMention> getTestEMs(ACEChiDoc document) {
		HashSet<EventMention> testEMs = new HashSet<EventMention>();
		HashSet<EventMention> candidateEMSet = new HashSet<EventMention>();
		TriggerIndent triggerIndent = new ChineseTriggerIndent();
		ArrayList<EventMention> candidates = triggerIndent.extractTrigger(document.fileID);
		candidateEMSet.addAll(candidates);
		testEMs.addAll(candidates);
		ArrayList<EventMention> ems = new ArrayList<EventMention>();
		ems.addAll(testEMs);
		return ems;
	}

	public static ArrayList<EventMention> getTrainEMs(ACEChiDoc document) {
		HashSet<EventMention> trainEMSet = new HashSet<EventMention>();
		trainEMSet.addAll(document.goldEventMentions);

//		TriggerIndent triggerIndent = new ChineseTriggerIndent();
//		ArrayList<EventMention> candidates = triggerIndent.extractTrigger(document.fileID);
//		for(EventMention candidate : candidates) {
//			if(!trainEMSet.contains(candidate)) {
//				trainEMSet.add(candidate);
//			}
//		}
		for (ParseResult pr : document.parseReults) {
			for (int i = 1; i < pr.words.size(); i++) {
				int start = pr.positions.get(i)[0];
				int end = pr.positions.get(i)[1];
				String word = pr.words.get(i).replace("\n", "").replaceAll("\\s+", "");
				EventMention em = new EventMention();
				em.setAnchorStart(start);
				em.setAnchorEnd(end);
				em.setAnchor(word);
				em.setType("null");
				em.setSubType("null");
				if (!trainEMSet.contains(em)) {
					trainEMSet.add(em);
				}
			}
		}
		
		ArrayList<EventMention> ems = new ArrayList<EventMention>();
		ems.addAll(trainEMSet);
		return ems;
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
		if(candidates.size()==0) {
			
		}
		return candidates;
	}
	
	public static HashMap<String, Integer> jointLabel = Common.readFile2Map("jointLabel");
	
	public ArrayList<String> buildTrainRoleFeatures(EventMention eventMention, ACEChiDoc document) {
		ArrayList<EventMentionArgument> arguments = eventMention.getEventMentionArguments();
		HashMap<String, EventMentionArgument> goldArgHash = new HashMap<String, EventMentionArgument>();

		for (EventMentionArgument arg : arguments) {
			goldArgHash.put(arg.toString(), arg);
		}

		ArrayList<EntityMention> entityMentions = this.getArgumentCandidates(eventMention, document);
		ArrayList<String> features = new ArrayList<String>();
		for (EntityMention mention : entityMentions) {
			String start = Integer.toString(mention.getStart());
			String end = Integer.toString(mention.getEnd());
			String role = "null";
			EventMentionArgument arg = goldArgHash.get(start + "," + end);
			if (arg != null) {
				role = arg.getRole();
			}
			ArrayList<String> argFeature = this.buildArgumentFeature(mention, eventMention, document, Integer.toString(Util.roles
					.indexOf(role) + 1));
			
			String triggerType = "null";
			if(document.goldEventMentionMap.containsKey(eventMention.toString())) {
				triggerType = document.goldEventMentionMap.get(eventMention.toString()).subType;
			}
//			System.out.println(triggerType+"_"+role);
			Random random = new Random();
			if(triggerType.equals("null")) {
				role = "Na" + Integer.toString(Math.abs(random.nextInt()%10));
			}
			
			String label = Integer.toString(jointLabel.get(triggerType+"_"+role));
			
			if(!role.equalsIgnoreCase("null") && !triggerType.equalsIgnoreCase("null")) {
//				System.out.println(triggerType+"_"+role + "#" + eventMention.getAnchor()+"_"+arg.getExtent());
			}
			String feature = this.buildFeature(eventMention, document, label, argFeature);
			
			features.add(feature);
		}
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
		ArrayList<EntityMention> entityMentions = this.getArgumentCandidates(eventMention, document);
		ArrayList<String> features = new ArrayList<String>();

		for (EntityMention entityMention : entityMentions) {
			ArrayList<String> argFeatures = this.buildArgumentFeature(entityMention, eventMention, document, Integer.toString(Util.roles
					.indexOf("null") + 1));
			
			String feature = this.buildFeature(eventMention, document, "1", argFeatures);
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

	public ArrayList<EntityMention> getEntities(ACEChiDoc document, int start, int end) {
		ArrayList<EntityMention> mentions = new ArrayList<EntityMention>();
		ArrayList<EntityMention> goldMentions = document.goldEntityMentions;
		for (EntityMention mention : goldMentions) {
			if (mention.start >= start && mention.end <= end) {
				mentions.add(mention);
			}
		}
		return mentions;
	}
	
	public ArrayList<Boolean> sameClause = new ArrayList<Boolean>();
	
	public MyTreeNode getNPTreeNode(EntityMention np, ArrayList<ParseResult> prs, int npSenIdx, int npWordStartIdx,
			int npWordEndIdx) {
		MyTreeNode NP = null;
		try {
			ArrayList<MyTreeNode> leaves = prs.get(npSenIdx).tree.leaves;
			MyTreeNode leftNp = leaves.get(npWordStartIdx);
			MyTreeNode rightNp = leaves.get(npWordEndIdx);
			// System.out.println(npWordEndIdx +np.getContent());
			ArrayList<MyTreeNode> leftAncestors = leftNp.getAncestors();
			ArrayList<MyTreeNode> rightAncestors = rightNp.getAncestors();
			for (int i = 0; i < leftAncestors.size() && i < rightAncestors.size(); i++) {
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
	
	public String buildFeature(EventMention em, ACEChiDoc document, String label, ArrayList<String> features) {
		String trigger = em.getAnchor();
		int position[] = ChineseUtil.findParseFilePosition(em.getAnchorStart(), em.getAnchorEnd(), document);
		ParseResult pr = document.parseReults.get(position[0]);
		int index = position[0];
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
		String semantic = "null";
		if (semanticDic.containsKey(trigger)) {
			semantic = semanticDic.get(trigger)[0];
		}

		features.add(trigger);
		features.add(pos);
		features.add(previousW + "_" + word);
		features.add(word + "_" + nextW);
		features.add(previousPOS + "_" + pos);
		features.add(pos + "_" + nextPOS);
		features.add(Integer.toString(leaf.getAncestors().size()));
		features.add(path);
		features.add(semantic);
		features.add(path2);

		features.add(leaf.parent.parent.value);
		features.add(leaf.parent.parent.productionRule());

		if (proBankPredicts.contains(trigger)) {
			features.add("+1");
		} else {
			features.add("-1");
		}
		ArrayList<EntityMention> leftNearbyMentions = this.getEntities(document, positions.get(1)[0], em
				.getAnchorStart());

		int shortest = Integer.MAX_VALUE;
		String type1 = "";
		String extent = "";
		for (EntityMention mention : leftNearbyMentions) {
			MyTreeNode node = this.getNPTreeNode(mention, document.parseReults, position[0], position[1], position[3]);
			int distance = this.getSyntaxTreeDistance(leaf, node);
			if (distance < shortest) {
				shortest = distance;
				type1 = mention.entity.getType();
				extent = mention.extent.replace(" ", "").replace("\n", "");
			}
		}
		features.add(type1);

		shortest = Integer.MAX_VALUE;
		String type2 = "";
		for (EntityMention mention : leftNearbyMentions) {
			int distance = 0;
			if (mention.end < em.getAnchorStart()) {
				distance = em.getAnchorStart() - mention.end;
			} else {
				distance = mention.start - em.getAnchorEnd();
			}
			if (distance < shortest) {
				shortest = distance;
				type2 = mention.entity.getType();
				extent = mention.extent.replace(" ", "").replace("\n", "");
			}
		}
		features.add(type2);

		ArrayList<EntityMention> rightNearbyMentions = this.getEntities(document, em.getAnchorEnd() + 1, positions
				.get(positions.size() - 1)[1]);
		shortest = Integer.MAX_VALUE;
		type1 = "null";
		for (EntityMention mention : rightNearbyMentions) {
			MyTreeNode node = this.getNPTreeNode(mention, document.parseReults, position[0], position[1], position[3]);
			int distance = this.getSyntaxTreeDistance(leaf, node);
			if (distance < shortest) {
				shortest = distance;
				type1 = mention.entity.getType();
				extent = mention.extent.replace(" ", "").replace("\n", "");
			}
		}
		features.add(type1);

		shortest = Integer.MAX_VALUE;
		type2 = "null";
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
				extent = mention.extent.replace(" ", "").replace("\n", "");
			}
		}
		features.add(type2);

		String eventType = "null";
		ArrayList<EntityMention> arguments = new ArrayList<EntityMention>();
		EventMention mentionTest = null;
//		if (pipelineResults.containsKey(document.fileID)
//				&& pipelineResults.get(document.fileID).containsKey(em.toString())) {
//			mentionTest = pipelineResults.get(document.fileID).get(em.toString());
//			eventType = mentionTest.type;
//			double confidence = mentionTest.confidence;
//			for (EventMentionArgument argument : mentionTest.eventMentionArguments) {
//				EntityMention argumentMention = this.getEntityMention(argument.getStart(), argument.getEnd(), document.allMentions);
//				arguments.add(argumentMention);
//			}
//		}
//		
		ArrayList<EventMention> eventMentions = new ArrayList<EventMention>();
//		eventMentions = document.goldEventMentions;
//		if (mode.equals("test")) {
//			HashMap<String, EventMention> eventMentionMap = pipelineResults.get(document.fileID);
//			if (eventMentionMap != null) {
//				for (String key : eventMentionMap.keySet()) {
//					if (eventMentionMap.get(key).confidence > 0.00) {
//						eventMentions.add(eventMentionMap.get(key));
//					}
//				}
//			}
//		}
//		HashSet<Integer> corefIDs = new HashSet<Integer>();
//		for (EventMention temp : eventMentions) {
//			if (mentionTest != null && !temp.equals(mentionTest)) {
//				for (EventMentionArgument argument : temp.eventMentionArguments) {
//					if(argument.getEntityMention().getType().equalsIgnoreCase("time") 
//							|| argument.getEntityMention().getType().equalsIgnoreCase("value")) {
//						continue;
//					}
//					corefIDs.add(argument.getEntityMention().entity.entityIdx);
//				}
//			}
//		}
//
//		boolean coref = false;
//		if (mentionTest != null) {
//			for (EventMentionArgument argument : mentionTest.eventMentionArguments) {
//				if (corefIDs.contains(argument.getEntityMention().entity.entityIdx)) {
//					coref = true;
//					break;
//				}
//			}
//		}
//
//		if (eventMentions == null) {
//			eventMentions = new ArrayList<EventMention>();
//		}

//		addCharFeature(features, trigger);
//		addSemanticRoleFeature(features, document, em);

		String documentType = "";

		return convert(features, label, documentType, eventMentions, eventType, arguments, document);
	}
	
	public void addCharFeature(ArrayList<String> features, String trigger) {
		if (trigger.length() == 1) {
			features.add(trigger);
			features.add(getSemantic(trigger));
			features.add(trigger);
			features.add(getSemantic(trigger));
		} else if (trigger.length() == 2) {
			features.add(trigger.substring(0, 1));
			features.add(getSemantic(trigger.substring(0, 1)));
			features.add(trigger.substring(1, 2));
			features.add(getSemantic(trigger.substring(1, 2)));
		} else if (trigger.length() == 3) {
			features.add(trigger.substring(0, 2));
			features.add(getSemantic(trigger.substring(0, 2)));
			features.add(trigger.substring(2));
			features.add(getSemantic(trigger.substring(2)));
		}
	}

	public void addSemanticRoleFeature(ArrayList<String> features, ACEChiDoc document, EventMention em) {
		HashMap<EventMention, SemanticRole> predicates = document.semanticRoles;
		SemanticRole role = predicates.get(em);
		if (role != null) {
			features.add("0");
		} else {
			features.add("1");
		}

		if (role != null) {
			if (role.arg0.size() != 0) {
				features.add(role.arg0.get(0).entity.getType());
				features.add(role.arg0.get(0).entity.getSubType());
			} else {
				features.add("null");
				features.add("null");
			}
			if (role.arg1.size() != 0) {
				features.add(role.arg1.get(0).entity.getType());
				features.add(role.arg1.get(0).entity.getSubType());
			} else {
				features.add("null");
				features.add("null");
			}
		} else {
			features.add("null");
			features.add("null");
			features.add("null");
			features.add("null");
		}
	}
	
	static HashMap<String, HashMap<String, EventMention>> pipelineResults;
	
	public static String getSemantic(String str) {
		String semantic = "null";
		if (semanticDic.containsKey(str)) {
			semantic = semanticDic.get(str)[0];
		}
		return semantic;
	}
	
	public ArrayList<String> buildArgumentFeature(EntityMention entityMention, EventMention eventMention, ACEChiDoc document,
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
			if(entityIP==eventIP) {
				sameClause.add(true);
			} else {
				sameClause.add(false);
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
		
//		features.add(eventMention.getSubType());
//		features.add(eventMention.getType());
		features.add(entityMention.head);
		features.add(entityMention.getType());
		features.add(entityMention.entity.type);
		features.add(entityMention.entity.subType);
		features.add(trigger + "_" + entityMention.head);
		features.add(trigger + "_" + entityMention.entity.type);
		features.add(trigger + "_" + entityMention.entity.subType);

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
//		Util.addZeroPronounFeature(features, document, entityMention, eventMention, label, sw);
		return features;
	}
	static boolean sw = false;
	public String convert(ArrayList<String> feaStrs, String label, String documentType,
			ArrayList<EventMention> eventMentions, String eventType, ArrayList<EntityMention> arguments,
			ACEChiDoc document) {
		ArrayList<Feature> feas = new ArrayList<Feature>();
		StringBuilder sb = new StringBuilder();
		sb.append(label);
		for (int i = 0; i < feaStrs.size(); i++) {
			String feaStr = feaStrs.get(i);
			if(feaStr.equals("\n")) {
				continue;
			}
			String extendFeaStr = feaStr.trim() + "_" + i;
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
//		if (!documentType.equals("null") && !eventType.equals("null")) {
//			double yyFea = this.getYYFeaValue(documentType, eventType);
			// sb.append(" ").append(yyFeaIdx-1).append(":").append(yyFea);
			// System.out.println(documentType + "@" + eventType + ":" + yyFea +
			// "#" + mode);
//		}
//		double yyList[] = this.getYYFeaValueList(eventMentions, eventType);
//		for (int i = 0; i < yyList.length; i++) {
//			sb.append(" ").append(i + yyFeaIdx).append(":").append(yyList[i]);
//		}

		return sb.toString().trim();
	}
	public static HashMap<String, HashMap<String, Double>> probs = loadProps();

	public static HashMap<String, HashMap<String, Double>> loadProps() {
		HashMap<String, HashMap<String, Double>> probLocal = new HashMap<String, HashMap<String, Double>>();
		ArrayList<String> lines = Common.getLines("probs");
		for (String line : lines) {
			String tokens[] = line.split("\\s+");
			HashMap<String, Double> prob = probLocal.get(tokens[0]);
			if (prob == null) {
				prob = new HashMap<String, Double>();
				probLocal.put(tokens[0], prob);
			}
			prob.put(tokens[1], Double.parseDouble(tokens[2]));
		}
		return probLocal;
	}
	public double getYYFeaValue(String documentType, String eventType) {
		if (eventType.equalsIgnoreCase(documentType)) {
			return 1;
		} else {
			if (probs.containsKey(documentType)) {
				if (probs.get(documentType).containsKey(eventType)) {
					return probs.get(documentType).get(eventType);
				} else {
					return 0;
				}
			} else {
				return 0;
			}
		}
	}

	public static int yyFeaIdx = 6000000;

	public double[] getYYFeaValueList(ArrayList<EventMention> eventMentions, String eventType) {
		HashMap<String, Integer> typeCounts = new HashMap<String, Integer>();
		double[] yyCounts = new double[8];
		for (EventMention eventMention : eventMentions) {
			String type = eventMention.type;
			Common.addKey(typeCounts, type);
		}
		for (int i = 0; i < 8; i++) {
			if (typeCounts.containsKey(Util.types.get(i))) {
				yyCounts[i] = typeCounts.get(Util.types.get(i));
			} else {
				yyCounts[i] = 0.0;
			}
		}

		double[] yyValues = new double[8];
		for (int i = 0; i < 8; i++) {
			double poss = yyCounts[i] / (double) eventMentions.size();
			yyValues[i] = 0;
			if (Util.types.get(i).equals(eventType)) {
				yyValues[i] = poss;
			}
			// else {
			// if(probs.containsKey(types.get(i))) {
			// if(probs.get(types.get(i)).containsKey(eventType)) {
			// yyValues[i] = poss * probs.get(types.get(i)).get(eventType);
			// }
			// }
			// }
		}

		// for(int i=0;i<8;i++) {
		// if(types.get(i).equals(eventType)) {
		// yyValues[i+8] = yyCounts[i];
		// }
		// }
		return yyValues;
	}

	static HashMap<String, HashMap<String, EventMention>> jointResults;
	
	public static void loadTest(String folder) {
		pipelineResults = Util.readResult("pipe_svm/result" + folder, "chi");
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
		pipelineResults = new HashMap<String, HashMap<String, EventMention>>();
		for (int i = 1; i <= 10; i++) {
			if (i == Integer.valueOf(folder)) {
				continue;
			}
			HashMap<String, HashMap<String, EventMention>> part = Util.readResult("pipe_svm/result"
					+ Integer.toString(i), "chi");
			for (String key : part.keySet()) {
				pipelineResults.put(key, part.get(key));
			}
		}
	}
}
