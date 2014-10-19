package event.attribute;

import java.util.ArrayList;

import model.ACEChiDoc;
import model.EventMention;
import model.EventMentionArgument;
import model.ParseResult;
import model.syntaxTree.MyTreeNode;
import util.ChineseUtil;
import util.Common;
import event.preProcess.ChineseTriggerIndent;

public class EventAttriFeature {

	public String convert(ArrayList<String> features, String label) {
		StringBuilder sb = new StringBuilder();
		sb.append(label);
		for (String feature : features) {
			sb.append(" ").append(feature);
		}
		return sb.toString().trim();
	}

	public String buildFeature(EventMention em, ACEChiDoc document, String label) {
		ArrayList<String> features = new ArrayList<String>();
		String trigger = em.getAnchor();
		int position[] = ChineseUtil.findParseFilePosition(em.getAnchorStart(), em.getAnchorEnd(), document);
		ParseResult pr = document.parseReults.get(position[0]);
		ArrayList<String> words = pr.words;
		ArrayList<String> posTags = pr.posTags;

		String pos = posTags.get(position[3]);

		String wordL1 = "-";
		String wordL2 = "-";
		String posL1 = "-";
		String posL2 = "-";
		String wordR1 = "-";
		String wordR2 = "-";
		String posR1 = "-";
		String posR2 = "-";

		if (position[1] >= 3) {
			wordL2 = words.get(position[1] - 2);
			posL2 = posTags.get(position[1] - 2);
		}
		if (position[1] >= 2) {
			wordL1 = words.get(position[1] - 1);
			posL1 = posTags.get(position[1] - 1);
		}
		if (position[3] <= words.size() - 3) {
			wordR2 = words.get(position[3] + 2);
			posR2 = posTags.get(position[3] + 2);
		}
		if (position[3] <= words.size() - 2) {
			wordR1 = words.get(position[3] + 1);
			posR1 = posTags.get(position[3] + 1);
		}

		features.add(trigger);
		features.add(pos);
		features.add(em.type);
		features.add(em.subType);
		features.add(wordL2);
		features.add(wordL1);
		features.add(wordR1);
		features.add(wordR2);

		features.add(posL2);
		features.add(posL1);
		features.add(posR1);
		features.add(posR2);

		if (classifier.equals("polarity")) {
			features.addAll(this.polarity(em, pr, document));
		} else if (classifier.equals("modality")) {
			features.addAll(this.modality(em, pr, document));
		} else if (classifier.equals("genericity")) {
			features.addAll(this.genericity(em, pr, document));
		} else if (classifier.equals("tense")) {
			features.addAll(this.tense(em, pr, document));
		}

		return convert(features, label);
	}

	static ArrayList<String> polarities = Common.getLines("polarity");
	static ArrayList<String> modalities = Common.getLines("modality");
	static ArrayList<String> genericities = Common.getLines("genericity");
	static ArrayList<String> tenses = Common.getLines("tense");

	public ArrayList<String> polarity(EventMention em, ParseResult pr, ACEChiDoc document) {
		ArrayList<String> features = new ArrayList<String>();
		int position[] = ChineseUtil.findParseFilePosition(em.getAnchorStart(), em.getAnchorEnd(), document);
		MyTreeNode leaf = pr.tree.leaves.get(position[3]);
		ArrayList<MyTreeNode> ancestors = leaf.getAncestors();
		String clause = "";
		for (MyTreeNode tmp : ancestors) {
			if (tmp.value.equals("IP")) {
				clause = tmp.toString();
				break;
			}
		}
		String negativeWord = "false";
		for (String negative : polarities) {
			if (clause.indexOf(negative) != -1) {
				negativeWord = "true";
				break;
			}
		}
		features.add(negativeWord);
		return features;
	}

	public ArrayList<String> modality(EventMention em, ParseResult pr, ACEChiDoc document) {
		ArrayList<String> features = new ArrayList<String>();
		int position[] = ChineseUtil.findParseFilePosition(em.getAnchorStart(), em.getAnchorEnd(), document);
		MyTreeNode leaf = pr.tree.leaves.get(position[3]);
		ArrayList<MyTreeNode> ancestors = leaf.getAncestors();
		String clause = "";
		for (MyTreeNode tmp : ancestors) {
			if (tmp.value.equals("IP")) {
				clause = tmp.toString();
				break;
			}
		}
		String modWord = "false";
		for (String negative : polarities) {
			if (clause.indexOf(negative) != -1) {
				modWord = "true";
				break;
			}
		}
		features.add(modWord);
		return features;
	}

	public ArrayList<String> genericity(EventMention em, ParseResult pr, ACEChiDoc document) {
		boolean place = false;
		boolean time = false;
		int k = 0;
		for (EventMentionArgument arg : em.eventMentionArguments) {
			if (arg.role.toLowerCase().startsWith("place")) {
				place = true;
			} else if (arg.role.toLowerCase().startsWith("time-within")) {
				place = true;
			} else {
				k++;
			}
		}
		ArrayList<String> features = new ArrayList<String>();
		
		features.add(Boolean.toString(place));
		features.add(Boolean.toString(time));
		features.add(Integer.toString(k));
		return features;
	}

	public ArrayList<String> tense(EventMention em, ParseResult pr, ACEChiDoc document) {
		ArrayList<String> features = new ArrayList<String>();
		String time = "";
		for (EventMentionArgument arg : em.eventMentionArguments) {
			if (arg.role.toLowerCase().startsWith("time-within")) {
				time = arg.getExtent();
			}
		}
		features.add(time);
		return features;
	}

	static String classifier;

	public static void main(String args[]) {
		if (args.length != 2) {
			System.out.println("java ~ [train|test|development] [polarity|modality|genericity|tense]");
		}
		classifier = args[1];
		ArrayList<String> eventAttributeFeature = new ArrayList<String>();
		ArrayList<String> files = Common.getLines("ACE_Chinese_" + args[0]);
		EventAttriFeature eventAttriFeature = new EventAttriFeature();
		ChineseTriggerIndent triggerIndent = new ChineseTriggerIndent();
		ArrayList<String> emLines = new ArrayList<String>();
		for (String file : files) {
			ACEChiDoc document = new ACEChiDoc(file);
			if (args[0].equalsIgnoreCase("train")) {
				ArrayList<EventMention> ems = document.goldEventMentions;
				for (EventMention em : ems) {
					String label = "";
					if (args[1].equalsIgnoreCase("polarity")) {
						label = em.polarity;
					} else if (args[1].equalsIgnoreCase("modality")) {
						label = em.modality;
					} else if (args[1].equalsIgnoreCase("genericity")) {
						label = em.genericity;
					} else if (args[1].equalsIgnoreCase("tense")) {
						label = em.tense;
					}
					eventAttributeFeature.add(eventAttriFeature.buildFeature(em, document, label));
					StringBuilder sb = new StringBuilder();
					sb.append(file).append(" ").append(em.getAnchorStart()).append(" ").append(em.getAnchorEnd());
					emLines.add(sb.toString());
				}
			} else {
				ArrayList<EventMention> ems = document.goldEventMentions;
				for (EventMention em : ems) {
					StringBuilder sb = new StringBuilder();
					sb.append(file).append(" ").append(em.getAnchorStart()).append(" ").append(em.getAnchorEnd())
							.append(" ").append(em.getType());
					String label = "";
					if (args[1].equalsIgnoreCase("polarity")) {
						label = em.polarity;
					} else if (args[1].equalsIgnoreCase("modality")) {
						label = em.modality;
					} else if (args[1].equalsIgnoreCase("genericity")) {
						label = em.genericity;
					} else if (args[1].equalsIgnoreCase("tense")) {
						label = em.tense;
					}
					eventAttributeFeature.add(eventAttriFeature.buildFeature(em, document, label));
				}
			}
		}
		Common.outputLines(eventAttributeFeature, "data/chinese_" + args[1] + "_" + args[0]);
		Common.outputLines(emLines, "data/chinese_" + args[1] + "_" + args[0] + "_em");
	}
}
