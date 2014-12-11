package event.attribute;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import model.ACEDoc;
import model.ACEEngDoc;
import model.EventMention;
import model.EventMentionArgument;
import model.ParseResult;
import model.syntaxTree.MyTreeNode;
import util.Common;
import util.Util;
import event.triggerEng.EngArgEval;

public class EventAttriFeaEng {

	public String convert(ArrayList<String> features, String label) {
		StringBuilder sb = new StringBuilder();
		sb.append(label);
		for (String feature : features) {
			sb.append(" ").append(feature);
		}
		return sb.toString().trim();
	}

	public String buildFeature(EventMention em, ACEDoc document, String label) {
		ArrayList<String> features = new ArrayList<String>();
		String trigger = em.getAnchor();
		
		int position[] = document.positionMap.get(em.getAnchorStart());
		
		ParseResult pr = document.parseReults.get(position[0]);
		ArrayList<String> words = pr.words;
		ArrayList<String> posTags = pr.posTags;

		String pos = posTags.get(position[1]);

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
		if (position[1] <= words.size() - 3) {
			wordR2 = words.get(position[1] + 2);
			posR2 = posTags.get(position[1] + 2);
		}
		if (position[1] <= words.size() - 2) {
			wordR1 = words.get(position[1] + 1);
			posR1 = posTags.get(position[1] + 1);
		}

		features.add("tr#" + trigger);
		features.add("trPos#" + pos);
		features.add("trType#" + em.type);
		features.add("trSubType#" + em.subType);
		features.add("wL2#" + wordL2);
		features.add("wL1#" + wordL1);
		features.add("wR1#" + wordR1);
		features.add("wR2#" + wordR2);

		features.add("posL2#" + posL2);
		features.add("posL1#" + posL1);
		features.add("posR1#" + posR1);
		features.add("posR2#" + posR2);

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

	static ArrayList<String> polarities = Common.getLines("polarityEng");
	static ArrayList<String> modalities = Common.getLines("modalityEng");
	static ArrayList<String> genericities = Common.getLines("genericityEng");
	static ArrayList<String> tenses = Common.getLines("tenseEng");

	public ArrayList<String> polarity(EventMention em, ParseResult pr, ACEDoc document) {
		ArrayList<String> features = new ArrayList<String>();
		int position[] = document.positionMap.get(em.getAnchorStart());
		MyTreeNode leaf = pr.tree.leaves.get(position[1]);
		ArrayList<MyTreeNode> ancestors = leaf.getAncestors();
		String clause = "";
		for (MyTreeNode tmp : ancestors) {
			if (tmp.value.equals("S")) {
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
		features.add("neg#" + negativeWord);
		return features;
	}

	public ArrayList<String> modality(EventMention em, ParseResult pr, ACEDoc document) {
		ArrayList<String> features = new ArrayList<String>();
		int position[] = document.positionMap.get(em.getAnchorStart());
		MyTreeNode leaf = pr.tree.leaves.get(position[1]);
		ArrayList<MyTreeNode> ancestors = leaf.getAncestors();
		MyTreeNode clause = null;
		for (MyTreeNode tmp : ancestors) {
			if (tmp.value.equals("S") || tmp.value.equals("ROOT")) {
				clause = tmp;
				break;
			}
		}
		for(MyTreeNode tmp : clause.getLeaves()) {
			if(modalities.contains(tmp.value)) {
				features.add("mod#" + tmp.value);
			}
		}
		return features;
	}

	public ArrayList<String> genericity(EventMention em, ParseResult pr, ACEDoc document) {
		boolean placeOrTime = false;
		int k = 0;
		for (EventMentionArgument arg : em.eventMentionArguments) {
			if (arg.role.toLowerCase().startsWith("place")) {
				placeOrTime = true;
			} else if (arg.role.toLowerCase().startsWith("time-within")) {
				placeOrTime = true;
			} else {
				k++;
			}
		}
		ArrayList<String> features = new ArrayList<String>();
		
		features.add("place#" +Boolean.toString(placeOrTime));
		features.add("other#" + Integer.toString(k));
		return features;
	}

	public ArrayList<String> tense(EventMention em, ParseResult pr, ACEDoc document) {
		ArrayList<String> features = new ArrayList<String>();
		String time = "";
		for (EventMentionArgument arg : em.eventMentionArguments) {
			if (arg.role.toLowerCase().startsWith("time-within")) {
				time = arg.getExtent();
			}
		}
		features.add("time#" + time);
		
		int position[] = document.positionMap.get(em.getAnchorStart());
		MyTreeNode leaf = pr.tree.leaves.get(position[1]);
		ArrayList<MyTreeNode> ancestors = leaf.getAncestors();
		for (MyTreeNode tmp : ancestors) {
			if (tmp.value.equals("S")) {
				
				for(MyTreeNode tmpLeaf : tmp.getLeaves()) {
					if(tmpLeaf.parent.value.startsWith("V")) {
						features.add("firstVB#" + tmpLeaf.value);
//						features.add(tmpLeaf.parent.value);
						break;
					}
				}
				
				break;
			}
		}
		
		return features;
	}

	static String classifier;

	public static void main(String args[]) {
		if (args.length != 3) {
			System.out.println("java ~ [train|test|development] [polarity|modality|genericity|tense] folder");
		}
		classifier = args[1];
		Util.part = args[2];
		ArrayList<String> eventAttributeFeature = new ArrayList<String>();
		ArrayList<String> files = Common.getLines("ACE_English_" + args[0] + args[2]);
		
		EventAttriFeaEng eventAttriFeature = new EventAttriFeaEng();

		HashMap<String, HashMap<String, EventMention>> jointSVMLines = EngArgEval.jointSVMLine();
		
		ArrayList<String> emLines = new ArrayList<String>();
		for (String file : files) {
			ACEDoc document = new ACEEngDoc(file);
			
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
				HashMap<String, EventMention> evmMaps = jointSVMLines.get(file);
				
				if(evmMaps==null) {
					evmMaps = new HashMap<String, EventMention>();
				}
				
				ArrayList<EventMention> ems = document.goldEventMentions;
//				ArrayList<EventMention> ems = new ArrayList<EventMention>(evmMaps.values());
				
				Collections.sort(ems);
				for (EventMention em : ems) {
					StringBuilder sb = new StringBuilder();
					sb.append(file).append(" ").append(em.getAnchorStart()).append(" ").append(em.getAnchorEnd())
							.append(" ").append(em.getType());
					emLines.add(sb.toString());
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
		Common.outputLines(eventAttributeFeature, "data/English_" + args[1] + "_" + args[0] + args[2]);
		Common.outputLines(emLines, "data/English_" + args[1] + "_" + args[0] + "_em" + args[2]);
	}
}
