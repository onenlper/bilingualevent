package event.trigger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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
import model.stanford.StanfordResult;
import model.stanford.StanfordXMLReader;
import model.syntaxTree.MyTreeNode;
import seeds.SeedUtil;
import util.ChineseUtil;
import util.Common;
import util.Util;
import entity.semantic.ACECommon;
import event.preProcess.ChineseTriggerIndent;

public class JointTriggerIndentSeed {

	public static HashMap<String, String[]> semanticDic = loadSemanticDic();

	public static HashSet<String> proBankPredicts = Common
			.readFile2Set("predicts");

	public ArrayList<EntityMention> getEntities(ACEChiDoc document, int start,
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
	
	public ArrayList<EntityMention> getEntities(ArrayList<EntityMention> goldMentions, int start,
			int end) {
		ArrayList<EntityMention> mentions = new ArrayList<EntityMention>();
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

	public String buildFeature(EventMention em, ACEDoc doc, 
			String label) {
		return this.buildFeature(em, doc.content, doc.parseReults, label, doc.goldEntityMentions);
	}
	
	public String buildFeature(EventMention em, String content, ArrayList<ParseResult> parseResults, 
					String label, ArrayList<EntityMention> entityMentions) {
		String trigger = em.getAnchor();
		int position[] = ChineseUtil.findParseFilePosition(em.getAnchorStart(),
				em.getAnchorEnd(), content, parseResults);
		ParseResult pr = parseResults.get(position[0]);
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
		ArrayList<EntityMention> leftNearbyMentions = this.getEntities(
				entityMentions, positions.get(1)[0], em.getAnchorStart());

		int shortest = Integer.MAX_VALUE;
		String type1 = "";
		String extent = "";
		for (EntityMention mention : leftNearbyMentions) {
			MyTreeNode node = this.getNPTreeNode(mention, parseResults,
					position[0], position[1], position[3]);
			int distance = this.getSyntaxTreeDistance(leaf, node);
			if (distance < shortest) {
				shortest = distance;
				type1 = mention.semClass;
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
				type2 = mention.semClass;
				extent = mention.extent.replace(" ", "").replace("\n", "");
			}
		}
		features.add(type2);

		ArrayList<EntityMention> rightNearbyMentions = this.getEntities(
				entityMentions, em.getAnchorEnd() + 1,
				positions.get(positions.size() - 1)[1]);
		shortest = Integer.MAX_VALUE;
		type1 = "null";
		for (EntityMention mention : rightNearbyMentions) {
			MyTreeNode node = this.getNPTreeNode(mention, parseResults,
					position[0], position[1], position[3]);
			int distance = this.getSyntaxTreeDistance(leaf, node);
			if (distance < shortest) {
				shortest = distance;
				type1 = mention.semClass;
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
				type2 = mention.semClass;
				extent = mention.extent.replace(" ", "").replace("\n", "");
			}
		}
		features.add(type2);

		String eventType = "null";
		ArrayList<EntityMention> arguments = new ArrayList<EntityMention>();
		// EventMention pipeMentionTest = null;
		// if (pipelineResults.containsKey(document.fileID)
		// && pipelineResults.get(document.fileID).containsKey(em.toString())) {
		// pipeMentionTest =
		// pipelineResults.get(document.fileID).get(em.toString());
		// eventType = pipeMentionTest.subType;
		// double confidence = pipeMentionTest.confidence;
		// for (EventMentionArgument argument :
		// pipeMentionTest.eventMentionArguments) {
		// EntityMention argumentMention = this.getEntityMention(argument,
		// document);
		// arguments.add(argumentMention);
		// }
		// }
		//
		// ArrayList<EventMention> pipeEventMentions = new
		// ArrayList<EventMention>();
		// if (mode.equals("test")) {
		// // if (true) {
		// HashMap<String, EventMention> eventMentionMap =
		// pipelineResults.get(document.fileID);
		// if (eventMentionMap != null) {
		// for (String key : eventMentionMap.keySet()) {
		// if (eventMentionMap.get(key).confidence > 0.00) {
		// pipeEventMentions.add(eventMentionMap.get(key));
		// }
		// }
		// }
		// } else {
		// pipeEventMentions.addAll(document.goldEventMentions);
		// }

		// EventMention jointMentionTest = null;
		// if (jointResults.containsKey(document.fileID)
		// && jointResults.get(document.fileID).containsKey(em.toString())) {
		// jointMentionTest =
		// jointResults.get(document.fileID).get(em.toString());
		// }

		// ArrayList<EventMention> jointEventMentions = new
		// ArrayList<EventMention>();
		// if (mode.equals("test")) {
		// HashMap<String, EventMention> eventMentionMap =
		// jointResults.get(document.fileID);
		// if (eventMentionMap != null) {
		// for (String key : eventMentionMap.keySet()) {
		// if (eventMentionMap.get(key).confidence > 0.00) {
		// jointEventMentions.add(eventMentionMap.get(key));
		// }
		// }
		// }
		// } else {
		// jointEventMentions.addAll(document.goldEventMentions);
		// }

		// ArrayList<EventMention> eventMentionsFromTest = new
		// ArrayList<EventMention>();
		// HashMap<String, EventMention> eventMentionMap =
		// jointResults.get(document.fileID);
		// if (eventMentionMap != null) {
		// for (String key : eventMentionMap.keySet()) {
		// eventMentionsFromTest.add(eventMentionMap.get(key));
		// }
		// }
		// if (pipeEventMentions == null) {
		// pipeEventMentions = new ArrayList<EventMention>();
		// }

		addCharFeature(features, trigger);
		// addSemanticRoleFeature(features, document, em);

		// String documentType = "";
		// documentType = this.getMainLabel(pipeEventMentions);

		// System.out.println(features);
		// Common.pause("");

		return convert(features, label, eventType, arguments, em);
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

	public void addSemanticRoleFeature(ArrayList<String> features,
			ACEChiDoc document, EventMention em) {
		HashMap<EventMention, SemanticRole> predicates = new HashMap<EventMention, SemanticRole>();
		for (SemanticRole role : document.semanticRoles.values()) {
			predicates.put(role.predict, role);
		}
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

	public MyTreeNode getNPTreeNode(EntityMention np,
			ArrayList<ParseResult> prs, int npSenIdx, int npWordStartIdx,
			int npWordEndIdx) {
		MyTreeNode NP = null;
		try {
			ArrayList<MyTreeNode> leaves = prs.get(npSenIdx).tree.leaves;
			MyTreeNode leftNp = leaves.get(npWordStartIdx);
			MyTreeNode rightNp = leaves.get(npWordEndIdx);
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

	static int discourseFeaIdx = 700000;

	static HashMap<String, Double> consisProb;

	static HashMap<String, Double> triggerProb;

	public String convert(ArrayList<String> feaStrs, String label,
			String eventType, ArrayList<EntityMention> arguments,

			EventMention origEM) {
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
		// if (pipeMentionTest != null) {
		// for (EventMentionArgument argument :
		// pipeMentionTest.eventMentionArguments) {
		// String role = argument.getRole();
		// int index = Util.roles.indexOf(role) + yyFeaIdx2;
		// // feas.add(new Feature(index, 1));
		// }
		// }
		//
		// HashSet<Integer> corefIDs = new HashSet<Integer>();
		// for (EventMention temp : documentEventMentions) {
		// if (jointMentionTest == null || !temp.equals(jointMentionTest)) {
		// for (EventMentionArgument argument : temp.eventMentionArguments) {
		// if (argument.getEntityMention().getType().equalsIgnoreCase("time")
		// || argument.getEntityMention().getType().equalsIgnoreCase("value")
		// || argument.role.equals("null")) {
		// continue;
		// }
		// corefIDs.add(argument.getEntityMention().entity.entityIdx);
		// }
		// }
		// }
		//
		// if (jointMentionTest != null) {
		// for (EventMentionArgument argument :
		// jointMentionTest.eventMentionArguments) {
		// if (corefIDs.contains(argument.getEntityMention().entity.entityIdx))
		// {
		// String role = argument.getRole();
		// if(!role.equals("null")) {
		// int index = Util.roles.indexOf(role) + yyFeaIdx3;
		// // coreference
		// // feas.add(new Feature(index, 1));
		// }
		// }
		// }
		// }

		HashSet<Feature> feaSets = new HashSet<Feature>();
		feaSets.addAll(feas);
		feas.clear();
		feas.addAll(feaSets);
		Collections.sort(feas);

		for (Feature fea : feas) {
			sb.append(" ").append(fea.idx).append(":").append(fea.value);
		}
		// String typeConsistentFea = "";
		// if (pipeMentionTest != null) {
		// // type distribution
		// typeConsistentFea = this.getYYFeaValueList(documentEventMentions,
		// eventType, pipeMentionTest);
		// }
		// sb.append(" ").append(typeConsistentFea);
		//
		// String discourseFea = "";
		// if (pipeMentionTest != null && discourse) {
		// // discourse consistency
		// // discourseFea = this.buildDiscourseFea(pipeMentionTest,
		// eventMentionsFromTest);
		// }
		// sb.append(" ").append(discourseFea);

//		if (triggerProb == null) {
//			triggerProb = Common.readFile2Map5("pipeline/triggerProbability"
//					+ Util.part);
//		}

		double prob = 0;

		// if (pipeMentionTest != null) {
		// if (triggerProb.containsKey(pipeMentionTest.getAnchor())) {
		// prob = triggerProb.get(pipeMentionTest.getAnchor());
		// } else {
		// if (triggerProb.containsKey(pipeMentionTest.inferFrom)) {
		// prob = triggerProb.get(pipeMentionTest.inferFrom);
		// }
		// }
		// sb.append(" 701000:").append(prob);
		// // sb.append(" ").append(701000 + (int)(prob*10)).append(":1");
		// // if(prob>.75) {
		// // sb.append(" :").append(1);
		// // } else if(prob>.5) {
		// // sb.append(" 701001:").append(1);
		// // } else if(prob>.25) {
		// // sb.append(" 701002:").append(1);
		// // } else if(prob>.0) {
		// // sb.append(" 701003:").append(1);
		// // } else {
		// // sb.append(" 701004:").append(1);
		// // }
		// }
		return sb.toString().trim();
	}

	public String buildDiscourseFea(EventMention em,
			Collection<EventMention> eventMentions) {
		ArrayList<Feature> feas = new ArrayList<Feature>();
		if (consisProb == null) {
			consisProb = Common.readFile2Map5("pipeline/discourse_consistency"
					+ Util.part);
		}
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
		Feature fea5 = new Feature(discourseFeaIdx + 4
				+ Util.subTypes.indexOf(em.subType), 1);
		feas.add(fea5);

		StringBuilder sb = new StringBuilder();
		for (Feature fea : feas) {
			sb.append(" ").append(fea.idx).append(":").append(fea.value);
		}
		return sb.toString();
	}

	public static int yyFeaIdx3 = 595000;

	public static int yyFeaIdx2 = 590000;

	public static int yyFeaIdx = 600000;

	public String getYYFeaValueList(ArrayList<EventMention> eventMentions,
			String eventType, EventMention mentionTest) {
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

	public static List<String> types = Arrays.asList("Life", "Movement",
			"Transaction", "Business", "Conflict", "Contact", "Personnel",
			"Justice", "null");

	public static String mode = "train";

	public static HashMap<String, Integer> featureSpace;

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

	public static boolean discourse = true;

	public static void main(String args[]) {
		if (args.length < 3) {
			System.out
					.println("java ~ [train|test|development] [svm|maxent] [folder]");
		}
		if (args.length == 4 && args[3].equals("discourse")) {
			discourse = true;
		}
		mode = args[0];
		System.out.println(mode);
		Util.part = args[2];
		loadProps();
		if (mode.equalsIgnoreCase("train")) {
			featureSpace = new HashMap<String, Integer>();
			// loadTrainForTest(args[2]);
		} else {
			featureSpace = Common.readFile2Map("triggerValueFeaSpaceJoint"
					+ Util.part);
			// loadTest(args[2]);
		}
		
		ArrayList<String> triggerSubTypeFeatures = new ArrayList<String>();
		JointTriggerIndentSeed triggerFeature = new JointTriggerIndentSeed();

		ArrayList<String> systemEMses = new ArrayList<String>();

		int trueMentions = 0;
		int falseMentions = 0;
		if (args[0].equalsIgnoreCase("train")) {

			String content = SeedUtil.getContent();
			StanfordResult sr = StanfordXMLReader.read("sents.xml");
			ArrayList<ParseResult> parseResults = ACECommon
					.standford2ParseResult(sr, content);

			ArrayList<EventMention> goldEMs = SeedUtil.getGoldEventMentions();
			ArrayList<EntityMention> goldEntities = SeedUtil.getGoldEntityMentions();
			ArrayList<EventMention> candidateTriggers;

			candidateTriggers = getTrainEMs(parseResults, goldEMs);
			for (EventMention em : candidateTriggers) {
				triggerSubTypeFeatures
						.add(triggerFeature.buildFeature(em, content, parseResults,
								Integer.toString(Util.subTypes
										.indexOf(em.subType) + 1), goldEntities)
								);
				
				if(!em.subType.equals("null")) {
					trueMentions += 1;
				} else {
					falseMentions += 1;
				}
			}
			
			ArrayList<String> lines = Common.getLines("ACE_Chinese_train6");
			int annotatedSent = 0;
			for (String line : lines) {
				String tks[] = line.trim().split("\\s+");
				if(tks.length==1) {
					continue;
				}
				String file = tks[0];
				ACEChiDoc document = new ACEChiDoc(file);
				
				boolean all = false;
				HashSet<Integer> sentIDs = new HashSet<Integer>();
				for(int i=1;i<tks.length;i++) {
					if(tks[i].equals("all")) {
						all = true;
						break;
					}
					annotatedSent += 1;
					sentIDs.add(Integer.parseInt(tks[i]));
					if(Integer.parseInt(tks[i])>=document.parseReults.size()) {
						Common.bangErrorPOS("");
					}
				}
				
				
				candidateTriggers = getTrainEMs(document);
				
				for (EventMention em : candidateTriggers) {
					if(sentIDs.contains(document.getSentID(em.getAnchorEnd())) || all) {
						triggerSubTypeFeatures
						.add(triggerFeature.buildFeature(em, document,
								Integer.toString(Util.subTypes
										.indexOf(em.subType) + 1)));
						
						if(!em.subType.equals("null")) {
							trueMentions += 1;
						} else {
							falseMentions += 1;
						}
					}
				}
			}
			System.out.println("True Mentions: " + trueMentions);
			System.out.println("False Mentions: " + falseMentions);
			System.out.println("Annotated Sent: " + annotatedSent);
		} else {
			ArrayList<String> files = Common.getLines("ACE_Chinese_test"
					+ Util.part);
			
			for (int i = 0; i < files.size(); i++) {
				String file = files.get(i);
				ACEChiDoc document = new ACEChiDoc(file);
				ArrayList<EventMention> candidateTriggers = getTestEMs(document);
				
				for (EventMention em : candidateTriggers) {
					StringBuilder sb = new StringBuilder();
					sb.append(file).append(" ").append(em.getAnchorStart())
							.append(" ").append(em.getAnchorEnd()).append(" ")
							.append(em.getType()).append(" ").append(em.inferFrom);
					systemEMses.add(sb.toString());

					triggerSubTypeFeatures
							.add(triggerFeature.buildFeature(em, document,
									Integer.toString(Util.subTypes
											.indexOf(em.subType) + 1)));
				}
			}
		}

		Common.outputLines(systemEMses, "data/Joint_triggers_" + args[0]
				+ "_system" + Util.part);

		Common.outputLines(triggerSubTypeFeatures,
				"data/Joint_triggersFeature_" + args[0] + Util.part);
		System.out.println("Build features for " + args[0] + Util.part);
		System.out.println("==========");
		if (mode.equalsIgnoreCase("train")) {
			Common.outputHashMap(featureSpace, "triggerValueFeaSpaceJoint"
					+ Util.part);
		}
	}

	// static HashMap<String, HashMap<String, EventMention>> pipelineResults;
	// static HashMap<String, HashMap<String, EventMention>> jointResults;

//	public static ArrayList<EventMention> getz
	
	public static ArrayList<EventMention> getTestEMs(ACEChiDoc document) {
		if(Util.part.equals("6")) {
			return getTestEMsForActive(document);
		}
		
		HashSet<EventMention> testEMs = new HashSet<EventMention>();
		HashSet<EventMention> candidateEMSet = new HashSet<EventMention>();
		TriggerIndent triggerIndent = new ChineseTriggerIndent();
		ArrayList<EventMention> candidates = triggerIndent
				.extractTrigger(document.fileID);
		candidateEMSet.addAll(candidates);
		testEMs.addAll(candidates);
		ArrayList<EventMention> ems = new ArrayList<EventMention>();
		ems.addAll(testEMs);
		return ems;
	}

	public static ArrayList<EventMention> getTestEMsForActive(ACEChiDoc document) {
		ArrayList<EventMention> testEMs = new ArrayList<EventMention>();
		for (ParseResult pr : document.parseReults) {
			for (int i = 1; i < pr.words.size(); i++) {
				int start = pr.positions.get(i)[0];
				int end = pr.positions.get(i)[1];
				String word = pr.words.get(i).replace("\n", "")
						.replaceAll("\\s+", "");
				
				String POS = pr.posTags.get(i);
				if(!POS.equals("NN") && !POS.equals("P")
						&& !POS.equals("VV")) {
					continue;
				}
				
				EventMention em = new EventMention();
				em.setAnchorStart(start);
				em.setAnchorEnd(end);
				em.setAnchor(word);
				em.setType("null");
				em.setSubType("null");
				
				testEMs.add(em);
			}
		}
		return testEMs;
	}
	
	public static ArrayList<EventMention> getTrainEMs(ACEChiDoc document) {
		HashSet<EventMention> trainEMs = new HashSet<EventMention>();
		HashSet<EventMention> goldEMSet = new HashSet<EventMention>();
		goldEMSet.addAll(document.goldEventMentions);
		for (ParseResult pr : document.parseReults) {
			for (int i = 1; i < pr.words.size(); i++) {
				int start = pr.positions.get(i)[0];
				int end = pr.positions.get(i)[1];
				String word = pr.words.get(i).replace("\n", "")
						.replaceAll("\\s+", "");
				
				String POS = pr.posTags.get(i);
				if(!POS.equals("NN") && !POS.equals("P")
						&& !POS.equals("VV")) {
					continue;
				}
				
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

	public static ArrayList<EventMention> getTrainEMs(
			ArrayList<ParseResult> parseReults,
			ArrayList<EventMention> goldEventMentions) {
		HashSet<EventMention> trainEMs = new HashSet<EventMention>();
		HashSet<EventMention> goldEMSet = new HashSet<EventMention>();
		goldEMSet.addAll(goldEventMentions);
		for (ParseResult pr : parseReults) {
			for (int i = 1; i < pr.words.size(); i++) {
				int start = pr.positions.get(i)[0];
				int end = pr.positions.get(i)[1];
				String word = pr.words.get(i).replace("\n", "")
						.replaceAll("\\s+", "");
				
				String POS = pr.posTags.get(i);
				if(!POS.equals("NN") && !POS.equals("P")
						&& !POS.equals("VV")) {
					continue;
				}
				
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

	// public static void loadTest(String folder) {
	// pipelineResults = Util.readResult("pipe_svm_3extend/result" + folder,
	// "chi");
	// jointResults = Util.readResult("joint_svm_3extend/result" + folder,
	// "chi");
	// }
	//
	// public static void loadTrainForTest(String folder) {
	// pipelineResults = new HashMap<String, HashMap<String, EventMention>>();
	// for (int i = 0; i < 5; i++) {
	// if (i == Integer.valueOf(folder)) {
	// continue;
	// }
	// HashMap<String, HashMap<String, EventMention>> part =
	// Util.readResult("pipe_svm_3extend/result"
	// + Integer.toString(i), "chi");
	// for (String key : part.keySet()) {
	// pipelineResults.put(key, part.get(key));
	// }
	// }
	//
	// jointResults = new HashMap<String, HashMap<String, EventMention>>();
	// for (int i = 0; i < 5; i++) {
	// if (i == Integer.valueOf(folder)) {
	// continue;
	// }
	// HashMap<String, HashMap<String, EventMention>> part =
	// Util.readResult("joint_svm_3extend/result"
	// + Integer.toString(i), "chi");
	// for (String key : part.keySet()) {
	// jointResults.put(key, part.get(key));
	// }
	// }
	// }
}
