package util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import model.ACEChiDoc;
import model.ACEDoc;
import model.ACEEngDoc;
import model.Depend;
import model.EntityMention;
import model.EventMention;
import model.EventMentionArgument;
import model.ParseResult;
import model.SemanticRole;
import model.syntaxTree.MyTree;
import model.syntaxTree.MyTreeNode;

public class Util {

	public static HashMap<String, String> subTypeMap = loadSubTypeMap();

	static HashMap<String, String> loadSubTypeMap() {
		HashMap<String, String> maps = new HashMap<String, String>();

		maps.put("Be-Born", "Life");
		maps.put("Marry", "Life");
		maps.put("Divorce", "Life");
		maps.put("Injure", "Life");
		maps.put("Die", "Life");

		maps.put("Transport", "Movement");

		maps.put("Transfer-Ownership", "Transaction");
		maps.put("Transfer-Money", "Transaction");

		maps.put("Start-Org", "Business");
		maps.put("Merge-Org", "Business");
		maps.put("Declare-Bankruptcy", "Business");
		maps.put("End-Org", "Business");

		maps.put("Attack", "Conflict");
		maps.put("Demonstrate", "Conflict");

		maps.put("Meet", "Contact");
		maps.put("Phone-Write", "Contact");

		maps.put("Start-Position", "Personnel");
		maps.put("End-Position", "Personnel");
		maps.put("Nominate", "Personnel");
		maps.put("Elect", "Personnel");

		maps.put("Extradite", "Justice");
		maps.put("Release-Parole", "Justice");
		maps.put("Fine", "Justice");
		maps.put("Arrest-Jail", "Justice");
		maps.put("Appeal", "Justice");
		maps.put("Trial-Hearing", "Justice");
		maps.put("Sentence", "Justice");
		maps.put("Sue", "Justice");
		maps.put("Convict", "Justice");
		maps.put("Execute", "Justice");
		maps.put("Acquit", "Justice");
		maps.put("Charge-Indict", "Justice");
		maps.put("Pardon", "Justice");

		maps.put("None", "None");
		return maps;
	}

	public static List<String> types = Arrays.asList("Life", "Movement", "Transaction", "Business", "Conflict",
			"Contact", "Personnel", "Justice", "null");

	public static List<String> subTypes = Arrays.asList("Start-Position", "Elect", "Transfer-Ownership", "Extradite",
			"Declare-Bankruptcy", "Marry", "Demonstrate", "Start-Org", "End-Org", "Appeal", "Trial-Hearing", "Attack",
			"Sue", "Convict", "Meet", "Pardon", "Charge-Indict", "Divorce", "End-Position", "Nominate", "Fine",
			"Release-Parole", "Transfer-Money", "Phone-Write", "Merge-Org", "Die", "Arrest-Jail", "Be-Born", "Injure",
			"Transport", "Sentence", "Acquit", "Execute", "None");

	public static List<String> roles = Arrays.asList("Crime", "Victim", "Origin", "Adjudicator", "Time-Holds",
			"Time-Before", "Target", "Time-At-End", "Org", "Recipient", "Vehicle", "Plaintiff", "Attacker", "Place",
			"Buyer", "Money", "Giver", "Beneficiary", "Agent", "Time-Ending", "Time-After", "Time-Starting", "Seller",
			"Defendant", "Time-Within", "Artifact", "Time-At-Beginning", "Prosecutor", "Sentence", "Price", "Position",
			"Instrument", "Destination", "Person", "Entity", "null");

	public static String part = "";

	public static String[] getEMLabel(String line) {
		String tokens[] = line.split("\\s+");
		String maxLabel = "";
		double maxValue = -1;
		for (int i = 0; i < tokens.length / 2; i++) {
			String label = tokens[i * 2];
			double value = Double.valueOf(tokens[i * 2 + 1]);
			if (value > maxValue) {
				maxLabel = label;
				maxValue = value;
			}
		}

		String yy[] = new String[2];
		yy[0] = maxLabel;
		yy[1] = Double.toString(maxValue);
		return yy;
	}

	public static void outputResult(HashMap<String, HashMap<String, EventMention>> allMentions, String filename) {
		ArrayList<String> lines = new ArrayList<String>();
		for (String file : allMentions.keySet()) {
			for (String key : allMentions.get(file).keySet()) {
				EventMention mention = allMentions.get(file).get(key);
				ArrayList<Object> atts = new ArrayList<Object>();
				atts.add(file);
				atts.add(mention.getAnchorStart());
				atts.add(mention.getAnchorEnd());
				atts.add(mention.confidence);
				atts.add(mention.type);
				atts.add(mention.typeConfidence);
				atts.add(mention.subType);
				atts.add(mention.subTypeConfidence);
				atts.add("-1");
				atts.add(mention.inferFrom);
				atts.add("-1");
				atts.add("-1");
				atts.add("-1");
				
//				System.out.println(mention.subType);
				
				for(double confidence : mention.typeConfidences) {
					atts.add(Double.toString(confidence));
				}
				
				lines.add(convert(atts));
				// System.out.println(document.content.substring(start,
				// end+1).replace("\n", "").replace(" ", "") + "\t" +
				// mention.confidence);
				for (EventMentionArgument argument : mention.eventMentionArguments) {
					// System.out.println(document.content.substring(start2,
					// end2+1).replace("\n", "").replace(" ", "") + "#" +
					// argument.getRole());

					atts = new ArrayList<Object>();
					atts.add(file);
					atts.add(mention.getAnchorStart());
					atts.add(mention.getAnchorEnd());
					atts.add(mention.confidence);
					atts.add(mention.type);
					atts.add(mention.typeConfidence);
					atts.add(mention.subType);
					atts.add(mention.subTypeConfidence);
					atts.add(argument.getStart());
					atts.add(argument.getEnd());
					atts.add(argument.confidence);
					atts.add(argument.getRole());
					atts.add(argument.roleConfidence);

					// role confidences
					for(double confidence : argument.roleConfidences) {
						atts.add(Double.toString(confidence));
					}
					
					lines.add(convert(atts));
				}
				// System.out.println("==============");
			}
		}
		Common.outputLines(lines, filename);
	}

	public static String convert(ArrayList<Object> atts) {
		StringBuilder sb = new StringBuilder();
		for (Object att : atts) {
			sb.append(att==null?"null":att.toString()).append(" ");
		}
		return sb.toString().trim();
	}
	
	public static HashMap<String, HashMap<String, EventMention>> readTriggers(String filename) {
		HashMap<String, HashMap<String, EventMention>> eventMentionsMap = new HashMap<String, HashMap<String, EventMention>>();
		ArrayList<String> lines = Common.getLines(filename);

		HashMap<String, ACEChiDoc> documentCache = new HashMap<String, ACEChiDoc>();

		for (String line : lines) {
			String tokens[] = line.split("\\s+");

			String fileID = tokens[0];

			ACEChiDoc document = documentCache.get(fileID);
			if (document == null) {
				document = new ACEChiDoc(fileID);
				documentCache.put(fileID, document);
			}

			HashMap<String, EventMention> eventMentions = eventMentionsMap.get(fileID);
			if (eventMentions == null) {
				eventMentions = new HashMap<String, EventMention>();
				eventMentionsMap.put(fileID, eventMentions);
			}

			int emStart = Integer.parseInt(tokens[1]);
			int emEnd = Integer.parseInt(tokens[2]);
			double emConfidence = Double.parseDouble(tokens[3]);
			String type = tokens[4];
			double typeConfidence = Double.parseDouble(tokens[5]);
			String subType = tokens[6];
			double subTypeConfidence = Double.parseDouble(tokens[7]);

			EventMention temp = new EventMention();
			temp.setAnchorStart(emStart);
			temp.setAnchorEnd(emEnd);
			temp.setAnchor(document.content.substring(emStart, emEnd + 1).replace("\n", "").replace(" ", ""));
			temp.confidence = emConfidence;
			temp.type = type;
			temp.typeConfidence = typeConfidence;
			temp.subType = subType;
			temp.subTypeConfidence = subTypeConfidence;

			EventMention eventMention = eventMentions.get(temp.toString());
			if (eventMention == null) {
				eventMention = temp;
				eventMentions.put(temp.toString(), eventMention);
			}

			if (Integer.parseInt(tokens[8]) == -1) {
				ArrayList<Double> confidences = new ArrayList<Double>();
				for(int k=13;k<tokens.length;k++) {
					confidences.add(Double.valueOf(tokens[k]));
				}
				eventMention.typeConfidences = confidences;
				eventMention.inferFrom = tokens[9];
				continue;
			}

			EventMentionArgument argument = new EventMentionArgument();
			argument.setStart(Integer.parseInt(tokens[8]));
			argument.setEnd(Integer.parseInt(tokens[9]));
			argument.confidence = Double.parseDouble(tokens[10]);
			argument.setRole(tokens[11]);
			argument.roleConfidence = Double.parseDouble(tokens[12]);
			argument.setEventMention(eventMention);
			argument.setEntityMention(document.goldNPMentionMap.get(tokens[8] + "," + tokens[9]));
			ArrayList<Double> confidences = new ArrayList<Double>();
			for(int k=13;k<tokens.length;k++) {
				confidences.add(Double.valueOf(tokens[k]));
			}
			argument.roleConfidences = confidences;
			eventMention.getEventMentionArguments().add(argument);
		}
		return eventMentionsMap;
	}
	

	public static HashMap<String, HashMap<String, EventMention>> readResult(String filename, String lang) {
		HashMap<String, HashMap<String, EventMention>> eventMentionsMap = new HashMap<String, HashMap<String, EventMention>>();
		ArrayList<String> lines = Common.getLines(filename);

		HashMap<String, ACEDoc> documentCache = new HashMap<String, ACEDoc>();

		for (String line : lines) {
			String tokens[] = line.split("\\s+");

			String fileID = tokens[0];

			ACEDoc document = documentCache.get(fileID);
			if (document == null) {
				if(lang.equals("chi")) {
					document = new ACEChiDoc(fileID);
				} else {
					document = new ACEEngDoc(fileID);
				}
				documentCache.put(fileID, document);
			}

			HashMap<String, EventMention> eventMentions = eventMentionsMap.get(fileID);
			if (eventMentions == null) {
				eventMentions = new HashMap<String, EventMention>();
				eventMentionsMap.put(fileID, eventMentions);
			}

			int emStart = Integer.parseInt(tokens[1]);
			int emEnd = Integer.parseInt(tokens[2]);
			double emConfidence = Double.parseDouble(tokens[3]);
			String type = tokens[4];
//			System.out.println(line);
			double typeConfidence = Double.parseDouble(tokens[5]);
			String subType = tokens[6];
			double subTypeConfidence = Double.parseDouble(tokens[7]);

			EventMention temp = new EventMention();
			temp.setAnchorStart(emStart);
			temp.setAnchorEnd(emEnd);
			temp.setAnchor(document.content.substring(emStart, emEnd + 1).replace("\n", "").replace(" ", ""));
			temp.confidence = emConfidence;
			temp.type = type;
			temp.typeConfidence = typeConfidence;
			temp.subType = subType;
			temp.subTypeConfidence = subTypeConfidence;

			EventMention eventMention = eventMentions.get(temp.toString());
			if (eventMention == null) {
				eventMention = temp;
				eventMentions.put(temp.toString(), eventMention);
			}

			if (Integer.parseInt(tokens[8]) == -1) {
				ArrayList<Double> confidences = new ArrayList<Double>();
				for(int k=13;k<tokens.length;k++) {
					confidences.add(Double.valueOf(tokens[k]));
				}
				eventMention.typeConfidences = confidences;
				eventMention.inferFrom = tokens[9];
				continue;
			}

			EventMentionArgument argument = new EventMentionArgument();
			argument.setStart(Integer.parseInt(tokens[8]));
			argument.setEnd(Integer.parseInt(tokens[9]));
			argument.confidence = Double.parseDouble(tokens[10]);
			argument.setRole(tokens[11]);
			argument.roleConfidence = Double.parseDouble(tokens[12]);
			argument.setEventMention(eventMention);
			argument.setEntityMention(document.goldNPMentionMap.get(tokens[8] + "," + tokens[9]));
			ArrayList<Double> confidences = new ArrayList<Double>();
			for(int k=13;k<tokens.length;k++) {
				confidences.add(Double.valueOf(tokens[k]));
			}
			argument.roleConfidences = confidences;
			eventMention.getEventMentionArguments().add(argument);
		}
		return eventMentionsMap;
	}

	public static EventMention getEM(String line) {
		String tokens[] = line.split("\\s+");
		EventMention em = new EventMention();
		em.fileID = tokens[0];
		int start = Integer.valueOf(tokens[1]);
		int end = Integer.valueOf(tokens[2]);
		em.setAnchorStart(start);
		em.setAnchorEnd(end);
		if (tokens.length > 3) {
			em.setType(tokens[3]);
		}
		if (tokens.length > 4) {
			em.setSubType(tokens[4]);
		}
		return em;
	}

	public static HashMap<String, ArrayList<EventMention>> loadPipelineEventMentions(String mode, boolean filter) {
		HashMap<String, ArrayList<EventMention>> systemEMses = new HashMap<String, ArrayList<EventMention>>();
		ArrayList<String> emLines = Common.getLines("data/Chinese_triggerIndent_" + mode + "_system" + Util.part);
		ArrayList<String> indentLines = Common.getLines("/users/yzcchen/tool/maxent/bin/" + mode + "_triggerIndent.txt"
				+ Util.part);
		ArrayList<String> typeLines = Common.getLines("/users/yzcchen/tool/maxent/bin/" + mode + "_triggerType.txt"
				+ Util.part);

		ArrayList<String> subTypeLines = Common.getLines("/users/yzcchen/tool/maxent/bin/" + mode
				+ "_triggerSubType.txt" + Util.part);

		for (int i = 0; i < emLines.size(); i++) {
			String line = emLines.get(i);
			String typeLine = typeLines.get(i);
			String subTypeLine = subTypeLines.get(i);

			String tokens[] = typeLine.split("\\s+");
			String type = "";
			double maxValue = 0;
			for (int k = 0; k < tokens.length / 2; k++) {
				String pre = tokens[k * 2];
				double val = Double.valueOf(tokens[k * 2 + 1]);
				if (val >= maxValue) {
					type = pre;
					maxValue = val;
				}
			}
			double typeConfidence = maxValue;

			maxValue = -1;
			String predict = "";
			String predictLine = indentLines.get(i);
			tokens = predictLine.split("\\s+");
			double confidence = -1;
			for (int k = 0; k < tokens.length / 2; k++) {
				String pre = tokens[k * 2];
				double val = Double.valueOf(tokens[k * 2 + 1]);
				if (val >= maxValue) {
					predict = pre;
					maxValue = val;
				}
				if (pre.equalsIgnoreCase("1")) {
					confidence = val;
				}
			}

			String subType = "";
			maxValue = -1;
			tokens = subTypeLine.split("\\s+");
			for (int k = 0; k < tokens.length / 2; k++) {
				String pre = tokens[k * 2];
				double val = Double.valueOf(tokens[k * 2 + 1]);
				if (val >= maxValue) {
					subType = pre;
					maxValue = val;
				}
			}
			double subTypeConfidence = maxValue;
			if (Integer.valueOf(predict) != 1 && filter) {
				continue;
			}

			tokens = line.split("\\s+");
			String fileID = tokens[0];
			int start = Integer.valueOf(tokens[1]);
			int end = Integer.valueOf(tokens[2]);
			EventMention em = new EventMention();
			em.setAnchorStart(start);
			em.setAnchorEnd(end);
			em.setType(type);
			em.setSubType(subType);
			em.confidence = confidence;
			em.subTypeConfidence = subTypeConfidence;
			em.typeConfidence = typeConfidence;
			if (systemEMses.containsKey(fileID)) {
				systemEMses.get(fileID).add(em);
			} else {
				ArrayList<EventMention> ems = new ArrayList<EventMention>();
				ems.add(em);
				systemEMses.put(fileID, ems);
			}
		}
		return systemEMses;
	}

	public static HashMap<String, ArrayList<EventMention>> loadMaxEntJointEventMentions(String folder) {
		HashMap<String, ArrayList<EventMention>> systemEMses = new HashMap<String, ArrayList<EventMention>>();
		systemEMses = new HashMap<String, ArrayList<EventMention>>();
		ArrayList<String> lines = Common.getLines("data/Joint_triggers_" + folder + "_system" + Util.part);

		ArrayList<String> typeLines = Common.getLines("/users/yzcchen/tool/maxent/bin/Joint_" + folder
				+ "_triggerIndent.txt" + Util.part);

		for (int i = 0; i < lines.size(); i++) {
			String line = lines.get(i);
			String typeLine = typeLines.get(i);

			String tokens[] = typeLine.split("\\s+");
			String type = "";
			double maxValue = 0;
			for (int k = 0; k < tokens.length / 2; k++) {
				String pre = tokens[k * 2];
				double val = Double.valueOf(tokens[k * 2 + 1]);
				if (val >= maxValue) {
					type = pre;
					maxValue = val;
				}
			}

			type = types.get(Integer.valueOf(type) - 1);

			if (type.equals("null")) {
				continue;
			}
			tokens = line.split("\\s+");
			String fileID = tokens[0];
			int start = Integer.valueOf(tokens[1]);
			int end = Integer.valueOf(tokens[2]);
			EventMention em = new EventMention();
			em.setAnchorStart(start);
			em.setAnchorEnd(end);
			em.setType(type);

			if (systemEMses.containsKey(fileID)) {
				systemEMses.get(fileID).add(em);
			} else {
				ArrayList<EventMention> ems = new ArrayList<EventMention>();
				ems.add(em);
				systemEMses.put(fileID, ems);
			}
		}
		return systemEMses;
	}

	public static HashMap<String, ArrayList<EventMention>> loadSVMJointEventMentions(String folder) {
		HashMap<String, ArrayList<EventMention>> systemEMses = new HashMap<String, ArrayList<EventMention>>();
		systemEMses = new HashMap<String, ArrayList<EventMention>>();
		ArrayList<String> lines = Common.getLines("data/Joint_triggers_" + folder + "_system" + Util.part);

		ArrayList<String> typeLines = Common.getLines("/users/yzcchen/tool/svm_multiclass/JointTriggerOutput_" + folder
				+ Util.part);

		for (int i = 0; i < lines.size(); i++) {
			String line = lines.get(i);
			String typeLine = typeLines.get(i);

			String tokens[] = typeLine.split("\\s+");

			String type = types.get(Integer.valueOf(tokens[0]) - 1);

			if (type.equals("null")) {
				continue;
			}
			tokens = line.split("\\s+");
			String fileID = tokens[0];
			int start = Integer.valueOf(tokens[1]);
			int end = Integer.valueOf(tokens[2]);
			EventMention em = new EventMention();
			em.setAnchorStart(start);
			em.setAnchorEnd(end);
			em.setType(type);

			if (systemEMses.containsKey(fileID)) {
				systemEMses.get(fileID).add(em);
			} else {
				ArrayList<EventMention> ems = new ArrayList<EventMention>();
				ems.add(em);
				systemEMses.put(fileID, ems);
			}
		}
		return systemEMses;
	}

	public static int right = 0;
	public static int wrong = 0;

	public static boolean addZeroPronounFeature(ArrayList<String> features, ACEChiDoc document,
			EntityMention entityMention, EventMention eventMention, String label, boolean sw) {
		boolean zeroPronoun = isZeroPronoun(eventMention, document);
		boolean find = false;
		if (zeroPronoun) {
			ArrayList<EntityMention> zeroSubjects = eventMention.zeroSubjects;
			if (zeroSubjects != null) {
				for (EntityMention zeroSubject : zeroSubjects) {
					if (zeroSubject.end == entityMention.end) {
						String sub = document.content.substring(entityMention.start, entityMention.end + 1).replace(
								"\n", "");
						String anchor = document.content.substring(eventMention.getAnchorStart(),
								eventMention.getAnchorEnd() + 1).replace("\n", "");
						if (sw) {
//							if (label.equalsIgnoreCase("36"))
//								System.out.println(label + "$" + sub + "#" + anchor + "# " + document.fileID);
						}
						if (label.equalsIgnoreCase("36")) {
							wrong++;
						} else {
							right++;
						}
						find = true;
					}
				}
			}
		}
		if(zeroPronoun) {
			features.add("true");
		} else {
			features.add("false");
		}
		
		if(find) {
			features.add("true");
		} else {
			features.add("false");
		}
		
		if(find && zeroPronoun) {
			return true;
		} else {
			return false;
		}
	}

	public static EntityMention formPhrase(MyTreeNode treeNode, ParseResult pr) {
		ArrayList<MyTreeNode> leaves = treeNode.getLeaves();
		int startIdx = leaves.get(0).leafIdx;
		int endIdx = leaves.get(leaves.size() - 1).leafIdx;
		int start = pr.positions.get(startIdx)[0];
		int end = pr.positions.get(endIdx)[1];
		StringBuilder sb = new StringBuilder();
		for (int i = startIdx; i <= endIdx; i++) {
			sb.append(pr.words.get(i));
		}
		EntityMention em = new EntityMention();
		em.start = start;
		em.end = end;
		em.source = sb.toString();
		return em;
	}

	public static ArrayList<EntityMention> getNPMentions(ParseResult pr, int start, int end) {
		ArrayList<EntityMention> nounPhrases = new ArrayList<EntityMention>();
		MyTree tree = pr.tree;
		MyTreeNode root = tree.root;
		ArrayList<MyTreeNode> frontie = new ArrayList<MyTreeNode>();
		frontie.add(root);
		while (frontie.size() > 0) {
			MyTreeNode tn = frontie.remove(0);
			if ((tn.value.toUpperCase().startsWith("NP") || tn.value.equalsIgnoreCase("qp"))) {
				// if it is subject
				boolean subject = false;
				ArrayList<MyTreeNode> laterSister = tn.getLaterSisters();
				for (MyTreeNode node : laterSister) {
					if (node.value.equalsIgnoreCase("vp") || node.value.equalsIgnoreCase("IP") || node.value.equals("CP")) {
						subject = true;
						break;
					}
				}
				if (!subject) {
					continue;
				}
				EntityMention element = formPhrase(tn, pr);
				if (element != null) {
					if (element.start == -1) {
						System.out.println();
					}
					if (element.start >= start && element.end <= end) {
						nounPhrases.add(element);
					}
				}
			}
			ArrayList<MyTreeNode> tns = tn.children;
			frontie.addAll(tns);
		}
		return nounPhrases;
	}
	
	public static boolean isZeroPronoun2(EventMention eventMention, ACEChiDoc document) {
		if (eventMention.isZeroPronoun == -1) {
			return false;
		} else if (eventMention.isZeroPronoun == 1) {
			return true;
		}
		int position2[] = ChineseUtil.findParseFilePosition(eventMention.getAnchorStart(), eventMention.getAnchorEnd(),
				document);
//		if(position2[2]!=0) {
//			eventMention.isZeroPronoun = -1;
//			return false;
//		}
		ParseResult pr = document.parseReults.get(position2[0]);
		ArrayList<int[]> pos = pr.positions;
		int wordPos = position2[1];
		if (wordPos < 2) {
			eventMention.isZeroPronoun = -1;
			return false;
		}
		ArrayList<String> words = pr.words;
		ArrayList<String> posTags = pr.posTags;

		String posTag = posTags.get(wordPos);
		String previousWord = words.get(wordPos - 1);

		MyTreeNode leaf = pr.tree.leaves.get(wordPos);

		if (leaf.parent.value.equalsIgnoreCase("vv") && leaf.parent.parent.value.equalsIgnoreCase("vp")) {
			ArrayList<MyTreeNode> beforeSisters = leaf.parent.parent.getOlderSisters();
			boolean NP = false;
			for (MyTreeNode sister : beforeSisters) {
				if (sister.value.equalsIgnoreCase("np")) {
					NP = true;
				}
			}
			if (!NP) {
				eventMention.isZeroPronoun = 1;
				eventMention.zeroSubjects = new ArrayList<EntityMention>();

				for (int p = wordPos - 1; p >= 0; p--) {
					if (words.get(p).equals("，") || p == 1) {
						boolean got = false;
						ArrayList<EntityMention> mentions = Util.getNPMentions(pr, pr.positions.get(1)[0], pr.positions
								.get(wordPos - 1)[1]);
						Collections.sort(mentions);
						Collections.reverse(mentions);
						if (mentions.size() > 0) {
							for (EntityMention mention : mentions) {
								if (document.allGoldNPEndMap.containsKey(mention.end)
										&& document.allGoldNPEndMap.get(mention.end).getType().equalsIgnoreCase("time")) {
								} else if(document.allGoldNPEndMap.containsKey(mention.end)){
									got = true;
									eventMention.zeroSubjects.add(document.allGoldNPEndMap.get(mention.end));
									break;
								}
							}
						}
						if (got) {
							return true;
						}
					}
				}
				return false;
			} else {
				eventMention.isZeroPronoun = -1;
				return false;
			}
		} else {
			return false;
		}

		// if (posTag.equalsIgnoreCase("vv") &&
		// previousWord.equalsIgnoreCase("，")) {
		// eventMention.isZeroPronoun = 1;
		// int from = pos.get(1)[0];
		// int to = pos.get(wordPos-1)[1];
		// ArrayList<SemanticRole> semanticRoles = document.semanticRoles;
		// for(int k=semanticRoles.size()-1;k>=0;k--) {
		// SemanticRole role = semanticRoles.get(k);
		// EventMention predicate = role.predict;
		// if(predicate.getAnchorStart()>=from && predicate.getAnchorEnd()<=to)
		// {
		// eventMention.zeroSubjects = role.arg0;
		// return true;
		// }
		// MyTreeNode node2 = pr.tree.leaves.get(wordPos-2);
		// ArrayList<MyTreeNode> ancestors = node2.getAncestors();
		// for(int i=ancestors.size()-1;i>0;i--) {
		// MyTreeNode ancestor = ancestors.get(i);
		// if(ancestor.value.equalsIgnoreCase("vp")) {
		// ArrayList<MyTreeNode> olderSisters = ancestor.getOlderSisters();
		// boolean find = false;
		// for(int j=olderSisters.size()-1;j>=0;j--) {
		// MyTreeNode oldSister = olderSisters.get(j);
		// if(oldSister.value.equalsIgnoreCase("np")) {
		// ArrayList<MyTreeNode> leaves = oldSister.getLeaves();
		// MyTreeNode firstLeaf = leaves.get(0);
		// MyTreeNode lastLeaf = leaves.get(leaves.size()-1);
		// int start = pos.get(firstLeaf.leafIdx)[0];
		// int end = pos.get(lastLeaf.leafIdx)[1];
		// EntityMention mention = new EntityMention();
		// mention.start = start;
		// mention.end = end;
		// eventMention.zeroSubject = mention;
		// find = true;
		// break;
		// }
		// }
		// if(find) {
		// break;
		// }
		// }
		// }
		// return true;
		// }
		// eventMention.isZeroPronoun = -1;
		// return false;
	}

	public static boolean isZeroPronoun(EventMention eventMention, ACEChiDoc document) {
		if (eventMention.isZeroPronoun == -1) {
			return false;
		} else if (eventMention.isZeroPronoun == 1) {
			return true;
		}
		int position2[] = ChineseUtil.findParseFilePosition(eventMention.getAnchorStart(), eventMention.getAnchorEnd(),
				document);
//		if(position2[2]!=0) {
//			eventMention.isZeroPronoun = -1;
//			return false;
//		}
		ParseResult pr = document.parseReults.get(position2[0]);
		ArrayList<int[]> pos = pr.positions;
		int wordPos = position2[1];
		if (wordPos < 2) {
			eventMention.isZeroPronoun = -1;
			return false;
		}
		ArrayList<String> words = pr.words;
		ArrayList<String> posTags = pr.posTags;

		String posTag = posTags.get(wordPos);
		String previousWord = words.get(wordPos - 1);

		MyTreeNode leaf = pr.tree.leaves.get(wordPos);

		if (leaf.parent.value.equalsIgnoreCase("vv") && leaf.parent.parent.value.equalsIgnoreCase("vp")) {
			ArrayList<MyTreeNode> beforeSisters = leaf.parent.parent.getOlderSisters();
			boolean NP = false;
			for (MyTreeNode sister : beforeSisters) {
				if (sister.value.equalsIgnoreCase("np")) {
					NP = true;
				}
			}
			if (!NP) {
				eventMention.isZeroPronoun = 1;
				eventMention.zeroSubjects = new ArrayList<EntityMention>();

				for (int p = wordPos - 1; p >= 0; p--) {
					if (words.get(p).equals("，") || p == 1) {
						boolean got = false;
						ArrayList<EntityMention> mentions = Util.getNPMentions(pr, pr.positions.get(1)[0], pr.positions
								.get(wordPos - 1)[1]);
						Collections.sort(mentions);
						Collections.reverse(mentions);
						if (mentions.size() > 0) {
							for (EntityMention mention : mentions) {
								if (document.allGoldNPEndMap.containsKey(mention.end)
										&& document.allGoldNPEndMap.get(mention.end).getType().equalsIgnoreCase("time")) {
								} else if(document.allGoldNPEndMap.containsKey(mention.end)){
									got = true;
									eventMention.zeroSubjects.add(document.allGoldNPEndMap.get(mention.end));
									break;
								}
							}
						}
						//的
						ArrayList<MyTreeNode> ancestors = leaf.getAncestors();
						MyTreeNode npAncestor = null;
						for(MyTreeNode node : ancestors) {
							if(node.value.equalsIgnoreCase("np")) {
								npAncestor = node;
								break;
							}
						}
						if(npAncestor!=null) {
							ArrayList<MyTreeNode> leafs = npAncestor.getLeaves();
							MyTreeNode deLeaf = null;
							for(MyTreeNode tmp : leafs) {
								if(tmp.value.equals("的")) {
									deLeaf = tmp;
									break;
								}
							}
							if(deLeaf!=null) {
								int end = pr.positions.get(leafs.get(leafs.size()-1).leafIdx)[1];
								if(document.allGoldNPEndMap.get(end)!=null) {
									eventMention.zeroSubjects.add(0, document.allGoldNPEndMap.get(end));
								}
							}
						}
						// 中华民国 and 中华民国政府
						if(eventMention.zeroSubjects.size()!=0) {
							EntityMention zero = eventMention.zeroSubjects.get(0);
							for(EntityMention mention : mentions) {
								if(mention.start==zero.start && mention.end>zero.end) {
									eventMention.zeroSubjects.add(0, mention);
									zero = mention;
								}
							}
						}
						
						if (got) {
							return true;
						}
					}
				}
				return false;
			} else {
				eventMention.isZeroPronoun = -1;
				return false;
			}
		} else {
			return false;
		}

		// if (posTag.equalsIgnoreCase("vv") &&
		// previousWord.equalsIgnoreCase("，")) {
		// eventMention.isZeroPronoun = 1;
		// int from = pos.get(1)[0];
		// int to = pos.get(wordPos-1)[1];
		// ArrayList<SemanticRole> semanticRoles = document.semanticRoles;
		// for(int k=semanticRoles.size()-1;k>=0;k--) {
		// SemanticRole role = semanticRoles.get(k);
		// EventMention predicate = role.predict;
		// if(predicate.getAnchorStart()>=from && predicate.getAnchorEnd()<=to)
		// {
		// eventMention.zeroSubjects = role.arg0;
		// return true;
		// }
		// MyTreeNode node2 = pr.tree.leaves.get(wordPos-2);
		// ArrayList<MyTreeNode> ancestors = node2.getAncestors();
		// for(int i=ancestors.size()-1;i>0;i--) {
		// MyTreeNode ancestor = ancestors.get(i);
		// if(ancestor.value.equalsIgnoreCase("vp")) {
		// ArrayList<MyTreeNode> olderSisters = ancestor.getOlderSisters();
		// boolean find = false;
		// for(int j=olderSisters.size()-1;j>=0;j--) {
		// MyTreeNode oldSister = olderSisters.get(j);
		// if(oldSister.value.equalsIgnoreCase("np")) {
		// ArrayList<MyTreeNode> leaves = oldSister.getLeaves();
		// MyTreeNode firstLeaf = leaves.get(0);
		// MyTreeNode lastLeaf = leaves.get(leaves.size()-1);
		// int start = pos.get(firstLeaf.leafIdx)[0];
		// int end = pos.get(lastLeaf.leafIdx)[1];
		// EntityMention mention = new EntityMention();
		// mention.start = start;
		// mention.end = end;
		// eventMention.zeroSubject = mention;
		// find = true;
		// break;
		// }
		// }
		// if(find) {
		// break;
		// }
		// }
		// }
		// return true;
		// }
		// eventMention.isZeroPronoun = -1;
		// return false;
	}

	public static void addDependencyFeature(ArrayList<String> features, ACEChiDoc document,
			EventMention eventMention, EntityMention entityMention, ParseResult pr) {
		// dependencies features
		ArrayList<Depend> depends = pr.depends;
		boolean subject = false;
		boolean object = false;
		for (Depend depend : depends) {
			String type = depend.type;
			int[] pFirst = pr.positions.get(depend.first);
			int[] pSecond = pr.positions.get(depend.second);

			if (!(pFirst[0] > eventMention.getAnchorEnd() || pFirst[1] < eventMention.getAnchorStart())
					&& !(pSecond[0] > entityMention.end || pSecond[1] < entityMention.start)) {
				if (type.contains("nsubj") || type.contains("dobj")) {
					// System.out.println(entityMention.extent + "#" +
					// eventMention.getAnchor() + "# " + document.fileID);
					subject = true;
				}
				if (type.equalsIgnoreCase("dobj")) {
					// System.out.println(eventMention.getAnchor() + "@" +
					// entityMention.extent);
					object = true;
				}
			}
		}
		// if(subject) {
		// features.add("1");
		// } else {
		// features.add("-1");
		// }
		// if(object) {
		// features.add("1");
		// } else {
		// features.add("-1");
		// }
	}

	public static void addSemanticRoleFeautre(ArrayList<String> features, ACEChiDoc document,
			EventMention eventMention, EntityMention entityMention) {
		ArrayList<SemanticRole> semanticRoles = document.semanticRoles;
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
		// features.add(eventMention.geta+"_" + entityMention.head);
		// features.add(trigger+"_" + entityMention.entity.type);
		// features.add(trigger+"_" + entityMention.entity.type);

	}
	
	public static HashMap<String, Integer> loadJointLabel() {
		HashMap<String, Integer> labels = new HashMap<String, Integer>();
		for(String eventType : types) {
			for(String role : roles) {
				String label = eventType + "_" + role;
				labels.put(label, labels.size()+1);
			}
		}
		return labels;
	}
	
	public static HashMap<String, HashSet<String>> typeRoleMap = loadTypeRoleMap();
	
	public static HashMap<String, HashSet<String>> loadTypeRoleMap() {
		HashMap<String, HashSet<String>> typeRoleMap = new HashMap<String, HashSet<String>>();
		HashSet<String> transactionRole = new HashSet<String>();
		transactionRole.addAll(Arrays.asList("Place", "Buyer", "Money", "Giver", "Beneficiary", "Time-Ending", "Time-Starting",
				"Seller", "Time-Before", "Time-Within", "Artifact", "Time-At-End", "Price", "Org", "Recipient"));
		typeRoleMap.put("Transaction", transactionRole);
		
		HashSet<String> BusinessRole = new HashSet<String>();
		BusinessRole.addAll(Arrays.asList("Place", "Time-Within", "Time-At-End", "Org", "Agent", "Time-Ending", "Time-Starting",
				"Time-Holds", "Time-Before"));
		typeRoleMap.put("Business", BusinessRole);
		
		HashSet<String> PersonnelRole = new HashSet<String>();
		PersonnelRole.addAll(Arrays.asList("Place", "Agent", "Time-Ending", "Time-After", "Time-Starting", "Time-Before", "Time-Holds",
				"Time-Within", "Time-At-Beginning", "Time-At-End", "Position", "Person", "Entity"));
		typeRoleMap.put("Personnel", PersonnelRole);
		
		HashSet<String> ContactRole = new HashSet<String>();
		ContactRole.addAll(Arrays.asList("Place", "Time-Within", "Time-Ending", "Time-Starting", "Time-Before", "Time-Holds",
				"Entity"));
		typeRoleMap.put("Contact", ContactRole);
		
		HashSet<String> JusticeRole = new HashSet<String>();
		JusticeRole.addAll(Arrays.asList("Plaintiff", "Place", "Crime", "Money", "Agent", "Time-Ending",
				"Origin", "Time-After", "Time-Starting", "Adjudicator", "Time-Before", "Defendant", "Time-Holds", "Time-Within", "Time-At-End"
				, "Time-At-Beginning", "Prosecutor", "Sentence", "Destination", "Person", "Entity"));
		typeRoleMap.put("Justice", JusticeRole);

		HashSet<String> LifeRole = new HashSet<String>();
		LifeRole.addAll(Arrays.asList("Place", "Victim", "Agent", "Time-Ending", "Time-After", "Time-Starting",
				"Time-Holds", "Time-Before", "Time-Within", "Time-At-End", "Time-At-Beginning", "Instrument", "Person"));
		typeRoleMap.put("Life", LifeRole);
		
		HashSet<String> ConflictRole = new HashSet<String>();
		ConflictRole.addAll(Arrays.asList("Attacker", "Place", "Time-Within", "Target", "Time-At-Beginning", "Time-Ending",
				"Time-After", "Time-Starting", "Instrument", "Time-Before", "Time-Holds", "Entity"));
		typeRoleMap.put("Conflict", ConflictRole);
		
		HashSet<String> MovementRole = new HashSet<String>();
		MovementRole.addAll(Arrays.asList("Place", "Agent", "Time-Ending", "Origin", "Time-After", "Time-Starting",
				"Time-Before", "Time-Holds", "Time-Within", "Artifact", "Time-At-End", "Time-At-Beginning", "Destination", "Entity", "Vehicle"));
		typeRoleMap.put("Movement", MovementRole);
		
		return typeRoleMap;
	}
	
}
