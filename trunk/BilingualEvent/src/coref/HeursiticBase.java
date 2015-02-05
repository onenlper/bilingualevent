package coref;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import model.ACEChiDoc;
import model.ACEDoc;
import model.EventChain;
import model.EventMention;
import util.Common;
import util.Util;

public class HeursiticBase {

	public static void main(String args[]) {
		if(args.length!=1) {
			System.out.println("java ~ part");
			System.exit(1);
		}
		long t1 = System.currentTimeMillis();
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
			ToSemEval.outputSemFormat(fileNames, lengths, "gold.keys." + Util.part , goldEventChains);
			ToSemEval.outputSemFormat(fileNames, lengths, "base.keys." + Util.part, systemEventChains);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		long t2 = System.currentTimeMillis();
//		System.out.println(t2 - t1);
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
//						!Util.highPrecissionNegativeConstraint(cand, anaphor) && 
//						(cand.antecedent==null || !Util.highPrecissionNegativeConstraint(cand.antecedent, anaphor))
//						&&
//						(cand.antecedent==null || cand.antecedent.antecedent==null || !Util.highPrecissionNegativeConstraint(cand.antecedent.antecedent, anaphor))
//						&&
						(cand.getAnchor().equalsIgnoreCase(anaphor.getAnchor())) 
								|| 
//						Util._commonBV_(cand, anaphor)
//						(false
//								)
//						cand.getAnchor().equals(anaphor.getAnchor())
//						cand.getSubType().equals(anaphor.getSubType())
//						) {
						BV_match(cand, anaphor)
//				if(
//						cand.getAnchor().equalsIgnoreCase(anaphor.getAnchor())
////						doc.getWord(cand.getAnchorStart()).equalsIgnoreCase(doc.getWord(anaphor.getAnchorStart()))
//						&&
//						cand.getSubType().equals(anaphor.getSubType()) 
//						&& cand.tense.equals(anaphor.tense) 
//						&& cand.modality.equals(anaphor.modality) && cand.genericity.equals(anaphor.genericity) 
//						&& cand.polarity.equals(anaphor.polarity)
						) {
					anaphor.antecedent = cand;
					break;
				}
			}
		}
	}
	
	private static boolean BV_match(EventMention em, EventMention an) {
		for (String bv1 : em.bvs.keySet()) {
			for (String bv2 : an.bvs.keySet()) {
				if (bv1.equals(bv2)) {
					return true;
				}
			}
		}
		return false;
	}
}
