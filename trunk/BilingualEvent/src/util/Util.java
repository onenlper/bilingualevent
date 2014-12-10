package util;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import model.ACEChiDoc;
import model.ACEDoc;
import model.ACEEngDoc;
import model.Depend;
import model.Entity;
import model.EntityMention;
import model.EntityMention.Gender;
import model.EntityMention.MentionType;
import model.EntityMention.Numb;
import model.EventMention;
import model.EventMentionArgument;
import model.ParseResult;
import model.SemanticRole;
import model.syntaxTree.GraphNode;
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

	public static List<String> types = Arrays.asList("Life", "Movement",
			"Transaction", "Business", "Conflict", "Contact", "Personnel",
			"Justice", "null");

	public static List<String> subTypes = Arrays.asList("Start-Position",
			"Elect", "Transfer-Ownership", "Extradite", "Declare-Bankruptcy",
			"Marry", "Demonstrate", "Start-Org", "End-Org", "Appeal",
			"Trial-Hearing", "Attack", "Sue", "Convict", "Meet", "Pardon",
			"Charge-Indict", "Divorce", "End-Position", "Nominate", "Fine",
			"Release-Parole", "Transfer-Money", "Phone-Write", "Merge-Org",
			"Die", "Arrest-Jail", "Be-Born", "Injure", "Transport", "Sentence",
			"Acquit", "Execute", "None");

	public static List<String> roles = Arrays.asList("Crime", "Victim",
			"Origin", "Adjudicator", "Time-Holds", "Time-Before", "Target",
			"Time-At-End", "Org", "Recipient", "Vehicle", "Plaintiff",
			"Attacker", "Place", "Buyer", "Money", "Giver", "Beneficiary",
			"Agent", "Time-Ending", "Time-After", "Time-Starting", "Seller",
			"Defendant", "Time-Within", "Artifact", "Time-At-Beginning",
			"Prosecutor", "Sentence", "Price", "Position", "Instrument",
			"Destination", "Person", "Entity", "null");

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

	public static void outputResult(
			HashMap<String, HashMap<String, EventMention>> allMentions,
			String filename) {
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

				// System.out.println(mention.subType);

				for (double confidence : mention.typeConfidences) {
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
					for (double confidence : argument.roleConfidences) {
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
			sb.append(att == null ? "null" : att.toString()).append(" ");
		}
		return sb.toString().trim();
	}

	public static HashMap<String, HashMap<String, EventMention>> readTriggers(
			String filename) {
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

			HashMap<String, EventMention> eventMentions = eventMentionsMap
					.get(fileID);
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
			temp.setAnchor(document.content.substring(emStart, emEnd + 1)
					.replace("\n", "").replace(" ", ""));
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
				for (int k = 13; k < tokens.length; k++) {
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
			argument.setEntityMention(document.goldNPMentionMap.get(tokens[8]
					+ "," + tokens[9]));
			ArrayList<Double> confidences = new ArrayList<Double>();
			for (int k = 13; k < tokens.length; k++) {
				confidences.add(Double.valueOf(tokens[k]));
			}
			argument.roleConfidences = confidences;
			eventMention.getEventMentionArguments().add(argument);
		}
		return eventMentionsMap;
	}

	public static HashMap<String, HashMap<String, EventMention>> readResult(
			String filename, String lang) {
		HashMap<String, HashMap<String, EventMention>> eventMentionsMap = new HashMap<String, HashMap<String, EventMention>>();
		ArrayList<String> lines = Common.getLines(filename);

		HashMap<String, ACEDoc> documentCache = new HashMap<String, ACEDoc>();

		for (String line : lines) {
			String tokens[] = line.split("\\s+");

			String fileID = tokens[0];

			ACEDoc document = documentCache.get(fileID);
			if (document == null) {
				if (lang.equals("chi")) {
					document = new ACEChiDoc(fileID);
				} else {
					document = new ACEEngDoc(fileID);
				}
				documentCache.put(fileID, document);
			}

			HashMap<String, EventMention> eventMentions = eventMentionsMap
					.get(fileID);
			if (eventMentions == null) {
				eventMentions = new HashMap<String, EventMention>();
				eventMentionsMap.put(fileID, eventMentions);
			}

			int emStart = Integer.parseInt(tokens[1]);
			int emEnd = Integer.parseInt(tokens[2]);
			double emConfidence = Double.parseDouble(tokens[3]);
			String type = tokens[4];
			// System.out.println(line);
			double typeConfidence = Double.parseDouble(tokens[5]);
			String subType = tokens[6];
			double subTypeConfidence = Double.parseDouble(tokens[7]);

			EventMention temp = new EventMention();
			temp.setAnchorStart(emStart);
			temp.setAnchorEnd(emEnd);
			temp.setAnchor(document.content.substring(emStart, emEnd + 1)
					.replace("\n", "").replace(" ", ""));
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
				for (int k = 13; k < tokens.length; k++) {
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
			argument.setEntityMention(document.goldNPMentionMap.get(tokens[8]
					+ "," + tokens[9]));
			ArrayList<Double> confidences = new ArrayList<Double>();
			for (int k = 13; k < tokens.length; k++) {
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

	public static HashMap<String, ArrayList<EventMention>> loadPipelineEventMentions(
			String mode, boolean filter) {
		HashMap<String, ArrayList<EventMention>> systemEMses = new HashMap<String, ArrayList<EventMention>>();
		ArrayList<String> emLines = Common
				.getLines("data/Chinese_triggerIndent_" + mode + "_system"
						+ Util.part);
		ArrayList<String> indentLines = Common
				.getLines("/users/yzcchen/tool/maxent/bin/" + mode
						+ "_triggerIndent.txt" + Util.part);
		ArrayList<String> typeLines = Common
				.getLines("/users/yzcchen/tool/maxent/bin/" + mode
						+ "_triggerType.txt" + Util.part);

		ArrayList<String> subTypeLines = Common
				.getLines("/users/yzcchen/tool/maxent/bin/" + mode
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

	public static HashMap<String, ArrayList<EventMention>> loadMaxEntJointEventMentions(
			String folder) {
		HashMap<String, ArrayList<EventMention>> systemEMses = new HashMap<String, ArrayList<EventMention>>();
		systemEMses = new HashMap<String, ArrayList<EventMention>>();
		ArrayList<String> lines = Common.getLines("data/Joint_triggers_"
				+ folder + "_system" + Util.part);

		ArrayList<String> typeLines = Common
				.getLines("/users/yzcchen/tool/maxent/bin/Joint_" + folder
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

	public static HashMap<String, ArrayList<EventMention>> loadSVMJointEventMentions(
			String folder) {
		HashMap<String, ArrayList<EventMention>> systemEMses = new HashMap<String, ArrayList<EventMention>>();
		systemEMses = new HashMap<String, ArrayList<EventMention>>();
		ArrayList<String> lines = Common.getLines("data/Joint_triggers_"
				+ folder + "_system" + Util.part);

		ArrayList<String> typeLines = Common
				.getLines("/users/yzcchen/tool/svm_multiclass/JointTriggerOutput_"
						+ folder + Util.part);

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

	public static boolean addZeroPronounFeature(ArrayList<String> features,
			ACEChiDoc document, EntityMention entityMention,
			EventMention eventMention, String label, boolean sw) {
		boolean zeroPronoun = isZeroPronoun(eventMention, document);
		boolean find = false;
		if (zeroPronoun) {
			ArrayList<EntityMention> zeroSubjects = eventMention.zeroSubjects;
			if (zeroSubjects != null) {
				for (EntityMention zeroSubject : zeroSubjects) {
					if (zeroSubject.end == entityMention.end) {
						String sub = document.content.substring(
								entityMention.start, entityMention.end + 1)
								.replace("\n", "");
						String anchor = document.content.substring(
								eventMention.getAnchorStart(),
								eventMention.getAnchorEnd() + 1).replace("\n",
								"");
						if (sw) {
							// if (label.equalsIgnoreCase("36"))
							// System.out.println(label + "$" + sub + "#" +
							// anchor + "# " + document.fileID);
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
		if (zeroPronoun) {
			features.add("true");
		} else {
			features.add("false");
		}

		if (find) {
			features.add("true");
		} else {
			features.add("false");
		}

		if (find && zeroPronoun) {
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

	public static ArrayList<EntityMention> getNPMentions(ParseResult pr,
			int start, int end) {
		ArrayList<EntityMention> nounPhrases = new ArrayList<EntityMention>();
		MyTree tree = pr.tree;
		MyTreeNode root = tree.root;
		ArrayList<MyTreeNode> frontie = new ArrayList<MyTreeNode>();
		frontie.add(root);
		while (frontie.size() > 0) {
			MyTreeNode tn = frontie.remove(0);
			if ((tn.value.toUpperCase().startsWith("NP") || tn.value
					.equalsIgnoreCase("qp"))) {
				// if it is subject
				boolean subject = false;
				ArrayList<MyTreeNode> laterSister = tn.getLaterSisters();
				for (MyTreeNode node : laterSister) {
					if (node.value.equalsIgnoreCase("vp")
							|| node.value.equalsIgnoreCase("IP")
							|| node.value.equals("CP")) {
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

	public static boolean isZeroPronoun2(EventMention eventMention,
			ACEChiDoc document) {
		if (eventMention.isZeroPronoun == -1) {
			return false;
		} else if (eventMention.isZeroPronoun == 1) {
			return true;
		}
		int position2[] = ChineseUtil.findParseFilePosition(
				eventMention.getAnchorStart(), eventMention.getAnchorEnd(),
				document);
		// if(position2[2]!=0) {
		// eventMention.isZeroPronoun = -1;
		// return false;
		// }
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

		if (leaf.parent.value.equalsIgnoreCase("vv")
				&& leaf.parent.parent.value.equalsIgnoreCase("vp")) {
			ArrayList<MyTreeNode> beforeSisters = leaf.parent.parent
					.getOlderSisters();
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
						ArrayList<EntityMention> mentions = Util.getNPMentions(
								pr, pr.positions.get(1)[0],
								pr.positions.get(wordPos - 1)[1]);
						Collections.sort(mentions);
						Collections.reverse(mentions);
						if (mentions.size() > 0) {
							for (EntityMention mention : mentions) {
								if (document.allGoldNPEndMap
										.containsKey(mention.end)
										&& document.allGoldNPEndMap
												.get(mention.end).getType()
												.equalsIgnoreCase("time")) {
								} else if (document.allGoldNPEndMap
										.containsKey(mention.end)) {
									got = true;
									eventMention.zeroSubjects
											.add(document.allGoldNPEndMap
													.get(mention.end));
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

	public static boolean isZeroPronoun(EventMention eventMention,
			ACEChiDoc document) {
		if (eventMention.isZeroPronoun == -1) {
			return false;
		} else if (eventMention.isZeroPronoun == 1) {
			return true;
		}
		int position2[] = ChineseUtil.findParseFilePosition(
				eventMention.getAnchorStart(), eventMention.getAnchorEnd(),
				document);
		// if(position2[2]!=0) {
		// eventMention.isZeroPronoun = -1;
		// return false;
		// }
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

		if (leaf.parent.value.equalsIgnoreCase("vv")
				&& leaf.parent.parent.value.equalsIgnoreCase("vp")) {
			ArrayList<MyTreeNode> beforeSisters = leaf.parent.parent
					.getOlderSisters();
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
						ArrayList<EntityMention> mentions = Util.getNPMentions(
								pr, pr.positions.get(1)[0],
								pr.positions.get(wordPos - 1)[1]);
						Collections.sort(mentions);
						Collections.reverse(mentions);
						if (mentions.size() > 0) {
							for (EntityMention mention : mentions) {
								if (document.allGoldNPEndMap
										.containsKey(mention.end)
										&& document.allGoldNPEndMap
												.get(mention.end).getType()
												.equalsIgnoreCase("time")) {
								} else if (document.allGoldNPEndMap
										.containsKey(mention.end)) {
									got = true;
									eventMention.zeroSubjects
											.add(document.allGoldNPEndMap
													.get(mention.end));
									break;
								}
							}
						}
						// 的
						ArrayList<MyTreeNode> ancestors = leaf.getAncestors();
						MyTreeNode npAncestor = null;
						for (MyTreeNode node : ancestors) {
							if (node.value.equalsIgnoreCase("np")) {
								npAncestor = node;
								break;
							}
						}
						if (npAncestor != null) {
							ArrayList<MyTreeNode> leafs = npAncestor
									.getLeaves();
							MyTreeNode deLeaf = null;
							for (MyTreeNode tmp : leafs) {
								if (tmp.value.equals("的")) {
									deLeaf = tmp;
									break;
								}
							}
							if (deLeaf != null) {
								int end = pr.positions.get(leafs.get(leafs
										.size() - 1).leafIdx)[1];
								if (document.allGoldNPEndMap.get(end) != null) {
									eventMention.zeroSubjects.add(0,
											document.allGoldNPEndMap.get(end));
								}
							}
						}
						// 中华民国 and 中华民国政府
						if (eventMention.zeroSubjects.size() != 0) {
							EntityMention zero = eventMention.zeroSubjects
									.get(0);
							for (EntityMention mention : mentions) {
								if (mention.start == zero.start
										&& mention.end > zero.end) {
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

	public static void addDependencyFeature(ArrayList<String> features,
			ACEChiDoc document, EventMention eventMention,
			EntityMention entityMention, ParseResult pr) {
		// dependencies features
		ArrayList<Depend> depends = pr.depends;
		boolean subject = false;
		boolean object = false;
		for (Depend depend : depends) {
			String type = depend.type;
			int[] pFirst = pr.positions.get(depend.first);
			int[] pSecond = pr.positions.get(depend.second);

			if (!(pFirst[0] > eventMention.getAnchorEnd() || pFirst[1] < eventMention
					.getAnchorStart())
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

	public static ArrayList<String> getPathFeature(String prefix,
			ArrayList<GraphNode> path, ParseResult s) {
		ArrayList<String> feas = new ArrayList<String>();
		StringBuilder vertexSb = new StringBuilder();
		StringBuilder edgeSb = new StringBuilder();
		for (int i = 0; i < path.size(); i++) {
			GraphNode n = path.get(i);
			String word = "#ROOT#";
			if (n.value != 0) {
				word = s.words.get(n.value - 1);
			}
			vertexSb.append(word).append(" ");
			if (i < path.size() - 1) {
				edgeSb.append(n.getEdgeName(path.get(i + 1))).append(" ");
				// feas.add(prefix + "subcate#" + word + "." +
				// n.getEdgeName(path.get(i + 1)) + "." +
				// Util.getTkFromDepNode(path.get(i+1), s).word);
			}
		}
		feas.add(prefix + "vertexWalk#" + vertexSb.toString().trim());
		feas.add(prefix + "edgeWalk#" + edgeSb.toString().trim());
		feas.add(prefix + "pathLength#" + path.size());

		// bigram along path
		for (int i = 0; i < path.size() - 1; i++) {
			GraphNode n1 = path.get(i);
			GraphNode n2 = path.get(i + 1);
			String t1 = s.words.get(n1.value);
			String t2 = s.words.get(n2.value);

			feas.add(prefix + "biLex#" + t1 + "_" + t2);
			feas.add(prefix + "biPos#" + s.posTags.get(n1.value) + "_"
					+ s.posTags.get(n2.value));

			if (i < path.size() - 2) {
				feas.add(prefix + prefix + "biDep#" + n1.getEdgeName(n2) + "_"
						+ n2.getEdgeName(path.get(i + 2)));
			}
		}

		// trigram along path
		for (int i = 0; i < path.size() - 2; i++) {
			GraphNode n1 = path.get(i);
			GraphNode n2 = path.get(i + 1);
			GraphNode n3 = path.get(i + 2);
			String t1 = s.words.get(n1.value);
			String t2 = s.words.get(n2.value);
			String t3 = s.words.get(n3.value);

			feas.add(prefix + "triLex#" + t1 + "_" + t2 + "_" + t3);
			feas.add(prefix + "triPos#" + s.posTags.get(n1.value) + "_"
					+ s.posTags.get(n2.value) + "_" + s.posTags.get(n3.value));

			if (i < path.size() - 3) {
				feas.add(prefix + "triDep#" + n1.getEdgeName(n2) + "_"
						+ n2.getEdgeName(n3) + "_"
						+ n3.getEdgeName(path.get(i + 3)));
			}
		}

		return feas;
	}

	public static ArrayList<GraphNode> findPath(GraphNode from, GraphNode to) {
		// System.out.println("start...");
		ArrayList<GraphNode> path = new ArrayList<GraphNode>();

		HashSet<GraphNode> visited = new HashSet<GraphNode>();
		ArrayList<GraphNode> fronties = new ArrayList<GraphNode>();
		fronties.add(from);
		if (from == null) {
			return path;
		}
		if (to == null) {
			return path;
		}

		if (from != to) {
			loop: while (true) {
				ArrayList<GraphNode> nextLevel = new ArrayList<GraphNode>();
				for (GraphNode node : fronties) {
					if (node == null) {
						Common.bangErrorPOS("Null Dep node");
					}
					for (GraphNode next : node.nexts) {
						if (!visited.contains(next)) {
							next.backNode = node;
							if (next.backNode == next) {
								// System.out.println(s.tokens.get(Integer
								// .valueOf(next.value) - 1).word);
								// System.out.println(s.d.fn);
								// Common.bangErrorPOS("Self Dep: " + next.value
								// + " " + next.backNode.value);
							}
							if (next == to) {
								break loop;
							}
							nextLevel.add(next);
						}
					}
					visited.add(node);
				}
				fronties = nextLevel;
				if (fronties.size() == 0) {
					// Token t1 = Util.getTkFromDepNode(from, s);
					// Token t2 = Util.getTkFromDepNode(to, s);
					// System.out.println(t1.word + " " + t1.idInSentence);
					// System.out.println(t2.word + " " + t2.idInSentence);
					// System.out.println(s.d.fn);
					// Common.bangErrorPOS("");
					// System.out.println("No Path");
					return path;
				}
			}
		}

		GraphNode tmp = to;
		while (true) {
			path.add(0, tmp);
			if (tmp == from) {
				break;
			}
			tmp = tmp.backNode;
			if (tmp == tmp.backNode) {
				// System.out.println("GEEE");
				return new ArrayList<GraphNode>();
			}
			// System.out.println(fronties.size() + " " + (tmp==tmp.backNode));
		}
		// System.out.println("end");
		return path;
	}

	// public static void addSemanticRoleFeautre(ArrayList<String> features,
	// ACEChiDoc document, EventMention eventMention,
	// EntityMention entityMention) {
	// ArrayList<SemanticRole> semanticRoles = document.semanticRoles;
	// boolean arg0 = false;
	// boolean arg1 = false;
	// boolean tmpArg = false;
	// boolean pred = false;
	// for (SemanticRole role : semanticRoles) {
	// EventMention predicate = role.predict;
	// if (!(predicate.getAnchorEnd() < eventMention.getAnchorStart() ||
	// predicate
	// .getAnchorStart() > eventMention.getAnchorEnd())) {
	// pred = true;
	// ArrayList<EntityMention> arg0s = role.arg0;
	// for (EntityMention arg : arg0s) {
	// if (arg.end == entityMention.end) {
	// arg0 = true;
	// }
	// }
	// ArrayList<EntityMention> arg1s = role.arg1;
	// for (EntityMention arg : arg1s) {
	// if (arg.end == entityMention.end) {
	// arg1 = true;
	// }
	// }
	// ArrayList<EntityMention> tempArgs = role.tmp;
	// for (EntityMention arg : tempArgs) {
	// if (arg.end == entityMention.end) {
	// tmpArg = true;
	// }
	// }
	// }
	// }
	//
	// if (pred) {
	// features.add("1");
	// } else {
	// features.add("0");
	// }
	//
	// if (arg0) {
	// features.add("1");
	// } else {
	// features.add("0");
	// }
	// if (arg1) {
	// features.add("1");
	// } else {
	// features.add("0");
	// }
	// if (tmpArg) {
	// features.add("1");
	// } else {
	// features.add("0");
	// }
	// // features.add(eventMention.geta+"_" + entityMention.head);
	// // features.add(trigger+"_" + entityMention.entity.type);
	// // features.add(trigger+"_" + entityMention.entity.type);
	//
	// }

	public static HashMap<String, Integer> loadJointLabel() {
		HashMap<String, Integer> labels = new HashMap<String, Integer>();
		for (String eventType : types) {
			for (String role : roles) {
				String label = eventType + "_" + role;
				labels.put(label, labels.size() + 1);
			}
		}
		return labels;
	}

	public static HashMap<String, HashSet<String>> typeRoleMap = loadTypeRoleMap();

	public static HashMap<String, HashSet<String>> loadTypeRoleMap() {
		HashMap<String, HashSet<String>> typeRoleMap = new HashMap<String, HashSet<String>>();
		HashSet<String> transactionRole = new HashSet<String>();
		transactionRole.addAll(Arrays.asList("Place", "Buyer", "Money",
				"Giver", "Beneficiary", "Time-Ending", "Time-Starting",
				"Seller", "Time-Before", "Time-Within", "Artifact",
				"Time-At-End", "Price", "Org", "Recipient"));
		typeRoleMap.put("Transaction", transactionRole);

		HashSet<String> BusinessRole = new HashSet<String>();
		BusinessRole.addAll(Arrays.asList("Place", "Time-Within",
				"Time-At-End", "Org", "Agent", "Time-Ending", "Time-Starting",
				"Time-Holds", "Time-Before"));
		typeRoleMap.put("Business", BusinessRole);

		HashSet<String> PersonnelRole = new HashSet<String>();
		PersonnelRole.addAll(Arrays.asList("Place", "Agent", "Time-Ending",
				"Time-After", "Time-Starting", "Time-Before", "Time-Holds",
				"Time-Within", "Time-At-Beginning", "Time-At-End", "Position",
				"Person", "Entity"));
		typeRoleMap.put("Personnel", PersonnelRole);

		HashSet<String> ContactRole = new HashSet<String>();
		ContactRole.addAll(Arrays.asList("Place", "Time-Within", "Time-Ending",
				"Time-Starting", "Time-Before", "Time-Holds", "Entity"));
		typeRoleMap.put("Contact", ContactRole);

		HashSet<String> JusticeRole = new HashSet<String>();
		JusticeRole.addAll(Arrays.asList("Plaintiff", "Place", "Crime",
				"Money", "Agent", "Time-Ending", "Origin", "Time-After",
				"Time-Starting", "Adjudicator", "Time-Before", "Defendant",
				"Time-Holds", "Time-Within", "Time-At-End",
				"Time-At-Beginning", "Prosecutor", "Sentence", "Destination",
				"Person", "Entity"));
		typeRoleMap.put("Justice", JusticeRole);

		HashSet<String> LifeRole = new HashSet<String>();
		LifeRole.addAll(Arrays.asList("Place", "Victim", "Agent",
				"Time-Ending", "Time-After", "Time-Starting", "Time-Holds",
				"Time-Before", "Time-Within", "Time-At-End",
				"Time-At-Beginning", "Instrument", "Person"));
		typeRoleMap.put("Life", LifeRole);

		HashSet<String> ConflictRole = new HashSet<String>();
		ConflictRole.addAll(Arrays.asList("Attacker", "Place", "Time-Within",
				"Target", "Time-At-Beginning", "Time-Ending", "Time-After",
				"Time-Starting", "Instrument", "Time-Before", "Time-Holds",
				"Entity"));
		typeRoleMap.put("Conflict", ConflictRole);

		HashSet<String> MovementRole = new HashSet<String>();
		MovementRole.addAll(Arrays.asList("Place", "Agent", "Time-Ending",
				"Origin", "Time-After", "Time-Starting", "Time-Before",
				"Time-Holds", "Time-Within", "Artifact", "Time-At-End",
				"Time-At-Beginning", "Destination", "Entity", "Vehicle"));
		typeRoleMap.put("Movement", MovementRole);

		return typeRoleMap;
	}

	public static void setSystemAttribute(ArrayList<EventMention> ems,
			HashMap<String, HashMap<String, String>> polarityMaps,
			HashMap<String, HashMap<String, String>> modalityMaps,
			HashMap<String, HashMap<String, String>> genericityMaps,
			HashMap<String, HashMap<String, String>> tenseMaps, String file) {
		for (EventMention em : ems) {
			String key = em.getAnchorStart() + "," + em.getAnchorEnd();
			em.polarity = polarityMaps.get(file).get(key);
			em.modality = modalityMaps.get(file).get(key);
			em.genericity = genericityMaps.get(file).get(key);
			em.tense = tenseMaps.get(file).get(key);
		}
	}

	public static void setSystemAttributeWithConf(
			ArrayList<EventMention> ems,
			HashMap<String, HashMap<String, HashMap<String, Double>>> polarityMaps,
			HashMap<String, HashMap<String, HashMap<String, Double>>> modalityMaps,
			HashMap<String, HashMap<String, HashMap<String, Double>>> genericityMaps,
			HashMap<String, HashMap<String, HashMap<String, Double>>> tenseMaps,
			String file) {
		for (EventMention em : ems) {
			String key = em.getAnchorStart() + "," + em.getAnchorEnd();
			// em.polarityConf.put(em.polarity, 1.0);
			// em.modalityConf.put(em.modality, 1.0);
			// em.genericityConf.put(em.genericity, 1.0);
			// em.tenseConf.put(em.tense, 1.0);
			em.polarityConf = polarityMaps.get(file).get(key);
			em.modalityConf = modalityMaps.get(file).get(key);
			em.genericityConf = genericityMaps.get(file).get(key);
			em.tenseConf = tenseMaps.get(file).get(key);
		}
	}

	/*
	 * Load All System Components
	 */
	public static ArrayList<EventMention> loadSystemComponents(ACEDoc doc) {
		ArrayList<EntityMention> entityMentions = new ArrayList<EntityMention>();
		entityMentions.addAll(getSieveCorefMentions(doc));

		// time mentions
		ArrayList<EntityMention> timeExpressions = getTimeExpressions(doc);
		// // value mentions
		ArrayList<EntityMention> valueExpressions = getValueExpression(doc);

		// event mentions
		ArrayList<EventMention> allEvents = getSystemEventMention(doc.fileID);

		for (EventMention em : allEvents) {
			em.setAnchor(doc.content
					.substring(em.getAnchorStart(), em.getAnchorEnd() + 1)
					.replace("\n", "").replace(" ", ""));
		}
		ArrayList<EntityMention> argumentCandidate = new ArrayList<EntityMention>();
		argumentCandidate.addAll(entityMentions);
		argumentCandidate.addAll(timeExpressions);
		argumentCandidate.addAll(valueExpressions);

		assignSemanticRole(allEvents, argumentCandidate, doc.semanticRoles);

		for (EventMention event : allEvents) {
//			assignSystemAttribute(doc.fileID, event, false);
		}
		assignArgumentWithEntityMentions(allEvents, entityMentions, timeExpressions, valueExpressions, doc);
		return allEvents;
	}
	
	public static void assignArgumentWithEntityMentions(ArrayList<EventMention> events, ArrayList<EntityMention> entityMentions,
			ArrayList<EntityMention> valueExpressions, ArrayList<EntityMention> timeExpressions, ACEDoc doc) {
		ArrayList<EntityMention> arguments = new ArrayList<EntityMention>();
		arguments.addAll(entityMentions);
		arguments.addAll(valueExpressions);
		arguments.addAll(timeExpressions);
		
		for(EntityMention  em : arguments) {
			setMentionType(em, doc);
			setGender(em, doc);
		}
		
		for(EventMention event : events) {
			for(EventMentionArgument argument : event.getEventMentionArguments()) {
				boolean find = false;
				
				for(EntityMention mention : arguments) {
					if((mention.start==argument.getStart() && mention.end==argument.getEnd()) ||
							(mention.headStart==argument.getStart() && mention.headEnd==argument.getEnd())){
						argument.mention = mention;
						find = true;
						break;
					}
				}
				if(!find) {
					EntityMention mention = new EntityMention();
					mention.headStart = argument.getStart();
					mention.headEnd = argument.getEnd();
					mention.head = doc.content.substring(argument.getStart(), argument.getEnd() + 1);
//					System.out.println(mention.head);
					mention.semClass = "time";
					mention.subType = "time";
					argument.mention = mention;
				}
			}
		}
		
		for(EventMention event : events) {
			calEventFeature(event, doc, arguments);
			calAttribute(event, doc);
			identBVs(event, doc);
			
			if(event.number!=Numb.SINGULAR) {
//				System.out.println(event.getAnchor());
			}
		}
	}
	
	public static void setMentionType(EntityMention mention, ACEDoc doc) {
		if (doc.getPostag(mention.headStart).startsWith("PN") || (Common.isPronoun(mention.head))) {
			mention.mentionType = MentionType.Pronominal;
			mention.isPronoun = true;
		} else if ((!mention.ner.equalsIgnoreCase("OTHER") && !mention.ner.equalsIgnoreCase("CARDINAL"))
				|| doc.getPostag(mention.headStart).startsWith("NR")) {
			mention.mentionType = MentionType.Proper;
			mention.isProperNoun = true;
		} else {
			mention.mentionType = MentionType.Nominal;
		}
		String head = mention.head;
		if (Common.getSemantic(head) == null) {
			mention.mentionType = MentionType.Proper;
		}
	}
	
	static ChDictionary dict = new ChDictionary();

	private static void setGender(EntityMention mention, ACEDoc doc) {
		mention.gender = Gender.UNKNOWN;
		if (mention.isPronoun) {
			if (dict.malePronouns.contains(mention.head.toLowerCase())) {
				mention.gender = Gender.MALE;
			} else if (dict.femalePronouns.contains(mention.head.toLowerCase())) {
				mention.gender = Gender.FEMALE;
			}
		} else {
			mention.gender = Gender.UNKNOWN;
		}
		if (mention.gender == Gender.UNKNOWN) {
			int yes = 0;
			int no = 0;
			Integer in;
			if ((in = dict.maleHead.get(mention.head)) != null) {
				yes = in.intValue();
			}
			if ((in = dict.femaleHead.get(mention.head)) != null) {
				no = in.intValue();
			}
			if (yes > no) {
				mention.gender = Gender.MALE;
			} else if (yes < no) {
				mention.gender = Gender.FEMALE;
			}
		}
	}
	
	static HashMap<String, String> pos2 = Common.readFile2Map2("dict/10POSDIC");
	
	public static void identBVs(EventMention em, ACEDoc doc) {
		String posTag = doc.getPostag(em.getAnchorStart());
		if (em.getAnchor().length() == 1 && posTag.equalsIgnoreCase("VV")
				) {
			em.bvs.put(em.getAnchor(), "BV");
		} else if (em.getAnchor().length() == 2) {
			String trigger = em.getAnchor();
			String str1 = Character.toString(trigger.charAt(0));
			String str2 = Character.toString(trigger.charAt(1));
			if (pos2.containsKey(str2) && pos2.get(str2).startsWith("V")) {
				if (pos2.containsKey(str1) && pos2.get(str1).startsWith("V")) {
					em.bvs.put(str2, "verb_BV");
				} else if (pos2.containsKey(str1) && pos2.get(str1).startsWith("N")) {
					em.bvs.put(str2, "np_BV");
				} else {
					em.bvs.put(str2, "adj_BV");
				}
			}
			if (pos2.containsKey(str1) && pos2.get(str1).startsWith("V")) {
				if (str2.equals("了")) {
					em.bvs.put(str1, "BV_comp");
				} else if (pos2.containsKey(str2) && pos2.get(str2).startsWith("V")) {
					em.bvs.put(str1, "BV_verb");
				} else if (pos2.containsKey(str2) && pos2.get(str2).startsWith("N")) {
					em.bvs.put(str1, "BV_np");
				} else {
					em.bvs.put(str1, "BV_adj");
				}
			}
		}
	}
	


	public static HashMap<String, HashMap<String, EventMention>> polarityMaps;
	public static HashMap<String, HashMap<String, EventMention>> tenseMaps;
	public static HashMap<String, HashMap<String, EventMention>> generecityMaps;
	public static HashMap<String, HashMap<String, EventMention>> modalityMaps;

	public static void assignSystemAttribute(String fileID,
			EventMention mention, boolean goldEvents) {
		if (polarityMaps == null) {
			polarityMaps = getSystemAtrribute("polarity", goldEvents);
		}
		if (tenseMaps == null) {
			tenseMaps = getSystemAtrribute("tense", goldEvents);
		}
		if (generecityMaps == null) {
			generecityMaps = getSystemAtrribute("genericity", goldEvents);
		}
		if (modalityMaps == null) {
			modalityMaps = getSystemAtrribute("modality", goldEvents);
		}
		fileID = fileID.replace(
				"/users/yzcchen/chen3/coling2012/LDC2006T06/data",
				"/users/yzcchen/ACL12/data/ACE2005")
				+ ".sgm";
		mention.polarity = polarityMaps.get(fileID).get(mention.toString()).polarity;
		mention.tense = tenseMaps.get(fileID).get(mention.toString()).tense;
		mention.genericity = generecityMaps.get(fileID).get(mention.toString()).genericity;
		mention.modality = modalityMaps.get(fileID).get(mention.toString()).modality;
	}

	public static HashMap<String, HashMap<String, EventMention>> getSystemAtrribute(
			String attribute, boolean goldEvents) {
		HashMap<String, HashMap<String, EventMention>> systemEMses = new HashMap<String, HashMap<String, EventMention>>();
		for (int folder = 0; folder < 5; folder++) {
			String f = Integer.toString(folder);

			String emFn = "/users/yzcchen/workspace/NAACL2013-B/src/data/"
					+ "joint_svm_systemEventMention_systemArgument_systemEntityMentions_systemSemantic"
					+ "/chinese_" + attribute + "_test_em" + f;

			String predicFn = "/users/yzcchen/tool/maxent/bin/"
					+ "joint_svm_systemEventMention_systemArgument_systemEntityMentions_systemSemantic"
					+ "/test_" + attribute + ".txt" + f;
			if (goldEvents) {
				emFn = "/users/yzcchen/workspace/NAACL2013-B/src/data/goldEventMentions/chinese_"
						+ attribute + "_test_em" + f;
				predicFn = "/users/yzcchen/tool/maxent/bin/goldEventMentions/test_"
						+ attribute + ".txt" + f;
			}

			ArrayList<String> emLines = Common.getLines(emFn);
			ArrayList<String> predictLines = Common.getLines(predicFn);
			for (int i = 0; i < emLines.size(); i++) {
				String predictLine = predictLines.get(i);
				String emLine = emLines.get(i);

				String tokens[] = emLine.split("\\s+");
				String file = tokens[0]
						.replace(
								"/users/yzcchen/chen3/coling2012/LDC2006T06/data/Chinese",
								"/users/yzcchen/ACL12/data/ACE2005/Chinese")
						+ ".sgm";
				int start = Integer.valueOf(tokens[1]);
				int end = Integer.valueOf(tokens[2]);
				EventMention em = new EventMention();
				em.setAnchorStart(start);
				em.setAnchorEnd(end);

				tokens = predictLine.split("\\s+");
				String label = "";
				double maxVal = -1;
				for (int k = 0; k < tokens.length / 2; k++) {
					String l = tokens[k * 2];
					double val = Double.valueOf(tokens[k * 2 + 1]);
					if (val > maxVal) {
						label = l;
						maxVal = val;
					}
				}
				HashMap<String, EventMention> map = systemEMses.get(file);
				if (map == null) {
					map = new HashMap<String, EventMention>();
					systemEMses.put(file, map);
				}
				map.put(em.toString(), em);
				try {
					em.getClass().getField(attribute).set(em, label);
				} catch (IllegalArgumentException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (SecurityException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (NoSuchFieldException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		return systemEMses;
	}

	public static void assignSemanticRole(
			ArrayList<EventMention> eventMentions,
			ArrayList<EntityMention> entityMentions,
			HashMap<EventMention, SemanticRole> roles) {
		if (eventMentions != null) {
			HashMap<Integer, EntityMention> entityMap = new HashMap<Integer, EntityMention>();

			for (EntityMention mention : entityMentions) {
				entityMap.put(mention.headEnd, mention);
			}
			for (EventMention mention : eventMentions) {
				if (roles.containsKey(mention)) {
					SemanticRole role = roles.get(mention);
					HashMap<String, ArrayList<EntityMention>> args = role.args;
					HashMap<String, ArrayList<EntityMention>> newArgs = new HashMap<String, ArrayList<EntityMention>>();
					for (String key : args.keySet()) {
						ArrayList<EntityMention> news = new ArrayList<EntityMention>();
						for (EntityMention old : args.get(key)) {
							if (entityMap.containsKey(old.headEnd)) {
								news.add(entityMap.get(old.headEnd));
							}
						}
						if (news.size() != 0) {
							newArgs.put(key, news);
						}
					}
					mention.srlArgs = newArgs;
				}
			}
		}
	}

	public static void main(String args[]) {
		Util.part = args[0];
		ArrayList<String> files = Common.getLines("ACE_Chinese_test"
				+ Util.part);
		double gold = 0;
		double sys = 0;
		double hit = 0;
		for (int i = 0; i < files.size(); i++) {
			String file = files.get(i);
			ACEDoc doc = new ACEChiDoc(file);
			doc.docID = i;
			ArrayList<EventMention> evms = loadSystemComponents(doc);

			ArrayList<EventMention> goldEvents = doc.goldEventMentions;
			gold += goldEvents.size();
			sys += evms.size();
			for (EventMention m : evms) {
				for (EventMention g : goldEvents) {
					if (m.equals(g)) {
						hit++;
					}
				}
			}
		}
		System.out.println("Prec: " + hit / sys);
		System.out.println("Reca: " + hit / gold);
	}

	static HashMap<String, HashMap<String, EventMention>> eventMentionsMap;

	private static ArrayList<EventMention> getSystemEventMention(String fileID) {
		if (eventMentionsMap == null) {
			eventMentionsMap = readAllSystemEventMention();
		}
		ArrayList<EventMention> evms = new ArrayList<EventMention>();
		
		String key = fileID;
		String os = System.getProperty("os.name");
		if(os.startsWith("Windows")) {
			int k = key.indexOf("Chinese");
			key = "/users/yzcchen/chen3/coling2012/LDC2006T06/data/" + key.substring(k).replace("\\", "/");
		}
		if (eventMentionsMap.containsKey(key)) {
			evms.addAll(eventMentionsMap.get(key).values());
		}
		return evms;
	}

	private static HashMap<String, HashMap<String, EventMention>> readAllSystemEventMention() {
		// if (pipelineResults == null) {
		// pipelineResults = readSystemPipelineEventMention();
		// }
		double svmTh = 0;
		eventMentionsMap = new HashMap<String, HashMap<String, EventMention>>();
		for (int folder = 0; folder < 5; folder++) {
			String inter = "joint_svm_systemEventMention_systemArgument_goldEntityMentions_goldSemantic/";
			// if(ACECommon.goldEventMention && ACECommon.goldEntityMention &&
			// ACECommon.goldSemantic) {
			// inter =
			// "joint_svm_goldEventMention_systemArgument_goldEntityMentions_goldSemantic/";
			// } else if(ACECommon.goldEventMention &&
			// ACECommon.goldEntityMention && !ACECommon.goldSemantic) {
			// inter =
			// "joint_svm_goldEventMention_systemArgument_goldEntityMentions_systemSemantic//";
			// } else if(ACECommon.goldEventArgument &&
			// !ACECommon.goldEntityMention && !ACECommon.goldSemantic) {
			// inter =
			// "joint_svm_goldEventMention_systemArgument_systemEntityMentions_systemSemantic///";
			// } else if(!ACECommon.goldEventArgument &&
			// ACECommon.goldEntityMention && ACECommon.goldSemantic) {
			// inter =
			// "joint_svm_systemEventMention_systemArgument_goldEntityMentions_goldSemantic/";
			// } else if(!ACECommon.goldEventArgument &&
			// !ACECommon.goldEntityMention && !ACECommon.goldSemantic) {
			inter = "joint_svm_systemEventMention_systemArgument_systemEntityMentions_systemSemantic/";
			// }
			String filename = "/users/yzcchen/workspace/NAACL2013-B/src/"
					+ inter + "/result" + Integer.toString(folder);
			String os = System.getProperty("os.name");
			if(os.startsWith("Windows")) {
				filename = "C:\\Users\\USER\\workspace\\BilingualEvent\\data\\joint_svm\\result" + Integer.toString(folder);
			}
			
			ArrayList<String> lines = Common.getLines(filename);
			int size = 0;
			HashMap<String, ACEDoc> documentCache = new HashMap<String, ACEDoc>();

			for (String line : lines) {
				String tokens[] = line.split("\\s+");

				String fileID = tokens[0];

				// ACEDoc document = documentCache.get(fileID);
				// if (document == null) {
				// document = new ACEChiDoc(fileID);
				// documentCache.put(fileID, document);
				// }

				HashMap<String, EventMention> eventMentions = eventMentionsMap
						.get(fileID);
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
				// temp.setAnchor(document.content.substring(emStart, emEnd +
				// 1).replace("\n", "").replace(" ", ""));
				temp.confidence = emConfidence;
				temp.type = type;
				temp.typeConfidence = typeConfidence;
				temp.subType = subType;

				if (temp.subType.equalsIgnoreCase("null")
						|| temp.confidence < svmTh) {
					continue;
				}

				// if (temp.subType.equalsIgnoreCase("null")) {
				// temp.subType =
				// pipelineResults.get(fileID).get(temp.toString()).subType;
				// System.err.println("GE: " + temp.subType);
				// }

				temp.subTypeConfidence = subTypeConfidence;

				EventMention eventMention = eventMentions.get(temp.toString());
				if (eventMention == null) {
					eventMention = temp;
					eventMentions.put(temp.toString(), eventMention);
					size++;
				}

				if (Integer.parseInt(tokens[8]) == -1) {
					ArrayList<Double> confidences = new ArrayList<Double>();
					for (int k = 13; k < tokens.length; k++) {
						confidences.add(Double.valueOf(tokens[k]));
					}
					// eventMention.typeConfidences = confidences;
					eventMention.inferFrom = tokens[9];
					continue;
				}

				EventMentionArgument argument = new EventMentionArgument();
				argument.setStart(Integer.parseInt(tokens[8]));
				argument.setEnd(Integer.parseInt(tokens[9]));
				argument.confidence = Double.parseDouble(tokens[10]);
				argument.setRole(tokens[11]);
				if (tokens[11].equalsIgnoreCase("null")) {
					continue;
				}
				argument.roleConfidence = Double.parseDouble(tokens[12]);
				argument.setEventMention(eventMention);
				ArrayList<Double> confidences = new ArrayList<Double>();
				for (int k = 13; k < tokens.length; k++) {
					confidences.add(Double.valueOf(tokens[k]));
				}
				argument.roleConfidences = confidences;
				eventMention.getEventMentionArguments().add(argument);
			}
		}
		return eventMentionsMap;
	}

	private static HashMap<String, ArrayList<EntityMention>> timeExpressions;

	private static ArrayList<EntityMention> getTimeExpressions(ACEDoc doc) {
		if (timeExpressions == null) {
			timeExpressions = getMentionsFromCRFFile(
					Common.getLines("ACE_Chinese_test" + Util.part),
					"yy_time" + Util.part);
		}
		String key = doc.fileID;
		String os = System.getProperty("os.name");
		if(os.startsWith("Windows")) {
			int k = key.indexOf("Chinese");
			key = "/users/yzcchen/chen3/coling2012/LDC2006T06/data/" + key.substring(k).replace("\\", "/");
		}
		return timeExpressions.get(key);
	}

	private static HashMap<String, ArrayList<EntityMention>> valueExpressions;

	public static ArrayList<EntityMention> getValueExpression(ACEDoc doc) {
		if (valueExpressions == null) {
			valueExpressions = getMentionsFromCRFFile(
					Common.getLines("ACE_Chinese_test" + Util.part),
					"yy_value" + Util.part);
		}
		String key = doc.fileID;
		String os = System.getProperty("os.name");
		if(os.startsWith("Windows")) {
			int k = key.indexOf("Chinese");
			key = "/users/yzcchen/chen3/coling2012/LDC2006T06/data/" + key.substring(k).replace("\\", "/");
		}
		return valueExpressions.get(key);
	}

	public static HashMap<String, ArrayList<EntityMention>> getMentionsFromCRFFile(
			ArrayList<String> files, String crfFile) {
		ArrayList<ArrayList<EntityMention>> entityMentionses = new ArrayList<ArrayList<EntityMention>>();
		ArrayList<String> lines = Common.getLines(crfFile);
		int fileIdx = 0;
		ACEDoc doc = new ACEChiDoc(files.get(fileIdx));
		int idx = doc.start - 1;
		String content = doc.content;
		int start = 0;
		int end = 0;
		int lastIdx = 0;
		ArrayList<EntityMention> currentArrayList = new ArrayList<EntityMention>();
		entityMentionses.add(currentArrayList);
		loop: for (int i = 0; i < lines.size();) {
			String line = lines.get(i);
			if (line.trim().isEmpty()) {
				i++;
				continue;
			}

			idx = content.indexOf(line.charAt(0), idx + 1);
			// System.out.println(line);
			if (idx == -1 || idx > doc.end) {
				fileIdx++;
				currentArrayList = new ArrayList<EntityMention>();
				entityMentionses.add(currentArrayList);
				// System.out.println(files.get(fileIdx));
				doc = new ACEChiDoc(files.get(fileIdx));
				idx = 0;
				content = doc.content;
				continue;
			}
			i++;
			if (line.endsWith("B")) {
				start = idx;
				while (true) {
					lastIdx = idx;
					if (!lines.get(i).endsWith("I") || lines.get(i).isEmpty()) {
						break;
					}
					idx = content
							.indexOf(lines.get(i++).charAt(0), lastIdx + 1);
				}
				end = lastIdx;
				EntityMention em = new EntityMention();
				// using head to do co-reference
				// System.out.println(start + "," + end);
				em.head = content.substring(start, end + 1)
						.replaceAll("\\s+", "").replace("\n", "")
						.replace("\r", "");
				// System.out.println(start + "," + end + "$" + em.head);
				em.headStart = start;
				em.headEnd = end;
				currentArrayList.add(em);

				if (crfFile.contains("time")) {
					em.semClass = "time";
					em.subType = "time";
				} else if (crfFile.contains("value")) {
					em.semClass = "value";
					em.subType = "value";
				}

			}
		}
		HashMap<String, ArrayList<EntityMention>> maps = new HashMap<String, ArrayList<EntityMention>>();
		for (int i = 0; i < files.size(); i++) {
			maps.put(files.get(i), entityMentionses.get(i));
		}
		return maps;
	}

	private static ArrayList<EntityMention> getSieveCorefMentions(ACEDoc doc) {
		// /users/yzcchen/chen3/conll12/chinese/goldEntityMentions/
		String baseFolder = "/users/yzcchen/chen3/conll12/chinese/systemEntityMentions/ACE_test_"
				+ Util.part + "/";
		
		String os = System.getProperty("os.name");
		if(os.startsWith("Windows")) {
			baseFolder = "C:\\Users\\USER\\workspace\\BilingualEvent\\data\\ACE_test_" + Util.part + "\\";
		}
		
		ArrayList<String> lines = Common.getLines(baseFolder + doc.docID
				+ ".entities.sieve.entity");
		ArrayList<EntityMention> allMentions = new ArrayList<EntityMention>();

		ArrayList<Entity> entities = new ArrayList<Entity>();

		for (String line : lines) {
			Entity entity = new Entity();
			String tokens[] = line.split("\\s+");
			for (String token : tokens) {
				String pos[] = token.split(",");
				EntityMention mention = new EntityMention();
				int charStart = Integer.valueOf(Integer.valueOf(pos[0]));
				int charEnd = Integer.valueOf(Integer.valueOf(pos[1]));
				mention.start = charStart;
				mention.end = charEnd;

				mention.headStart = charStart;
				mention.headEnd = charEnd;
				mention.entity = entity;
				mention.head = doc.content.substring(mention.headStart,
						mention.headEnd + 1);

				assignSystemSemantic(mention, doc.fileID);
				allMentions.add(mention);
			}
			entities.add(entity);
		}
		doc.setEntityCorefMap(entities);

		return allMentions;
	}

	static HashMap<String, ArrayList<EntityMention>> allSemanticResult;

	private static void assignSystemSemantic(EntityMention mention,
			String fileID) {
		if (allSemanticResult == null) {
			allSemanticResult = loadSemanticResult();
		}
		String stem = fileID.substring(fileID.indexOf("Chinese"));
		String key = "/users/yzcchen/ACL12/data/ACE2005/" + stem + ".sgm";
		key = key.replace("\\", "/");
		ArrayList<EntityMention> systems = allSemanticResult.get(key);
		boolean find = false;
		for (EntityMention system : systems) {
			if (system.headStart == mention.headStart
					&& system.headEnd == mention.headEnd) {
				mention.subType = system.subType;
				mention.semClass = system.semClass;
				find = true;
				break;
			}
		}
		if (!find) {
			System.err.println("GEE");
			Common.bangErrorPOS("");
			System.exit(1);
		}
	}

	public static ArrayList<String> semClasses = new ArrayList<String>(
			Arrays.asList("wea", "veh", "per", "fac", "gpe", "loc", "org"));
	public static ArrayList<String> semSubTypes = new ArrayList<String>(
			Arrays.asList("f-airport", "f-building-grounds", "f-path",
					"f-plant", "f-subarea-facility", "g-continent",
					"g-county-or-district", "g-gpe-cluster", "g-nation",
					"g-population-center", "g-special", "g-state-or-province",
					"l-address", "l-boundary", "l-celestial",
					"l-land-region-natural", "l-region-general",
					"l-region-international", "l-water-body", "o-commercial",
					"o-educational", "o-entertainment", "o-government",
					"o-media", "o-medical-science", "o-non-governmental",
					"o-religious", "o-sports", "p-group", "p-indeterminate",
					"p-individual", "v-air", "v-land", "v-subarea-vehicle",
					"v-underspecified", "v-water", "w-biological", "w-blunt",
					"w-chemical", "w-exploding", "w-nuclear", "w-projectile",
					"w-sharp", "w-shooting", "w-underspecified", "o-other"));

	public static HashMap<String, ArrayList<EntityMention>> loadSemanticResult() {
		HashMap<String, ArrayList<EntityMention>> allSVMResult = new HashMap<String, ArrayList<EntityMention>>();
		// /users/yzcchen/chen3/conll12/chinese/semantic_gold_mention
		String folder = "/users/yzcchen/ACL12/model/ACE2005/semantic_system_mention/";
		
		String os = System.getProperty("os.name");
		if(os.startsWith("Windows")) {
			folder = "C:\\Users\\USER\\workspace\\BilingualEvent\\data\\semantic_system_mention\\";
		}
		ArrayList<String> mentionStrs = Common.getLines(folder + "mention.test"
				+ Util.part);
		ArrayList<String> typeResult = Common.getLines(folder
				+ "multiType.result2" + Util.part);
		ArrayList<String> subTypeResult = Common.getLines(folder
				+ "multiSubType.result2" + Util.part);

		for (int i = 0; i < mentionStrs.size(); i++) {
			String mentionStr = mentionStrs.get(i);
			String fileKey = mentionStr.split("\\s+")[1];
			String startEndStr = mentionStr.split("\\s+")[0];
			int headStart = Integer.valueOf(startEndStr.split(",")[0]);
			int headEnd = Integer.valueOf(startEndStr.split(",")[1]);
			EntityMention em = new EntityMention();
			em.headStart = headStart;
			em.headEnd = headEnd;

			int typeIndex = Integer.valueOf(typeResult.get(i).split("\\s+")[0]);
			int subTypeIndex = Integer.valueOf(subTypeResult.get(i).split(
					"\\s+")[0]);

			em.semClass = semClasses.get(typeIndex - 1);
			em.subType = semSubTypes.get(subTypeIndex - 1);

			if (allSVMResult.containsKey(fileKey)) {
				allSVMResult.get(fileKey).add(em);
			} else {
				ArrayList<EntityMention> ems = new ArrayList<EntityMention>();
				ems.add(em);
				allSVMResult.put(fileKey, ems);
			}
		}
		return allSVMResult;
	}

	public static void calAttribute(EventMention em, ACEDoc doc) {
		em.number = Numb.SINGULAR;
		em.posTag = doc.getPostag(em.getAnchorStart());

		if (em.posTag.equals("NN")) {
			em.noun = true;
			calEventNounAttribute(em, doc);
			// System.err.println(em.head + "#" + em.modifyList + "#" +
			// em.number + "#" + em.goldChainID);
		}

		for (EventMentionArgument arg : em.eventMentionArguments) {
			ArrayList<EventMentionArgument> args = em.argHash.get(arg.role);
			if (args == null) {
				args = new ArrayList<EventMentionArgument>();
				em.argHash.put(arg.role, args);
			}
			args.add(arg);
		}
	}

	public static void calEventFeature(EventMention eventMention, ACEDoc doc,
			ArrayList<EntityMention> argumentCandidate) {
		if (isZeroPronoun(eventMention, doc, argumentCandidate)) {
			boolean pasive = false;
			if (eventMention.getAnchor().startsWith("被")
					|| (eventMention.getAnchorStart() > 0 && doc.content
							.charAt(eventMention.getAnchorStart() - 1) == '被')) {
				pasive = true;
			}
			if (pasive) {
				if (!eventMention.srlArgs.containsKey("A1")) {
					ArrayList<EntityMention> srlRoles = new ArrayList<EntityMention>();
					srlRoles.add(eventMention.zeroSubjects.get(0));
					eventMention.srlArgs.put("A1", srlRoles);
				}
			} else {
				if (!eventMention.srlArgs.containsKey("A0")) {
					ArrayList<EntityMention> srlRoles = new ArrayList<EntityMention>();
					srlRoles.add(eventMention.zeroSubjects.get(0));
					eventMention.srlArgs.put("A0", srlRoles);
				}
			}
		}
	}

	// identify zero and recognize its antecedent
	public static boolean isZeroPronoun(EventMention eventMention, ACEDoc doc,
			ArrayList<EntityMention> candidateMentions) {
		int position[] = doc.positionMap.get(eventMention.getAnchorStart());
		int wordPos = position[1];
		// if (wordPos < 2) {
		// eventMention.isZeroPronoun = -1;
		// return false;
		// }
		ParseResult pr = doc.parseReults.get(position[0]);
		MyTreeNode leaf = doc.parseReults.get(position[0]).tree.leaves.get(wordPos);
		if (leaf.parent.value.equalsIgnoreCase("vv")
				&& leaf.parent.parent.value.equalsIgnoreCase("vp")) {
			ArrayList<MyTreeNode> beforeSisters = leaf.parent.parent
					.getLeftSisters();
			boolean NP = false;
			for (MyTreeNode sister : beforeSisters) {
				if (sister.value.equalsIgnoreCase("np")) {
					NP = true;
				}
			}
			if (!NP) {
				eventMention.isZeroPronoun = 1;
				eventMention.zeroSubjects = new ArrayList<EntityMention>();
				for (int p = wordPos - 1; p >= 1; p--) {
					if (pr.words.get(p).equals("，") || p == 1) {
						boolean got = false;
						ArrayList<EntityMention> mentions = new ArrayList<EntityMention>();
						int start = pr.positions.get(1)[0];
						int end = eventMention.getAnchorEnd() - 1;

						for (EntityMention candidate : candidateMentions) {
							if (candidate.headStart >= start
									&& candidate.headEnd <= end
									&& !candidate.semClass.equals("time")) {
								eventMention.zeroSubjects.add(candidate);
								got = true;
							}
						}
						Collections.sort(eventMention.zeroSubjects);
						Collections.reverse(eventMention.zeroSubjects);
						// 的
						ArrayList<MyTreeNode> ancestors = leaf.getAncestors();
						MyTreeNode npAncestor = null;
						for (MyTreeNode node : ancestors) {
							if (node.value.equalsIgnoreCase("np")) {
								npAncestor = node;
								break;
							}
						}
						if (npAncestor != null) {
							ArrayList<MyTreeNode> leafs = npAncestor
									.getLeaves();
							MyTreeNode deLeaf = null;
							for (MyTreeNode tmp : leafs) {
								if (tmp.value.equals("的")) {
									deLeaf = tmp;
									break;
								}
							}
							if (deLeaf != null) {
								end = pr.positions.get(leafs.get(leafs
										.size() - 1).leafIdx)[1];
								EntityMention zero = null;
								for (EntityMention candidate : candidateMentions) {
									if (candidate.headEnd == end) {
										zero = candidate;
										break;
									}
								}
								if (zero != null) {
									eventMention.zeroSubjects.add(0, zero);
									got = true;
								}
							}
						}
						// 中华民国 and 中华民国政府
						if (eventMention.zeroSubjects.size() != 0) {
							EntityMention zero = eventMention.zeroSubjects
									.get(0);
							for (EntityMention mention : mentions) {
								if (mention.start == zero.start
										&& mention.end > zero.end) {
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
	}

	public static void calEventNounAttribute(EventMention em, ACEDoc doc) {

		int position[] = doc.positionMap.get(em.getAnchorStart());
		ParseResult pr = doc.parseReults.get(position[0]);
		ArrayList<Depend> depends = doc.parseReults.get(position[0]).depends;
		
		HashSet<String> nonModifyPOS = new HashSet<String>(Arrays.asList("DEG",
				"P", "CC", "DT", "M", "LC", "DEC", "VV"));

		for (Depend depend : depends) {
			String type = depend.type;
			int wordIdx1 = depend.first;
			int wordIdx2 = depend.second;
			if ((type.endsWith("mod") || type.equals("nn"))
					&& wordIdx1 == position[1]) {
				String word2 = pr.words.get(wordIdx2);
				if (nonModifyPOS.contains(pr.posTags.get(wordIdx2))) {
					continue;
				}
				if (pr.posTags.get(wordIdx2).equals("CD") && !isNumber(word2)) {
					em.CD = pr.words.get(wordIdx2);
					continue;
				}

				if (!em.getAnchor().contains(word2) && !word2.contains(em.getAnchor())) {
					em.modifyList.add(word2);
				}
			}
		}

		for (MyTreeNode leaf : getMaxNPTreeNode(em.getAnchorStart(), doc).getLeaves()) {
			if (!leaf.value.contains(em.getAnchor())
					&& !em.modifyList.contains(leaf.value)
					&& !nonModifyPOS.contains(leaf.parent.value)
					&& !em.getAnchor().contains(leaf.value)) {
				if (leaf.parent.value.equals("CD") && !isNumber(leaf.value)) {
					em.CD = leaf.value;
					continue;
				}
				em.modifyList.add(leaf.value);
			}
		}

		for (String str : em.modifyList) {
			if (pluralModify.contains(str)) {
				em.number = Numb.PLURAL;
			}
		}
	}
	
	public static MyTreeNode getMaxNPTreeNode(int idx, ACEDoc doc) {
		int position[] = doc.positionMap.get(idx);
		ArrayList<MyTreeNode> leaves = doc.parseReults.get(position[0]).tree.leaves;
		MyTreeNode rightNp = leaves.get(position[1]);
		ArrayList<MyTreeNode> rightAncestors = rightNp.getAncestors();
		MyTreeNode NP = null;
		for (int i = rightAncestors.size() - 1; i >= 0; i--) {
			MyTreeNode tmp = rightAncestors.get(i);
			ArrayList<MyTreeNode> tmpLeaves = tmp.getLeaves();
			if ((tmp.value.toLowerCase().startsWith("np") || tmp.value.toLowerCase().startsWith("qp"))
					&& tmpLeaves.get(tmpLeaves.size() - 1).leafIdx == position[1]) {
				NP = tmp;
			}
		}
		if (NP == null) {
			NP = rightNp.parent;
		}
		return NP;
	}

	public static final Set<String> pluralModify = new HashSet<String>(
			Arrays.asList("几", "多", "不少", "很多", "一些", "有些", "部分", "多数", "少数",
					"更多", "更少", "所有", "５", "大量"));

	public static boolean isNumber(String str) {
		try {
			Integer i = Integer.valueOf(str);
			return true;
		} catch (Exception e) {
			return false;
		}
	}
	
	public static boolean conflictArg_(EventMention ant, EventMention em, String role) {
		if (em.argHash.containsKey(role) && ant.argHash.containsKey(role)) {
			boolean conflict = false;
			for (EventMentionArgument arg1 : em.argHash.get(role)) {
				EntityMention m1 = arg1.mention;
				for (EventMentionArgument arg2 : ant.argHash.get(role)) {
					EntityMention m2 = arg2.mention;
					if (m1.entity != m2.entity) {
						conflict = true;
					} else {
						return false;
					}
				}
			}
			return conflict;
		}
		return false;
	}
	
	public static boolean _conflictSubType_(EventMention ant, EventMention em) {
		boolean conflict = !em.subType.equals(ant.subType) && !em.getAnchor().equals(ant.getAnchor());
		return conflict;
	}
	
	public static boolean _conflictOverlap_(EventMention ant, EventMention em, ACEDoc doc) {
		return ant.getAnchorEnd() >= em.getAnchorStart();
	}
	
	public static boolean _conflictNumber_(EventMention ant, EventMention em) {
		return em.number != ant.number;
	}
	
	public static boolean _conflictPersonArgument_(EventMention ant, EventMention em, ACEDoc doc) {
		boolean conflict = false;
		loop: for (String role1 : em.argHash.keySet()) {
			for (String role2 : ant.argHash.keySet()) {
				if (role1.equalsIgnoreCase(role2)) {
					ArrayList<EventMentionArgument> arg1 = em.argHash.get(role1);
					ArrayList<EventMentionArgument> arg2 = ant.argHash.get(role2);

					if (arg1.size() != 1 || arg2.size() != 1) {
						continue;
					}
					if (!arg1.get(0).mention.semClass.equalsIgnoreCase("per")
							|| !arg2.get(0).mention.semClass.equalsIgnoreCase("per")) {
						continue;
					}

					if (arg1.get(0).mention.entity != arg2.get(0).mention.entity) {
						if (personCompatible(arg1.get(0).mention, arg2.get(0).mention, doc)) {
						} else {
							conflict = true;
							break loop;
						}
					}
				}
			}
		}
		// arg0, arg1
		if (em.srlArgs.containsKey("A0") && ant.srlArgs.containsKey("A0")) {
			EntityMention m1 = em.srlArgs.get("A0").get(0);
			EntityMention m2 = ant.srlArgs.get("A0").get(0);
			if (m1.semClass.equalsIgnoreCase("per") && m2.semClass.equalsIgnoreCase("per")
					&& !personCompatible(m1, m2, doc)) {
				conflict = true;
			}
		}
		if (em.srlArgs.containsKey("A1") && ant.srlArgs.containsKey("A1")) {
			EntityMention m1 = em.srlArgs.get("A1").get(0);
			EntityMention m2 = ant.srlArgs.get("A1").get(0);
			if (m1.semClass.equalsIgnoreCase("per") && m2.semClass.equalsIgnoreCase("per")
					&& !personCompatible(m1, m2, doc)) {
				conflict = true;
			}
		}
		if (conflict) {
			return true;
		} else {
			return false;
		}
	}
	
	private static boolean personCompatible(EntityMention em, EntityMention ant, ACEDoc doc) {
		if (em.gender == Gender.MALE && ant.gender == Gender.FEMALE) {
			return false;
		}
		if (em.gender == Gender.FEMALE && ant.gender == Gender.MALE) {
			return false;
		}
		EntityMention m = em;
		EntityMention an = ant;
		if (m.mentionType == MentionType.Proper && an.mentionType == MentionType.Proper && !m.head.equals(an.head)) {
			return false;
		}
		String value1 = "";
		for (MyTreeNode node : Util.getMaxNPTreeNode(m.headStart, doc).getLeaves()) {
			if (node.parent.value.equals("CD")) {
				value1 = node.value;
			}
		}
		String value2 = "";
		for (MyTreeNode node : Util.getMaxNPTreeNode(an.headStart, doc).getLeaves()) {
			if (node.parent.value.equals("CD")) {
				value1 = node.value;
			}
		}
		HashMap<String, Integer> cluster = new HashMap<String, Integer>();
		cluster.put("3", 3);
		cluster.put("三", 3);
		if (!value1.isEmpty()
				&& !value2.isEmpty()
				&& !value1.equals(value2)
				&& !(cluster.containsKey(value1) && cluster.containsKey(value2) && cluster.get(value1) == cluster
						.get(value2)) && em.ner.equals("CARDINAL") && ant.ner.equals("CARDINAL")) {
			return false;
		}
		return true;
	}
	
	public static boolean _conflictValueArgument_(EventMention ant, EventMention em) {
		boolean conflict = false;
		loop: for (String role1 : em.argHash.keySet()) {
			for (String role2 : ant.argHash.keySet()) {
				if (role1.equalsIgnoreCase(role2)) {
					ArrayList<EventMentionArgument> arg1 = em.argHash.get(role1);
					ArrayList<EventMentionArgument> arg2 = ant.argHash.get(role2);
					boolean extra1 = false;
					boolean extra2 = false;
					for (EventMentionArgument a1 : arg1) {
						EntityMention m1 = a1.mention;
						if (!m1.semClass.equalsIgnoreCase("value")) {
							continue;
						}
						boolean extra = true;
						for (EventMentionArgument a2 : arg2) {
							EntityMention m2 = a2.mention;
							if (!m2.semClass.equalsIgnoreCase("value")) {
								continue;
							}
							if (m2.head.contains(m1.head)) {
								extra = false;
								break;
							}
						}
						if (extra) {
							extra1 = true;
							break;
						}
					}

					for (EventMentionArgument a2 : arg2) {
						EntityMention m2 = a2.mention;
						if (!m2.semClass.equalsIgnoreCase("value")) {
							continue;
						}
						boolean extra = true;
						for (EventMentionArgument a1 : arg1) {
							EntityMention m1 = a1.mention;
							if (!m1.semClass.equalsIgnoreCase("value")) {
								continue;
							}
							if (m1.head.contains(m2.head)) {
								extra = false;
								break;
							}
						}
						if (extra) {
							extra2 = true;
							break;
						}
					}
					if (extra1 && extra2) {
						conflict = true;
						break loop;
					}
				}
			}
		}
		if (conflict) {
			return true;
		} else {
			return false;
		}
	}
	
	public static boolean _conflictTimeArgument_(EventMention ant, EventMention em) {
		boolean conflict = false;
		loop: for (String role1 : em.argHash.keySet()) {
			for (String role2 : ant.argHash.keySet()) {
				if (role1.equalsIgnoreCase(role2)) {
					ArrayList<EventMentionArgument> arg1 = em.argHash.get(role1);
					ArrayList<EventMentionArgument> arg2 = ant.argHash.get(role2);
					boolean extra1 = false;
					boolean extra2 = false;
					for (EventMentionArgument a1 : arg1) {
						EntityMention m1 = a1.mention;
						if (!m1.semClass.equalsIgnoreCase("time")) {
							continue;
						}
						boolean extra = true;
						for (EventMentionArgument a2 : arg2) {
							EntityMention m2 = a2.mention;
							if (!m2.semClass.equalsIgnoreCase("time")) {
								continue;
							}
							if (m2.head.contains(m1.head) || m1.head.contains(m2.head)) {
								extra = false;
								break;
							}
						}
						if (extra) {
							extra1 = true;
							break;
						}
					}

					for (EventMentionArgument a2 : arg2) {
						EntityMention m2 = a2.mention;
						if (!m2.semClass.equalsIgnoreCase("time")) {
							continue;
						}
						boolean extra = true;
						for (EventMentionArgument a1 : arg1) {
							EntityMention m1 = a1.mention;
							if (!m1.semClass.equalsIgnoreCase("time")) {
								continue;
							}
							if (m2.head.contains(m1.head) || m1.head.contains(m2.head)) {
								extra = false;
								break;
							}
						}
						if (extra) {
							extra2 = true;
							break;
						}
					}
					if (extra1 && extra2) {
						conflict = true;
						break loop;
					}
				}
			}
		}
		if (conflict) {
			return true;
		} else {
			return false;
		}
	}
	
	public static boolean _conflictDestination_(EventMention ant, EventMention em, ACEDoc doc) {
		if (em.argHash.containsKey("Destination") && ant.argHash.containsKey("Destination")) {
			boolean conflict = false;
			for (EventMentionArgument arg1 : em.argHash.get("Destination")) {
				EntityMention m1 = arg1.mention;
				if (m1.ner.equalsIgnoreCase("other")) {
					continue;
				}
				for (EventMentionArgument arg2 : ant.argHash.get("Destination")) {
					EntityMention m2 = arg2.mention;
					if (m2.ner.equalsIgnoreCase("other")) {
						continue;
					}
					if (m1.entity != m2.entity) {
						conflict = true;
					} else {
						return false;
					}
				}
			}
			return conflict;
		}
		return false;
	}
	
	public static boolean _conflictModify_(EventMention ant, EventMention em, ACEDoc doc) {
		if (!em.modifyList.containsAll(ant.modifyList) && !ant.modifyList.containsAll(em.modifyList)) {
			return true;
		}
		return false;
	}
	
	public static boolean _conflictACERoleSemantic_(EventMention ant, EventMention em) {
		boolean conflict = false;
		for (String role1 : em.argHash.keySet()) {
			for (String role2 : ant.argHash.keySet()) {
				if (role1.equalsIgnoreCase(role2)) {
					ArrayList<EventMentionArgument> arg1 = em.argHash.get(role1);
					ArrayList<EventMentionArgument> arg2 = ant.argHash.get(role2);

					if (arg1.size() != 1 || arg2.size() != 1) {
//						continue;
					}
//					EntityMention m1 = arg1.get(0).mention;
//					EntityMention m2 = arg2.get(0).mention;
					conflict = true;
					for(EventMentionArgument a1 : arg1) {
						for(EventMentionArgument a2 : arg2) {
							EntityMention m1 = a1.mention;
							EntityMention m2 = a2.mention;
							if (m1.semClass.equals(m2.semClass)) {
								conflict = false;
							}
						}
					}
					
					if(conflict) {
						return true;
					}
				}
			}
		}
		return conflict;
	}
	
	public static boolean highPrecissionNegativeConstraint(EventMention ant, EventMention em) {
		if (Util._conflictSubType_(ant, em)) {
			return true;
		}
		if (Util._conflictACERoleSemantic_(ant, em)) {
			return true;
		}
		if (Util._conflictNumber_(ant, em)) {
			return true;
		}
		if (Util._conflictValueArgument_(ant, em)) {
			return true;
		}
		ArrayList<String> discreteRoles = new ArrayList<String>(Arrays.asList("Place", "Org", "Position",
				"Adjudicator", "Origin", "Giver", "Recipient", "Defendant", "Destination", "Person", "Victim"));
		for (String role : discreteRoles) {
			if (Util.conflictArg_(ant, em, role)) {
				return true;
			}
		}
		
		
//		if (_conflictPersonArgument_(ant, em, doc)) {
//		return true;
//	}
//		if(ant.CD!=null && em.CD!=null && !ant.CD.equals(em.CD)) {
//			return true;
//		}
//		if (_conflictOverlap_(ant, em, doc)) {
//			System.out.println("Conflict Overlap: " + ant.getAnchor() + "#" + em.getAnchor());
//			return true;
//		}
//		if (_conflictDestination_(ant, em, doc)) {
//			return true;
//		}
		
//		if(_conflictModify_(ant, em, doc)) {
//			return true;
//		}
		
		return false;
	}
	
	public static boolean _commonBV_(EventMention ant, EventMention em) {
		if (ant.getAnchor().equals(em.getAnchor())) {
			return false;
		}
		// common character
		boolean common = false;
		loop: for (int i = 0; i < ant.getAnchor().length(); i++) {
			for (int j = 0; j < em.getAnchor().length(); j++) {
				if (ant.getAnchor().charAt(i) == em.getAnchor().charAt(j)) {
					common = true;
					break loop;
				}
			}
		}
		// same meaning
		/*
		 * // String[] sem1 = Common.getSemantic(em.head); // String[] sem2 =
		 * Common.getSemantic(an.head); // if(sem1!=null && sem2!=null) { //
		 * for(String s1 : sem1) { // for(String s2 : sem2) { //
		 * if(s1.equals(s2) && s1.endsWith("=")) { // return true; // } // } //
		 * } // }
		 */

		if (common) {
			if (!conflictBV(ant, em))
				return true;
//			} else {
				// EventMention gEM =
				// RuleCoref.goldEventMentionMap.get(em.toString());
				// EventMention gAn =
				// RuleCoref.goldEventMentionMap.get(an.toString());
				// if (gEM != null && gAn != null && gEM.goldChainID ==
				// gAn.goldChainID) {
				// RuleCoref.printPair(em, an);
				// }
//			}
		}
		return false;
	}
	
	public static boolean conflictBV(EventMention em, EventMention an) {
		if (em.getAnchor().equals(an.getAnchor())) {
			return false;
		}
		for (String bv1 : em.bvs.keySet()) {
			String pattern1 = em.bvs.get(bv1);
			int idx1 = em.getAnchor().indexOf(bv1);
			for (String bv2 : an.bvs.keySet()) {
				String pattern2 = an.bvs.get(bv2);
				int idx2 = an.getAnchor().indexOf(bv2);
				if (bv1.equals(bv2)) {
					if (idx1 != idx2 && em.getAnchor().length() != 1 && an.getAnchor().length() != 1) {
//						System.out.println("Conflict: " + em.getAnchor() + "#" + an.getAnchor());
						return true;
					}
					if (pattern1.equals(pattern2) && (pattern1.equals("verb_BV") || pattern2.equals("BV_verb"))) {
						return true;
					}

					if (pattern1.equals(pattern2) && pattern1.equals("adj_BV")) {
						return true;
					}

					if ((pattern1.equals("adj_BV#BV") && pattern1.equals("BV"))
							|| (pattern1.equals("BV") && pattern1.equals("adj_BV#BV"))) {
						return true;
					}
					return false;
				}
			}
		}
		return true;
	}
}
