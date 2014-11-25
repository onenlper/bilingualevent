package coref;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

import model.ACEChiDoc;
import model.ACEDoc;
import model.EventChain;
import model.EventMention;
import util.Common;
import util.Util;

public class SimpleSuperviseBaseline {

	public static void main(String args[]) {
		if(args.length!=1) {
			System.out.println("java ~ part");
			System.exit(1);
		}
		Util.part = args[0];
		HashSet<String> corefPairs = extractCorefPairs();
		
		ArrayList<String> files = Common.getLines("ACE_Chinese_test" + args[0]);
		ArrayList<ArrayList<EventChain>> systemEventChains = new ArrayList<ArrayList<EventChain>>();
		ArrayList<Integer> lengths = new ArrayList<Integer>();
		ArrayList<String> fileNames = new ArrayList<String>();
		ArrayList<ArrayList<EventChain>> goldEventChains = new ArrayList<ArrayList<EventChain>>();
		
		Util.part = args[0];
		
		for(int i=0;i<files.size();i++) {
			String file = files.get(i);
			
			ACEDoc doc = new ACEChiDoc(file);
			doc.docID = i;
			
			fileNames.add(doc.fileID);
			goldEventChains.add(doc.goldEventChains);
			
			lengths.add(doc.content.length());
			
//			ArrayList<EventMention> events = doc.goldEventMentions;
//			Util.assignArgumentWithEntityMentions(doc.goldEventMentions,
//					doc.goldEntityMentions, doc.goldValueMentions,
//					doc.goldTimeMentions, doc);
			
			ArrayList<EventMention> events = Util.loadSystemComponents(doc);
			
			Collections.sort(events);
			
			ArrayList<EventMention> candidates = new ArrayList<EventMention>();
			for (EventMention m : events) {
				candidates.add(m);
			}
			Collections.sort(candidates);
			ArrayList<EventMention> anaphors = new ArrayList<EventMention>();
			for (EventMention m : events) {
				anaphors.add(m);
			}
			Collections.sort(anaphors);
			
			findAntecedent(doc, anaphors, candidates,corefPairs);

			ArrayList<EventChain> systemChain = new ArrayList<EventChain>();
			HashMap<String, EventChain> eventChainMap = new HashMap<String, EventChain>();
			
			for (EventMention m : candidates) {
				EventChain ec = null;
				if (m.antecedent == null) {
					ec = new EventChain();
					systemChain.add(ec);
				} else {
					ec = eventChainMap.get(m.antecedent.toName());
				}
				ec.addEventMention(m);
				eventChainMap.put(m.toName(), ec);
			}
			systemEventChains.add(systemChain);
		}
		try {
			ToSemEval.outputSemFormat(fileNames, lengths, "gold.keys." + Util.part , goldEventChains);
			ToSemEval.outputSemFormat(fileNames, lengths, "simpleSuper.keys." + Util.part, systemEventChains);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	private static void findAntecedent(ACEDoc doc,
			ArrayList<EventMention> anaphors,
			ArrayList<EventMention> allCandidates,
			HashSet<String> corefPairs
			) {
		for (EventMention anaphor : anaphors) {
			ArrayList<EventMention> cands = new ArrayList<EventMention>();
			for (int h = allCandidates.size() - 1; h >= 0; h--) {
				EventMention cand = allCandidates.get(h);
				if (cand.getAnchorEnd() < anaphor.getAnchorStart()) {
					cands.add(cand);
				}
			}
			
			for(int i=0;i<cands.size();i++) {
				EventMention cand = cands.get(i);
				String tr1 = anaphor.getAnchor();
				String tr2 = cand.getAnchor();
				String key = "";
				if(tr1.compareTo(tr2)<0) {
					key = tr1 + "#" + tr2;
				} else {
					key = tr2 + "#" + tr1;
				}
				
				if(corefPairs.contains(key)
						) {
					anaphor.antecedent = cand;
					break;
				}
			}
		}
	}

	private static HashSet<String> extractCorefPairs() {
		ArrayList<String> trainFiles = Common.getLines("ACE_Chinese_train" + Util.part);
		HashSet<String> corefPairs = new HashSet<String>();
		for(String file : trainFiles) {
			ACEDoc doc = new ACEChiDoc(file);
			ArrayList<EventChain> goldChains = doc.goldEventChains;
			for(EventChain chain : goldChains) {
				
				for(int i=0;i<chain.getEventMentions().size();i++) {
					EventMention event1 = chain.getEventMentions().get(i);
					String tr1 = event1.getAnchor();
					for(int j=i+1;j<chain.getEventMentions().size();j++) {
						EventMention event2 = chain.getEventMentions().get(j);
						String tr2 = event2.getAnchor();
						String key = "";
						if(tr1.compareTo(tr2)<0) {
							key = tr1 + "#" + tr2;
						} else {
							key = tr2 + "#" + tr1;
						}
						corefPairs.add(key);
					}
					
				}
			}
		}
		return corefPairs;
	}
}
