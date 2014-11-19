package event.preProcess;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

import model.ACEChiDoc;
import model.EntityMention;
import model.EventMention;
import model.EventMentionArgument;
import model.ParseResult;
import model.SemanticRole;
import util.ChineseUtil;
import util.Common;
import util.Util;
import event.trigger.TriggerIndent;

public class ChineseTriggerIndent implements TriggerIndent {

	static HashMap<String, HashSet<String>> knownTrigger; 
	static HashMap<String, HashSet<String>> BVs;
	static HashMap<String, HashSet<String>> BVStruct;

	static HashMap<String, ArrayList<String>> errata1;

	static HashMap<String, ArrayList<ArrayList<String>>> errata2;
	public static HashMap<String, String> BVPatterns;
	public void loadErrata() {
		knownTrigger = Common.readFile2Map6("chinese_trigger_known"+Util.part);
		BVs = Common.readFile2Map6("chinese_BVs"+Util.part);
		BVStruct = Common.readFile2Map6("BVPositions"+Util.part);
		errata1 = new HashMap<String, ArrayList<String>>();
		errata2 = new HashMap<String, ArrayList<ArrayList<String>>>();
		ArrayList<String> lines = Common.getLines("errata1"+Util.part);
		BVPatterns = Common.readFile2Map2("BVPatterns"+Util.part);
		for (String line : lines) {
			String tokens[] = line.split("_");
			String word = tokens[1];
			String trigger = tokens[0];
			if (errata1.containsKey(word)) {
				errata1.get(word).add(trigger);
			} else {
				ArrayList<String> triggers = new ArrayList<String>();
				triggers.add(trigger);
				errata1.put(word, triggers);
			}
		}
		lines = Common.getLines("errata2"+Util.part);
		for (String line : lines) {
			String tokens[] = line.split("_");
			ArrayList<String> ts = new ArrayList<String>();
			for (int i = 2; i < tokens.length; i++) {
				ts.add(tokens[i]);
			}
			ts.add(tokens[0]);
			if (errata2.containsKey(tokens[1])) {
				errata2.get(tokens[1]).add(ts);
			} else {
				ArrayList<ArrayList<String>> tses = new ArrayList<ArrayList<String>>();
				tses.add(ts);
				errata2.put(tokens[1], tses);
			}
		}
	}

	// Ji's errata table method
	public ArrayList<EventMention> extractTriggerJi(String file) {
		if (errata1 == null || errata2 == null) {
			loadErrata();
		}
		HashSet<EventMention> triggerHash = new HashSet<EventMention>();
		ArrayList<EventMention> triggers = new ArrayList<EventMention>();
		ACEChiDoc document = new ACEChiDoc(file);
		for (ParseResult pr : document.parseReults) {
			for (int i = 0; i < pr.words.size(); i++) {
				String word = pr.words.get(i);
				int position[] = pr.positions.get(i);
				if (knownTrigger.containsKey(word)) {
					EventMention em = new EventMention();
					em.setAnchorStart(position[0]);
					em.setAnchorEnd(position[1]);
					em.setAnchor(word);
					triggerHash.add(em);
				}
				if (errata1.containsKey(word)) {
					ArrayList<String> trigs = errata1.get(word);
					for (String trig : trigs) {
						EventMention em = new EventMention();
						em.setAnchorStart(document.content.indexOf(trig.charAt(0), position[0]));
						em.setAnchorEnd(document.content.lastIndexOf(trig.charAt(trig.length() - 1), position[1]));
						em.setAnchor(trig);
						triggerHash.add(em);
					}
				}
//				// start with word
				if (errata2.containsKey(word)) {
					for (ArrayList<String> ts : errata2.get(word)) {
						String trigger = ts.get(ts.size() - 1);
						boolean conflict = false;
						for (int k = 0; k < ts.size() - 1; k++) {
							if (i + 1 + k < pr.words.size() && ts.get(k).equals(pr.words.get(i + 1 + k))) {
							} else {
								conflict = true;
								break;
							}
						}
						if (!conflict) {
							EventMention em = new EventMention();
							em.setAnchorStart(document.content.indexOf(trigger.charAt(0), position[0]));
							em.setAnchorEnd(document.content.lastIndexOf(trigger.charAt(trigger.length() - 1),
									pr.positions.get(i + ts.size() - 1)[1]));
							em.setAnchor(trigger);
							triggerHash.add(em);
						}
					}
				}
			}
		}
		triggers.addAll(triggerHash);
		return triggers;
	}

	static HashSet<String> train_words;
	
	public ArrayList<EventMention> extractChen(String file, Collection<EventMention> existTriggers, HashSet<EventMention> knownTriggers) {
		ArrayList<EventMention> eventMentions = new ArrayList<EventMention>();
		ACEChiDoc document = new ACEChiDoc(file);
		ArrayList<SemanticRole> semanticRoles = new ArrayList<SemanticRole>(document.semanticRoles.values());
		HashSet<Integer> corefIDs = new HashSet<Integer>();
		int k = 0;
		for(EventMention trigger: existTriggers) {
			if(trigger.confidence<0) {
				continue;
			}
			for(EventMentionArgument argument : trigger.getEventMentionArguments()) {
				EntityMention mention = argument.getEntityMention();
				if(mention.getType().equalsIgnoreCase("time") || mention.getType().equalsIgnoreCase("value")) {
					continue;
				}
				corefIDs.add(mention.entity.entityIdx);
			}
		}
		for(SemanticRole role : semanticRoles) {
			EventMention candidate = role.predict;
			String word = candidate.getAnchor();
			if(train_words.contains(word) && !knownTrigger.containsKey(word)) {
				continue;
			}
			ArrayList<EntityMention> arg0s = role.arg0;
			ArrayList<EntityMention> arg1s = role.arg1;
			boolean qualify = false;
			for(EntityMention arg : arg0s) {
				if(corefIDs.contains(arg.entity.entityIdx)) {
					qualify = true;
				}
			}
			for(EntityMention arg :arg1s) {
				if(corefIDs.contains(arg.entity.entityIdx)) {
					qualify = true;
				}
			}
			if(!knownTriggers.contains(candidate)) {
				eventMentions.add(candidate);
				if(document.goldEventMentions.contains(candidate)) {
					k++;
				}
			}
		}
		System.out.println(k);
		return eventMentions;
	}
	
	static HashMap<String, HashMap<String, EventMention>> lastResults;
	
	static HashSet<String> allAnchors = Common.readFile2Set("allAnchors");
	
	// zhou's EMNLP2012 method
	public ArrayList<EventMention> extractTrigger(String file) {
//		if(lastResults==null) {
//			lastResults = Util.readResult("joint_svm/result" + Util.part);
//		}
		if (errata1 == null || errata2 == null) {
			loadErrata();
		}
		if (train_words == null) {
			train_words = Common.readFile2Set("train_words"+Util.part);
		}
		HashSet<EventMention> triggerHash = new HashSet<EventMention>();
		ArrayList<EventMention> triggers = new ArrayList<EventMention>();
		ACEChiDoc document = new ACEChiDoc(file);
		for (ParseResult pr : document.parseReults) {
			for (int i = 0; i < pr.words.size(); i++) {
				String word = pr.words.get(i);
				String posTag = pr.posTags.get(i);
				int position[] = pr.positions.get(i);
				int start = position[0];
				int end = position[1];
				if (knownTrigger.containsKey(word) && knownTrigger.get(word).contains(posTag)) {
					EventMention em = new EventMention();
					em.setAnchorStart(position[0]);
					em.setAnchorEnd(position[1]);
					em.setAnchor(word);
					triggerHash.add(em);
				}
				if (errata1.containsKey(word)) {
					ArrayList<String> trigs = errata1.get(word);
					for (String trig : trigs) {
						EventMention em = new EventMention();
						em.setAnchorStart(document.content.indexOf(trig.charAt(0), position[0]));
						em.setAnchorEnd(document.content.lastIndexOf(trig.charAt(trig.length() - 1), position[1]));
						em.setAnchor(trig);
						triggerHash.add(em);
					}
				}
				// start with word
				if (errata2.containsKey(word)) {
					for (ArrayList<String> ts : errata2.get(word)) {
						String trigger = ts.get(ts.size() - 1);
						boolean conflict = false;
						for (int k = 0; k < ts.size() - 1; k++) {
							if (i + 1 + k < pr.words.size() && ts.get(k).equals(pr.words.get(i + 1 + k))) {
							} else {
								conflict = true;
								break;
							}
						}
						if (!conflict) {
							EventMention em = new EventMention();
							em.setAnchorStart(document.content.indexOf(trigger.charAt(0), position[0]));
							em.setAnchorEnd(document.content.lastIndexOf(trigger.charAt(trigger.length() - 1),
									pr.positions.get(i + ts.size() - 1)[1]));
							em.setAnchor(trigger);
							triggerHash.add(em);
							break;
						}
					}
				}
				if(train_words.contains(word) && !knownTrigger.containsKey(word)) {
					continue;
				}
				EventMention em = new EventMention();
				em.setAnchorStart(position[0]);
				em.setAnchorEnd(position[1]);
				em.setAnchor(word);
				if (start == end) {
					if (BVs.containsKey(word) && BVs.get(word).contains(posTag) && BVStruct.get(word).contains("BV")) {
						triggerHash.add(em);
					}
				} else if (start + 1 == end) {
					String str1 = word.substring(0, 1);
					String str2 = word.substring(1, 2);
					String pattern = "null";
					if (BVs.containsKey(str1) && BVs.get(str1).contains(posTag)) {
						if (str2.equals("了")) {
							pattern = "BV_comp";
							em.pattern = str1+"_BV_comp";
						} else if (pos2.containsKey(str2) && pos2.get(str2).startsWith("V")) {
							pattern = "BV_verb";
							em.pattern = str1+"_BV_verb";
						} else {
							pattern = "BV_np/adj";
							em.pattern = str1+"_BV_np/adj";
						}
						if(BVStruct.get(str1).contains(pattern)) {
							em.inferFrom = BVPatterns.get(em.pattern);
//							if(!em.getAnchor().equals(em.inferFrom) && !allAnchors.contains(em.getAnchor()))
//							System.out.println(em.getAnchor() + "#" + em.inferFrom);
							triggerHash.add(em);
						} else {
//							System.out.println(em.getAnchor());
						}
					}
					pattern = "null";
					if (BVs.containsKey(str2) && BVs.get(str2).contains(posTag)) {
						if (pos2.containsKey(str1) && pos2.get(str1).startsWith("V")) {
							pattern = "verb_BV";
							em.pattern = str2+"_verb_BV";
						} else {
							pattern = "np/adj_BV";
							em.pattern = str2+"_np/adj_BV";
						}
						if(BVStruct.get(str2).contains(pattern)) {
							em.inferFrom = BVPatterns.get(em.pattern);
//							if(!em.getAnchor().equals(em.inferFrom) && !allAnchors.contains(em.getAnchor()))
//							System.out.println(em.getAnchor() + "#" + em.inferFrom);
							triggerHash.add(em);
						} else {
//							System.out.println(em.getAnchor());
						}
					}
				}
			}
		}
//		triggerHash.clear();
//		triggerHash.addAll(document.goldEventMentions);
//		ArrayList<EventMention> lastMentions = new ArrayList<EventMention>();
//		if(lastResults.containsKey(file)) {		
//			lastMentions.addAll(lastResults.get(file).values()); 
//		} 
//		ArrayList<EventMention> chens = this.extractChen(file, lastMentions, triggerHash);
//		int a = triggerHash.size();
//		triggerHash.addAll(chens);
//		System.out.println(triggerHash.size()-a);
		triggers.addAll(triggerHash);
		filterEMs(document, triggers);
		return triggers;
	}

	public static void filterEMs(ACEChiDoc document, ArrayList<EventMention> eventMentions) {
		int leftBound = document.start;
		int rightBound = document.end;
		for(int i=0;i<eventMentions.size();i++) {
			EventMention mention = eventMentions.get(i);
			int start = mention.getAnchorStart();
			int end = mention.getAnchorEnd();
			if(start<leftBound || end> rightBound) {
				eventMentions.remove(i);
				i--;
			}
		}
	}
	
	static HashMap<String, String> pos2 = Common.readFile2Map2("dict/10POSDIC");

	public static void main(String args[]) {
		if(args.length!=1) {
			System.out.println("java ~ [folder]");
			System.exit(1);
		}
		Util.part = args[0];
		System.out.println("Build BVs....\n===============");
		ArrayList<String> trainList = Common.getLines("ACE_Chinese_train"+Util.part);
		// HashMap<String, String> pos1 = Common.readFile2Map2("dict/09POSDIC");
		HashMap<String, HashSet<String>> BVs = new HashMap<String, HashSet<String>>();
		HashSet<String> trainWords = new HashSet<String>();
		HashMap<String, HashSet<String>> BVStruct = new HashMap<String, HashSet<String>>();
		HashMap<String, HashSet<String>> knownTrigger = new HashMap<String, HashSet<String>>();
		
		HashMap<String, String> BVPatterns = new HashMap<String, String>();
		
		for (String file : trainList) {
			ACEChiDoc document = new ACEChiDoc(file);
			for (ParseResult pr : document.parseReults) {
				for (int i = 1; i < pr.words.size(); i++) {
					trainWords.add(pr.words.get(i));
				}
			}
			ArrayList<EventMention> ems = document.goldEventMentions;
			for (EventMention em : ems) {
				int start = em.getAnchorStart();
				int end = em.getAnchorEnd();
				int position[] = ChineseUtil.findParseFilePosition(start, end, document);
				ParseResult pr = document.parseReults.get(position[0]);
				String posTag = pr.posTags.get(position[1]);
				addEntry(knownTrigger, em.getAnchor(), posTag);
				if (start == end && posTag.equalsIgnoreCase("VV")) {
					addEntry(BVs, em.getAnchor(), posTag);
					addEntry(BVStruct, em.getAnchor(), "BV");
					BVPatterns.put(em.getAnchor()+"_BV", em.getAnchor());
					continue;
				} else if (start + 1 == end) {
					String trigger = em.getAnchor();
					String str1 = Character.toString(trigger.charAt(0));
					String str2 = Character.toString(trigger.charAt(1));
					if (pos2.containsKey(str1) && pos2.get(str1).startsWith("V")) {
						addEntry(BVs, str1, posTag);
						if (str2.equals("了")) {
							addEntry(BVStruct, str1, "BV_comp");
							BVPatterns.put(str1+"_BV_comp", em.getAnchor());
						} else if (pos2.containsKey(str2) && pos2.get(str2).startsWith("V")) {
							addEntry(BVStruct, str1, "BV_verb");
							BVPatterns.put(str1+"_BV_verb", em.getAnchor());
						} else {
							addEntry(BVStruct, str1, "BV_np/adj");
							BVPatterns.put(str1+"_BV_np/adj", em.getAnchor());
						}
					}
					if (pos2.containsKey(str2) && pos2.get(str2).startsWith("V")) {
						addEntry(BVs, str2, posTag);
						if (pos2.containsKey(str1) && pos2.get(str1).startsWith("V")) {
							addEntry(BVStruct, str2, "verb_BV");
							BVPatterns.put(str2+"_verb_BV", em.getAnchor());
						} else {
							addEntry(BVStruct, str2, "np/adj_BV");
							BVPatterns.put(str2+"_np/adj_BV", em.getAnchor());
						}
					}
				}
			}
		}
		Common.outputHashMap6(knownTrigger, "chinese_trigger_known"+Util.part);
		Common.outputHashMap6(BVs, "chinese_BVs"+Util.part);
		Common.outputHashSet(trainWords, "train_words"+Util.part);
		Common.outputHashMap6(BVStruct, "BVPositions"+Util.part);
		Common.outputHashMap(BVPatterns, "BVPatterns"+Util.part);
	}

	public static void addEntry(HashMap<String, HashSet<String>> maps, String key, String value) {
		if (maps.containsKey(key)) {
			maps.get(key).add(value);
		} else {
			HashSet<String> values = new HashSet<String>();
			values.add(value);
			maps.put(key, values);
		}
	}

	// // baseline, extract those triggers occurring in the training data
	// there may be some bug
	// public ArrayList<EventMention> extractTrigger(String file) {
	// HashSet<EventMention> triggerHash = new HashSet<EventMention>();
	// ArrayList<EventMention> triggers = new ArrayList<EventMention>();
	// ACEDocument document = new ACEDocument(file);
	// String content = document.content;
	// for (ParseResult pr : document.parseReults) {
	// String sentence = pr.plainSentence.replace("\n", "").replaceAll("\\s+",
	// "");
	// // System.out.println(sentence);
	// ArrayList<int[]> positions = pr.positions;
	// for(String trigger : knownTrigger) {
	// int k = sentence.indexOf(trigger, 0);
	// while(k!=-1) {
	// int start = positions.get(1)[0];
	// for(int m=0;m<k;) {
	// char ch = content.charAt(start);
	// // System.out.println(ch);
	// if(ch==' ' || ch=='\n') {
	// } else {
	// m++;
	// }
	// start++;
	// }
	// // System.out.println(content.charAt(start));
	// // System.out.println("=======");
	// while(content.charAt(start)==' ' || content.charAt(start)=='\n') {
	// // start++;
	// // System.out.println("++");
	// }
	// int end = start;
	// for(int m=1;m<trigger.length();m++) {
	// end = content.indexOf(trigger.charAt(m), end+1);
	// }
	// EventMention em = new EventMention();
	// em.setAnchorStart(start);
	// em.setAnchorEnd(end);
	// em.setAnchor(trigger);
	//					
	// String str = content.substring(start, end+1).replace("\n",
	// "").replaceAll("\\s+", "");
	// if(!str.equals(trigger)) {
	// // System.out.println(trigger);
	// // System.out.println("#" + str + "#");
	// // System.out.println("#" + content.charAt(start)+"#");
	// // System.out.println(start + "$" + end);
	// // System.exit(1);
	// }
	//					
	// triggerHash.add(em);
	// k = sentence.indexOf(trigger, k+1);
	// }
	// }
	// }
	// triggers.addAll(triggerHash);
	// return triggers;
	// }

}
