package coref;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import model.ACEDoc;
import model.ACEEngDoc;
import model.EventChain;
import model.EventMention;
import util.Common;

public class Baseline {

	public static void main(String args[]) {

		ArrayList<String> files = Common.getLines("ACE_English_test0");
		ArrayList<ArrayList<EventChain>> systemEventChains = new ArrayList<ArrayList<EventChain>>();
		ArrayList<Integer> lengths = new ArrayList<Integer>();
		ArrayList<String> fileNames = new ArrayList<String>();
		for (String file : files) {
			ACEDoc doc = new ACEEngDoc(file);
			fileNames.add(doc.fileID);
			lengths.add(doc.content.length());
			
			ArrayList<EventMention> goldEvents = doc.goldEventMentions;
			ArrayList<EventMention> candidates = new ArrayList<EventMention>();
			for (EventMention m : goldEvents) {
				candidates.add(m);
			}
			Collections.sort(candidates);
			ArrayList<EventMention> anaphors = new ArrayList<EventMention>();
			for (EventMention m : goldEvents) {
				anaphors.add(m);
			}
			Collections.sort(anaphors);
			
			findAntecedent(doc, anaphors, candidates);

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
			ToSemEval.outputSemFormat(fileNames, lengths, "base.keys.all", systemEventChains);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static void findAntecedent(ACEDoc doc,
			ArrayList<EventMention> anaphors,
			ArrayList<EventMention> allCandidates) {
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
				if(
//						cand.getAnchor().equalsIgnoreCase(anaphor.getAnchor())
//						doc.getWord(cand.getAnchorStart()).equalsIgnoreCase(doc.getWord(anaphor.getAnchorStart()))
//						&&
						cand.getSubType().equals(anaphor.getSubType()) && cand.tense.equals(anaphor.tense) 
						&& cand.modality.equals(anaphor.modality) && cand.genericity.equals(anaphor.genericity) 
						&& cand.polarity.equals(anaphor.polarity)
						) {
					anaphor.antecedent = cand;
					break;
				}
			}
		}
	}
}
