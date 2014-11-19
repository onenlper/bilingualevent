package event.trigger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import model.ACEChiDoc;
import model.EntityMention;
import model.EventMention;
import model.EventMentionArgument;
import model.ParseResult;
import model.SemanticRole;
import model.syntaxTree.MyTreeNode;
import util.ChineseUtil;
import util.Common;
import util.Util;
import event.preProcess.ChineseTriggerIndent;

public class JointTriggerIndentMaxEnt {

	public static HashMap<String, String[]> semanticDic = loadSemanticDic();

	public static HashSet<String> proBankPredicts = Common.readFile2Set("predicts");

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

	public String buildFeature(EventMention em, ACEChiDoc document, String label) {
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

		ArrayList<String> features = new ArrayList<String>();
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
		EventMention pipeMentionTest = null;
		if (pipelineResults.containsKey(document.fileID)
				&& pipelineResults.get(document.fileID).containsKey(em.toString())) {
			pipeMentionTest = pipelineResults.get(document.fileID).get(em.toString());
			eventType = pipeMentionTest.subType;
			double confidence = pipeMentionTest.confidence;
			for (EventMentionArgument argument : pipeMentionTest.eventMentionArguments) {
				EntityMention argumentMention = this.getEntityMention(argument, document);
				arguments.add(argumentMention);
			}
		}

		ArrayList<EventMention> pipeEventMentions = new ArrayList<EventMention>();
		if (mode.equals("test")) {
			// if (true) {
			HashMap<String, EventMention> eventMentionMap = pipelineResults.get(document.fileID);
			if (eventMentionMap != null) {
				for (String key : eventMentionMap.keySet()) {
					if (eventMentionMap.get(key).confidence > 0.00) {
						pipeEventMentions.add(eventMentionMap.get(key));
					}
				}
			}
		} else {
			pipeEventMentions.addAll(document.goldEventMentions);
		}

		EventMention jointMentionTest = null;
		if (jointResults.containsKey(document.fileID) && jointResults.get(document.fileID).containsKey(em.toString())) {
			jointMentionTest = jointResults.get(document.fileID).get(em.toString());
		}

		ArrayList<EventMention> jointEventMentions = new ArrayList<EventMention>();
		if (mode.equals("test")) {
			HashMap<String, EventMention> eventMentionMap = jointResults.get(document.fileID);
			if (eventMentionMap != null) {
				for (String key : eventMentionMap.keySet()) {
					if (eventMentionMap.get(key).confidence > 0.00) {
						jointEventMentions.add(eventMentionMap.get(key));
					}
				}
			}
		} else {
			jointEventMentions.addAll(document.goldEventMentions);
		}

		ArrayList<EventMention> eventMentionsFromTest = new ArrayList<EventMention>();
		HashMap<String, EventMention> eventMentionMap = jointResults.get(document.fileID);
		if (eventMentionMap != null) {
			for (String key : eventMentionMap.keySet()) {
				eventMentionsFromTest.add(eventMentionMap.get(key));
			}
		}

		if (pipeEventMentions == null) {
			pipeEventMentions = new ArrayList<EventMention>();
		}

		addCharFeature(features, trigger);
		addSemanticRoleFeature(features, document, em);

		String documentType = "";
		documentType = this.getMainLabel(pipeEventMentions);

		return convert(features, label, documentType, jointEventMentions, eventType, arguments, document,
				pipeMentionTest, jointMentionTest, jointEventMentions, eventMentionsFromTest, em);
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

	public EntityMention getEntityMention(EventMentionArgument argument, ACEChiDoc document) {
		ArrayList<EntityMention> mentions = new ArrayList<EntityMention>();
		mentions.addAll(document.goldEntityMentions);
		mentions.addAll(document.goldTimeMentions);
		mentions.addAll(document.goldValueMentions);
		for (EntityMention mention : mentions) {
			if (mention.start == argument.getStart() && mention.end == argument.getEnd()) {
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

	public static String getSemantic(String str) {
		String semantic = "null";
		if (semanticDic.containsKey(str)) {
			semantic = semanticDic.get(str)[0];
		}
		return semantic;
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

	static HashMap<String, Double> triggerProb;

	public String convert(ArrayList<String> feaStrs, String label, String documentType,
			ArrayList<EventMention> documentEventMentions, String eventType, ArrayList<EntityMention> arguments,
			ACEChiDoc document, EventMention pipeMentionTest, EventMention jointMentionTest,
			ArrayList<EventMention> jointEventMentions, ArrayList<EventMention> eventMentionsFromTest,
			EventMention origEM) {

		if(triggerProb==null) {
			triggerProb = Common.readFile2Map5("pipeline/triggerProbability" + Util.part);
		}
		
		if (pipeMentionTest != null) {
			// type distribution
			this.getYYFeaValueList(documentEventMentions, eventType, pipeMentionTest, feaStrs);
			double prob = 0;
			if (triggerProb.containsKey(pipeMentionTest.getAnchor())) {
				prob = triggerProb.get(pipeMentionTest.getAnchor());
			} else {
				if (triggerProb.containsKey(pipeMentionTest.inferFrom)) {
					prob = triggerProb.get(pipeMentionTest.inferFrom);
				}
			}
			feaStrs.add(Integer.toString((int) prob * 10));
//			System.err.println("not null");
		}
		
		
		
		StringBuilder sb = new StringBuilder();
		sb.append(label).append(" ");
		for (int i = 0; i < feaStrs.size(); i++) {
			String feaStr = feaStrs.get(i);
			String extendFeaStr = feaStr.trim() + "_" + i;
			extendFeaStr.replaceAll("\\s+", "").replace("\n", "");
			sb.append(extendFeaStr).append(" ");
		}
		
		return sb.toString().trim();
	}

	public void getYYFeaValueList(ArrayList<EventMention> eventMentions, String eventType, EventMention mentionTest,
			ArrayList<String> feas) {
		HashMap<String, Integer> typeCounts = new HashMap<String, Integer>();
		double[] yyCounts = new double[33];
		for (EventMention eventMention : eventMentions) {
			String type = eventMention.subType;
			Common.addKey(typeCounts, type);
		}
		for (int i = 0; i < 33; i++) {
			if (typeCounts.containsKey(Util.subTypes.get(i))) {
				yyCounts[i] = typeCounts.get(Util.subTypes.get(i));
			} else {
				yyCounts[i] = 0.0;
			}
		}

		double[] yyValues = new double[33];
		for (int i = 0; i < 33; i++) {
			double poss = yyCounts[i] / (double) eventMentions.size();
			yyValues[i] = 0;
			if (Util.subTypes.get(i).equals(eventType)) {
				yyValues[i] = poss;
			}
		}

		for (int i = 0; i < yyValues.length; i++) {
			// type distribution
			feas.add(Integer.toString((int) (yyValues[i] * 10)));
		}

	}

	public static String mode = "train";

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

	public static boolean discourse = true;

	public static void main(String args[]) {
		if (args.length < 3) {
			System.out.println("java ~ [train|test|development] [svm|maxent] [folder]");
		}
		if (args.length == 4 && args[3].equals("discourse")) {
			discourse = true;
		}
		mode = args[0];
		System.out.println(mode);
		Util.part = args[2];
		loadProps();
		if (mode.equalsIgnoreCase("train")) {
			loadTrainForTest(args[2]);
		} else {
			loadTest(args[2]);
		}
		ArrayList<String> files = Common.getLines("ACE_Chinese_" + args[0] + Util.part);

		// ArrayList<String> triggerTypeFeatures = new ArrayList<String>();
		ArrayList<String> triggerSubTypeFeatures = new ArrayList<String>();

		JointTriggerIndentMaxEnt triggerFeature = new JointTriggerIndentMaxEnt();
		ArrayList<String> systemEMses = new ArrayList<String>();
		for (String file : files) {
			// System.out.println(file);
			HashSet<EventMention> goldEMs = new HashSet<EventMention>();
			ACEChiDoc document = new ACEChiDoc(file);
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
				sb.append(file).append(" ").append(em.getAnchorStart()).append(" ").append(em.getAnchorEnd()).append(
						" ").append(em.getType()).append(" ").append(em.inferFrom);
				systemEMses.add(sb.toString());

				EventMention mentionTest = null;
				if (pipelineResults.containsKey(document.fileID)
						&& pipelineResults.get(document.fileID).containsKey(em.toString())) {
					mentionTest = pipelineResults.get(document.fileID).get(em.toString());
				}

				// if(mentionTest==null) {
				// continue;
				// }

				// triggerTypeFeatures.add(triggerFeature.buildFeature(em,
				// document, Integer.toString(types
				// .indexOf(em.type) + 1)));
				triggerSubTypeFeatures.add(triggerFeature.buildFeature(em, document, Integer.toString(Util.subTypes
						.indexOf(em.subType) + 1)));
			}
		}
		Common.outputLines(systemEMses, "data/Joint_triggers_" + args[0] + "_system" + Util.part);
		// Common.outputLines(triggerTypeFeatures, "data/Joint_triggersFeature_"
		// + args[0] + Util.part);

		Common.outputLines(triggerSubTypeFeatures, "data/Joint_triggersFeature_" + args[0] + Util.part);
		System.out.println("Build features for " + args[0] + Util.part);
		System.out.println("==========");
	}

	static HashMap<String, HashMap<String, EventMention>> pipelineResults;
	static HashMap<String, HashMap<String, EventMention>> jointResults;

	public static ArrayList<EventMention> getTestEMs(ACEChiDoc document) {
		HashSet<EventMention> testEMs = new HashSet<EventMention>();
		HashSet<EventMention> candidateEMSet = new HashSet<EventMention>();
		TriggerIndent triggerIndent = new ChineseTriggerIndent();
		ArrayList<EventMention> candidates = triggerIndent.extractTrigger(document.fileID);
		candidateEMSet.addAll(candidates);
		// for (ParseResult pr : document.parseReults) {
		// for (int i = 1; i < pr.words.size(); i++) {
		// int start = pr.positions.get(i)[0];
		// int end = pr.positions.get(i)[1];
		// String word = pr.words.get(i).replace("\n", "").replaceAll("\\s+",
		// "");
		// EventMention em = new EventMention();
		// em.setAnchorStart(start);
		// em.setAnchorEnd(end);
		// em.setAnchor(word);
		// em.setType("null");
		// if(!candidateEMSet.contains(em)) {
		// testEMs.add(em);
		// }
		// }
		// }
		testEMs.addAll(candidates);
		ArrayList<EventMention> ems = new ArrayList<EventMention>();
		ems.addAll(testEMs);
		return ems;
	}

	public static ArrayList<EventMention> getTrainEMs(ACEChiDoc document) {
		HashSet<EventMention> trainEMs = new HashSet<EventMention>();
		HashSet<EventMention> goldEMSet = new HashSet<EventMention>();
		goldEMSet.addAll(document.goldEventMentions);
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

	public static void loadTest(String folder) {
		pipelineResults = Util.readResult("pipe_svm_3extend/result" + folder, "chi");
		jointResults = Util.readResult("joint_svm_3extend/result" + folder, "chi");
	}

	public static void loadTrainForTest(String folder) {
		pipelineResults = new HashMap<String, HashMap<String, EventMention>>();
		for (int i = 1; i <= 10; i++) {
			if (i == Integer.valueOf(folder)) {
				continue;
			}
			HashMap<String, HashMap<String, EventMention>> part = Util.readResult("pipe_svm_3extend/result"
					+ Integer.toString(i), "chi");
			for (String key : part.keySet()) {
				pipelineResults.put(key, part.get(key));
			}
		}

		jointResults = new HashMap<String, HashMap<String, EventMention>>();
		for (int i = 1; i <= 10; i++) {
			if (i == Integer.valueOf(folder)) {
				continue;
			}
			HashMap<String, HashMap<String, EventMention>> part = Util.readResult("joint_svm_3extend/result"
					+ Integer.toString(i), "chi");
			for (String key : part.keySet()) {
				jointResults.put(key, part.get(key));
			}
		}

	}
}
