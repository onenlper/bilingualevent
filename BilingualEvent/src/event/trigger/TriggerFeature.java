package event.trigger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

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

public class TriggerFeature {

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

	public static String getSemantic(String str) {
		String semantic = "null";
		if (semanticDic.containsKey(str)) {
			semantic = semanticDic.get(str)[0];
		}
		return semantic;
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
			}
		}
		features.add(type2);

		addCharFeature(features, trigger);
		addSemanticRoleFeature(features, document, em);

		ArrayList<EventMention> eventMentionsFromTest = new ArrayList<EventMention>();
		HashMap<String, EventMention> eventMentionMap = pipelineResults.get(document.fileID);
		if (eventMentionMap != null) {
			for (String key : eventMentionMap.keySet()) {
				eventMentionsFromTest.add(eventMentionMap.get(key));
			}
		}

		EventMention mentionTest = null;
		if (pipelineResults.containsKey(document.fileID)
				&& pipelineResults.get(document.fileID).containsKey(em.toString())) {
			mentionTest = pipelineResults.get(document.fileID).get(em.toString());
		}

		if (eventMentionsFromTest == null) {
			eventMentionsFromTest = new ArrayList<EventMention>();
		}

		ArrayList<EventMention> pipeEventMentions = new ArrayList<EventMention>();
		if (mode.equals("test")) {
			// if (true) {
			eventMentionMap = pipelineResults.get(document.fileID);
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
		
		return convert(features, label, pipeEventMentions, mentionTest, eventMentionsFromTest, em);
	}

	public static HashMap<String, Double> triggerProb;
	
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

	public HashMap<String, Integer> featureSpace;

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

	public String convert(ArrayList<String> feaStrs, String label, ArrayList<EventMention> documentEventMentions, 
			EventMention mentionTest, ArrayList<EventMention> eventMentionsFromTest, EventMention origEM) {
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

		HashSet<Integer> corefIDs = new HashSet<Integer>();
		for (EventMention temp : documentEventMentions) {
			if (mentionTest == null || !temp.equals(mentionTest)) {
				for (EventMentionArgument argument : temp.eventMentionArguments) {
					if (argument.getEntityMention().getType().equalsIgnoreCase("time")
							|| argument.getEntityMention().getType().equalsIgnoreCase("value")
							|| argument.role.equals("null")) {
						continue;
					}
					corefIDs.add(argument.getEntityMention().entity.entityIdx);
				}
			}
		}

		if (mentionTest != null) {
			for (EventMentionArgument argument : mentionTest.eventMentionArguments) {
				if (corefIDs.contains(argument.getEntityMention().entity.entityIdx)) {
					String role = argument.getRole();
					if(!role.equals("null")) {
						int index = Util.roles.indexOf(role) + yyFeaIdx3;
						// coreference
						feas.add(new Feature(index, 1));
					}
				}
			}
		}
		
		HashSet<Feature> feaSets = new HashSet<Feature>();
		feaSets.addAll(feas);
		feas.clear();
		feas.addAll(feaSets);
		
		Collections.sort(feas);
		for (Feature fea : feas) {
			sb.append(" ").append(fea.idx).append(":").append(fea.value);
		}
		
		String typeConsistentFea = "";
		if (mentionTest != null) {
			// type distribution
			 typeConsistentFea = this.getYYFeaValueList(documentEventMentions, mentionTest.getSubType(), mentionTest);
		}
		sb.append(" ").append(typeConsistentFea);
		
		String discourseFea = "";
		if (mentionTest != null) {
			discourseFea = this.buildDiscourseFea(mentionTest, eventMentionsFromTest);
		}
		
		sb.append(" ").append(discourseFea).append(" ");
		
		if (triggerProb == null) {
			triggerProb = Common.readFile2Map5("pipeline/triggerProbability" + Util.part);
		}

		double prob = 0;

		if (mentionTest != null) {
			if (triggerProb.containsKey(mentionTest.getAnchor())) {
				prob = triggerProb.get(mentionTest.getAnchor());
			} else {
				if (triggerProb.containsKey(mentionTest.inferFrom)) {
					prob = triggerProb.get(mentionTest.inferFrom);
				}
			}
			sb.append(" 701000:").append(prob);
		}
		return sb.toString();
	}
	
	public String getYYFeaValueList(ArrayList<EventMention> eventMentions, String eventType, EventMention mentionTest) {
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
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < yyValues.length; i++) {
			// type distribution
			sb.append(" ").append(i + yyFeaIdx).append(":").append(yyValues[i]);
		}

		ArrayList<Feature> feas = new ArrayList<Feature>();
		for (Feature fea : feas) {
			sb.append(" ").append(fea.idx).append(":").append(fea.value);
		}
		return sb.toString();
	}

	static int discourseFeaIdx = 700000;
	
	public static int yyFeaIdx3 = 595000;

	public static int yyFeaIdx2 = 590000;

	public static int yyFeaIdx = 600000;

	public String buildDiscourseFea(EventMention em, Collection<EventMention> eventMentions) {
		ArrayList<Feature> feas = new ArrayList<Feature>();
		HashMap<String, Double> consisProb = Common.readFile2Map5("pipeline/discourse_consistency" + Util.part);
		double prob = 0;
		if (consisProb.containsKey(em.getAnchor())) {
			prob = consisProb.get(em.getAnchor());
		} else {
			if (consisProb.containsKey(em.inferFrom)) {
				prob = consisProb.get(em.inferFrom);
			}
		}

		int trigger = 0;
		int nonTrigger = 0;

		for (EventMention eventMention : eventMentions) {
			if (em.getAnchor().equals(eventMention.getAnchor())) {
				if (eventMention.confidence > 0) {
					trigger++;
				} else {
					nonTrigger++;
				}
			}
		}

		Feature fea1 = new Feature(discourseFeaIdx, prob);
		feas.add(fea1);
		Feature fea2 = new Feature(discourseFeaIdx + 1, trigger);
		feas.add(fea2);
		Feature fea3 = new Feature(discourseFeaIdx + 2, nonTrigger);
		feas.add(fea3);
		Feature fea4 = new Feature(discourseFeaIdx + 3, em.typeConfidence);
		feas.add(fea4);
		Feature fea5 = new Feature(discourseFeaIdx + 4 + Util.types.indexOf(em.type), 1);
		feas.add(fea5);

		// System.out.println(em.typeConfidence + "$" + em.type);

		StringBuilder sb = new StringBuilder();
		for (Feature fea : feas) {
			sb.append(" ").append(fea.idx).append(":").append(fea.value);
		}
		return sb.toString();
	}

	public static boolean discourse = false;

	public static String mode;

	public static void main(String args[]) {
		if (args.length < 2) {
			System.out.println("java ~ [train|test|development] [folder]");
		}
		if (args.length == 3 && args[2].equals("discourse")) {
			discourse = true;
		}
		mode = args[0];
		Util.part = args[1];
		TriggerFeature triggerFeature = new TriggerFeature();
		if (mode.equalsIgnoreCase("train")) {
			triggerFeature.featureSpace = new HashMap<String, Integer>();
			loadTrainForTest(Util.part);
		} else {
			triggerFeature.featureSpace = Common.readFile2Map("triggerPipeFeaSpace" + Util.part);
			loadTest(Util.part);
		}

		ArrayList<String> files = Common.getLines("ACE_Chinese_" + args[0] + Util.part);
		ArrayList<String> triggerIndentFeatures = new ArrayList<String>();
//		ArrayList<String> triggerTypeFeatures = new ArrayList<String>();

		ArrayList<String> triggerSubTypeFeatures = new ArrayList<String>();

		ChineseTriggerIndent triggerIndent = new ChineseTriggerIndent();
		ArrayList<String> systemEMses = new ArrayList<String>();
		for (String file : files) {
			HashSet<EventMention> goldEMs = new HashSet<EventMention>();
			ACEChiDoc document = new ACEChiDoc(file);
			ArrayList<EventMention> ems = document.goldEventMentions;
			for (EventMention em : ems) {
				if (args[0].equalsIgnoreCase("train")) {
//					triggerTypeFeatures.add(triggerFeature.buildFeature(em, document, Integer.toString(Util.types
//							.indexOf(em.getType()) + 1)));
					triggerSubTypeFeatures.add(triggerFeature.buildFeature(em, document, Integer.toString(Util.subTypes
							.indexOf(em.getSubType()) + 1)));
					if(Util.subTypes.indexOf(em.getSubType())==-1) {
						System.out.println(em.getSubType());
						System.exit(1);
					}
				}
			}
			goldEMs.addAll(ems);
			ArrayList<EventMention> candidateTriggers;
			if (args[0].equals("train")) {
				candidateTriggers = triggerFeature.getTrainEMs(document);
			} else {
				candidateTriggers = triggerIndent.extractTrigger(file);
			}
			for (EventMention em : candidateTriggers) {
				StringBuilder sb = new StringBuilder();
				sb.append(file).append(" ").append(em.getAnchorStart()).append(" ").append(em.getAnchorEnd()).append(
						" ").append(em.getType()).append(" ").append(em.inferFrom);
				systemEMses.add(sb.toString());
				if (goldEMs.contains(em)) {
					triggerIndentFeatures.add(triggerFeature.buildFeature(em, document, "1"));
				} else {
					triggerIndentFeatures.add(triggerFeature.buildFeature(em, document, "2"));
				}
				if (!args[0].equalsIgnoreCase("train")) {
//					triggerTypeFeatures.add(triggerFeature.buildFeature(em, document, "1"));
					triggerSubTypeFeatures.add(triggerFeature.buildFeature(em, document, "1"));
				}
			}
		}
		Common.outputLines(triggerIndentFeatures, "data/Pipe_triggerIndent_" + args[0] + Util.part);
		Common.outputLines(systemEMses, "data/Pipe_triggerLines_" + args[0] + Util.part);
//		Common.outputLines(triggerTypeFeatures, "data/Pipe_triggerType_" + args[0] + Util.part);
		Common.outputLines(triggerSubTypeFeatures, "data/Pipe_triggerSubType_" + args[0] + Util.part);
		System.out.println("Build features for " + args[0] + Util.part);
		System.out.println("==========");

		if (mode.equalsIgnoreCase("train")) {
			Common.outputHashMap(triggerFeature.featureSpace, "triggerPipeFeaSpace" + Util.part);
		}
	}

	public ArrayList<EventMention> getTrainEMs(ACEChiDoc document) {
		HashSet<EventMention> trainEMs = new HashSet<EventMention>();
		for (ParseResult pr : document.parseReults) {
			for (int i = 1; i < pr.words.size(); i++) {
				int start = pr.positions.get(i)[0];
				int end = pr.positions.get(i)[1];
				String word = pr.words.get(i).replace("\n", "").replaceAll("\\s+", "");
				EventMention em = new EventMention();
				em.setAnchorStart(start);
				em.setAnchorEnd(end);
				em.setAnchor(word);
				trainEMs.add(em);
			}
		}
		trainEMs.addAll(document.goldEventMentions);
		ArrayList<EventMention> ems = new ArrayList<EventMention>();
		ems.addAll(trainEMs);
		return ems;
	}

	static HashMap<String, HashMap<String, EventMention>> pipelineResults;

	public static void loadTest(String folder) {
		pipelineResults = Util.readResult("pipe_svm_3extend/result" + folder, "chi");
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
	}
}
